package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSubmissionGuardTest {
    @Test
    void vanillaFallbackIsSafeBeforeSubmissionStarts() {
        var guard = new SpaceRendererSubmissionGuard(4, 3);

        assertTrue(guard.mayUseVanillaFallback());
        guard.failBeforeSubmission();
        assertEquals(SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION, guard.state());
        assertTrue(guard.mayUseVanillaFallback());
    }

    @Test
    void successfulSubmissionClosesFallbackWindow() {
        var guard = new SpaceRendererSubmissionGuard(5, 2);
        guard.beginSubmission();
        guard.complete(2);

        assertEquals(SpaceRendererSubmissionGuard.State.COMPLETED, guard.state());
        assertFalse(guard.mayUseVanillaFallback());
    }

    @Test
    void failureAfterSubmissionStartsMustNeverFallBackToVanilla() {
        var guard = new SpaceRendererSubmissionGuard(6, 4);
        guard.beginSubmission();
        guard.failAfterSubmission();

        assertEquals(SpaceRendererSubmissionGuard.State.FAILED_AFTER_SUBMISSION, guard.state());
        assertFalse(guard.mayUseVanillaFallback());
    }

    @Test
    void completionRequiresExactDrawCount() {
        var guard = new SpaceRendererSubmissionGuard(7, 3);
        guard.beginSubmission();

        assertThrows(IllegalArgumentException.class, () -> guard.complete(2));
        assertEquals(SpaceRendererSubmissionGuard.State.SUBMITTING, guard.state());
        guard.failAfterSubmission();
        assertFalse(guard.mayUseVanillaFallback());
    }

    @Test
    void illegalTransitionsAreRejected() {
        var guard = new SpaceRendererSubmissionGuard(8, 1);

        assertThrows(IllegalStateException.class, () -> guard.complete(1));
        assertThrows(IllegalStateException.class, guard::failAfterSubmission);
        guard.beginSubmission();
        assertThrows(IllegalStateException.class, guard::failBeforeSubmission);
        guard.failAfterSubmission();
        assertThrows(IllegalStateException.class, guard::beginSubmission);
    }

    @Test
    void rejectsInvalidFrameOrDrawCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererSubmissionGuard(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererSubmissionGuard(1, 0));
    }
}
