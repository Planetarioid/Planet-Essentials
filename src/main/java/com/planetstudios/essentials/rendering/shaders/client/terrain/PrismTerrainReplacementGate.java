package com.planetstudios.essentials.rendering.shaders.client.terrain;

/**
 * Fail-open policy for a future per-render-group vanilla terrain takeover.
 * This class owns no renderer state and never suppresses vanilla by itself.
 */
public final class PrismTerrainReplacementGate {
    private PrismTerrainReplacementGate() {}

    public enum FallbackReason {
        NONE, EMPTY_GROUP, INCOMPLETE_COVERAGE, PROGRAM_UNAVAILABLE,
        INCOMPATIBLE_DEVICE, FRAME_NOT_READY
    }

    public record Readiness(int requiredDraws, int readyDraws,
            boolean programReady, boolean deviceCompatible, boolean frameReady) {
        public Readiness {
            if (requiredDraws < 0 || readyDraws < 0 || readyDraws > requiredDraws) {
                throw new IllegalArgumentException("Invalid Prism replacement draw coverage");
            }
        }
    }

    public static FallbackReason evaluate(Readiness readiness) {
        if (readiness.requiredDraws() == 0) return FallbackReason.EMPTY_GROUP;
        if (readiness.readyDraws() != readiness.requiredDraws()) return FallbackReason.INCOMPLETE_COVERAGE;
        if (!readiness.programReady()) return FallbackReason.PROGRAM_UNAVAILABLE;
        if (!readiness.deviceCompatible()) return FallbackReason.INCOMPATIBLE_DEVICE;
        if (!readiness.frameReady()) return FallbackReason.FRAME_NOT_READY;
        return FallbackReason.NONE;
    }

    public static boolean mayReplace(Readiness readiness) {
        return evaluate(readiness) == FallbackReason.NONE;
    }
}
