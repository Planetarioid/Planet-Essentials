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
    void retainsLongerFrameHistoryWithoutWallClockTimestamps() {
        PerformanceMonitor monitor = new PerformanceMonitor(20);
        for (int i = 0; i < 700; i++) monitor.recordFrameTime(10_000_000L + i);
        assertEquals(600, monitor.frameSampleCount());
        assertEquals(100L, monitor.frameTimeSnapshot().getFirst().frameIndex());
        assertTrue(monitor.frameTimePercentileMillis(0.95) >= 10.0);
    }

    @Test
    void clearRemovesAllSamplesAndResetsFrameSequence() {
        PerformanceMonitor monitor = new PerformanceMonitor(20);
        monitor.recordFps(144);
        monitor.recordFrameTime(16_000_000);
        monitor.clear();

        assertEquals(0, monitor.sampleCount());
        assertEquals(0, monitor.latestFps());
        assertEquals(0.0, monitor.averageFps());
        assertEquals(0, monitor.frameSampleCount());
        monitor.recordFrameTime(16_000_000);
        assertEquals(0, monitor.frameTimeSnapshot().getFirst().frameIndex());
    }

    @Test
    void rejectsUnsupportedCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new PerformanceMonitor(19));
    }
}
