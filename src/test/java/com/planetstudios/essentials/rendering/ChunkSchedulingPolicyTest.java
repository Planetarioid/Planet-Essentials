package com.planetstudios.essentials.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkSchedulingPolicyTest {
    @Test void prioritizesForwardChunksOverEquidistantChunksBehindCamera() {
        ChunkSchedulingPolicy.Look look = new ChunkSchedulingPolicy.Look(0, 0, 1);
        double forward = ChunkSchedulingPolicy.score(0, 0, 64, look);
        double behind = ChunkSchedulingPolicy.score(0, 0, -64, look);
        assertTrue(forward < behind);
    }

    @Test void keepsDistanceAsThePrimarySignal() {
        ChunkSchedulingPolicy.Look look = new ChunkSchedulingPolicy.Look(0, 0, 1);
        double nearBehind = ChunkSchedulingPolicy.score(0, 0, -16, look);
        double farForward = ChunkSchedulingPolicy.score(0, 0, 128, look);
        assertTrue(nearBehind < farForward);
    }

    @Test void normalizesPublishedLookDirection() {
        ChunkSchedulingPolicy.updateLook(0, 0, 4);
        assertEquals(1.0, ChunkSchedulingPolicy.look().z(), 0.00001);
    }
}
