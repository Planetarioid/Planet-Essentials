package com.planetstudios.essentials.rendering.shaders.client.terrain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates asynchronous section rebuilds with cache invalidation.
 * A build may publish only while its section revision is still current.
 */
public final class PrismTerrainSectionLifecycle {
    private final PrismTerrainSectionCache cache;
    private final AtomicLong revisions = new AtomicLong();
    private final Map<SectionKey, Long> currentRevisions = new HashMap<>();

    public PrismTerrainSectionLifecycle(PrismTerrainSectionCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    /** Starts a rebuild and immediately invalidates cached payloads for that section. */
    public synchronized BuildToken beginRebuild(int sectionX, int sectionY, int sectionZ) {
        SectionKey key = new SectionKey(sectionX, sectionY, sectionZ);
        cache.invalidateSection(sectionX, sectionY, sectionZ);
        long revision = revisions.incrementAndGet();
        if (revision <= 0) throw new IllegalStateException("Section revision counter exhausted");
        currentRevisions.put(key, revision);
        return new BuildToken(sectionX, sectionY, sectionZ, revision);
    }

    /**
     * Publishes a completed immutable payload only if no newer rebuild or unload
     * has superseded its token. Returns false for stale asynchronous results.
     */
    public synchronized boolean publish(BuildToken token, PrismTerrainVertexData data) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(data, "data");
        if (data.sectionX() != token.sectionX() || data.sectionY() != token.sectionY()
                || data.sectionZ() != token.sectionZ()) {
            throw new IllegalArgumentException("Terrain payload does not match its section build token");
        }
        Long current = currentRevisions.get(new SectionKey(token.sectionX(), token.sectionY(), token.sectionZ()));
        if (current == null || current.longValue() != token.revision()) return false;
        return cache.put(data);
    }

    /** Invalidates a section and makes all outstanding build tokens stale. */
    public synchronized int invalidateSection(int sectionX, int sectionY, int sectionZ) {
        currentRevisions.remove(new SectionKey(sectionX, sectionY, sectionZ));
        return cache.invalidateSection(sectionX, sectionY, sectionZ);
    }

    /**
     * Invalidates every vertical section in a chunk, inclusive. Call this on
     * chunk unload and use the dimension's actual section range.
     */
    public synchronized int unloadChunk(int chunkX, int minSectionY, int maxSectionY, int chunkZ) {
        if (minSectionY > maxSectionY || (long) maxSectionY - minSectionY > 1024L) {
            throw new IllegalArgumentException("Invalid chunk section range");
        }
        int removed = 0;
        for (int sectionY = minSectionY; ; sectionY++) {
            currentRevisions.remove(new SectionKey(chunkX, sectionY, chunkZ));
            removed += cache.invalidateSection(chunkX, sectionY, chunkZ);
            if (sectionY == maxSectionY) break;
        }
        return removed;
    }

    public record BuildToken(int sectionX, int sectionY, int sectionZ, long revision) {
        public BuildToken {
            if (revision <= 0) throw new IllegalArgumentException("Build revision must be positive");
        }
    }

    private record SectionKey(int x, int y, int z) {}
}