package com.planetstudios.essentials.textures;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the lifetime of asynchronous GPU registrations.
 *
 * <p>Reload invalidates work already queued for the client thread while keeping
 * the registrar usable. Shutdown permanently closes the lifecycle.</p>
 */
final class TextureRegistrationLifecycle {
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    long beginRegistration() {
        if (closed.get()) {
            throw new IllegalStateException("Texture GPU registrar is closed");
        }
        return generation.get();
    }

    boolean isCurrent(long requestedGeneration) {
        return !closed.get() && generation.get() == requestedGeneration;
    }

    long invalidatePending() {
        return generation.incrementAndGet();
    }

    long close() {
        closed.set(true);
        return generation.incrementAndGet();
    }

    boolean isClosed() {
        return closed.get();
    }
}
