package com.planetstudios.essentials.diagnostics;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BottleneckAnalyzerTest {
    @Test void requestsMoreDataForShortHistories() {
        assertEquals(BottleneckAnalyzer.Finding.INSUFFICIENT_DATA,
                BottleneckAnalyzer.analyze(List.of(), List.of(), List.of()));
    }

    @Test void detectsHighHeapPressure() {
        var sample = new PerformanceSample(Instant.now(), 60, 900, 1000);
        assertEquals(BottleneckAnalyzer.Finding.MEMORY_PRESSURE,
                BottleneckAnalyzer.analyze(List.of(sample), List.of(), List.of()));
    }

    @Test void detectsUnevenFramePacing() {
        List<FrameTimeSample> frames = new ArrayList<>();
        for (int i = 0; i < 29; i++) frames.add(new FrameTimeSample(Instant.now(), 16_000_000));
        frames.add(new FrameTimeSample(Instant.now(), 80_000_000));
        assertEquals(BottleneckAnalyzer.Finding.FRAME_PACING,
                BottleneckAnalyzer.analyze(List.of(), frames, List.of()));
    }
}
