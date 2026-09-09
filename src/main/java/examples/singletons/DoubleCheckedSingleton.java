package examples.singletons;

/**
 * A lazy singleton using double-checked locking and safe volatile publication.
 */
public final class DoubleCheckedSingleton {

    private static volatile DoubleCheckedSingleton instance;

    // Intentionally not final to test safe publication.
    private int value;

    private DoubleCheckedSingleton() {
        value = 42;
        // Do not publish this or call getInstance() from the constructor.
    }

    public static DoubleCheckedSingleton getInstance() {
        DoubleCheckedSingleton local = instance;

        if (local == null) {
            synchronized (DoubleCheckedSingleton.class) {
                // Another thread may have created the instance while we waited.
                local = instance;
                if (local == null) {
                    local = new DoubleCheckedSingleton();
                    instance = local;
                }
            }
        }

        return local;
    }

    public int getValue() {
        return value;
    }
}
