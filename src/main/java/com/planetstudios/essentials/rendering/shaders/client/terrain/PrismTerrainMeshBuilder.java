package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import java.util.Arrays;
import java.util.Objects;

/** Bounded builder for section-local Prism procedural terrain quads. */
public final class PrismTerrainMeshBuilder {
    private final int sectionX, sectionY, sectionZ;
    private final String materialId;
    private final PrismFrameStage stage;
    private float[] positions = new float[12 * 16];
    private int size;
    private boolean built;

    public PrismTerrainMeshBuilder(int sectionX, int sectionY, int sectionZ,
            String materialId, PrismFrameStage stage) {
        this.sectionX = sectionX;
        this.sectionY = sectionY;
        this.sectionZ = sectionZ;
        this.materialId = Objects.requireNonNull(materialId, "materialId");
        this.stage = Objects.requireNonNull(stage, "stage");
        if (stage != PrismFrameStage.BEFORE_ENTITIES && stage != PrismFrameStage.BEFORE_TRANSLUCENT) {
            throw new IllegalArgumentException("Unsupported terrain stage: " + stage);
        }
    }

    /** Adds one quad as four XYZ vertices in section-local coordinates. */
    public PrismTerrainMeshBuilder addQuad(
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        ensureMutable();
        float[] quad = {x0,y0,z0, x1,y1,z1, x2,y2,z2, x3,y3,z3};
        for (float value : quad) {
            if (!Float.isFinite(value) || value < 0.0f || value > 16.0f) {
                throw new IllegalArgumentException("Section-local terrain coordinates must be finite and within [0, 16]");
            }
        }
        ensureCapacity(size + quad.length);
        System.arraycopy(quad, 0, positions, size, quad.length);
        size += quad.length;
        return this;
    }

    private void ensureCapacity(int required) {
        if (required / 3 > PrismTerrainMesh.MAX_VERTICES) {
            throw new IllegalStateException("Terrain mesh vertex limit exceeded");
        }
        if (required > positions.length) {
            positions = Arrays.copyOf(positions, Math.min(
                    PrismTerrainMesh.MAX_VERTICES * 3,
                    Math.max(required, positions.length * 2)));
        }
    }

    private void ensureMutable() {
        if (built) throw new IllegalStateException("Terrain mesh builder has already been built");
    }

    public PrismTerrainMesh build() {
        ensureMutable();
        if (size == 0) throw new IllegalStateException("Cannot build an empty terrain mesh");
        built = true;
        return new PrismTerrainMesh(sectionX, sectionY, sectionZ, materialId,
                stage, Arrays.copyOf(positions, size));
    }
}
