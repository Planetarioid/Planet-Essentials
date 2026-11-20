package com.planetstudios.essentials.textures;

import java.util.Objects;

/** Immutable CPU-side RGBA texture pixels. Does not own a GPU resource. */
public final class DecodedTexture {
    private final int width;
    private final int height;
    private final int[] argb;

    public DecodedTexture(int width, int height, int[] argb) {
        if (width < 1 || height < 1 || (long) width * height != Objects.requireNonNull(argb, "argb").length) {
            throw new IllegalArgumentException("Pixel array does not match texture dimensions");
        }
        this.width = width;
        this.height = height;
        this.argb = argb.clone();
    }

    public int width() { return width; }
    public int height() { return height; }
    public long estimatedBytes() { return (long) argb.length * Integer.BYTES; }

    public int pixel(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IndexOutOfBoundsException("Pixel outside texture bounds");
        }
        return argb[y * width + x];
    }

    public int[] copyPixels() { return argb.clone(); }
}
