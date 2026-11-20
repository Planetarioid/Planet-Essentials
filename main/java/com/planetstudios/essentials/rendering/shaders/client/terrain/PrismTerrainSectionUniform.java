package com.planetstudios.essentials.rendering.shaders.client.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;

/**
 * Canonical std140 payload for Prism's per-section camera-relative origin.
 *
 * <p>The payload is one vec4 (16 bytes): xyz is the camera-relative section
 * origin and w is reserved as 1.0. A renderer adapter uploads this payload to
 * a uniform buffer and binds its slice using {@link #BINDING_NAME}.</p>
 *
 * <p>GPU allocation is deliberately left to the renderer adapter so the
 * payload can be uploaded through the active Minecraft device/command encoder
 * without introducing a second buffer-ownership system.</p>
 */
public final class PrismTerrainSectionUniform {
    public static final String BINDING_NAME = "PrismSection";
    public static final int BYTE_SIZE = 16;
    public static final int ORIGIN_X_OFFSET = 0;
    public static final int ORIGIN_Y_OFFSET = 4;
    public static final int ORIGIN_Z_OFFSET = 8;
    public static final int RESERVED_OFFSET = 12;

    private PrismTerrainSectionUniform() {}

    /** Returns a new native-order, direct std140 vec4 ready for GPU upload. */
    public static ByteBuffer encode(PrismTerrainCameraTransform transform) {
        Objects.requireNonNull(transform, "transform");
        ByteBuffer data = ByteBuffer.allocateDirect(BYTE_SIZE).order(ByteOrder.nativeOrder());
        data.putFloat(transform.originX());
        data.putFloat(transform.originY());
        data.putFloat(transform.originZ());
        data.putFloat(1.0f);
        data.flip();
        return data;
    }

    /** Ensures a compiled terrain program declares the required section-origin UBO. */
    public static void requireDeclared(ProgramBindings programBindings) {
        Objects.requireNonNull(programBindings, "programBindings");
        if (!programBindings.uniformBuffers().contains(BINDING_NAME)) {
            throw new IllegalArgumentException(
                    "Terrain program must declare the " + BINDING_NAME + " uniform buffer");
        }
    }

    /** Narrow view of a compiled program's immutable binding declaration. */
    @FunctionalInterface
    public interface ProgramBindings {
        List<String> uniformBuffers();
    }
}
