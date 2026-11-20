package com.planetstudios.essentials.diagnostics;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded, in-memory performance history. Sampling is caller-driven and does
 * no work unless the user enables performance monitoring.
 */
public final class PerformanceMonitor {
    private final ArrayDeque<PerformanceSample> samples = new ArrayDeque<>();
    private final ArrayDeque<FrameTimeSample> frameTimes = new ArrayDeque<>();
    private int capacity;

    public PerformanceMonitor(int capacity) {
        setCapacity(capacity);
    }

    public synchronized void setCapacity(int capacity) {
        if (capacity < 20 || capacity > 1200) throw new IllegalArgumentException("capacity must be 20..1200");
        this.capacity = capacity;
        while (samples.size() > capacity) samples.removeFirst();
        while (frameTimes.size() > capacity) frameTimes.removeFirst();
    }

    public synchronized void recordFps(int fps) {
        if (samples.size() == capacity) samples.removeFirst();
        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long maxHeap = runtime.maxMemory();
        samples.addLast(new PerformanceSample(Instant.now(), fps, usedHeap, maxHeap));
    }

    /** Records one frame duration. Call only from the render thread while monitoring is enabled. */
    public synchronized void recordFrameTime(long frameTimeNanos) {
        if (frameTimeNanos <= 0 || frameTimeNanos > 10_000_000_000L) return;
        if (frameTimes.size() == capacity) frameTimes.removeFirst();
        frameTimes.addLast(new FrameTimeSample(Instant.now(), frameTimeNanos));
    }

    public synchronized List<PerformanceSample> snapshot() {
        return List.copyOf(samples);
    }

    public synchronized List<FrameTimeSample> frameTimeSnapshot() {
        return List.copyOf(frameTimes);
    }

    public synchronized void clear() {
        samples.clear();
        frameTimes.clear();
    }

    public synchronized int latestFps() {
        return samples.isEmpty() ? 0 : samples.getLast().framesPerSecond();
    }

    public synchronized double averageFps() {
        if (samples.isEmpty()) return 0;
        long total = 0;
        for (PerformanceSample sample : samples) total += sample.framesPerSecond();
        return (double) total / samples.size();
    }

    public synchronized double frameTimePercentileMillis(double percentile) {
        if (percentile < 0.0 || percentile > 1.0) throw new IllegalArgumentException("percentile must be 0..1");
        if (frameTimes.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(frameTimes.size());
        for (FrameTimeSample sample : frameTimes) sorted.add(sample.frameTimeNanos());
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index))) / 1_000_000.0;
    }

    public synchronized double averageFrameTimeMillis() {
        if (frameTimes.isEmpty()) return 0.0;
        long total = 0L;
        for (FrameTimeSample sample : frameTimes) total += sample.frameTimeNanos();
        return total / (double) frameTimes.size() / 1_000_000.0;
    }

    public synchronized double worstFrameTimeMillis() {
        long worst = 0L;
        for (FrameTimeSample sample : frameTimes) worst = Math.max(worst, sample.frameTimeNanos());
        return worst / 1_000_000.0;
    }

    public synchronized int sampleCount() {
        return samples.size();
    }

    public synchronized int frameSampleCount() {
        return frameTimes.size();
    }
}
