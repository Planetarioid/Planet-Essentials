package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded CPU-side cache for immutable section terrain payloads.
 *
 * <p>This is not a GPU allocation cache. A renderer adapter may use it as
 * rebuild input, while owning GPU buffers and render-thread disposal itself.</p>
 */
public final class PrismTerrainSectionCache {
    public static final long DEFAULT_MAX_BYTES = 32L * 1024L * 1024L;

    private final long maxBytes;
    private final LinkedHashMap<Key, PrismTerrainVertexData> entries =
            new LinkedHashMap<>(16, 0.75f, true);
    private long currentBytes;

    public PrismTerrainSectionCache() {
        this(DEFAULT_MAX_BYTES);
    }

    public PrismTerrainSectionCache(long maxBytes) {
        if (maxBytes <= 0) throw new IllegalArgumentException("Cache byte budget must be positive");
        this.maxBytes = maxBytes;
    }

    /**
     * Inserts or replaces a section payload. Least-recently-used entries are
     * evicted until the configured CPU memory budget is respected.
     *
     * @return false if the payload alone exceeds the entire cache budget.
     */
    public synchronized boolean put(PrismTerrainVertexData data) {
        Objects.requireNonNull(data, "data");
        long bytes = data.estimatedCpuBytes();
        if (bytes > maxBytes) return false;

        Key key = Key.of(data);
        PrismTerrainVertexData previous = entries.remove(key);
        if (previous != null) currentBytes -= previous.estimatedCpuBytes();

        while (currentBytes + bytes > maxBytes) {
            Iterator<Map.Entry<Key, PrismTerrainVertexData>> iterator = entries.entrySet().iterator();
            if (!iterator.hasNext()) break;
            Map.Entry<Key, PrismTerrainVertexData> eldest = iterator.next();
            currentBytes -= eldest.getValue().estimatedCpuBytes();
            iterator.remove();
        }

        entries.put(key, data);
        currentBytes += bytes;
        return true;
    }

    /** Access updates LRU order. Payloads themselves remain immutable. */
    public synchronized PrismTerrainVertexData get(int sectionX, int sectionY, int sectionZ,
            String materialId, PrismFrameStage stage) {
        return entries.get(new Key(sectionX, sectionY, sectionZ, materialId, stage));
    }

    /** Immutable point-in-time payload list; iteration does not mutate LRU order. */
    public synchronized List<PrismTerrainVertexData> snapshot() {
        return List.copyOf(new ArrayList<>(entries.values()));
    }

    /** Invalidates every material/stage payload for a rebuilt or unloaded section. */
    public synchronized int invalidateSection(int sectionX, int sectionY, int sectionZ) {
        int removed = 0;
        Iterator<Map.Entry<Key, PrismTerrainVertexData>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, PrismTerrainVertexData> entry = iterator.next();
            Key key = entry.getKey();
            if (key.x == sectionX && key.y == sectionY && key.z == sectionZ) {
                currentBytes -= entry.getValue().estimatedCpuBytes();
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized void clear() {
        entries.clear();
        currentBytes = 0;
    }

    public synchronized int size() { return entries.size(); }
    public synchronized long currentBytes() { return currentBytes; }
    public long maxBytes() { return maxBytes; }

    private record Key(int x, int y, int z, String materialId, PrismFrameStage stage) {
        private static Key of(PrismTerrainVertexData data) {
            return new Key(data.sectionX(), data.sectionY(), data.sectionZ(),
                    data.materialId(), data.stage());
        }
    }
}
