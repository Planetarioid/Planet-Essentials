package com.planetstudios.essentials.diagnostics;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Bounded, in-memory FPS observation history.
 * Sampling is caller-driven and does no work unless diagnostics are enabled.
 */
public final class PerformanceMonitor {
    private final ArrayDeque<PerformanceSample> samples = new ArrayDeque<>();
    private int capacity;

    public PerformanceMonitor(int capacity) {
        setCapacity(capacity);
    }

    public synchronized void setCapacity(int capacity) {
        if (capacity < 20 || capacity > 1200) throw new IllegalArgumentException("capacity must be 20..1200");
        this.capacity = capacity;
        while (samples.size() > capacity) samples.removeFirst();
    }

    public synchronized void recordFps(int fps) {
        if (samples.size() == capacity) samples.removeFirst();
        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long maxHeap = runtime.maxMemory();
        samples.addLast(new PerformanceSample(Instant.now(), fps, usedHeap, maxHeap));
    }

    public synchronized List<PerformanceSample> snapshot() {
        return List.copyOf(samples);
    }

    public synchronized void clear() {
        samples.clear();
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

    public synchronized int sampleCount() {
        return samples.size();
    }
}
