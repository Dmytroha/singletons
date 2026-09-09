package examples.singletons;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * A multithreaded test with no third-party dependencies.
 * Each JVM run tests concurrent first access to all three implementations.
 * This is a smoke/stress test, not a proof of correctness under the memory model.
 */
public final class SingletonConcurrencyTest {

    private static final int THREADS = 64;
    private static final int CALLS_PER_THREAD = 5_000;
    private static final int TIMEOUT_SECONDS = 30;

    private SingletonConcurrencyTest() {
    }

    public static void main(String[] args) throws Exception {
        check("BoundedCasSingleton",
                BoundedCasSingleton::getInstance,
                BoundedCasSingleton::getValue);

        check("HolderSingleton",
                HolderSingleton::getInstance,
                HolderSingleton::getValue);

        check("DoubleCheckedSingleton",
                DoubleCheckedSingleton::getInstance,
                DoubleCheckedSingleton::getValue);

        System.out.println("PASS: all three correct implementations.");
    }

    private static <T> void check(
            String name,
            Supplier<T> getInstance,
            ToIntFunction<T> getValue) throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < THREADS; thread++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new AssertionError(name + ": start timed out");
                    }

                    T first = null;
                    for (int call = 0; call < CALLS_PER_THREAD; call++) {
                        T current = getInstance.get();
                        if (current == null) {
                            throw new AssertionError(name + ": received null");
                        }

                        // Read the field in the worker thread, before Future.get()
                        // or any other action that passes results to the main thread.
                        int value = getValue.applyAsInt(current);
                        if (value != 42) {
                            throw new AssertionError(
                                    name + ": unexpected value " + value);
                        }

                        if (first == null) {
                            first = current;
                        } else if (current != first) {
                            throw new AssertionError(
                                    name + ": instance changed within a thread");
                        }
                    }
                    return first;
                }));
            }

            if (!ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(name + ": threads are not ready");
            }
            start.countDown();

            T expected = futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Future<T> future : futures) {
                T actual = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (actual != expected) {
                    throw new AssertionError(
                            name + ": different threads received different instances");
                }
            }

            System.out.printf(
                    "PASS: %s; threads=%d; calls=%d%n",
                    name, THREADS, THREADS * CALLS_PER_THREAD);
        } finally {
            start.countDown();
            pool.shutdownNow();
            if (!pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(name + ": threads did not terminate");
            }
        }
    }
}
