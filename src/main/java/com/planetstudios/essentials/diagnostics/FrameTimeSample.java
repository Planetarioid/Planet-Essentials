package com.planetstudios.essentials.diagnostics;

/** One render-frame duration captured locally for frame-pacing analysis. */
public record FrameTimeSample(long frameIndex, long frameTimeNanos) {
    public FrameTimeSample {
        if (frameIndex < 0) throw new IllegalArgumentException("frameIndex cannot be negative");
        if (frameTimeNanos <= 0) throw new IllegalArgumentException("frameTimeNanos must be positive");
    }

    public double milliseconds() {
        return frameTimeNanos / 1_000_000.0;
    }

    public double estimatedFps() {
        return 1_000_000_000.0 / frameTimeNanos;
    }
}
