package com.planetstudios.essentials.rendering.shaders.client.terrain;

import java.util.List;
import java.util.Objects;

/** CPU-side section AABB culling for cooperating Prism terrain renderers. */
public final class PrismTerrainCulling {
    private PrismTerrainCulling() {}

    /** Filters a frame using section-sized world-space bounds, preserving frame order. */
    public static List<PrismTerrainMesh> visibleMeshes(PrismTerrainFrame frame, PrismTerrainFrustum frustum) {
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(frustum, "frustum");
        return frame.meshes().stream().filter(mesh -> {
            double minX = (double) mesh.sectionX() * 16.0;
            double minY = (double) mesh.sectionY() * 16.0;
            double minZ = (double) mesh.sectionZ() * 16.0;
            return frustum.intersects(minX, minY, minZ, minX + 16.0, minY + 16.0, minZ + 16.0);
        }).toList();
    }
}