package com.planetstudios.essentials.rendering.shaders.client.terrain;

/**
 * Tracks the irreversible boundary of a synchronous Prism terrain takeover.
 *
 * <p>Vanilla fallback is safe only before the first Prism draw is submitted.
 * RenderPass.close() does not roll back submitted GPU work, so a failure after
 * submission must never be reported as permission to draw the same group again.</p>
 */
public final class PrismTerrainSubmissionState {
    public enum Phase {
        PREPARED,
        SUBMITTING,
        COMPLETE,
        FAILED_BEFORE_SUBMISSION,
        FAILED_AFTER_SUBMISSION
    }

    private Phase phase = Phase.PREPARED;
    private int submittedDraws;

    /** Enters the submission phase after every fallible preflight step succeeds. */
    public void begin() {
        requirePhase(Phase.PREPARED);
        phase = Phase.SUBMITTING;
    }

    /** Records one draw only after RenderPass.drawIndexed returned successfully. */
    public void recordSubmittedDraw() {
        requirePhase(Phase.SUBMITTING);
        submittedDraws = Math.incrementExact(submittedDraws);
    }

    /** Marks the complete group as submitted. */
    public void complete() {
        requirePhase(Phase.SUBMITTING);
        if (submittedDraws == 0) {
            throw new IllegalStateException("Cannot complete a terrain group without submitted draws");
        }
        phase = Phase.COMPLETE;
    }

    /**
     * Records a failure and returns whether vanilla may safely render the group.
     * Once a Prism draw was submitted, vanilla fallback would duplicate geometry.
     */
    public boolean fail() {
        if (phase == Phase.FAILED_BEFORE_SUBMISSION || phase == Phase.FAILED_AFTER_SUBMISSION) {
            return phase == Phase.FAILED_BEFORE_SUBMISSION;
        }
        if (phase == Phase.COMPLETE) {
            throw new IllegalStateException("A completed terrain group cannot fail");
        }
        if (phase == Phase.PREPARED || submittedDraws == 0) {
            phase = Phase.FAILED_BEFORE_SUBMISSION;
            return true;
        }
        phase = Phase.FAILED_AFTER_SUBMISSION;
        return false;
    }

    public Phase phase() {
        return phase;
    }

    public int submittedDraws() {
        return submittedDraws;
    }

    public boolean mayFallBackToVanilla() {
        return phase == Phase.PREPARED || phase == Phase.FAILED_BEFORE_SUBMISSION;
    }

    private void requirePhase(Phase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Expected terrain submission phase " + expected + " but was " + phase);
        }
    }
}
