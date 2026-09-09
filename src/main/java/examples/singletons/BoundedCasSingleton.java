package examples.singletons;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Допускает максимум два успешно созданных кандидата, но возвращает
 * только первый опубликованный через CAS экземпляр.
 */
public final class BoundedCasSingleton {

    private static final AtomicReference<BoundedCasSingleton> INSTANCE =
            new AtomicReference<>();

    private static final Semaphore CREATION_SLOTS = new Semaphore(2);

    // Намеренно не final: на этом поле можно проверять безопасную публикацию.
    private int value;

    private BoundedCasSingleton() {
        value = 42;
        // Не публиковать this и не вызывать getInstance() из конструктора.
    }

    public static BoundedCasSingleton getInstance() {
        BoundedCasSingleton current = INSTANCE.get();
        if (current != null) {
            return current;
        }

        CREATION_SLOTS.acquireUninterruptibly();
        try {
            // Пока поток ждал разрешение, победителя могли уже опубликовать.
            current = INSTANCE.get();
            if (current != null) {
                return current;
            }

            BoundedCasSingleton candidate = new BoundedCasSingleton();
            if (INSTANCE.compareAndSet(null, candidate)) {
                return candidate;
            }

            candidate.dispose();
            return INSTANCE.get();
        } finally {
            CREATION_SLOTS.release();
        }
    }

    public int getValue() {
        return value;
    }

    private void dispose() {
        // В примере внешних ресурсов нет, поэтому освобождать нечего.
        // При их добавлении закрыть здесь ресурсы именно этого кандидата.
        // Метод не должен выбрасывать исключения.
        // Память объекта освобождает GC, а не этот метод.
    }
}
