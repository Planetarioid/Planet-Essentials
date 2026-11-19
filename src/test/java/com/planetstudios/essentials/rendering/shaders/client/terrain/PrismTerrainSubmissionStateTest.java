package com.planetstudios.essentials.rendering.shaders.client.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainSubmissionStateTest {
    @Test
    void preflightFailureKeepsVanillaFallbackAvailable() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        assertTrue(state.mayFallBackToVanilla());
        assertTrue(state.fail());
        assertEquals(PrismTerrainSubmissionState.Phase.FAILED_BEFORE_SUBMISSION, state.phase());
        assertTrue(state.mayFallBackToVanilla());
    }

    @Test
    void failureBeforeFirstDrawAllowsVanillaFallback() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        state.begin();
        assertTrue(state.fail());
        assertEquals(PrismTerrainSubmissionState.Phase.FAILED_BEFORE_SUBMISSION, state.phase());
        assertEquals(0, state.drawSubmissionAttempts());
    }

    @Test
    void backendFailureAfterSubmissionAttemptForbidsVanillaFallback() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        state.begin();
        state.markDrawSubmissionStarted();
        assertFalse(state.fail());
        assertEquals(PrismTerrainSubmissionState.Phase.FAILED_AFTER_SUBMISSION, state.phase());
        assertEquals(1, state.drawSubmissionAttempts());
        assertFalse(state.mayFallBackToVanilla());
        assertFalse(state.fail());
    }

    @Test
    void completeRequiresAtLeastOneAttemptAndCannotBeFailed() {
        PrismTerrainSubmissionState empty = new PrismTerrainSubmissionState();
        empty.begin();
        assertThrows(IllegalStateException.class, empty::complete);

        PrismTerrainSubmissionState complete = new PrismTerrainSubmissionState();
        complete.begin();
        complete.markDrawSubmissionStarted();
        complete.complete();
        assertEquals(PrismTerrainSubmissionState.Phase.COMPLETE, complete.phase());
        assertThrows(IllegalStateException.class, complete::fail);
    }

    @Test
    void transitionsRejectOutOfOrderCalls() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        assertThrows(IllegalStateException.class, state::markDrawSubmissionStarted);
        state.begin();
        assertThrows(IllegalStateException.class, state::begin);
        state.markDrawSubmissionStarted();
        assertEquals(1, state.drawSubmissionAttempts());
    }
}
