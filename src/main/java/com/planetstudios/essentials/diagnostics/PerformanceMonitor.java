package com.planetstudios.essentials.diagnostics;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded, in-memory performance history. Frame timing uses a monotonic sequence
 * instead of a wall-clock query per frame to keep the render hot path inexpensive.
 */
public final class PerformanceMonitor {
    private final ArrayDeque<PerformanceSample> samples = new ArrayDeque<>();
    private final ArrayDeque<FrameTimeSample> frameTimes = new ArrayDeque<>();
    private int capacity;
    private int frameCapacity;
    private long frameSequence;

    public PerformanceMonitor(int capacity) {
        setCapacity(capacity);
    }

    public synchronized void setCapacity(int capacity) {
        if (capacity < 20 || capacity > 1200) throw new IllegalArgumentException("capacity must be 20..1200");
        this.capacity = capacity;
        this.frameCapacity = Math.min(36_000, capacity * 30);
        while (samples.size() > capacity) samples.removeFirst();
        while (frameTimes.size() > frameCapacity) frameTimes.removeFirst();
    }

    public synchronized void recordFps(int fps) {
        if (samples.size() == capacity) samples.removeFirst();
        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long maxHeap = runtime.maxMemory();
        samples.addLast(new PerformanceSample(Instant.now(), fps, usedHeap, maxHeap));
    }

    /** Records one frame duration without reading the system wall clock. */
    public synchronized void recordFrameTime(long frameTimeNanos) {
        if (frameTimeNanos <= 0 || frameTimeNanos > 10_000_000_000L) return;
        if (frameTimes.size() == frameCapacity) frameTimes.removeFirst();
        frameTimes.addLast(new FrameTimeSample(frameSequence++, frameTimeNanos));
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
        frameSequence = 0L;
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
        long[] sorted = frameTimes.stream().mapToLong(FrameTimeSample::frameTimeNanos).sorted().toArray();
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))] / 1_000_000.0;
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
