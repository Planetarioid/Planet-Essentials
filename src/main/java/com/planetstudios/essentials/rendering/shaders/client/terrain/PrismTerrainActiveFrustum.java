package com.planetstudios.essentials.rendering.shaders.client.terrain;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

/**
 * Immutable snapshot of Minecraft's active per-frame culling frustum.
 * A copy is retained because LevelRenderer reuses and mutates its own Frustum.
 */
public final class PrismTerrainActiveFrustum {
    private static volatile Frustum active;

    private PrismTerrainActiveFrustum() {}

    public static void publish(Frustum frustum) {
        active = new Frustum(Objects.requireNonNull(frustum, "frustum"));
    }

    public static void clear() {
        active = null;
    }

    /** Fails open until LevelRenderer has supplied a current-frame frustum. */
    public static boolean isVisible(int sectionX, int sectionY, int sectionZ) {
        Frustum snapshot = active;
        if (snapshot == null) return true;
        double x = (double) sectionX * 16.0;
        double y = (double) sectionY * 16.0;
        double z = (double) sectionZ * 16.0;
        return snapshot.isVisible(new AABB(x, y, z, x + 16.0, y + 16.0, z + 16.0));
    }
}
