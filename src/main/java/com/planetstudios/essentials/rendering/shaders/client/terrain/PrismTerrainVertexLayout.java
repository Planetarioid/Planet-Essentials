package com.planetstudios.essentials.rendering.shaders.client.terrain;

/** Versioned, tightly packed terrain vertex contract. */
public final class PrismTerrainVertexLayout {
    public static final int VERSION = 1;
    public static final int POSITION_OFFSET = 0;
    public static final int UV_OFFSET = 12;
    public static final int COLOR_RGBA8_OFFSET = 20;
    public static final int LIGHT_OFFSET = 24;
    public static final int OVERLAY_OFFSET = 28;
    public static final int NORMAL_OFFSET = 32 - Integer.BYTES;
    public static final int STRIDE = 32;
    private PrismTerrainVertexLayout() {}
    public static String fingerprint() {
        return "prism-terrain-v1:xyz_f32@0:uv_f32@12:rgba8@20:light_i32@24:overlay_i32@28:normal_i32@28";
    }
}
