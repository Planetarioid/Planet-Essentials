package com.planetstudios.essentials.rendering.shaders.client.terrain;

/**
 * Tracks the irreversible boundary of a synchronous Prism terrain takeover.
 * A draw attempt is treated as submitted before entering the GPU backend,
 * because the backend may partially submit work and then throw.
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
    private int drawSubmissionAttempts;

    /** Enters the submission phase after every fallible preflight step succeeds. */
    public void begin() {
        requirePhase(Phase.PREPARED);
        phase = Phase.SUBMITTING;
    }

    /**
     * Marks the point of no safe vanilla fallback immediately before invoking
     * RenderPass.drawIndexed or another backend submission operation.
     */
    public void markDrawSubmissionStarted() {
        requirePhase(Phase.SUBMITTING);
        drawSubmissionAttempts = Math.incrementExact(drawSubmissionAttempts);
    }

    /** Marks the complete group as submitted. */
    public void complete() {
        requirePhase(Phase.SUBMITTING);
        if (drawSubmissionAttempts == 0) {
            throw new IllegalStateException("Cannot complete a terrain group without draw submissions");
        }
        phase = Phase.COMPLETE;
    }

    /** Records a failure and returns whether vanilla may safely render the group. */
    public boolean fail() {
        if (phase == Phase.FAILED_BEFORE_SUBMISSION || phase == Phase.FAILED_AFTER_SUBMISSION) {
            return phase == Phase.FAILED_BEFORE_SUBMISSION;
        }
        if (phase == Phase.COMPLETE) {
            throw new IllegalStateException("A completed terrain group cannot fail");
        }
        if (phase == Phase.PREPARED || drawSubmissionAttempts == 0) {
            phase = Phase.FAILED_BEFORE_SUBMISSION;
            return true;
        }
        phase = Phase.FAILED_AFTER_SUBMISSION;
        return false;
    }

    public Phase phase() { return phase; }
    public int drawSubmissionAttempts() { return drawSubmissionAttempts; }
    public boolean mayFallBackToVanilla() {
        return phase == Phase.PREPARED || phase == Phase.FAILED_BEFORE_SUBMISSION;
    }

    private void requirePhase(Phase expected) {
        if (phase != expected) {
            throw new IllegalStateException("Expected terrain submission phase " + expected + " but was " + phase);
        }
    }
}
