package com.planetstudios.essentials.diagnostics;

import java.util.List;

/** Conservative heuristic diagnosis from available client-side timing data. */
public final class BottleneckAnalyzer {
    public enum Finding {
        INSUFFICIENT_DATA("Collect more samples to estimate the bottleneck."),
        MEMORY_PRESSURE("Possible memory pressure: JVM heap usage is high."),
        MAIN_THREAD_PRESSURE("Possible main-thread pressure: slow client ticks were recorded."),
        FRAME_PACING("Frame pacing is uneven: high-percentile frame times are elevated."),
        NO_CLEAR_BOTTLENECK("No clear bottleneck detected from the available measurements.");

        private final String description;
        Finding(String description) { this.description = description; }
        public String description() { return description; }
    }

    private BottleneckAnalyzer() {}

    public static Finding analyze(List<PerformanceSample> fps,
                                  List<FrameTimeSample> frames,
                                  List<DiagnosticEvent> events) {
        if (fps == null || frames == null || events == null) throw new NullPointerException("diagnostic histories");
        if (fps.size() < 5 && frames.size() < 30) return Finding.INSUFFICIENT_DATA;

        PerformanceSample latest = fps.isEmpty() ? null : fps.get(fps.size() - 1);
        if (latest != null && latest.heapUsageRatio() >= 0.88) return Finding.MEMORY_PRESSURE;

        long slowTicks = events.stream()
                .filter(event -> event.type() == DiagnosticEventType.SLOW_CLIENT_TICK)
                .filter(event -> event.durationMillis() >= 50)
                .count();
        if (slowTicks >= 3) return Finding.MAIN_THREAD_PRESSURE;

        if (frames.size() >= 30) {
            double p50 = percentileMillis(frames, 0.50);
            double p95 = percentileMillis(frames, 0.95);
            if (p95 >= 33.4 && p95 > Math.max(1.0, p50 * 2.0)) return Finding.FRAME_PACING;
        }
        return Finding.NO_CLEAR_BOTTLENECK;
    }

    private static double percentileMillis(List<FrameTimeSample> samples, double percentile) {
        long[] values = samples.stream().mapToLong(FrameTimeSample::frameTimeNanos).sorted().toArray();
        int index = Math.max(0, Math.min(values.length - 1, (int) Math.ceil(percentile * values.length) - 1));
        return values[index] / 1_000_000.0;
    }
}
