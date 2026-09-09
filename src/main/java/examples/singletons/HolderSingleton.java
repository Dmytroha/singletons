package examples.singletons;

/**
 * Ленивый singleton на основе инициализации вложенного класса.
 */
public final class HolderSingleton {

    // Намеренно не final для проверки безопасной публикации.
    private int value;

    private HolderSingleton() {
        value = 42;
        // Не публиковать this и не вызывать getInstance() из конструктора.
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
