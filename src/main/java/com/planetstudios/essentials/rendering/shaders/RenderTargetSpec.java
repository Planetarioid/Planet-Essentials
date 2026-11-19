package com.planetstudios.essentials.rendering.shaders;

/** Validated dimensions and attachment requirements for a Prism render target. */
public record RenderTargetSpec(int width, int height, boolean depthAttachment) {
    public static final long MAX_PIXELS = 16_777_216L;

    public RenderTargetSpec {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Render-target dimensions must be positive");
        }
        if ((long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("Render target exceeds the Prism pixel budget");
        }
    }

    public void validateMaxTextureSize(int maxTextureSize) {
        if (maxTextureSize <= 0 || width > maxTextureSize || height > maxTextureSize) {
            throw new IllegalArgumentException("Render target exceeds the GPU's maximum texture dimensions");
        }
    }
}
