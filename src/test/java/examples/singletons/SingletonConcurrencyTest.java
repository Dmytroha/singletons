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
 * Многопоточная проверка без сторонних зависимостей.
 * Каждый запуск JVM проверяет конкурентный первый доступ к трём вариантам.
 * Это smoke/stress-тест, а не доказательство корректности по модели памяти.
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

        System.out.println("PASS: все три корректных варианта.");
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
                        throw new AssertionError(name + ": таймаут старта");
                    }

                    T first = null;
                    for (int call = 0; call < CALLS_PER_THREAD; call++) {
                        T current = getInstance.get();
                        if (current == null) {
                            throw new AssertionError(name + ": получен null");
                        }

                        // Читаем поле прямо в рабочем потоке, до Future.get()
                        // и других действий, передающих результат главному потоку.
                        int value = getValue.applyAsInt(current);
                        if (value != 42) {
                            throw new AssertionError(
                                    name + ": некорректное значение " + value);
                        }

                        if (first == null) {
                            first = current;
                        } else if (current != first) {
                            throw new AssertionError(
                                    name + ": экземпляр сменился внутри потока");
                        }
                    }
                    return first;
                }));
            }

            if (!ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(name + ": потоки не готовы");
            }
            start.countDown();

            T expected = futures.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Future<T> future : futures) {
                T actual = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (actual != expected) {
                    throw new AssertionError(
                            name + ": разные потоки получили разные экземпляры");
                }
            }

            System.out.printf(
                    "PASS: %s; потоков=%d; вызовов=%d%n",
                    name, THREADS, THREADS * CALLS_PER_THREAD);
        } finally {
            start.countDown();
            pool.shutdownNow();
            if (!pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(name + ": потоки не завершились");
            }
        }
    }
}
