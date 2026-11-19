package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded, synchronized extraction queue. Only immutable meshes cross frame phases. */
public final class PrismTerrainFrameQueue {
    public static final int MAX_MESHES_PER_FRAME = 512;
    public static final int MAX_VERTICES_PER_FRAME = 1_048_576;
    public static final long MAX_CPU_BYTES_PER_FRAME =
            (long) MAX_VERTICES_PER_FRAME * 3L * Float.BYTES;

    private final List<PrismTerrainMesh> meshes = new ArrayList<>();
    private final Set<MeshKey> keys = new HashSet<>();
    private long generation;
    private int vertexCount;
    private boolean frameOpen;

    /** Starts a new extraction generation and discards unconsumed prior-frame data. */
    public synchronized long beginFrame() {
        generation++;
        meshes.clear();
        keys.clear();
        vertexCount = 0;
        frameOpen = true;
        return generation;
    }

    public synchronized void submit(PrismTerrainMesh mesh) {
        Objects.requireNonNull(mesh, "mesh");
        if (!frameOpen) throw new IllegalStateException("beginFrame must be called before terrain submission");
        if (meshes.size() >= MAX_MESHES_PER_FRAME) {
            throw new IllegalStateException("Prism terrain mesh count limit exceeded");
        }
        if ((long) vertexCount + mesh.vertexCount() > MAX_VERTICES_PER_FRAME) {
            throw new IllegalStateException("Prism terrain frame vertex limit exceeded");
        }
        MeshKey key = new MeshKey(mesh.sectionX(), mesh.sectionY(), mesh.sectionZ(),
                mesh.materialId(), mesh.stage());
        if (!keys.add(key)) {
            throw new IllegalArgumentException("Duplicate terrain mesh for section/material/stage: " + key);
        }
        meshes.add(mesh);
        vertexCount += mesh.vertexCount();
    }

    /** Freezes and closes the current extraction generation for draw-phase consumption. */
    public synchronized PrismTerrainFrame publish() {
        if (!frameOpen) throw new IllegalStateException("No Prism terrain extraction frame is open");
        frameOpen = false;
        List<PrismTerrainMesh> ordered = meshes.stream()
                .sorted(PrismTerrainFrame.deterministicOrder()).toList();
        return new PrismTerrainFrame(generation, ordered, vertexCount);
    }

    public synchronized void clear() {
        meshes.clear();
        keys.clear();
        vertexCount = 0;
        frameOpen = false;
    }

    private record MeshKey(int x, int y, int z, String materialId, PrismFrameStage stage) {}
}
