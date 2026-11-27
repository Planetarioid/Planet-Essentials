package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererGroupReplacementPolicyTest {
    @Test
    void skippedGroupLeavesVanillaInControl() {
        var decision = SpaceRendererGroupReplacementPolicy.decide(attempt(
                SpaceRendererSubmissionGuard.State.SKIPPED, 0, true));

        assertEquals(SpaceRendererGroupReplacementPolicy.VanillaAction.RENDER,
                decision.vanillaAction());
        assertEquals(SpaceRendererGroupReplacementPolicy.Reason.NO_CUSTOM_DRAW_SUBMITTED,
                decision.reason());
        assertFalse(decision.shouldCancelVanilla());
    }

    @Test
    void preSubmissionFailureLeavesVanillaInControl() {
        var decision = SpaceRendererGroupReplacementPolicy.decide(attempt(
                SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION, 0, true));

        assertFalse(decision.shouldCancelVanilla());
        assertFalse(decision.replacementCompleted());
        assertFalse(decision.submissionMayBePartial());
    }

    @Test
    void completedSubmissionCancelsVanilla() {
        var decision = SpaceRendererGroupReplacementPolicy.decide(attempt(
                SpaceRendererSubmissionGuard.State.COMPLETED, 4, false));

        assertTrue(decision.shouldCancelVanilla());
        assertTrue(decision.replacementCompleted());
        assertFalse(decision.submissionMayBePartial());
    }

    @Test
    void indeterminatePostSubmissionFailureCancelsVanillaToAvoidDoubleDraw() {
        var decision = SpaceRendererGroupReplacementPolicy.decide(attempt(
                SpaceRendererSubmissionGuard.State.FAILED_AFTER_SUBMISSION, -1, false));

        assertTrue(decision.shouldCancelVanilla());
        assertFalse(decision.replacementCompleted());
        assertTrue(decision.submissionMayBePartial());
        assertEquals(SpaceRendererGroupReplacementPolicy.Reason.SUBMISSION_INDETERMINATE,
                decision.reason());
    }

    @Test
    void nonTerminalAttemptCannotBeRepresentedAsAResult() {
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererDrawExecutor.SubmissionAttempt(
                42, SpaceRendererSubmissionGuard.State.PREPARED, 0, false, null, null));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererDrawExecutor.SubmissionAttempt(
                42, SpaceRendererSubmissionGuard.State.SUBMITTING, 0, false, null, null));
    }

    @Test
    void inconsistentFallbackClaimIsRejectedBeforePolicyEvaluation() {
        assertThrows(IllegalArgumentException.class,
                () -> attempt(SpaceRendererSubmissionGuard.State.FAILED_AFTER_SUBMISSION, -1, true));
        assertThrows(IllegalArgumentException.class,
                () -> attempt(SpaceRendererSubmissionGuard.State.COMPLETED, 4, true));
    }

    @Test
    void invalidDrawCountsAndFailureDetailsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> attempt(SpaceRendererSubmissionGuard.State.COMPLETED, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> attempt(SpaceRendererSubmissionGuard.State.SKIPPED, -1, true));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererDrawExecutor.SubmissionAttempt(
                        42, SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION,
                        0, true, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererDrawExecutor.SubmissionAttempt(
                        42, SpaceRendererSubmissionGuard.State.FAILED_AFTER_SUBMISSION,
                        -1, false, null, null));
    }

    private static SpaceRendererDrawExecutor.SubmissionAttempt attempt(
            SpaceRendererSubmissionGuard.State state, int draws, boolean fallbackSafe) {
        String failure = state == SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION
                || state == SpaceRendererSubmissionGuard.State.FAILED_AFTER_SUBMISSION
                ? "java.lang.IllegalStateException" : null;
        String message = failure == null ? null : "test failure";
        return new SpaceRendererDrawExecutor.SubmissionAttempt(
                42, state, draws, fallbackSafe, failure, message);
    }
}
