package com.planetstudios.essentials.diagnostics;

import java.time.Instant;

/** One lightweight client performance observation. */
public record PerformanceSample(Instant timestamp, int framesPerSecond) {
    public PerformanceSample {
        if (timestamp == null) throw new NullPointerException("timestamp");
        if (framesPerSecond < 0) throw new IllegalArgumentException("framesPerSecond cannot be negative");
    }
}
