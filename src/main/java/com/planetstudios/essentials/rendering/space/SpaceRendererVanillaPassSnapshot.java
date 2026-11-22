package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Captures the live Minecraft terrain group's target, texture resources, and
 * currently available default-uniform resources.
 *
 * <p>This is a resource snapshot, not a claim of complete vanilla render-state
 * parity. It does not capture or replay viewport, scissor, blend, depth, or
 * per-layer pipeline state. The caller owns any pass opened from it.</p>
 */
public record SpaceRendererVanillaPassSnapshot(
        ChunkSectionLayerGroup group,
        RenderTarget target,
        GpuTextureView colorAttachment,
        Optional<GpuTextureView> depthAttachment,
        int targetWidth,
        int targetHeight,
        GpuTextureView blockAtlas,
        GpuSampler blockAtlasSampler,
        GpuTextureView lightmap,
        GpuSampler lightmapSampler,
        List<ChunkSectionLayer> layers,
        Matrix4f modelView,
        DefaultUniformInputs defaultUniforms,
        GpuDevice device) {

    public SpaceRendererVanillaPassSnapshot {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(colorAttachment, "colorAttachment");
        Objects.requireNonNull(depthAttachment, "depthAttachment");
        Objects.requireNonNull(blockAtlas, "blockAtlas");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        Objects.requireNonNull(lightmap, "lightmap");
        Objects.requireNonNull(lightmapSampler, "lightmapSampler");
        Objects.requireNonNull(layers, "layers");
        Objects.requireNonNull(modelView, "modelView");
        Objects.requireNonNull(defaultUniforms, "defaultUniforms");
        Objects.requireNonNull(device, "device");
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Terrain target dimensions must be positive");
        }
        layers = List.copyOf(layers);
        if (layers.isEmpty() || !layers.equals(List.of(group.layers()))) {
            throw new IllegalArgumentException("Captured layers must match the native render-group order");
        }
        modelView = new Matrix4f(modelView);
    }

    @Override
    public Matrix4f modelView() {
        return new Matrix4f(modelView);
    }

    /**
     * Captures resources available at the native renderGroup boundary.
     * Must run on the render thread while the supplied group is current.
     */
    public static SpaceRendererVanillaPassSnapshot capture(
            ChunkSectionsToRender input,
            ChunkSectionLayerGroup group,
            GpuSampler blockAtlasSampler) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        RenderSystem.assertOnRenderThread();

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = Objects.requireNonNull(group.outputTarget(), "terrain group output target");
        GpuTextureView color = Objects.requireNonNull(
                target.getColorTextureView(), "terrain color attachment");
        GpuTextureView lightmap = Objects.requireNonNull(
                minecraft.gameRenderer.lightTexture().getTextureView(), "terrain lightmap");
        GpuSampler lightmapSampler = RenderSystem.getSamplerCache()
                .getClampToEdge(FilterMode.LINEAR);
        List<ChunkSectionLayer> layers = List.of(group.layers());

        return new SpaceRendererVanillaPassSnapshot(
                group,
                target,
                color,
                Optional.ofNullable(target.getDepthTextureView()),
                target.width,
                target.height,
                Objects.requireNonNull(input.textureView(), "terrain block atlas"),
                blockAtlasSampler,
                lightmap,
                lightmapSampler,
                layers,
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                new DefaultUniformInputs(
                        RenderSystem.getProjectionMatrixBuffer(),
                        RenderSystem.getShaderFog(),
                        RenderSystem.getGlobalSettingsUniform(),
                        RenderSystem.getShaderLights()),
                Objects.requireNonNull(RenderSystem.getDevice(), "active GPU device"));
    }

    /**
     * Opens a no-clear pass on the captured attachments. This is suitable for
     * an additive experiment only; it does not by itself reproduce every
     * vanilla pass-state setting. Never open one pass per layer in a multi-layer
     * group because attachment clear operations could erase earlier draws.
     */
    public RenderPass openPass(GpuDevice currentDevice, String debugLabel) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(currentDevice, "currentDevice");
        if (debugLabel == null || debugLabel.isBlank()) {
            throw new IllegalArgumentException("Render-pass debug label cannot be blank");
        }
        validateCurrent(currentDevice);
        return currentDevice.createCommandEncoder().createRenderPass(
                () -> debugLabel,
                colorAttachment,
                java.util.OptionalInt.empty(),
                depthAttachment.orElse(null),
                java.util.OptionalDouble.empty());
    }

    /** Rejects resized/recreated targets and GPU-device changes after capture. */
    public void validateCurrent(GpuDevice currentDevice) {
        RenderSystem.assertOnRenderThread();
        if (currentDevice != device || RenderSystem.getDevice() != device) {
            throw new IllegalStateException("Captured terrain group belongs to a different GPU device");
        }
        if (target.width != targetWidth || target.height != targetHeight
                || target.getColorTextureView() != colorAttachment
                || target.getDepthTextureView() != depthAttachment.orElse(null)) {
            throw new IllegalStateException("Captured terrain target changed after the group snapshot");
        }
    }

    /** Nullable resources are retained explicitly rather than replaced with guessed defaults. */
    public record DefaultUniformInputs(
            GpuBufferSlice projection,
            GpuBufferSlice fog,
            GpuBuffer globals,
            GpuBufferSlice lighting) {

        public Set<String> availableNames() {
            HashSet<String> names = new HashSet<>();
            if (projection != null) names.add("Projection");
            if (fog != null) names.add("Fog");
            if (globals != null) names.add("Globals");
            if (lighting != null) names.add("Lighting");
            return Set.copyOf(names);
        }

        /** Rebinds exactly the default-uniform resources captured in this snapshot. */
        public Set<String> bindTo(RenderPass pass) {
            Objects.requireNonNull(pass, "pass");
            HashSet<String> names = new HashSet<>();
            if (projection != null) {
                pass.setUniform("Projection", projection);
                names.add("Projection");
            }
            if (fog != null) {
                pass.setUniform("Fog", fog);
                names.add("Fog");
            }
            if (globals != null) {
                pass.setUniform("Globals", globals);
                names.add("Globals");
            }
            if (lighting != null) {
                pass.setUniform("Lighting", lighting);
                names.add("Lighting");
            }
            return Set.copyOf(names);
        }
    }
}
