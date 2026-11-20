package com.planetstudios.essentials.rendering.space;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Aligned best-fit allocator for ranges in a shared terrain GPU region. */
public final class SpaceRendererRegionAllocator {
    private final int capacityBytes;
    private final int alignmentBytes;
    private final TreeMap<Integer, Integer> freeRanges = new TreeMap<>();
    private final Map<Long, Allocation> allocations = new HashMap<>();
    private long nextId = 1;
    private long allocatedBytes;

    public SpaceRendererRegionAllocator(int capacityBytes, int alignmentBytes) {
        if (capacityBytes <= 0 || alignmentBytes <= 0) throw new IllegalArgumentException("Capacity and alignment must be positive");
        this.capacityBytes = capacityBytes;
        this.alignmentBytes = alignmentBytes;
        freeRanges.put(0, capacityBytes);
    }

    public Allocation allocate(int requestedBytes) {
        if (requestedBytes <= 0) throw new IllegalArgumentException("Allocation size must be positive");
        int reserved = alignUp(requestedBytes, alignmentBytes);
        if (reserved > capacityBytes) return null;
        int selectedStart = -1, selectedOffset = -1, selectedLength = 0, selectedWaste = Integer.MAX_VALUE;
        for (var entry : freeRanges.entrySet()) {
            int start = entry.getKey(), length = entry.getValue(), offset = alignUp(start, alignmentBytes);
            long end = (long) offset + reserved, freeEnd = (long) start + length;
            if (end > freeEnd) continue;
            int waste = (int) (freeEnd - end);
            if (waste < selectedWaste) {
                selectedStart = start; selectedOffset = offset; selectedLength = length; selectedWaste = waste;
                if (waste == 0) break;
            }
        }
        if (selectedStart < 0) return null;
        freeRanges.remove(selectedStart);
        int prefix = selectedOffset - selectedStart;
        int suffixStart = selectedOffset + reserved;
        int suffix = selectedLength - prefix - reserved;
        if (prefix > 0) freeRanges.put(selectedStart, prefix);
        if (suffix > 0) freeRanges.put(suffixStart, suffix);
        if (nextId <= 0) throw new IllegalStateException("Allocation identity exhausted");
        Allocation allocation = new Allocation(nextId++, selectedOffset, requestedBytes, reserved);
        allocations.put(allocation.id(), allocation);
        allocatedBytes += reserved;
        return allocation;
    }

    public void release(Allocation allocation) {
        Objects.requireNonNull(allocation, "allocation");
        Allocation live = allocations.remove(allocation.id());
        if (!allocation.equals(live)) {
            if (live != null) allocations.put(live.id(), live);
            throw new IllegalArgumentException("Allocation is stale, foreign, or already released");
        }
        allocatedBytes -= live.reservedBytes();
        int start = live.offsetBytes(), length = live.reservedBytes();
        var lower = freeRanges.floorEntry(start);
        if (lower != null && (long) lower.getKey() + lower.getValue() == start) {
            start = lower.getKey(); length += lower.getValue(); freeRanges.remove(lower.getKey());
        }
        var higher = freeRanges.ceilingEntry(start);
        if (higher != null && (long) start + length == higher.getKey()) {
            length += higher.getValue(); freeRanges.remove(higher.getKey());
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
        long result = ((long) value + alignment - 1L) / alignment * alignment;
        if (result > Integer.MAX_VALUE) throw new IllegalArgumentException("Aligned size overflow");
        return (int) result;
    }
    public record Allocation(long id, int offsetBytes, int requestedBytes, int reservedBytes) {
        public Allocation {
            if (id <= 0 || offsetBytes < 0 || requestedBytes <= 0 || reservedBytes < requestedBytes) {
                throw new IllegalArgumentException("Invalid allocation");
            }
        }
    }
}
