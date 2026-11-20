package com.planetstudios.essentials.textures;

/**
 * Conservative resource limits for the future texture decode pipeline.
 * These limits are preflight policy, not a promise that every accepted image
 * can be uploaded by every GPU.
 */
public record TextureDecodeLimits(int maxDimension, long maxPixels, long maxEncodedBytes) {
    public static final TextureDecodeLimits DEFAULT =
            new TextureDecodeLimits(8_192, 16_777_216L, 16L * 1024 * 1024);

    public TextureDecodeLimits {
        if (maxDimension < 1 || maxPixels < 1 || maxEncodedBytes < 33) {
            throw new IllegalArgumentException("Texture decode limits must be positive and permit a PNG header");
        }
    }

    public void validateDimensions(long width, long height) {
        if (width < 1 || height < 1 || width > maxDimension || height > maxDimension) {
            throw new IllegalArgumentException("PNG dimensions exceed configured limits");
        }
        if (width > maxPixels / height) {
            throw new IllegalArgumentException("PNG pixel count exceeds configured limit");
        }
    }

    public void validateEncodedLength(long encodedBytes) {
        if (encodedBytes < 1 || encodedBytes > maxEncodedBytes) {
            throw new IllegalArgumentException("Encoded PNG size exceeds configured limits");
        }
    }
}
