package com.planetstudios.essentials.rendering.space;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Std140 payload for the Space Renderer per-section uniform block.
 *
 * <p>Terrain vertex positions are section-local, so this vec4 stores the
 * section's world-space origin minus the current camera position.</p>
 */
public final class SpaceRendererSectionUniformData {
    public static final int BYTE_SIZE = 16;

    private SpaceRendererSectionUniformData() {}

    /**
     * Encodes SectionOrigin as one std140 vec4 in native byte order.
     * The final component is reserved and set to zero.
     */
    public static ByteBuffer encode(int sectionX, int sectionY, int sectionZ,
            double cameraX, double cameraY, double cameraZ) {
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("Camera coordinates must be finite");
        }

        float originX = toFiniteFloat((double) sectionX * 16.0 - cameraX);
        float originY = toFiniteFloat((double) sectionY * 16.0 - cameraY);
        float originZ = toFiniteFloat((double) sectionZ * 16.0 - cameraZ);

        ByteBuffer buffer = ByteBuffer.allocateDirect(BYTE_SIZE).order(ByteOrder.nativeOrder());
        buffer.putFloat(originX);
        buffer.putFloat(originY);
        buffer.putFloat(originZ);
        buffer.putFloat(0.0f);
        return buffer.flip();
    }

    private static float toFiniteFloat(double value) {
        float converted = (float) value;
        if (!Float.isFinite(converted)) {
            throw new IllegalArgumentException("Section origin exceeds the supported float coordinate range");
        }
        return converted;
    }
}
