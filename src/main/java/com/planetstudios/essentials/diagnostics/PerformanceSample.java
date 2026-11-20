package com.planetstudios.essentials.diagnostics;

import java.time.Instant;

/** One lightweight client performance observation, including JVM heap usage. */
public record PerformanceSample(Instant timestamp, int framesPerSecond,
                                long usedHeapBytes, long maxHeapBytes) {
    public PerformanceSample {
        if (timestamp == null) throw new NullPointerException("timestamp");
        if (framesPerSecond < 0) throw new IllegalArgumentException("framesPerSecond cannot be negative");
        if (usedHeapBytes < 0 || maxHeapBytes < 0 || usedHeapBytes > maxHeapBytes) {
            throw new IllegalArgumentException("Invalid JVM heap observation");
        }
    }

    public double heapUsageRatio() {
        return maxHeapBytes == 0 ? 0.0 : (double) usedHeapBytes / maxHeapBytes;
    }
}
