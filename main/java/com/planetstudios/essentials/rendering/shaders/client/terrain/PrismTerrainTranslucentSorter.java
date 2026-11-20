package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;

/** Rebuilds vanilla-compatible far-to-near quad indices for captured translucent sections. */
public final class PrismTerrainTranslucentSorter {
    private PrismTerrainTranslucentSorter() {}

    public static short[] sortedIndices(PrismTerrainVertexData data,
            double cameraX, double cameraY, double cameraZ) {
        if (data == null || !data.hasQuadCenters()) {
            throw new IllegalArgumentException("Translucent terrain payload has no captured sort centers");
        }
        CompactVectorArray centers = new CompactVectorArray(data.quadCount());
        for (int i = 0; i < data.quadCount(); i++) {
            centers.set(i, data.quadCenterX(i), data.quadCenterY(i), data.quadCenterZ(i));
        }
        float localCameraX = (float) (cameraX - (double) data.sectionX() * 16.0);
        float localCameraY = (float) (cameraY - (double) data.sectionY() * 16.0);
        float localCameraZ = (float) (cameraZ - (double) data.sectionZ() * 16.0);
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
