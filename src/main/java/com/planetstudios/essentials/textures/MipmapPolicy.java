package com.planetstudios.essentials.textures;

/**
 * CPU mip-chain policy. maxLevels includes level zero (the original image).
 * maxGeneratedBytes bounds only additional levels, not the source image.
 */
public record MipmapPolicy(Mode mode, int maxLevels, long maxGeneratedBytes) {
    public static final MipmapPolicy DEFAULT =
            new MipmapPolicy(Mode.BOX_LINEAR_SRGB, 6, 32L * 1024 * 1024);
    public static final MipmapPolicy DISABLED =
            new MipmapPolicy(Mode.NONE, 1, 0);

    public MipmapPolicy {
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        if (maxLevels < 1 || maxGeneratedBytes < 0) {
            throw new IllegalArgumentException("Invalid mipmap limits");
        }
        if (mode == Mode.NONE && maxLevels != 1) {
            throw new IllegalArgumentException("NONE mode must contain only the base level");
        }
    }

    public enum Mode {
        /** No derived levels. Useful for pixel art and data textures. */
        NONE,
        /** Area-weighted, alpha-premultiplied filtering in linear-light sRGB. */
        BOX_LINEAR_SRGB,
        /** Nearest source sample. Preserves hard pixel boundaries but can alias. */
        NEAREST
    }
}
