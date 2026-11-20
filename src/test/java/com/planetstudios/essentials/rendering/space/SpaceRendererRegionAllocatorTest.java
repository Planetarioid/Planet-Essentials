package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererRegionAllocatorTest {
    @Test
    void alignsAllocationsAndTracksReservedBytes() {
        var allocator = new SpaceRendererRegionAllocator(1024, 256);
        var first = allocator.allocate(300);
        var second = allocator.allocate(1);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(0, first.offsetBytes());
        assertEquals(512, first.reservedBytes());
        assertEquals(512, second.offsetBytes());
        assertEquals(256, second.reservedBytes());
        assertEquals(768, allocator.allocatedBytes());
        assertEquals(256, allocator.freeBytes());
    }

    @Test
    void coalescesAdjacentReleasedRanges() {
        var allocator = new SpaceRendererRegionAllocator(1024, 64);
        var first = allocator.allocate(128);
        var second = allocator.allocate(128);
        var third = allocator.allocate(128);

        allocator.release(second);
        allocator.release(first);
        allocator.release(third);

        assertEquals(0, allocator.allocatedBytes());
        assertEquals(1024, allocator.largestFreeRangeBytes());
        assertEquals(0, allocator.allocationCount());
    }

    @Test
    void reusesBestFitRangeWithoutBreakingAlignment() {
        var allocator = new SpaceRendererRegionAllocator(1024, 64);
        var first = allocator.allocate(256);
        var second = allocator.allocate(128);
        var third = allocator.allocate(256);
        allocator.release(first);
        allocator.release(third);

        var replacement = allocator.allocate(192);

        assertNotNull(replacement);
        assertEquals(0, replacement.offsetBytes());
        assertEquals(192, replacement.requestedBytes());
        assertEquals(192, replacement.reservedBytes());
        assertEquals(256, second.offsetBytes());
    }

    @Test
    void returnsNullWhenNoContiguousRangeCanFitRequest() {
        var allocator = new SpaceRendererRegionAllocator(512, 64);
        var first = allocator.allocate(192);
        var second = allocator.allocate(192);
        allocator.release(first);

        assertNull(allocator.allocate(256));
        assertEquals(192, allocator.allocatedBytes());
        assertEquals(320, allocator.freeBytes());
        assertEquals(192, second.reservedBytes());
    }

    @Test
    void rejectsDoubleReleaseAndForeignHandles() {
        var allocator = new SpaceRendererRegionAllocator(512, 64);
        var allocation = allocator.allocate(64);
        var foreign = new SpaceRendererRegionAllocator.Allocation(
                allocation.id(), allocation.offsetBytes(), allocation.requestedBytes(), allocation.reservedBytes());

        allocator.release(allocation);
        assertThrows(IllegalArgumentException.class, () -> allocator.release(allocation));
        assertThrows(IllegalArgumentException.class, () -> allocator.release(foreign));
    }

    @Test
    void rejectsInvalidConfigurationAndRequests() {
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererRegionAllocator(0, 16));
        assertThrows(IllegalArgumentException.class, () -> new SpaceRendererRegionAllocator(64, 0));
        var allocator = new SpaceRendererRegionAllocator(64, 16);
        assertThrows(IllegalArgumentException.class, () -> allocator.allocate(0));
        assertNull(allocator.allocate(65));
    }
}
