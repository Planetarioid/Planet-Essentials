package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.util.Objects;

/** Immutable section-local position mesh for procedural terrain integrations. */
public final class PrismTerrainMesh {
    public static final int MAX_VERTICES = 65_536;
    private final int sectionX, sectionY, sectionZ;
    private final String materialId;
    private final PrismFrameStage stage;
    private final float[] positions;

    PrismTerrainMesh(int sectionX, int sectionY, int sectionZ, String materialId,
            PrismFrameStage stage, float[] positions) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        this.materialId = requireId(materialId);
        this.stage = Objects.requireNonNull(stage, "stage");
        if (stage != PrismFrameStage.BEFORE_ENTITIES && stage != PrismFrameStage.BEFORE_TRANSLUCENT) {
            throw new IllegalArgumentException("Terrain meshes must target BEFORE_ENTITIES or BEFORE_TRANSLUCENT");
        }
        Objects.requireNonNull(positions, "positions");
        if (positions.length == 0 || positions.length % 12 != 0) {
            throw new IllegalArgumentException("Terrain mesh must contain complete four-vertex quads");
        }
        if (positions.length / 3 > MAX_VERTICES) {
            throw new IllegalArgumentException("Terrain mesh exceeds " + MAX_VERTICES + " vertices");
        }
        this.positions = positions.clone();
        for (int i = 0; i < this.positions.length; i++) {
            float value = this.positions[i];
            if (!Float.isFinite(value) || value < 0.0f || value > 16.0f) {
                throw new IllegalArgumentException("Section-local vertex coordinate out of range at index " + i);
            }
        }
    }

    private static String requireId(String id) {
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || id.contains("..") || id.contains("//")) {
            throw new IllegalArgumentException("Invalid Prism terrain material ID: " + id);
        }
        return id;
    }

    public int sectionX() { return sectionX; }
    public int sectionY() { return sectionY; }
    public int sectionZ() { return sectionZ; }
    public String materialId() { return materialId; }
    public PrismFrameStage stage() { return stage; }
    public int vertexCount() { return positions.length / 3; }
    public int quadCount() { return positions.length / 12; }
    public float[] positions() { return positions.clone(); }
    public long estimatedCpuBytes() { return (long) positions.length * Float.BYTES; }
}
