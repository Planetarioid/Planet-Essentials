package com.planetstudios.essentials.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMonitorTest {
    @Test
    void retainsOnlyNewestSamplesWithinCapacity() {
        PerformanceMonitor monitor = new PerformanceMonitor(20);
        for (int fps = 1; fps <= 25; fps++) monitor.recordFps(fps);

        assertEquals(20, monitor.sampleCount());
        assertEquals(25, monitor.latestFps());
        assertEquals(15.5, monitor.averageFps(), 0.0001);
        var latest = monitor.snapshot().getLast();
        assertTrue(latest.usedHeapBytes() >= 0);
        assertTrue(latest.maxHeapBytes() >= latest.usedHeapBytes());
        assertTrue(latest.heapUsageRatio() >= 0.0 && latest.heapUsageRatio() <= 1.0);
    }

    @Test
    void clearRemovesAllSamples() {
        PerformanceMonitor monitor = new PerformanceMonitor(20);
        monitor.recordFps(144);
        monitor.clear();

        assertEquals(0, monitor.sampleCount());
        assertEquals(0, monitor.latestFps());
        assertEquals(0.0, monitor.averageFps());
    }

    @Test
    void rejectsUnsupportedCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new PerformanceMonitor(19));
    }
}
