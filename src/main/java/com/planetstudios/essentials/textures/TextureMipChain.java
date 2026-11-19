package com.planetstudios.essentials.textures;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic CPU mip-chain generation. It does not allocate GPU resources.
 *
 * Generated levels use floor-halved dimensions, matching conventional GPU mip
 * dimensions. Area resampling ensures odd-sized source edges still contribute.
 */
public final class TextureMipChain {
    private TextureMipChain() {}

    public static List<DecodedTexture> generate(DecodedTexture base, MipmapPolicy policy) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(policy, "policy");

        List<DecodedTexture> levels = new ArrayList<>();
        levels.add(base);
        if (policy.mode() == MipmapPolicy.Mode.NONE) {
            return List.copyOf(levels);
        }

        long generatedBytes = 0;
        DecodedTexture previous = base;
        while (levels.size() < policy.maxLevels()
                && (previous.width() > 1 || previous.height() > 1)) {
            int width = Math.max(1, previous.width() / 2);
            int height = Math.max(1, previous.height() / 2);
            long nextBytes = (long) width * height * Integer.BYTES;
            if (nextBytes > policy.maxGeneratedBytes() - generatedBytes) {
                break;
            }

            int[] pixels = policy.mode() == MipmapPolicy.Mode.NEAREST
                    ? downsampleNearest(previous, width, height)
                    : downsampleLinearSrgb(previous, width, height);
            DecodedTexture next = new DecodedTexture(width, height, pixels);
            levels.add(next);
            generatedBytes += nextBytes;
            previous = next;
        }
        return List.copyOf(levels);
    }

    private static int[] downsampleNearest(DecodedTexture source, int width, int height) {
        int[] output = new int[width * height];
        for (int y = 0; y < height; y++) {
            int sy = Math.min(source.height() - 1,
                    (int) (((long) (2 * y + 1) * source.height()) / (2L * height)));
            for (int x = 0; x < width; x++) {
                int sx = Math.min(source.width() - 1,
                        (int) (((long) (2 * x + 1) * source.width()) / (2L * width)));
                output[y * width + x] = source.pixel(sx, sy);
            }
        }
        return output;
    }

    private static int[] downsampleLinearSrgb(DecodedTexture source, int width, int height) {
        int[] output = new int[width * height];
        for (int y = 0; y < height; y++) {
            double top = (double) y * source.height() / height;
            double bottom = (double) (y + 1) * source.height() / height;
            int firstY = (int) Math.floor(top);
            int lastY = Math.min(source.height() - 1, (int) Math.ceil(bottom) - 1);
            for (int x = 0; x < width; x++) {
                double left = (double) x * source.width() / width;
                double right = (double) (x + 1) * source.width() / width;
                int firstX = (int) Math.floor(left);
                int lastX = Math.min(source.width() - 1, (int) Math.ceil(right) - 1);

                double totalWeight = 0;
                double alphaSum = 0;
                double redSum = 0;
                double greenSum = 0;
                double blueSum = 0;

                for (int sy = firstY; sy <= lastY; sy++) {
                    double wy = Math.max(0, Math.min(bottom, sy + 1.0) - Math.max(top, sy));
                    for (int sx = firstX; sx <= lastX; sx++) {
                        double wx = Math.max(0, Math.min(right, sx + 1.0) - Math.max(left, sx));
                        double weight = wx * wy;
                        int argb = source.pixel(sx, sy);
                        double alpha = ((argb >>> 24) & 0xFF) / 255.0;
                        totalWeight += weight;
                        alphaSum += alpha * weight;
                        redSum += toLinear((argb >>> 16) & 0xFF) * alpha * weight;
                        greenSum += toLinear((argb >>> 8) & 0xFF) * alpha * weight;
                        blueSum += toLinear(argb & 0xFF) * alpha * weight;
                    }
                }

                double alpha = totalWeight == 0 ? 0 : alphaSum / totalWeight;
                int a = toByte(alpha);
                int r = alphaSum == 0 ? 0 : toSrgb(redSum / alphaSum);
                int g = alphaSum == 0 ? 0 : toSrgb(greenSum / alphaSum);
                int b = alphaSum == 0 ? 0 : toSrgb(blueSum / alphaSum);
                output[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return output;
    }

    private static double toLinear(int srgbByte) {
        double value = srgbByte / 255.0;
        return value <= 0.04045 ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int toSrgb(double linear) {
        double value = Math.max(0, Math.min(1, linear));
        double srgb = value <= 0.0031308 ? value * 12.92
                : 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
        return toByte(srgb);
    }

    private static int toByte(double normalized) {
        return (int) Math.round(Math.max(0, Math.min(1, normalized)) * 255.0);
    }
}
