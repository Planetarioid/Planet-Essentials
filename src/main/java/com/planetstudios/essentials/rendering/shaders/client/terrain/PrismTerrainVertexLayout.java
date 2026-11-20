package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Versioned 36-byte Prism terrain vertex contract.
 *
 * <p>Attribute order deliberately matches the serializer:
 * position, UV0, color, light (UV2), overlay (UV1), normal, one byte padding.</p>
 */
public final class PrismTerrainVertexLayout {
    public static final int VERSION = 1;
    public static final int POSITION_OFFSET = 0;
    public static final int UV_OFFSET = 12;
    public static final int COLOR_RGBA8_OFFSET = 20;
    public static final int LIGHT_OFFSET = 24;
    public static final int OVERLAY_OFFSET = 28;
    public static final int NORMAL_OFFSET = 32;
    public static final int STRIDE = 36;

    private static final VertexFormat MINECRAFT_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV2", VertexFormatElement.UV2)
            .add("UV1", VertexFormatElement.UV1)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .build();

    private PrismTerrainVertexLayout() {}

    /** Minecraft 1.21.11 format matching the serialized payload byte-for-byte. */
    public static VertexFormat minecraftVertexFormat() {
        if (MINECRAFT_FORMAT.getVertexSize() != STRIDE) {
            throw new IllegalStateException("Prism terrain V1 layout mismatch: expected "
                    + STRIDE + " bytes, got " + MINECRAFT_FORMAT.getVertexSize());
        }
        return MINECRAFT_FORMAT;
    }

    public static String fingerprint() {
        return "prism-terrain-v1:xyz_f32@0:uv_f32@12:rgba8@20:light_uv2@24:overlay_uv1@28:normal_i8x3@32:pad1@35:stride36";
    }
}
