package com.planetstudios.essentials.rendering.space;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic aligned range allocator for shared terrain GPU regions.
 *
 * <p>The allocator is graphics-API agnostic. Its owner is responsible for
 * confining calls to the renderer's resource-management thread and associating
 * returned ranges with actual GPU storage.</p>
 */
public final class SpaceRendererRegionAllocator {
    private final int capacityBytes;
    private final int alignmentBytes;
    private final TreeMap<Integer, Integer> freeRanges = new TreeMap<>();
    private final Map<Long, Allocation> allocations = new HashMap<>();
    private long nextId = 1;
    private long allocatedBytes;

    public SpaceRendererRegionAllocator(int capacityBytes, int alignmentBytes) {
        if (capacityBytes <= 0) throw new IllegalArgumentException("Region capacity must be positive");
        if (alignmentBytes <= 0) throw new IllegalArgumentException("Alignment must be positive");
        this.capacityBytes = capacityBytes;
        this.alignmentBytes = alignmentBytes;
        freeRanges.put(0, capacityBytes);
    }

    /** Allocates the smallest fitting free range after alignment, or returns null. */
    public Allocation allocate(int requestedBytes) {
        if (requestedBytes <= 0) throw new IllegalArgumentException("Allocation size must be positive");
        int reservedBytes = alignUp(requestedBytes, alignmentBytes);
        if (reservedBytes > capacityBytes) return null;

        int selectedStart = -1;
        int selectedOffset = -1;
        int selectedLength = 0;
        int selectedWaste = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Integer> entry : freeRanges.entrySet()) {
            int start = entry.getKey();
            int length = entry.getValue();
            int offset = alignUp(start, alignmentBytes);
            long end = (long) offset + reservedBytes;
            long freeEnd = (long) start + length;
            if (end > freeEnd) continue;
            int waste = (int) (freeEnd - end);
            if (waste < selectedWaste) {
                selectedStart = start;
                selectedOffset = offset;
                selectedLength = length;
                selectedWaste = waste;
                if (waste == 0) break;
            }
        }

        if (selectedStart < 0) return null;
        freeRanges.remove(selectedStart);
        int prefix = selectedOffset - selectedStart;
        int suffixStart = selectedOffset + reservedBytes;
        int suffix = selectedLength - prefix - reservedBytes;
        if (prefix > 0) freeRanges.put(selectedStart, prefix);
        if (suffix > 0) freeRanges.put(suffixStart, suffix);

        if (nextId <= 0) throw new IllegalStateException("Region allocation identity exhausted");
        Allocation allocation = new Allocation(nextId++, selectedOffset, requestedBytes, reservedBytes);
        allocations.put(allocation.id(), allocation);
        allocatedBytes += reservedBytes;
        return allocation;
    }

    /** Releases a live allocation and coalesces adjacent free ranges. */
    public void release(Allocation allocation) {
        Objects.requireNonNull(allocation, "allocation");
        Allocation live = allocations.remove(allocation.id());
        if (!allocation.equals(live)) {
            if (live != null) allocations.put(live.id(), live);
            throw new IllegalArgumentException("Allocation is stale, foreign, or already released");
        }

        allocatedBytes -= live.reservedBytes();
        int start = live.offsetBytes();
        int length = live.reservedBytes();

        Map.Entry<Integer, Integer> lower = freeRanges.floorEntry(start);
        if (lower != null && (long) lower.getKey() + lower.getValue() == start) {
            start = lower.getKey();
            length += lower.getValue();
            freeRanges.remove(lower.getKey());
        }

        Map.Entry<Integer, Integer> higher = freeRanges.ceilingEntry(start);
        if (higher != null && (long) start + length == higher.getKey()) {
            length += higher.getValue();
            freeRanges.remove(higher.getKey());
        }
        freeRanges.put(start, length);
    }

    public int capacityBytes() { return capacityBytes; }
    public long allocatedBytes() { return allocatedBytes; }
    public long freeBytes() { return capacityBytes - allocatedBytes; }
    public int allocationCount() { return allocations.size(); }

    public int largestFreeRangeBytes() {
        int largest = 0;
        for (int length : freeRanges.values()) largest = Math.max(largest, length);
        return largest;
    }

    private static int alignUp(int value, int alignment) {
        long aligned = ((long) value + alignment - 1L) / alignment * alignment;
        if (aligned > Integer.MAX_VALUE) throw new IllegalArgumentException("Aligned size exceeds integer range");
        return (int) aligned;
    }

    public record Allocation(long id, int offsetBytes, int requestedBytes, int reservedBytes) {
        public Allocation {
            if (id <= 0 || offsetBytes < 0 || requestedBytes <= 0 || reservedBytes < requestedBytes) {
                throw new IllegalArgumentException("Invalid region allocation");
            }
        }
    }
}
