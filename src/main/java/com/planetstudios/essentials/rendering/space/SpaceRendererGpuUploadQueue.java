package com.planetstudios.essentials.rendering.space;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Bounded CPU-to-render-thread handoff. This class never invokes graphics APIs. */
public final class SpaceRendererGpuUploadQueue<T> {
    private final int maxEntries;
    private final long maxQueuedBytes;
    private final LinkedHashMap<UploadKey, UploadRequest<T>> pending = new LinkedHashMap<>();
    private long queuedBytes;

    public SpaceRendererGpuUploadQueue(int maxEntries, long maxQueuedBytes) {
        if (maxEntries <= 0 || maxQueuedBytes <= 0) throw new IllegalArgumentException("Upload queue limits must be positive");
        this.maxEntries = maxEntries;
        this.maxQueuedBytes = maxQueuedBytes;
    }

    /** Replaces a queued section/material upload atomically; rejection preserves the old request. */
    public synchronized boolean offer(SpaceRendererSectionManager.BuildToken token,
                                      String materialId, T payload, long byteSize) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(payload, "payload");
        if (materialId.isBlank() || byteSize <= 0)
            throw new IllegalArgumentException("Invalid upload request");
        if (byteSize > maxQueuedBytes) return false;
        UploadKey key = new UploadKey(token.key(), materialId);
        UploadRequest<T> previous = pending.get(key);
        long previousBytes = previous == null ? 0 : previous.byteSize();
        long nextBytes = queuedBytes - previousBytes + byteSize;
        int nextEntries = pending.size() + (previous == null ? 1 : 0);
        if (nextBytes > maxQueuedBytes || nextEntries > maxEntries) return false;
        pending.put(key, new UploadRequest<>(token, materialId, payload, byteSize));
        queuedBytes = nextBytes;
        return true;
    }

    /** Removes a bounded FIFO batch for consumption on the render thread. */
    public synchronized List<UploadRequest<T>> pollBatch(int maxRequests, long maxBytes) {
        if (maxRequests < 0 || maxBytes < 0) throw new IllegalArgumentException("Batch limits must not be negative");
        if (maxRequests == 0 || maxBytes == 0 || pending.isEmpty()) return List.of();
        List<UploadRequest<T>> batch = new ArrayList<>();
        long bytes = 0;
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext() && batch.size() < maxRequests) {
            UploadRequest<T> request = iterator.next().getValue();
            if (request.byteSize() > maxBytes - bytes) break;
            batch.add(request);
            bytes += request.byteSize();
            iterator.remove();
        }
        queuedBytes -= bytes;
        return List.copyOf(batch);
    }

    public synchronized int removeSection(SpaceRendererSectionManager.SectionKey section) {
        Objects.requireNonNull(section, "section");
        int removed = 0;
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().section().equals(section)) {
                queuedBytes -= entry.getValue().byteSize();
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized int removeDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int removed = 0;
        var iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().section().dimensionId().equals(dimensionId)) {
                queuedBytes -= entry.getValue().byteSize();
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized void clear() { pending.clear(); queuedBytes = 0; }
    public synchronized int size() { return pending.size(); }
    public synchronized long queuedBytes() { return queuedBytes; }
    public int maxEntries() { return maxEntries; }
    public long maxQueuedBytes() { return maxQueuedBytes; }

    private record UploadKey(SpaceRendererSectionManager.SectionKey section, String materialId) {}
    public record UploadRequest<T>(SpaceRendererSectionManager.BuildToken token,
                                   String materialId, T payload, long byteSize) {
        public UploadRequest {
            Objects.requireNonNull(token, "token");
            Objects.requireNonNull(materialId, "materialId");
            Objects.requireNonNull(payload, "payload");
            if (materialId.isBlank() || byteSize <= 0) throw new IllegalArgumentException("Invalid upload request");
        }
    }
}
