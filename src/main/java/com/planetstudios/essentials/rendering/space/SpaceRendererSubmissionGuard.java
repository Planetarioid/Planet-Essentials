package com.planetstudios.essentials.rendering.space;

/**
 * Tracks whether a render-group attempt has crossed the point where vanilla
 * terrain fallback could duplicate GPU work.
 *
 * <p>Before submission starts, a failed attempt may safely leave the group to
 * vanilla. Once submission starts, the result is treated as indeterminate
 * until every expected draw has been accepted.</p>
 */
final class SpaceRendererSubmissionGuard {
    enum State {
        PREPARED,
        SUBMITTING,
        COMPLETED,
        SKIPPED,
        FAILED_BEFORE_SUBMISSION,
        FAILED_AFTER_SUBMISSION
    }

    private final long frameSequence;
    private final int expectedDraws;
    private State state = State.PREPARED;

    SpaceRendererSubmissionGuard(long frameSequence, int expectedDraws) {
        if (frameSequence <= 0) {
            throw new IllegalArgumentException("Frame sequence must be positive");
        }
        if (expectedDraws <= 0) {
            throw new IllegalArgumentException("Expected draw count must be positive");
        }
        this.frameSequence = frameSequence;
        this.expectedDraws = expectedDraws;
    }

    long frameSequence() {
        return frameSequence;
    }

    int expectedDraws() {
        return expectedDraws;
    }

    State state() {
        return state;
    }

    boolean mayUseVanillaFallback() {
        return state == State.PREPARED || state == State.FAILED_BEFORE_SUBMISSION;
    }

    void beginSubmission() {
        requireState(State.PREPARED, "begin submission");
        state = State.SUBMITTING;
    }

    void complete(int submittedDraws) {
        requireState(State.SUBMITTING, "complete submission");
        if (submittedDraws != expectedDraws) {
            throw new IllegalArgumentException(
                    "Submitted draw count " + submittedDraws
                            + " does not match expected count " + expectedDraws);
        }
        state = State.COMPLETED;
    }

    void failBeforeSubmission() {
        requireState(State.PREPARED, "fail before submission");
        state = State.FAILED_BEFORE_SUBMISSION;
    }

    void failAfterSubmission() {
        requireState(State.SUBMITTING, "fail after submission");
        state = State.FAILED_AFTER_SUBMISSION;
    }

    private void requireState(State expected, String operation) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Cannot " + operation + " while submission state is " + state);
        }
    }
}
