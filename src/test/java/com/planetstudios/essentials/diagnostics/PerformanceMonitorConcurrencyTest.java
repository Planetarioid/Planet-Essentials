package com.planetstudios.essentials.diagnostics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceMonitorConcurrencyTest {
    @Test
    void concurrentRecordingAndSnapshotsStayBoundedAndConsistent() throws Exception {
        PerformanceMonitor monitor = new PerformanceMonitor(120);
        int workers = 6;
        int samplesPerWorker = 500;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int worker = 0; worker < workers; worker++) {
                final int offset = worker * samplesPerWorker;
                tasks.add(executor.submit(() -> {
                    for (int i = 0; i < samplesPerWorker; i++) {
                        monitor.recordFps(offset + i);
                        List<PerformanceSample> snapshot = monitor.snapshot();
                        assertTrue(snapshot.size() <= 120);
                        assertTrue(monitor.averageFps() >= 0);
                    }
                }));
            }
            for (Future<?> task : tasks) task.get(15, TimeUnit.SECONDS);
            assertEquals(120, monitor.sampleCount());
            assertEquals(120, monitor.snapshot().size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
