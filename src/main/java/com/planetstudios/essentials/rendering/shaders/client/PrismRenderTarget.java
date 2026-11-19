package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.planetstudios.essentials.rendering.shaders.RenderTargetSpec;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Owned off-screen color target with an optional depth attachment.
 *
 * Creation, render-pass creation, and destruction are render-thread operations.
 * The target owns its textures and views; callers own and must close each RenderPass.
 */
public final class PrismRenderTarget implements AutoCloseable {
    private final RenderTargetSpec spec;
    private final GpuTexture colorTexture;
    private final GpuTextureView colorView;
    private final GpuTexture depthTexture;
    private final GpuTextureView depthView;
    private boolean closed;

    private PrismRenderTarget(RenderTargetSpec spec, GpuTexture colorTexture,
            GpuTextureView colorView, GpuTexture depthTexture, GpuTextureView depthView) {
        this.spec = spec;
        this.colorTexture = colorTexture;
        this.colorView = colorView;
        this.depthTexture = depthTexture;
        this.depthView = depthView;
    }

    public static PrismRenderTarget create(GpuDevice device, String label, RenderTargetSpec spec) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(spec, "spec");
        spec.validateMaxTextureSize(device.getMaxTextureSize());

        GpuTexture color = null;
        GpuTextureView colorView = null;
        GpuTexture depth = null;
        GpuTextureView depthView = null;
        try {
            int colorUsage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING;
            color = device.createTexture(label + "-color", colorUsage,
                    TextureFormat.RGBA8, spec.width(), spec.height(), 1, 1);
            colorView = device.createTextureView(color);

            if (spec.depthAttachment()) {
                int depthUsage = GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING;
                depth = device.createTexture(label + "-depth", depthUsage,
                        TextureFormat.DEPTH32, spec.width(), spec.height(), 1, 1);
                depthView = device.createTextureView(depth);
            }
            return new PrismRenderTarget(spec, color, colorView, depth, depthView);
        } catch (RuntimeException | Error failure) {
            closeQuietly(depthView);
            closeQuietly(depth);
            closeQuietly(colorView);
            closeQuietly(color);
            throw failure;
        }
    }

    public RenderTargetSpec spec() {
        return spec;
    }

    public GpuTextureView colorView() {
        ensureOpen();
        return colorView;
    }

    public Optional<GpuTextureView> depthView() {
        ensureOpen();
        return Optional.ofNullable(depthView);
    }

    public RenderPass beginPass(CommandEncoder encoder, String label,
            OptionalInt clearColor, OptionalDouble clearDepth) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(clearColor, "clearColor");
        Objects.requireNonNull(clearDepth, "clearDepth");
        if (depthView == null) {
            if (clearDepth.isPresent()) {
                throw new IllegalArgumentException("Cannot clear depth on a target without a depth attachment");
            }
            return encoder.createRenderPass(() -> label, colorView, clearColor);
        }
        return encoder.createRenderPass(() -> label, colorView, clearColor, depthView, clearDepth);
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prism render target is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        closeQuietly(depthView);
        closeQuietly(depthTexture);
        closeQuietly(colorView);
        closeQuietly(colorTexture);
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource == null) return;
        try {
            resource.close();
        } catch (Exception ignored) {
            // Continue releasing the remaining owned GPU resources.
        }
    }
}
