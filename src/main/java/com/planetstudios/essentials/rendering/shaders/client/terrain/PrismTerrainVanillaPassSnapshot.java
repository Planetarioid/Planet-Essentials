package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import com.planetstudios.essentials.rendering.shaders.client.MinecraftShaderBindings;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Read-only snapshot of the inputs used by Minecraft 1.21.11's
 * ChunkSectionsToRender.renderGroup implementation.
 *
 * <p>This records resource identities and immutable draw ordering. It does not
 * open a RenderPass or mutate vanilla GPU state.</p>
 */
public record PrismTerrainVanillaPassSnapshot(
        ChunkSectionLayerGroup group,
        RenderTarget target,
        GpuTextureView colorAttachment,
        Optional<GpuTextureView> depthAttachment,
        int targetWidth,
        int targetHeight,
        PassOptions passOptions,
        IndexBufferInput indexBuffer,
        List<String> dynamicUniformSlots,
        GpuTextureView blockAtlas,
        GpuSampler blockAtlasSampler,
        GpuTextureView lightmap,
        GpuSampler lightmapSampler,
        Map<ChunkSectionLayer, LayerInput> layers,
        DefaultUniformInputs defaultUniforms,
        Matrix4f modelView,
        boolean wireframe,
        int maximumSequentialIndexCount) {

    public PrismTerrainVanillaPassSnapshot {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(colorAttachment, "colorAttachment");
        Objects.requireNonNull(depthAttachment, "depthAttachment");
        Objects.requireNonNull(passOptions, "passOptions");
        Objects.requireNonNull(indexBuffer, "indexBuffer");
        Objects.requireNonNull(dynamicUniformSlots, "dynamicUniformSlots");
        dynamicUniformSlots = List.copyOf(dynamicUniformSlots);
        Objects.requireNonNull(blockAtlas, "blockAtlas");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        Objects.requireNonNull(lightmap, "lightmap");
        Objects.requireNonNull(lightmapSampler, "lightmapSampler");
        Objects.requireNonNull(layers, "layers");
        Objects.requireNonNull(defaultUniforms, "defaultUniforms");
        Objects.requireNonNull(modelView, "modelView");
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Render target dimensions must be positive");
        }
        if (maximumSequentialIndexCount < 0) {
            throw new IllegalArgumentException("Maximum sequential index count cannot be negative");
        }
        EnumMap<ChunkSectionLayer, LayerInput> copy = new EnumMap<>(ChunkSectionLayer.class);
        layers.forEach((layer, input) -> copy.put(
                Objects.requireNonNull(layer, "layer"), Objects.requireNonNull(input, "layer input")));
        layers = Collections.unmodifiableMap(copy);
        modelView = new Matrix4f(modelView);
    }

    @Override
    public Matrix4f modelView() {
        return new Matrix4f(modelView);
    }

    /**
     * Opens a new pass against the exact captured vanilla attachments and clear
     * options. The caller owns and must close the returned pass.
     */
    public RenderPass openCapturedTargetPass(GpuDevice device, String debugLabel) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(device, "device");
        if (debugLabel == null || debugLabel.isBlank()) {
            throw new IllegalArgumentException("Render-pass debug label cannot be blank");
        }
        validateCapturedTarget(device);
        return device.createCommandEncoder().createRenderPass(
                () -> debugLabel, colorAttachment, passOptions.colorClear(),
                depthAttachment.orElse(null), passOptions.depthClear());
    }

    /**
     * Rejects snapshots whose device or render-target attachments changed after
     * capture. A resized/recreated target must be recaptured before submission.
     */
    private void validateCapturedTarget(GpuDevice device) {
        if (RenderSystem.getDevice() != device) {
            throw new IllegalStateException("Captured terrain pass belongs to a different GPU device");
        }
        if (target.width != targetWidth || target.height != targetHeight
                || target.getColorTextureView() != colorAttachment
                || target.getDepthTextureView() != depthAttachment.orElse(null)) {
            throw new IllegalStateException("Captured terrain render target changed after snapshot creation");
        }
    }

    /**
     * Builds only the sampler bindings declared by a Prism terrain program,
     * using the exact vanilla atlas/lightmap resources. Unknown sampler names
     * fail closed instead of silently binding the wrong texture.
     */
    public MinecraftShaderBindings samplerBindingsFor(
            ChunkSectionLayer layer, Set<String> requiredSamplers) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(requiredSamplers, "requiredSamplers");
        LayerInput layerInput = layers.get(layer);
        MinecraftShaderBindings.Builder builder = MinecraftShaderBindings.builder();
        for (String name : requiredSamplers) {
            switch (name) {
                case "Sampler0" -> {
                    if (layerInput == null || !layerInput.pipelineApplied()) {
                        throw new IllegalStateException("Vanilla did not bind the block atlas for empty layer " + layer);
                    }
                    builder.sampler("Sampler0", blockAtlas, blockAtlasSampler);
                }
                case "Sampler2" -> builder.sampler("Sampler2", lightmap, lightmapSampler);
                default -> throw new IllegalArgumentException(
                        "Unsupported vanilla terrain sampler in captured pass: " + name);
            }
        }
        return builder.build();
    }

    /**
     * Replays the exact optional default-uniform bindings captured from
     * RenderSystem.bindDefaultUniforms. The returned names can be supplied as
     * externally-bound uniforms when validating the Prism shader layout.
     */
    public Set<String> bindCapturedDefaultUniforms(RenderPass pass) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        return defaultUniforms.bindTo(pass);
    }

    /** Captures the same resource choices and ordering made by vanilla 1.21.11. */
    public static PrismTerrainVanillaPassSnapshot capture(
            ChunkSectionsToRender input, ChunkSectionLayerGroup group, GpuSampler blockAtlasSampler) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        RenderSystem.assertOnRenderThread();

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = Objects.requireNonNull(group.outputTarget(), "vanilla terrain output target");
        GpuTextureView color = Objects.requireNonNull(
                target.getColorTextureView(), "vanilla terrain color attachment");
        GpuTextureView lightmap = Objects.requireNonNull(
                minecraft.gameRenderer.lightTexture().getTextureView(), "vanilla lightmap texture");
        GpuSampler lightmapSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        boolean wireframe = SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;

        EnumMap<ChunkSectionLayer, LayerInput> layers = new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : group.layers()) {
            List<?> source = input.drawsPerLayer().get(layer);
            List<?> ordered = effectiveDrawOrder(layer, source == null ? List.of() : source);
            RenderPipeline pipeline = wireframe ? RenderPipelines.WIREFRAME : layer.pipeline();
            boolean pipelineApplied = !ordered.isEmpty();
            layers.put(layer, new LayerInput(pipeline,
                    pipelineApplied ? PipelineState.capture(pipeline) : null,
                    ordered, layer == ChunkSectionLayer.TRANSLUCENT, pipelineApplied));
        }

        RenderSystem.AutoStorageIndexBuffer sequential =
                RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        // Capture the required range and format only. getBuffer() can allocate or
        // grow GPU storage, so resolve the actual buffer later during replacement
        // preflight, before opening the Prism render pass.
        Optional<VertexFormat.IndexType> indexType = input.maxIndicesRequired() == 0
                ? Optional.empty() : Optional.of(sequential.type());

        DefaultUniformInputs uniforms = new DefaultUniformInputs(
                RenderSystem.getProjectionMatrixBuffer(),
                RenderSystem.getShaderFog(),
                RenderSystem.getGlobalSettingsUniform(),
                RenderSystem.getShaderLights());

        return new PrismTerrainVanillaPassSnapshot(
                group, target, color, Optional.ofNullable(target.getDepthTextureView()),
                target.width, target.height,
                passOptions(target),
                new IndexBufferInput(input.maxIndicesRequired(), indexType), List.of("ChunkSection"),
                input.textureView(), blockAtlasSampler,
                lightmap, lightmapSampler,
                layers, uniforms, new Matrix4f(RenderSystem.getModelViewMatrix()),
                wireframe, input.maxIndicesRequired());
    }

    /**
     * Vanilla reverses only the translucent layer's draw list before submission.
     * Keep this rule isolated and unit-testable so the replacement path cannot
     * accidentally reverse opaque, cutout, or tripwire draws.
     */
    public static List<?> effectiveDrawOrder(ChunkSectionLayer layer, List<?> source) {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(source, "source");
        ArrayList<Object> ordered = new ArrayList<>(source);
        if (layer == ChunkSectionLayer.TRANSLUCENT) {
            Collections.reverse(ordered);
        }
        return List.copyOf(ordered);
    }

    public record LayerInput(RenderPipeline pipeline, PipelineState state,
            List<?> orderedDraws, boolean reversedByVanilla, boolean pipelineApplied) {
        public LayerInput {
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(orderedDraws, "orderedDraws");
            if (pipelineApplied != (state != null)) {
                throw new IllegalArgumentException("Pipeline state must exist exactly when vanilla applies the pipeline");
            }
            orderedDraws = List.copyOf(orderedDraws);
        }
    }

    /**
     * Immutable copy of the pipeline state applied by RenderPass.setPipeline.
     * Both this data and the original pipeline identity are retained for parity checks.
     */
    public record PipelineState(
            Identifier location,
            Identifier vertexShader,
            Identifier fragmentShader,
            DepthTestFunction depthTest,
            PolygonMode polygonMode,
            boolean cull,
            Optional<BlendFunction> blend,
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

    /**
     * Captures backend defaults only when the active vanilla device is the
     * known 1.21.11 OpenGL backend. Unknown backends remain explicitly unknown.
     */
    private static PassOptions passOptions(RenderTarget target) {
        boolean openGl = RenderSystem.getDevice() instanceof GlDevice;
        Optional<ViewportState> viewport = openGl
                ? Optional.of(new ViewportState(0, 0, target.width, target.height))
                : Optional.empty();
        Optional<ScissorState> scissor = openGl
                ? Optional.of(new ScissorState(false, 0, 0, 0, 0))
                : Optional.empty();
        return new PassOptions(OptionalInt.empty(), OptionalDouble.empty(),
                false, viewport, scissor);
    }

    public record ViewportState(int x, int y, int width, int height) {
        public ViewportState {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid viewport dimensions");
        }
    }

    public record ScissorState(boolean enabled, int x, int y, int width, int height) {
        public ScissorState {
            if (enabled && (width <= 0 || height <= 0)) {
                throw new IllegalArgumentException("Enabled scissor must have positive dimensions");
            }
        }
    }

    /** Exact createRenderPass options used by vanilla for this group. */
    public record PassOptions(OptionalInt colorClear, OptionalDouble depthClear,
            boolean stencilAttachment, Optional<ViewportState> viewport, Optional<ScissorState> scissor) {
        public PassOptions {
            Objects.requireNonNull(colorClear, "colorClear");
            Objects.requireNonNull(depthClear, "depthClear");
            Objects.requireNonNull(viewport, "viewport");
            Objects.requireNonNull(scissor, "scissor");
        }
    }

    /**
     * Sequential QUADS index requirements captured without allocating or growing
     * GPU storage. Resolve the buffer during replacement preflight, before a
     * render pass is opened.
     */
    public record IndexBufferInput(int maximumIndexCount, Optional<VertexFormat.IndexType> type) {
        public IndexBufferInput {
            if (maximumIndexCount < 0) {
                throw new IllegalArgumentException("Maximum index count cannot be negative");
            }
            Objects.requireNonNull(type, "type");
            if ((maximumIndexCount > 0) != type.isPresent()) {
                throw new IllegalArgumentException("A positive index range requires an index type");
            }
        }

        public Optional<GpuBuffer> resolveBuffer() {
            return resolveBufferForIndexCount(maximumIndexCount).map(PreparedIndexBuffer::buffer);
        }

        /**
         * Resolves enough sequential QUADS indices for both the captured vanilla
         * requirement and a caller's complete Prism replacement draw set.
         * This may grow GPU storage and therefore belongs in preflight, before
         * opening a RenderPass. The post-growth index type is returned with the
         * allocation rather than assuming the type observed during capture.
         */
        public Optional<PreparedIndexBuffer> resolveBufferForIndexCount(int requiredIndexCount) {
            RenderSystem.assertOnRenderThread();
            if (requiredIndexCount < 0) {
                throw new IllegalArgumentException("Required index count cannot be negative");
            }
            int capacityCount = Math.max(maximumIndexCount, requiredIndexCount);
            if (capacityCount == 0) return Optional.empty();
            RenderSystem.AutoStorageIndexBuffer sequential =
                    RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer buffer = sequential.getBuffer(capacityCount);
            VertexFormat.IndexType resolvedType = sequential.type();
            if (!PrismTerrainDrawExecutor.indexRangeFits(buffer.size(), resolvedType, 0, capacityCount)) {
                throw new IllegalStateException("Sequential QUADS index buffer is smaller than its requested range");
            }
            return Optional.of(new PreparedIndexBuffer(buffer, resolvedType, capacityCount));
        }

        /** Six sequential indices address each complete four-vertex quad. */
        public static int indexCountForQuadVertexCount(int vertexCount) {
            if (vertexCount < 0 || vertexCount % 4 != 0) {
                throw new IllegalArgumentException("Terrain vertex count must be a nonnegative multiple of four");
            }
            long indexCount = (long) (vertexCount / 4) * 6L;
            if (indexCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Terrain index requirement exceeds supported integer range");
            }
            return (int) indexCount;
        }
    }

    /** Buffer and index type resolved together after any required growth. */
    public record PreparedIndexBuffer(GpuBuffer buffer, VertexFormat.IndexType type, int indexCount) {
        public PreparedIndexBuffer {
            Objects.requireNonNull(buffer, "buffer");
            Objects.requireNonNull(type, "type");
            if (indexCount <= 0) throw new IllegalArgumentException("Prepared index count must be positive");
            if (!PrismTerrainDrawExecutor.indexRangeFits(buffer.size(), type, 0, indexCount)) {
                throw new IllegalArgumentException("Prepared index buffer does not cover its declared range");
            }
        }
    }

    /** Captured nullable default bindings, replayed with vanilla's exact names. */
    public record DefaultUniformInputs(
            GpuBufferSlice projection,
            GpuBufferSlice fog,
            GpuBuffer globals,
            GpuBufferSlice lighting) {
        /** Names available for binding without opening or mutating a render pass. */
        public Set<String> availableNames() {
            HashSet<String> names = new HashSet<>();
            if (projection != null) names.add("Projection");
            if (fog != null) names.add("Fog");
            if (globals != null) names.add("Globals");
            if (lighting != null) names.add("Lighting");
            return Set.copyOf(names);
        }

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
