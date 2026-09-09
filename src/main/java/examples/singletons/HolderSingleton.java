package examples.singletons;

/**
 * A lazy singleton based on nested class initialization.
 */
public final class HolderSingleton {

    // Intentionally not final to test safe publication.
    private int value;

    private HolderSingleton() {
        value = 42;
        // Do not publish this or call getInstance() from the constructor.
    }

    private static class Holder {
        private static final HolderSingleton INSTANCE = new HolderSingleton();
    }

    public static HolderSingleton getInstance() {
        return Holder.INSTANCE;
    }

    public int getValue() {
        return value;
    }
}
