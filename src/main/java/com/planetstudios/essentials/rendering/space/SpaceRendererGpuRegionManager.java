package com.planetstudios.essentials.rendering.space;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** Transactional CPU-side ownership table for ranges in shared terrain GPU storage. */
public final class SpaceRendererGpuRegionManager {
    private final SpaceRendererRegionAllocator allocator;
    private final Map<SpaceRendererSectionManager.SectionKey, SpaceRendererRegionAllocator.Allocation> sections = new HashMap<>();
    private final Map<Replacement, Boolean> pending = new IdentityHashMap<>();

    public SpaceRendererGpuRegionManager(SpaceRendererRegionAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator, "allocator");
    }
    public synchronized Replacement reserveReplacement(SpaceRendererSectionManager.SectionKey key, int bytes) {
        Objects.requireNonNull(key, "key");
        var previous = sections.get(key);
        var next = allocator.allocate(bytes);
        if (next == null) return null;
        var replacement = new Replacement(key, previous, next);
        pending.put(replacement, Boolean.TRUE);
        return replacement;
    }
    public synchronized void commitReplacement(Replacement replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (!pending.containsKey(replacement)) throw new IllegalStateException("Unknown or resolved replacement");
        if (!Objects.equals(sections.get(replacement.key()), replacement.previous())) {
            throw new IllegalStateException("Section allocation changed before commit");
        }
        pending.remove(replacement);
        sections.put(replacement.key(), replacement.next());
        if (replacement.previous() != null) allocator.release(replacement.previous());
    }
    public synchronized void rollbackReplacement(Replacement replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (pending.remove(replacement) == null) throw new IllegalStateException("Unknown or resolved replacement");
        allocator.release(replacement.next());
    }
    public synchronized boolean releaseSection(SpaceRendererSectionManager.SectionKey key) {
        var allocation = sections.remove(Objects.requireNonNull(key, "key"));
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
                allocator.release(entry.getValue()); iterator.remove(); released++;
            }
        }
        return released;
    }
    public synchronized SpaceRendererRegionAllocator.Allocation allocationFor(SpaceRendererSectionManager.SectionKey key) {
        return sections.get(Objects.requireNonNull(key, "key"));
    }
    public synchronized int sectionCount() { return sections.size(); }
    public synchronized long allocatedBytes() { return allocator.allocatedBytes(); }
    public synchronized int freeBytes() { return (int) allocator.freeBytes(); }

    public record Replacement(SpaceRendererSectionManager.SectionKey key,
            SpaceRendererRegionAllocator.Allocation previous, SpaceRendererRegionAllocator.Allocation next) {
        public Replacement {
            Objects.requireNonNull(key, "key"); Objects.requireNonNull(next, "next");
            if (Objects.equals(previous, next)) throw new IllegalArgumentException("Replacement must be distinct");
        }
    }
}
