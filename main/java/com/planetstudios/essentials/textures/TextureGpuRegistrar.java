package com.planetstudios.essentials.textures;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Explicit, opt-in bridge from immutable CPU pixels to Minecraft-managed GPU textures.
 *
 * <p>CPU mip generation and NativeImage preparation happen before dispatch.
 * GPU allocation, mip uploads, sampler creation, TextureManager registration, and
 * GPU destruction are dispatched to the client lifecycle. Vanilla texture loading
 * and atlas ownership are never intercepted.</p>
 */
public final class TextureGpuRegistrar {
    private static final String TEXTURE_NAMESPACE = "planet_essentials";
    private static final String TEXTURE_PATH_PREFIX = "decoded/";
    private static final Set<Identifier> OWNED_TEXTURES = ConcurrentHashMap.newKeySet();
    private static final Set<PendingRegistration> PENDING = ConcurrentHashMap.newKeySet();
    private static final TextureRegistrationLifecycle LIFECYCLE =
            new TextureRegistrationLifecycle();

    private TextureGpuRegistrar() {}

    public static CompletableFuture<Identifier> register(
            Minecraft client, Identifier sourceId, DecodedTexture decoded) {
        return register(client, sourceId, decoded, MipmapPolicy.DEFAULT);
    }

    public static CompletableFuture<Identifier> register(
            Minecraft client, Identifier sourceId, DecodedTexture decoded, MipmapPolicy mipmapPolicy) {
        if (client == null || sourceId == null || decoded == null || mipmapPolicy == null) {
            return CompletableFuture.failedFuture(
                    new NullPointerException("client, sourceId, decoded, and mipmapPolicy are required"));
        }

        final long requestedGeneration;
        try {
            requestedGeneration = LIFECYCLE.beginRegistration();
        } catch (IllegalStateException closed) {
            return CompletableFuture.failedFuture(closed);
        }

        List<NativeImage> images = new ArrayList<>();
        try {
            List<DecodedTexture> levels = TextureMipChain.generate(decoded, mipmapPolicy);
            for (DecodedTexture level : levels) {
                images.add(toNativeImage(level));
            }
        } catch (RuntimeException failure) {
            closeImages(images);
            return CompletableFuture.failedFuture(failure);
        }

        PendingRegistration registration = new PendingRegistration(
                client, sourceId, decoded, identifierFor(sourceId), requestedGeneration, images);
        PENDING.add(registration);
        try {
            client.execute(registration);
        } catch (RuntimeException rejected) {
            registration.cancel("Minecraft client executor rejected texture registration");
        }
        return registration.result;
    }

    /**
     * Owns the CPU-side upload buffers until either the queued render task finishes
     * or reload/shutdown cancels it. Synchronization makes cleanup idempotent and
     * prevents cancellation from closing NativeImages while an upload is using them.
     */
    private static final class PendingRegistration implements Runnable {
        private final Minecraft client;
        private final Identifier sourceId;
        private final DecodedTexture decoded;
        private final Identifier textureId;
        private final long requestedGeneration;
        private final List<NativeImage> images;
        private final CompletableFuture<Identifier> result = new CompletableFuture<>();
        private final AtomicBoolean imagesClosed = new AtomicBoolean();
        private boolean cancelled;

        private PendingRegistration(
                Minecraft client,
                Identifier sourceId,
                DecodedTexture decoded,
                Identifier textureId,
                long requestedGeneration,
                List<NativeImage> images) {
            this.client = client;
            this.sourceId = sourceId;
            this.decoded = decoded;
            this.textureId = textureId;
            this.requestedGeneration = requestedGeneration;
            this.images = images;
        }

        @Override
        public synchronized void run() {
            if (cancelled) return;
            if (!LIFECYCLE.isCurrent(requestedGeneration)) {
                cancel("Texture registration invalidated by reload or shutdown");
                return;
            }

            GpuTexture texture = null;
            GpuTextureView view = null;
            GpuSampler sampler = null;
            MipmappedGpuTexture managedTexture = null;
            try {
                RenderSystem.assertOnRenderThread();
                var device = RenderSystem.getDevice();
                int mipLevels = images.size();
                texture = device.createTexture(
                        () -> "Planet Essentials mipmapped " + sourceId,
                        GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                        TextureFormat.RGBA8,
                        decoded.width(),
                        decoded.height(),
                        1,
                        mipLevels
                );

                CommandEncoder encoder = device.createCommandEncoder();
                for (int level = 0; level < mipLevels; level++) {
                    NativeImage image = images.get(level);
                    encoder.writeToTexture(texture, image, level, 0,
                            0, 0, image.getWidth(), image.getHeight(), 0, 0);
                }

                view = device.createTextureView(texture);
                sampler = device.createSampler(
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        FilterMode.LINEAR,
                        FilterMode.LINEAR,
                        1,
                        OptionalDouble.of(Math.max(0, mipLevels - 1))
                );
                managedTexture = new MipmappedGpuTexture(texture, view, sampler);
                client.getTextureManager().register(textureId, managedTexture);
                OWNED_TEXTURES.add(textureId);
                result.complete(textureId);
            } catch (RuntimeException failure) {
                if (managedTexture != null) {
                    managedTexture.close();
                } else {
                    if (sampler != null) sampler.close();
                    if (view != null) view.close();
                    if (texture != null) texture.close();
                }
                result.completeExceptionally(failure);
            } finally {
                closeImagesOnce();
                PENDING.remove(this);
            }
        }

        private synchronized void cancel(String reason) {
            if (cancelled || result.isDone()) return;
            cancelled = true;
            closeImagesOnce();
            PENDING.remove(this);
            result.completeExceptionally(new IllegalStateException(reason));
        }

        private void closeImagesOnce() {
            if (imagesClosed.compareAndSet(false, true)) {
                closeImages(images);
            }
        }
    }

    private static NativeImage toNativeImage(DecodedTexture decoded) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                decoded.width(), decoded.height(), false);
        try {
            int[] pixels = decoded.copyPixels();
            for (int y = 0; y < decoded.height(); y++) {
                int row = y * decoded.width();
                for (int x = 0; x < decoded.width(); x++) {
                    image.setPixelABGR(x, y, argbToAbgr(pixels[row + x]));
                }
            }
            return image;
        } catch (RuntimeException failure) {
            image.close();
            throw failure;
        }
    }

    private static void closeImages(List<NativeImage> images) {
        for (NativeImage image : images) {
            image.close();
        }
        images.clear();
    }

    /** Invalidates queued registrations and releases only textures owned by this adapter. */
    public static void releaseAll(Minecraft client) {
        LIFECYCLE.invalidatePending();
        for (PendingRegistration registration : PENDING.toArray(PendingRegistration[]::new)) {
            registration.cancel("Texture registration invalidated by resource reload");
        }
        releaseOwned(client);
    }

    /** Permanently closes this adapter during client shutdown. */
    public static void shutdown(Minecraft client) {
        LIFECYCLE.close();
        for (PendingRegistration registration : PENDING.toArray(PendingRegistration[]::new)) {
            registration.cancel("Texture registrar closed during client shutdown");
        }
        releaseOwned(client);
    }

    private static void releaseOwned(Minecraft client) {
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
