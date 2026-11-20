package com.planetstudios.essentials.rendering.space;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe revision authority for loaded terrain sections.
 *
 * <p>Workers may build from immutable snapshots, but must check their token
 * before publishing. Dimension identity is part of every key, preventing
 * coordinates from different worlds from aliasing.</p>
 */
public final class SpaceRendererSectionManager {
    private final AtomicLong revisions = new AtomicLong();
    private final ConcurrentMap<SectionKey, Long> current = new ConcurrentHashMap<>();

    /** Starts a new build revision and invalidates every earlier token for the section. */
    public BuildToken beginBuild(SectionKey key) {
        Objects.requireNonNull(key, "key");
        long revision = revisions.incrementAndGet();
        if (revision <= 0) throw new IllegalStateException("Section revision counter exhausted");
        current.put(key, revision);
        return new BuildToken(key, revision);
    }

    /** Marks a section unavailable and invalidates every outstanding build token. */
    public boolean unload(SectionKey key) {
        return current.remove(Objects.requireNonNull(key, "key")) != null;
    }

    /** Invalidates all tracked sections in a dimension. */
    public int unloadDimension(String dimensionId) {
        Objects.requireNonNull(dimensionId, "dimensionId");
        int removed = 0;
        for (SectionKey key : current.keySet()) {
            if (key.dimensionId().equals(dimensionId) && current.remove(key) != null) removed++;
        }
        return removed;
    }

    /** True only while this exact build revision remains the latest for its section. */
    public boolean isCurrent(BuildToken token) {
        Objects.requireNonNull(token, "token");
        Long revision = current.get(token.key());
        return revision != null && revision.longValue() == token.revision();
    }

    public long currentRevision(SectionKey key) {
        return current.getOrDefault(Objects.requireNonNull(key, "key"), 0L);
    }

    public int trackedSectionCount() {
        return current.size();
    }

    public record SectionKey(String dimensionId, int x, int y, int z) {
        public SectionKey {
            if (dimensionId == null || dimensionId.isBlank()) {
                throw new IllegalArgumentException("Dimension ID must not be blank");
            }
        }
    }

    public record BuildToken(SectionKey key, long revision) {
        public BuildToken {
            Objects.requireNonNull(key, "key");
            if (revision <= 0) throw new IllegalArgumentException("Build revision must be positive");
        }
    }
}
