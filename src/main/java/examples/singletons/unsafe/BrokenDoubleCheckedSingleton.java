package examples.singletons.unsafe;

/**
 * EDUCATIONAL ANTI-PATTERN. UNSAFE IN A MULTITHREADED PROGRAM.
 *
 * The reference is intentionally not volatile, and value is not final.
 * A thread taking the fast path may obtain the reference but read value == 0.
 * A run without a failure does not prove correctness.
 */
public final class BrokenDoubleCheckedSingleton {

    private static BrokenDoubleCheckedSingleton instance; // BUG: missing volatile

    private int value;

    private BrokenDoubleCheckedSingleton() {
        value = 42;
    }

    public static BrokenDoubleCheckedSingleton getInstance() {
        BrokenDoubleCheckedSingleton local = instance;

        if (local == null) {
            synchronized (BrokenDoubleCheckedSingleton.class) {
                local = instance;
                if (local == null) {
                    local = new BrokenDoubleCheckedSingleton();
                    instance = local; // No safe publication for the fast path
                }
            }
        }

        return local;
    }

    public int getValue() {
        return value;
    }
}
