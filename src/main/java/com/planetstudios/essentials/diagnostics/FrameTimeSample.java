package com.planetstudios.essentials.diagnostics;

import java.time.Instant;
import java.util.Objects;

/** One render-frame duration captured locally for frame-pacing analysis. */
public record FrameTimeSample(Instant timestamp, long frameTimeNanos) {
    public FrameTimeSample {
        Objects.requireNonNull(timestamp, "timestamp");
        if (frameTimeNanos <= 0) throw new IllegalArgumentException("frameTimeNanos must be positive");
    }

    public double milliseconds() {
        return frameTimeNanos / 1_000_000.0;
    }

    public double estimatedFps() {
        return 1_000_000_000.0 / frameTimeNanos;
    }
}
