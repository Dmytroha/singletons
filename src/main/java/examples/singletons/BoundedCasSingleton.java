package examples.singletons;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows at most two successfully constructed candidates, but returns
 * only the first instance successfully published through CAS.
 */
public final class BoundedCasSingleton {

    private static final AtomicReference<BoundedCasSingleton> INSTANCE =
            new AtomicReference<>();

    private static final Semaphore CREATION_SLOTS = new Semaphore(2);

    // Intentionally not final so this field can be used to test safe publication.
    private int value;

    private BoundedCasSingleton() {
        value = 42;
        // Do not publish this or call getInstance() from the constructor.
    }

    public static BoundedCasSingleton getInstance() {
        BoundedCasSingleton current = INSTANCE.get();
        if (current != null) {
            return current;
        }

        CREATION_SLOTS.acquireUninterruptibly();
        try {
            // Another thread may have published the winner while we waited.
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
        // This example has no external resources, so there is nothing to release.
        // If resources are added, close only this candidate's resources here.
        // This method must not throw exceptions.
        // The GC reclaims the object's memory, not this method.
    }
}
