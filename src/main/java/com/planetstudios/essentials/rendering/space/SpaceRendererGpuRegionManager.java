package com.planetstudios.essentials.rendering.space;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CPU-side ownership table for ranges reserved in a shared terrain GPU region.
 *
 * <p>This class does not issue GPU uploads. It provides transactional range
 * ownership so a render-thread backend can safely allocate replacement space,
 * upload new data, and only then retire the old range.</p>
 */
public final class SpaceRendererGpuRegionManager {
    private final SpaceRendererRegionAllocator allocator;
    private final Map<SpaceRendererSectionManager.SectionKey, SpaceRendererRegionAllocator.Allocation> sections =
            new HashMap<>();

    public SpaceRendererGpuRegionManager(SpaceRendererRegionAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator, "allocator");
    }

    /**
     * Reserves space for a replacement mesh. The previous range remains live
     * until the caller confirms upload completion with commitReplacement.
     */
    public synchronized Replacement reserveReplacement(
            SpaceRendererSectionManager.SectionKey key, int requestedBytes) {
        Objects.requireNonNull(key, "key");
        SpaceRendererRegionAllocator.Allocation previous = sections.get(key);
        SpaceRendererRegionAllocator.Allocation replacement = allocator.allocate(requestedBytes);
        if (replacement == null) return null;
        return new Replacement(key, previous, replacement);
    }

    /**
     * Commits a successfully uploaded replacement and releases the prior range.
     * A failed or stale replacement can instead be rolled back.
     */
    public synchronized void commitReplacement(Replacement replacement) {
        Objects.requireNonNull(replacement, "replacement");
        SpaceRendererRegionAllocator.Allocation current = sections.get(replacement.key());
        if (!Objects.equals(current, replacement.previous())) {
            throw new IllegalStateException("Section GPU allocation changed before replacement commit");
        }
        sections.put(replacement.key(), replacement.next());
        if (replacement.previous() != null) allocator.release(replacement.previous());
    }

    /** Releases a replacement reservation when its upload fails or is cancelled. */
    public synchronized void rollbackReplacement(Replacement replacement) {
        Objects.requireNonNull(replacement, "replacement");
        SpaceRendererRegionAllocator.Allocation current = sections.get(replacement.key());
        if (!Objects.equals(current, replacement.previous())) {
            throw new IllegalStateException("Section GPU allocation changed before replacement rollback");
        }
        allocator.release(replacement.next());
    }

    public synchronized boolean releaseSection(SpaceRendererSectionManager.SectionKey key) {
        SpaceRendererRegionAllocator.Allocation allocation = sections.remove(Objects.requireNonNull(key, "key"));
        if (allocation == null) return false;
        allocator.release(allocation);
        return true;
    }

    public synchronized int releaseDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int released = 0;
        var iterator = sections.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().dimensionId().equals(dimensionId)) {
                allocator.release(entry.getValue());
                iterator.remove();
                released++;
            }
        }
        return released;
    }

    public synchronized SpaceRendererRegionAllocator.Allocation allocationFor(
            SpaceRendererSectionManager.SectionKey key) {
        return sections.get(Objects.requireNonNull(key, "key"));
    }

    public synchronized int sectionCount() { return sections.size(); }
    public synchronized long allocatedBytes() { return allocator.allocatedBytes(); }
    public synchronized int freeBytes() { return (int) allocator.freeBytes(); }

    public record Replacement(SpaceRendererSectionManager.SectionKey key,
            SpaceRendererRegionAllocator.Allocation previous,
            SpaceRendererRegionAllocator.Allocation next) {
        public Replacement {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(next, "next");
            if (previous != null && previous.equals(next)) {
                throw new IllegalArgumentException("Replacement must reserve a distinct allocation");
            }
        }
    }
}
