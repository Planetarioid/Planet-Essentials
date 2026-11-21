package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererGpuUploadQueueTest {
    private static SpaceRendererSectionManager.BuildToken token(int x) {
        var manager = new SpaceRendererSectionManager();
        return manager.beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:overworld", x, 0, 0));
    }

    @Test void enforcesEntryAndByteLimits() {
        var queue = new SpaceRendererGpuUploadQueue<String>(1, 100);
        assertTrue(queue.offer(token(0), "opaque", "first", 80));
        assertFalse(queue.offer(token(1), "opaque", "second", 10));
        assertEquals(80, queue.queuedBytes());
        assertEquals(1, queue.size());
    }

    @Test void coalescesNewerMeshForSameSectionAndMaterial() {
        var queue = new SpaceRendererGpuUploadQueue<String>(2, 100);
        var first = token(0);
        var newer = new SpaceRendererSectionManager().beginBuild(first.key());
        assertTrue(queue.offer(first, "opaque", "old", 40));
        assertTrue(queue.offer(newer, "opaque", "new", 60));
        assertEquals(1, queue.size());
        assertEquals(60, queue.queuedBytes());
        assertEquals("new", queue.pollBatch(1, 100).get(0).payload());
    }

    @Test void respectsPerBatchByteBudgetAndFifoOrder() {
        var queue = new SpaceRendererGpuUploadQueue<String>(4, 100);
        assertTrue(queue.offer(token(0), "opaque", "a", 30));
        assertTrue(queue.offer(token(1), "opaque", "b", 40));
        assertTrue(queue.offer(token(2), "opaque", "c", 20));
        var batch = queue.pollBatch(3, 50);
        assertEquals(1, batch.size());
        assertEquals("a", batch.get(0).payload());
        assertEquals(60, queue.queuedBytes());
    }

    @Test void removesAllMaterialsForUnloadedSection() {
        var queue = new SpaceRendererGpuUploadQueue<String>(4, 100);
        var section = token(0).key();
        assertTrue(queue.offer(token(0), "opaque", "a", 20));
        assertTrue(queue.offer(token(0), "translucent", "b", 30));
        assertTrue(queue.offer(token(1), "opaque", "c", 40));
        assertEquals(2, queue.removeSection(section));
        assertEquals(40, queue.queuedBytes());
        assertEquals(1, queue.size());
    }

    @Test void removesOnlyUploadsForRequestedDimension() {
        var queue = new SpaceRendererGpuUploadQueue<String>(4, 100);
        var overworld = token(0);
        var nether = new SpaceRendererSectionManager().beginBuild(
                new SpaceRendererSectionManager.SectionKey("minecraft:the_nether", 0, 0, 0));
        assertTrue(queue.offer(overworld, "opaque", "overworld", 20));
        assertTrue(queue.offer(nether, "opaque", "nether", 30));
        assertEquals(1, queue.removeDimension("minecraft:overworld"));
        assertEquals(30, queue.queuedBytes());
        assertEquals("nether", queue.pollBatch(1, 100).get(0).payload());
    }

    @Test void rejectedReplacementPreservesExistingRequest() {
        var queue = new SpaceRendererGpuUploadQueue<String>(2, 50);
        var current = token(0);
        assertTrue(queue.offer(current, "opaque", "kept", 30));
        assertFalse(queue.offer(current, "opaque", "too-large", 60));
        assertEquals("kept", queue.pollBatch(1, 50).get(0).payload());
    }

    @Test void validatesLimitsAndRequests() {
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererGpuUploadQueue<String>(0, 10));
        var queue = new SpaceRendererGpuUploadQueue<String>(2, 10);
        assertThrows(IllegalArgumentException.class, () -> queue.offer(token(0), "opaque", "x", 0));
        assertThrows(IllegalArgumentException.class, () -> queue.pollBatch(-1, 1));
    }
}
