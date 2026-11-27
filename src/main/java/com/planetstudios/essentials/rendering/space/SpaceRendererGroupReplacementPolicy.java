package com.planetstudios.essentials.rendering.space;

import java.util.Objects;

/**
 * Defines the vanilla-cancellation decision for one native render-group attempt.
 *
 * <p>This policy is intentionally separate from the live mixin. It makes the
 * dangerous boundary explicit and testable before any caller is allowed to
 * activate replacement. Before submission, vanilla remains the fallback.
 * After submission may have started, vanilla must not also draw the group:
 * some custom GPU work may already be queued.</p>
 */
public final class SpaceRendererGroupReplacementPolicy {
    private SpaceRendererGroupReplacementPolicy() {}

    public static Decision decide(SpaceRendererDrawExecutor.SubmissionAttempt attempt) {
        Objects.requireNonNull(attempt, "attempt");
        return switch (attempt.state()) {
            case SKIPPED, FAILED_BEFORE_SUBMISSION ->
                    new Decision(VanillaAction.RENDER, Reason.NO_CUSTOM_DRAW_SUBMITTED);
            case COMPLETED ->
                    new Decision(VanillaAction.CANCEL, Reason.CUSTOM_GROUP_SUBMITTED);
            case FAILED_AFTER_SUBMISSION ->
                    new Decision(VanillaAction.CANCEL, Reason.SUBMISSION_INDETERMINATE);
            case PREPARED, SUBMITTING ->
                    throw new IllegalArgumentException(
                            "A live render-group decision requires a terminal submission outcome");
        };
    }

    public enum VanillaAction {
        /** Leave the native renderGroup method untouched. */
        RENDER,
        /** Cancel vanilla to prevent a second draw of work already submitted or possibly submitted. */
        CANCEL
    }

    public enum Reason {
        NO_CUSTOM_DRAW_SUBMITTED,
        CUSTOM_GROUP_SUBMITTED,
        SUBMISSION_INDETERMINATE
    }

    public record Decision(VanillaAction vanillaAction, Reason reason) {
        public Decision {
            Objects.requireNonNull(vanillaAction, "vanillaAction");
            Objects.requireNonNull(reason, "reason");
            if ((reason == Reason.NO_CUSTOM_DRAW_SUBMITTED)
                    != (vanillaAction == VanillaAction.RENDER)) {
                throw new IllegalArgumentException("Vanilla action does not match replacement outcome");
            }
        }

        public boolean shouldCancelVanilla() {
            return vanillaAction == VanillaAction.CANCEL;
        }

        public boolean replacementCompleted() {
            return reason == Reason.CUSTOM_GROUP_SUBMITTED;
        }

        public boolean submissionMayBePartial() {
            return reason == Reason.SUBMISSION_INDETERMINATE;
        }
    }
}
