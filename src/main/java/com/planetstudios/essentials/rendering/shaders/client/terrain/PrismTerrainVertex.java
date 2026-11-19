package com.planetstudios.essentials.rendering.shaders.client.terrain;

/** One complete Prism terrain vertex in section-local coordinates. */
public record PrismTerrainVertex(float x, float y, float z, float u, float v,
        int colorRgba8, int packedLight, int packedOverlay, int packedNormal) {
    public PrismTerrainVertex {
        checkPosition(x, "x"); checkPosition(y, "y"); checkPosition(z, "z");
        if (!Float.isFinite(u) || !Float.isFinite(v))
            throw new IllegalArgumentException("Terrain UV coordinates must be finite");
    }
    private static void checkPosition(float value, String axis) {
        if (!Float.isFinite(value) || value < 0.0f || value > 16.0f)
            throw new IllegalArgumentException("Section-local " + axis + " coordinate must be finite and within [0, 16]");
    }
}
