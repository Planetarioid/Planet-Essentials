package com.planetstudios.essentials.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explicit, opt-in bridge from immutable CPU pixels to Minecraft-managed GPU textures.
 *
 * Native pixel allocation/fill happens on the caller (normally decode worker) thread.
 * DynamicTexture construction, TextureManager registration, and release are dispatched
 * to the Minecraft client thread. The manager owns registered DynamicTexture instances.
 */
public final class TextureGpuRegistrar {
    private static final String TEXTURE_NAMESPACE = "planet_essentials";
    private static final String TEXTURE_PATH_PREFIX = "decoded/";
    private static final Set<Identifier> OWNED_TEXTURES = ConcurrentHashMap.newKeySet();

    private TextureGpuRegistrar() {}

    public static CompletableFuture<Identifier> register(
            Minecraft client, Identifier sourceId, DecodedTexture decoded) {
        if (client == null || sourceId == null || decoded == null) {
            return CompletableFuture.failedFuture(
                    new NullPointerException("client, sourceId, and decoded are required"));
        }

        final NativeImage image;
        try {
            image = new NativeImage(NativeImage.Format.RGBA,
                    decoded.width(), decoded.height(), false);
            int[] pixels = decoded.copyPixels();
            for (int y = 0; y < decoded.height(); y++) {
                int row = y * decoded.width();
                for (int x = 0; x < decoded.width(); x++) {
                    image.setPixelABGR(x, y, argbToAbgr(pixels[row + x]));
                }
            }
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }

        Identifier textureId = identifierFor(sourceId);
        CompletableFuture<Identifier> result = new CompletableFuture<>();
        try {
            client.execute(() -> {
                try {
                    DynamicTexture texture = new DynamicTexture(
                            () -> "Planet Essentials decoded " + sourceId, image);
                    client.getTextureManager().register(textureId, texture);
                    OWNED_TEXTURES.add(textureId);
                    result.complete(textureId);
                } catch (RuntimeException failure) {
                    image.close();
                    result.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException rejected) {
            image.close();
            result.completeExceptionally(rejected);
        }
        return result;
    }

    /**
     * Releases only identifiers registered by this adapter. It never releases or
     * replaces a vanilla/resource-pack identifier.
     */
    public static void releaseAll(Minecraft client) {
        if (client == null || OWNED_TEXTURES.isEmpty()) {
            return;
        }
        Runnable release = () -> {
            for (Identifier id : OWNED_TEXTURES) {
                client.getTextureManager().release(id);
            }
            OWNED_TEXTURES.clear();
        };
        if (client.isSameThread()) {
            release.run();
        } else {
            client.execute(release);
        }
    }

    static Identifier identifierFor(Identifier sourceId) {
        return Identifier.fromNamespaceAndPath(
                TEXTURE_NAMESPACE, TEXTURE_PATH_PREFIX + sha256(sourceId.toString()));
    }

    static int argbToAbgr(int argb) {
        return (argb & 0xFF00FF00)
                | ((argb & 0x00FF0000) >>> 16)
                | ((argb & 0x000000FF) << 16);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
