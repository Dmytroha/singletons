package examples.singletons.unsafe;

/**
 * УЧЕБНЫЙ АНТИПРИМЕР. НЕКОРРЕКТЕН В МНОГОПОТОЧНОЙ ПРОГРАММЕ.
 *
 * У ссылки намеренно отсутствует volatile, а value намеренно не final.
 * Поток на быстром пути может получить ссылку, но прочитать value == 0.
 * Отсутствие ошибки в конкретном запуске не доказывает корректность.
 */
public final class BrokenDoubleCheckedSingleton {

    private static BrokenDoubleCheckedSingleton instance; // ОШИБКА: нет volatile

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
                    instance = local; // Нет безопасной публикации для быстрого пути
                }
            }
        }

        return local;
    }

    public int getValue() {
        return value;
    }
}
