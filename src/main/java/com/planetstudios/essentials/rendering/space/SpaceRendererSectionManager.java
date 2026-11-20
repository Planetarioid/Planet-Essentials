package com.planetstudios.essentials.rendering.space;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe authority for terrain section identities and build revisions. */
public final class SpaceRendererSectionManager {
    private final AtomicLong revisions = new AtomicLong();
    private final ConcurrentMap<SectionKey, Long> current = new ConcurrentHashMap<>();

    public BuildToken beginBuild(SectionKey key) {
        Objects.requireNonNull(key, "key");
        long revision = revisions.incrementAndGet();
        if (revision <= 0) throw new IllegalStateException("Section revision counter exhausted");
        current.put(key, revision);
        return new BuildToken(key, revision);
    }
    public boolean unload(SectionKey key) { return current.remove(Objects.requireNonNull(key, "key")) != null; }
    public int unloadDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int removed = 0;
        for (SectionKey key : current.keySet()) {
            if (key.dimensionId().equals(dimensionId) && current.remove(key) != null) removed++;
        }
        return removed;
    }
    public boolean isCurrent(BuildToken token) {
        Objects.requireNonNull(token, "token");
        Long revision = current.get(token.key());
        return revision != null && revision.longValue() == token.revision();
    }
    public long currentRevision(SectionKey key) { return current.getOrDefault(Objects.requireNonNull(key, "key"), 0L); }
    public int trackedSectionCount() { return current.size(); }

    public record SectionKey(String dimensionId, int x, int y, int z) {
        public SectionKey {
            if (dimensionId == null || dimensionId.isBlank()) throw new IllegalArgumentException("Dimension ID must not be blank");
        }
    }
    public record BuildToken(SectionKey key, long revision) {
        public BuildToken {
            Objects.requireNonNull(key, "key");
            if (revision <= 0) throw new IllegalArgumentException("Revision must be positive");
        }
    }
}
