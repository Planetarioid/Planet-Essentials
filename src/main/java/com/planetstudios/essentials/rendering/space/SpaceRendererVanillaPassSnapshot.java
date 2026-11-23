package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.ScissorState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Captures the live Minecraft terrain group's target, texture resources, and
 * currently available default-uniform resources, each layer's native
 * fixed-function pipeline configuration, and the render-type scissor state.
 *
 * <p>This is a capture snapshot, not a claim of complete vanilla render-state
 * parity. It replays the captured scissor rectangle but does not yet replay
 * viewport or all backend state. The caller owns any pass opened from it.</p>
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
        Map<ChunkSectionLayer, LayerPipelineInput> layerPipelines,
        boolean wireframe,
        Matrix4f modelView,
        DefaultUniformInputs defaultUniforms,
        ScissorInput scissor,
        ViewportInput viewport,
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
        Objects.requireNonNull(layerPipelines, "layerPipelines");
        Objects.requireNonNull(modelView, "modelView");
        Objects.requireNonNull(defaultUniforms, "defaultUniforms");
        Objects.requireNonNull(scissor, "scissor");
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(device, "device");
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Terrain target dimensions must be positive");
        }
        layers = List.copyOf(layers);
        if (layers.isEmpty() || !layers.equals(List.of(group.layers()))) {
            throw new IllegalArgumentException("Captured layers must match the native render-group order");
        }
        EnumMap<ChunkSectionLayer, LayerPipelineInput> pipelineCopy = new EnumMap<>(ChunkSectionLayer.class);
        layerPipelines.forEach((layer, input) -> pipelineCopy.put(
                Objects.requireNonNull(layer, "layer"), Objects.requireNonNull(input, "layer pipeline input")));
        if (!pipelineCopy.keySet().equals(new java.util.HashSet<>(layers))) {
            throw new IllegalArgumentException("Captured pipeline states must cover exactly the native render-group layers");
        }
        layerPipelines = Collections.unmodifiableMap(pipelineCopy);
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
        boolean wireframe = SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;
        EnumMap<ChunkSectionLayer, LayerPipelineInput> layerPipelines =
                new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : layers) {
            List<?> draws = input.drawsPerLayer().get(layer);
            RenderPipeline pipeline = wireframe ? RenderPipelines.WIREFRAME : layer.pipeline();
            boolean applied = draws != null && !draws.isEmpty();
            layerPipelines.put(layer, new LayerPipelineInput(
                    pipeline, applied ? java.util.Optional.of(PipelineState.capture(pipeline)) : java.util.Optional.empty(),
                    applied));
        }

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
                layerPipelines,
                wireframe,
                new Matrix4f(RenderSystem.getModelViewMatrix()),
                new DefaultUniformInputs(
                        RenderSystem.getProjectionMatrixBuffer(),
                        RenderSystem.getShaderFog(),
                        RenderSystem.getGlobalSettingsUniform(),
                        RenderSystem.getShaderLights()),
                ScissorInput.capture(RenderSystem.getScissorStateForRenderTypeDraws()),
                ViewportInput.capture(),
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
        RenderPass pass = currentDevice.createCommandEncoder().createRenderPass(
                () -> debugLabel,
                colorAttachment,
                java.util.OptionalInt.empty(),
                depthAttachment.orElse(null),
                java.util.OptionalDouble.empty());
        scissor.applyTo(pass);
        viewport.apply();
        return pass;
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

    /**
     * Exact OpenGL viewport captured at the native render-group boundary.
     * RenderPass has no viewport setter, so the value is restored explicitly
     * after opening our pass. Capture is render-thread-only and fails closed
     * if the active graphics context cannot report a valid viewport.
     */
    public record ViewportInput(int x, int y, int width, int height) {
        public ViewportInput {
            if (x < 0 || y < 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid captured terrain viewport");
            }
        }

        public static ViewportInput capture() {
            RenderSystem.assertOnRenderThread();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer values = stack.mallocInt(4);
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, values);
                return new ViewportInput(values.get(0), values.get(1), values.get(2), values.get(3));
            }
        }

        public void apply() {
            RenderSystem.assertOnRenderThread();
            RenderSystem.viewport(x, y, width, height);
        }
    }

    /** Immutable snapshot of RenderSystem's render-type scissor rectangle. */
    public record ScissorInput(boolean enabled, int x, int y, int width, int height) {
        public ScissorInput {
            if (enabled && (width <= 0 || height <= 0)) {
                throw new IllegalArgumentException("Enabled scissor dimensions must be positive");
            }
        }

        public static ScissorInput capture(ScissorState state) {
            Objects.requireNonNull(state, "state");
            return new ScissorInput(
                    state.enabled(), state.x(), state.y(), state.width(), state.height());
        }

        public void applyTo(RenderPass pass) {
            Objects.requireNonNull(pass, "pass");
            if (enabled) {
                pass.enableScissor(x, y, width, height);
            } else {
                pass.disableScissor();
            }
        }
    }

    /** Captured pipeline identity and fixed-function state for one native terrain layer. */
    public record LayerPipelineInput(
            RenderPipeline pipeline,
            java.util.Optional<PipelineState> state,
            boolean pipelineApplied) {
        public LayerPipelineInput {
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(state, "state");
            if (pipelineApplied != state.isPresent()) {
                throw new IllegalArgumentException("Pipeline state must exist exactly when vanilla applies the pipeline");
            }
        }
    }

    /** Immutable fixed-function state copied from the pipeline vanilla selects for a layer. */
    public record PipelineState(
            Identifier location,
            Identifier vertexShader,
            Identifier fragmentShader,
            DepthTestFunction depthTest,
            PolygonMode polygonMode,
            boolean cull,
            java.util.Optional<BlendFunction> blend,
            boolean writeColor,
            boolean writeAlpha,
            boolean writeDepth,
            LogicOp colorLogic,
            float depthBiasScale,
            float depthBiasConstant,
            VertexFormat vertexFormat,
            VertexFormat.Mode vertexMode) {
        public PipelineState {
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(vertexShader, "vertexShader");
            Objects.requireNonNull(fragmentShader, "fragmentShader");
            Objects.requireNonNull(depthTest, "depthTest");
            Objects.requireNonNull(polygonMode, "polygonMode");
            Objects.requireNonNull(blend, "blend");
            Objects.requireNonNull(colorLogic, "colorLogic");
            Objects.requireNonNull(vertexFormat, "vertexFormat");
            Objects.requireNonNull(vertexMode, "vertexMode");
        }

        public static PipelineState capture(RenderPipeline pipeline) {
            Objects.requireNonNull(pipeline, "pipeline");
            return new PipelineState(
                    pipeline.getLocation(), pipeline.getVertexShader(), pipeline.getFragmentShader(),
                    pipeline.getDepthTestFunction(), pipeline.getPolygonMode(), pipeline.isCull(),
                    pipeline.getBlendFunction(), pipeline.isWriteColor(), pipeline.isWriteAlpha(),
                    pipeline.isWriteDepth(), pipeline.getColorLogic(),
                    pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant(),
                    pipeline.getVertexFormat(), pipeline.getVertexFormatMode());
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
