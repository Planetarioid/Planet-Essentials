package com.planetstudios.essentials.rendering.space;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainGpuMesh;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Render-thread-confined, byte-bounded owner of uploaded Space Renderer terrain meshes. */
public final class SpaceRendererGpuCache implements AutoCloseable {
    public static final long DEFAULT_MAX_BYTES = 128L * 1024L * 1024L;
    private final long maxBytes;
    private final LinkedHashMap<Key, Entry> entries = new LinkedHashMap<>(32, 0.75f, true);
    private long currentBytes;
    private boolean closed;
    public SpaceRendererGpuCache() { this(DEFAULT_MAX_BYTES); }
    public SpaceRendererGpuCache(long maxBytes) {
        if (maxBytes <= 0) throw new IllegalArgumentException("GPU cache budget must be positive");
        this.maxBytes = maxBytes;
    }
    public boolean upload(GpuDevice device, SpaceRendererSectionManager.BuildToken token, SpaceRendererMesh mesh) {
        RenderSystem.assertOnRenderThread(); ensureOpen();
        Objects.requireNonNull(device, "device"); Objects.requireNonNull(token, "token"); Objects.requireNonNull(mesh, "mesh");
        var source = mesh.source();
        if (token.key().x() != mesh.sectionX() || token.key().y() != mesh.sectionY() || token.key().z() != mesh.sectionZ())
            throw new IllegalArgumentException("Mesh coordinates do not match its section token");
        int bytes = source.byteSize();
        if (bytes <= 0 || bytes > maxBytes) return false;
        Key key = new Key(token.key(), mesh.materialId(), source.stage(), mesh.layer());
        Entry existing = entries.get(key);
        if (existing != null && existing.hash.equals(source.contentHash())) return true;
        PrismTerrainGpuMesh replacement = PrismTerrainGpuMesh.upload(device, source);
        Entry next = new Entry(replacement, bytes, source.contentHash());
        Entry previous = entries.remove(key);
        if (previous != null) { currentBytes -= previous.bytes; retire(previous.mesh); }
        while (currentBytes > maxBytes - bytes) {
            Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();
            if (!iterator.hasNext()) break;
            Entry eldest = iterator.next().getValue();
            iterator.remove(); currentBytes -= eldest.bytes; retire(eldest.mesh);
        }
        entries.put(key, next); currentBytes += bytes; return true;
    }
    public PrismTerrainGpuMesh meshFor(SpaceRendererSectionManager.SectionKey section, String material, PrismFrameStage stage) {
        return meshFor(section, material, stage, ChunkSectionLayer.SOLID);
    }
    public PrismTerrainGpuMesh meshFor(SpaceRendererSectionManager.SectionKey section, String material, PrismFrameStage stage, ChunkSectionLayer layer) {
        RenderSystem.assertOnRenderThread(); ensureOpen();
        Entry entry = entries.get(new Key(Objects.requireNonNull(section, "section"),
                Objects.requireNonNull(material, "material"), Objects.requireNonNull(stage, "stage"),
                Objects.requireNonNull(layer, "layer")));
        return entry == null ? null : entry.mesh;
    }
    public int invalidateSection(SpaceRendererSectionManager.SectionKey section) {
        RenderSystem.assertOnRenderThread(); ensureOpen(); int removed = 0;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().section.equals(section)) {
                currentBytes -= entry.getValue().bytes; retire(entry.getValue().mesh); iterator.remove(); removed++;
            }
        }
        return removed;
    }
    public int invalidateChunk(String dimension, int chunkX, int chunkZ) {
        RenderSystem.assertOnRenderThread(); ensureOpen(); int removed = 0;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next(); var key = entry.getKey().section;
            if (key.dimensionId().equals(dimension) && key.x() == chunkX && key.z() == chunkZ) {
                currentBytes -= entry.getValue().bytes; retire(entry.getValue().mesh); iterator.remove(); removed++;
            }
        }
        return removed;
    }
    public int clearDimension(String dimension) {
        RenderSystem.assertOnRenderThread(); ensureOpen(); int removed = 0;
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getKey().section.dimensionId().equals(dimension)) {
                currentBytes -= entry.getValue().bytes; retire(entry.getValue().mesh); iterator.remove(); removed++;
            }
        }
        return removed;
    }
    public void clear() {
        RenderSystem.assertOnRenderThread(); if (closed) return;
        for (Entry entry : entries.values()) retire(entry.mesh);
        entries.clear();
        currentBytes = 0;
    }
    public int size() { RenderSystem.assertOnRenderThread(); ensureOpen(); return entries.size(); }
    public long currentBytes() { RenderSystem.assertOnRenderThread(); ensureOpen(); return currentBytes; }
    public long maxBytes() { return maxBytes; }
    /**
     * Keep replaced or evicted vertex allocations alive until all GPU commands
     * already submitted against the previous frame have completed.
     */
    private static void retire(PrismTerrainGpuMesh mesh) {
        RenderSystem.queueFencedTask(mesh::close);
    }

    private void ensureOpen() { if (closed) throw new IllegalStateException("Space Renderer GPU cache is closed"); }
    @Override public void close() { RenderSystem.assertOnRenderThread(); if (closed) return; clear(); closed = true; }
    private record Key(SpaceRendererSectionManager.SectionKey section, String material, PrismFrameStage stage, ChunkSectionLayer layer) {}
    private record Entry(PrismTerrainGpuMesh mesh, int bytes, String hash) {}
}
