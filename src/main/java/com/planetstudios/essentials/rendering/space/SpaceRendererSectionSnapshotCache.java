package com.planetstudios.essentials.rendering.space;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Render-independent, byte-bounded LRU storage for immutable section snapshots. */
public final class SpaceRendererSectionSnapshotCache {
    private final long maxBytes;
    private final LinkedHashMap<SpaceRendererSectionManager.SectionKey, SpaceRendererSectionSnapshot> entries =
            new LinkedHashMap<>(32, 0.75f, true);
    private long currentBytes;

    public SpaceRendererSectionSnapshotCache(long maxBytes) {
        if (maxBytes <= 0) throw new IllegalArgumentException("Snapshot cache capacity must be positive");
        this.maxBytes = maxBytes;
    }

    public synchronized boolean put(SpaceRendererSectionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        long size = snapshot.estimatedBytes();
        if (size > maxBytes) return false;
        SpaceRendererSectionSnapshot previous = entries.remove(snapshot.key());
        if (previous != null) currentBytes -= previous.estimatedBytes();
        entries.put(snapshot.key(), snapshot);
        currentBytes += size;
        while (currentBytes > maxBytes && !entries.isEmpty()) {
            Map.Entry<SpaceRendererSectionManager.SectionKey, SpaceRendererSectionSnapshot> eldest =
                    entries.entrySet().iterator().next();
            currentBytes -= eldest.getValue().estimatedBytes();
            entries.remove(eldest.getKey());
        }
        return entries.containsKey(snapshot.key());
    }

    public synchronized SpaceRendererSectionSnapshot get(SpaceRendererSectionManager.SectionKey key) {
        return entries.get(Objects.requireNonNull(key, "key"));
    }

    public synchronized boolean remove(SpaceRendererSectionManager.SectionKey key) {
        SpaceRendererSectionSnapshot removed = entries.remove(Objects.requireNonNull(key, "key"));
        if (removed == null) return false;
        currentBytes -= removed.estimatedBytes();
        return true;
    }

    public synchronized int removeDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int removed = 0;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().dimensionId().equals(dimensionId)) {
                currentBytes -= entry.getValue().estimatedBytes();
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized int clear() {
        int removed = entries.size();
        entries.clear();
        currentBytes = 0;
        return removed;
    }

    public synchronized int size() { return entries.size(); }
    public synchronized long currentBytes() { return currentBytes; }
    public long maxBytes() { return maxBytes; }
}
