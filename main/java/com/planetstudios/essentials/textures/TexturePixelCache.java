package com.planetstudios.essentials.textures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Synchronized access-order LRU cache bounded by decoded pixel bytes. */
public final class TexturePixelCache {
    public static final long DEFAULT_MAX_BYTES = 128L * 1024 * 1024;

    private final long maxBytes;
    private final LinkedHashMap<String, DecodedTexture> entries =
            new LinkedHashMap<>(16, 0.75f, true);
    private long retainedBytes;
    private long generation;

    public TexturePixelCache() { this(DEFAULT_MAX_BYTES); }

    public TexturePixelCache(long maxBytes) {
        if (maxBytes < 4) throw new IllegalArgumentException("maxBytes must be at least one pixel");
        this.maxBytes = maxBytes;
    }

    public synchronized Optional<DecodedTexture> get(String resourceId) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(resourceId, "resourceId")));
    }

    public synchronized void put(String resourceId, DecodedTexture texture) {
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(texture, "texture");
        long bytes = texture.estimatedBytes();
        if (bytes > maxBytes) return;
        DecodedTexture previous = entries.remove(resourceId);
        if (previous != null) retainedBytes -= previous.estimatedBytes();
        entries.put(resourceId, texture);
        retainedBytes += bytes;
        evictToBudget();
    }

    private void evictToBudget() {
        var iterator = entries.entrySet().iterator();
        while (retainedBytes > maxBytes && iterator.hasNext()) {
            Map.Entry<String, DecodedTexture> eldest = iterator.next();
            retainedBytes -= eldest.getValue().estimatedBytes();
            iterator.remove();
        }
    }

    /** Invalidates every decoded image when the active resource-pack generation changes. */
    public synchronized long invalidateForReload() {
        entries.clear();
        retainedBytes = 0L;
        return ++generation;
    }

    public synchronized void clear() {
        entries.clear();
        retainedBytes = 0L;
    }

    public synchronized long generation() { return generation; }
    public synchronized long retainedBytes() { return retainedBytes; }
    public long maxBytes() { return maxBytes; }
    public synchronized int size() { return entries.size(); }
}
