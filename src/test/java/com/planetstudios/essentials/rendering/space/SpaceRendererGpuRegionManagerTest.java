package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererGpuRegionManagerTest {
    private static SpaceRendererSectionManager.SectionKey key(int x) {
        return new SpaceRendererSectionManager.SectionKey("minecraft:overworld", x, 0, 0);
    }

    @Test
    void commitsReplacementOnlyAfterNewRangeHasBeenReserved() {
        var allocator = new SpaceRendererRegionAllocator(1024, 64);
        var manager = new SpaceRendererGpuRegionManager(allocator);
        var first = manager.reserveReplacement(key(0), 128);
        assertNotNull(first);
        manager.commitReplacement(first);
        var original = manager.allocationFor(key(0));

        var replacement = manager.reserveReplacement(key(0), 256);
        assertNotNull(replacement);
        assertEquals(original, manager.allocationFor(key(0)));
        assertEquals(384, manager.allocatedBytes());

        manager.commitReplacement(replacement);
        assertEquals(replacement.next(), manager.allocationFor(key(0)));
        assertEquals(256, manager.allocatedBytes());
    }

    @Test
    void rollbackPreservesExistingAllocation() {
        var allocator = new SpaceRendererRegionAllocator(512, 64);
        var manager = new SpaceRendererGpuRegionManager(allocator);
        var initial = manager.reserveReplacement(key(1), 128);
        manager.commitReplacement(initial);
        var original = manager.allocationFor(key(1));

        var failedUpload = manager.reserveReplacement(key(1), 192);
        assertNotNull(failedUpload);
        manager.rollbackReplacement(failedUpload);

        assertEquals(original, manager.allocationFor(key(1)));
        assertEquals(128, manager.allocatedBytes());
    }

    @Test
    void failedReservationDoesNotDiscardPreviousMesh() {
        var allocator = new SpaceRendererRegionAllocator(256, 64);
        var manager = new SpaceRendererGpuRegionManager(allocator);
        var initial = manager.reserveReplacement(key(2), 192);
        manager.commitReplacement(initial);

        assertNull(manager.reserveReplacement(key(2), 128));
        assertEquals(initial.next(), manager.allocationFor(key(2)));
        assertEquals(192, manager.allocatedBytes());
    }

    @Test
    void releasesSectionsByDimension() {
        var allocator = new SpaceRendererRegionAllocator(1024, 64);
        var manager = new SpaceRendererGpuRegionManager(allocator);
        var a = manager.reserveReplacement(key(0), 128);
        var b = manager.reserveReplacement(new SpaceRendererSectionManager.SectionKey(
                "minecraft:the_nether", 0, 0, 0), 128);
        manager.commitReplacement(a);
        manager.commitReplacement(b);

        assertEquals(1, manager.releaseDimension("minecraft:overworld"));
        assertNull(manager.allocationFor(key(0)));
        assertNotNull(manager.allocationFor(new SpaceRendererSectionManager.SectionKey(
                "minecraft:the_nether", 0, 0, 0)));
        assertEquals(128, manager.allocatedBytes());
    }

    @Test
    void rejectsStaleReplacementCommit() {
        var allocator = new SpaceRendererRegionAllocator(1024, 64);
        var manager = new SpaceRendererGpuRegionManager(allocator);
        var first = manager.reserveReplacement(key(0), 128);
        manager.commitReplacement(first);
        var stale = manager.reserveReplacement(key(0), 192);
        manager.releaseSection(key(0));

        assertThrows(IllegalStateException.class, () -> manager.commitReplacement(stale));
        manager.rollbackReplacement(stale);
        assertEquals(0, manager.allocatedBytes());
    }
}
