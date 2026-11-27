package com.planetstudios.essentials.rendering.space;

import java.util.Objects;

/**
 * Tracks the one-way boundary between safe vanilla fallback and a Space
 * Renderer group submission that may already have queued GPU work.
 *
 * <p>Once submission begins, failure is indeterminate and vanilla must not be
 * rendered as a second copy of the same group.</p>
 */
public final class SpaceRendererSubmissionGuard {
    private final long frameSequence;
    private final int expectedDraws;
    private State state = State.PREPARED;

    public SpaceRendererSubmissionGuard(long frameSequence, int expectedDraws) {
        if (frameSequence <= 0 || expectedDraws <= 0) {
            throw new IllegalArgumentException("A submission guard requires a positive frame and draw count");
        }
        this.frameSequence = frameSequence;
        this.expectedDraws = expectedDraws;
    }

    public void beginSubmission() {
        requireState(State.PREPARED);
        state = State.SUBMITTING;
    }

    public void complete(int submittedDraws) {
        requireState(State.SUBMITTING);
        if (submittedDraws != expectedDraws) {
            throw new IllegalArgumentException("Submitted draw count does not match the prepared group");
        }
        state = State.COMPLETED;
    }

    public void failBeforeSubmission() {
        requireState(State.PREPARED);
        state = State.FAILED_BEFORE_SUBMISSION;
    }

    public void failAfterSubmission() {
        requireState(State.SUBMITTING);
        state = State.FAILED_AFTER_SUBMISSION;
    }

    public long frameSequence() { return frameSequence; }
    public int expectedDraws() { return expectedDraws; }
    public State state() { return state; }

    /** True only when no Space Renderer draw could have been queued. */
    public boolean mayUseVanillaFallback() {
        return state == State.PREPARED || state == State.FAILED_BEFORE_SUBMISSION;
    }

    private void requireState(State expected) {
        Objects.requireNonNull(expected, "expected");
        if (state != expected) {
            throw new IllegalStateException("Expected submission state " + expected + " but was " + state);
        }
    }

    public enum State {
        PREPARED,
        SUBMITTING,
        COMPLETED,
        FAILED_BEFORE_SUBMISSION,
        FAILED_AFTER_SUBMISSION
    }
}
