package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;

/** Rebuilds vanilla-compatible far-to-near quad indices for captured translucent sections. */
public final class PrismTerrainTranslucentSorter {
    private static final int MAX_UNSIGNED_SHORT_QUADS = (0x1_0000 / 4);

    private PrismTerrainTranslucentSorter() {}

    public static short[] sortedIndices(PrismTerrainVertexData data,
            double cameraX, double cameraY, double cameraZ) {
        if (data == null || !data.hasQuadCenters()) {
            throw new IllegalArgumentException("Translucent terrain payload has no captured sort centers");
        }
        float[] centers = new float[Math.multiplyExact(data.quadCount(), 3)];
        for (int i = 0; i < data.quadCount(); i++) {
            centers[i * 3] = data.quadCenterX(i);
            centers[i * 3 + 1] = data.quadCenterY(i);
            centers[i * 3 + 2] = data.quadCenterZ(i);
        }
        return sortedIndices(centers, data.sectionX(), data.sectionY(), data.sectionZ(),
                cameraX, cameraY, cameraZ);
    }

    /** Sorts retained GPU-mesh centroid metadata without retaining the full CPU vertex payload. */
    public static short[] sortedIndices(float[] quadCenters, int sectionX, int sectionY, int sectionZ,
            double cameraX, double cameraY, double cameraZ) {
        if (quadCenters == null || quadCenters.length == 0 || quadCenters.length % 3 != 0) {
            throw new IllegalArgumentException("Translucent sort centers must contain complete XYZ triples");
        }
        int quadCount = quadCenters.length / 3;
        if (quadCount > MAX_UNSIGNED_SHORT_QUADS) {
            throw new IllegalArgumentException("Translucent mesh exceeds the unsigned-short index range");
        }
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("Translucent sort camera coordinates must be finite");
        }
        CompactVectorArray centers = new CompactVectorArray(quadCount);
        for (int i = 0; i < quadCenters.length; i += 3) {
            float x = quadCenters[i], y = quadCenters[i + 1], z = quadCenters[i + 2];
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("Translucent sort centers must be finite");
            }
            centers.set(i / 3, x, y, z);
        }
        float localCameraX = (float) (cameraX - (double) sectionX * 16.0);
        float localCameraY = (float) (cameraY - (double) sectionY * 16.0);
        float localCameraZ = (float) (cameraZ - (double) sectionZ * 16.0);
        if (!Float.isFinite(localCameraX) || !Float.isFinite(localCameraY) || !Float.isFinite(localCameraZ)) {
            throw new IllegalArgumentException("Translucent sort camera is outside the supported coordinate range");
        }
        int[] order = VertexSorting.byDistance(localCameraX, localCameraY, localCameraZ).sort(centers);
        short[] indices = new short[Math.multiplyExact(order.length, 6)];
        int cursor = 0;
        for (int quad : order) {
            int base = quad * 4;
            indices[cursor++] = (short) base;
            indices[cursor++] = (short) (base + 1);
            indices[cursor++] = (short) (base + 2);
            indices[cursor++] = (short) (base + 2);
            indices[cursor++] = (short) (base + 3);
            indices[cursor++] = (short) base;
        }
        return indices;
    }
}
