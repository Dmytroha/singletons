package examples.singletons;

/**
 * Ленивый singleton с двойной проверкой и безопасной volatile-публикацией.
 */
public final class DoubleCheckedSingleton {

    private static volatile DoubleCheckedSingleton instance;

    // Намеренно не final для проверки безопасной публикации.
    private int value;

    private DoubleCheckedSingleton() {
        value = 42;
        // Не публиковать this и не вызывать getInstance() из конструктора.
    }

    public static DoubleCheckedSingleton getInstance() {
        DoubleCheckedSingleton local = instance;

        if (local == null) {
            synchronized (DoubleCheckedSingleton.class) {
                // Другой поток мог создать экземпляр, пока мы ждали монитор.
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
