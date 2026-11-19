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
        assertEquals(0, state.submittedDraws());
    }

    @Test
    void failureAfterAnySubmittedDrawForbidsVanillaFallback() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        state.begin();
        state.recordSubmittedDraw();

        assertFalse(state.fail());
        assertEquals(PrismTerrainSubmissionState.Phase.FAILED_AFTER_SUBMISSION, state.phase());
        assertEquals(1, state.submittedDraws());
        assertFalse(state.mayFallBackToVanilla());
        assertFalse(state.fail());
    }

    @Test
    void completeRequiresAtLeastOneDrawAndCannotBeFailed() {
        PrismTerrainSubmissionState empty = new PrismTerrainSubmissionState();
        empty.begin();
        assertThrows(IllegalStateException.class, empty::complete);

        PrismTerrainSubmissionState complete = new PrismTerrainSubmissionState();
        complete.begin();
        complete.recordSubmittedDraw();
        complete.complete();
        assertEquals(PrismTerrainSubmissionState.Phase.COMPLETE, complete.phase());
        assertThrows(IllegalStateException.class, complete::fail);
    }

    @Test
    void transitionsRejectOutOfOrderCalls() {
        PrismTerrainSubmissionState state = new PrismTerrainSubmissionState();
        assertThrows(IllegalStateException.class, state::recordSubmittedDraw);
        state.begin();
        assertThrows(IllegalStateException.class, state::begin);
        state.recordSubmittedDraw();
        assertTrue(state.submittedDraws() == 1);
    }
}
