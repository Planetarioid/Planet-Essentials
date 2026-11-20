package com.planetstudios.essentials.textures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureRegistrationLifecycleTest {
    @Test
    void reloadInvalidatesPendingWorkButAllowsNewRegistrations() {
        TextureRegistrationLifecycle lifecycle = new TextureRegistrationLifecycle();
        long pending = lifecycle.beginRegistration();

        lifecycle.invalidatePending();

        assertFalse(lifecycle.isCurrent(pending));
        long afterReload = lifecycle.beginRegistration();
        assertTrue(lifecycle.isCurrent(afterReload));
    }

    @Test
    void shutdownInvalidatesPendingWorkAndPermanentlyClosesLifecycle() {
        TextureRegistrationLifecycle lifecycle = new TextureRegistrationLifecycle();
        long pending = lifecycle.beginRegistration();

        lifecycle.close();

        assertFalse(lifecycle.isCurrent(pending));
        assertTrue(lifecycle.isClosed());
        assertThrows(IllegalStateException.class, lifecycle::beginRegistration);
    }

    @Test
    void repeatedReloadsKeepGenerationMonotonic() {
        TextureRegistrationLifecycle lifecycle = new TextureRegistrationLifecycle();
        long initial = lifecycle.beginRegistration();
        long firstReload = lifecycle.invalidatePending();
        long secondReload = lifecycle.invalidatePending();

        assertTrue(firstReload > initial);
        assertTrue(secondReload > firstReload);
        assertFalse(lifecycle.isCurrent(initial));
    }
}
