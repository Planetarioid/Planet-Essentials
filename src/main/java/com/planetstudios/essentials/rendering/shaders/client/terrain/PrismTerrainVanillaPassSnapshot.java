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
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
            layers.put(layer, new LayerInput(pipeline, PipelineState.capture(pipeline), ordered,
                    layer == ChunkSectionLayer.TRANSLUCENT));
        }

        DefaultUniformInputs uniforms = new DefaultUniformInputs(
                RenderSystem.getProjectionMatrixBuffer(),
                RenderSystem.getShaderFog(),
                RenderSystem.getGlobalSettingsUniform(),
                RenderSystem.getShaderLights());

        return new PrismTerrainVanillaPassSnapshot(
                group, target, color, Optional.ofNullable(target.getDepthTextureView()),
                target.width, target.height,
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
            List<?> orderedDraws, boolean reversedByVanilla) {
        public LayerInput {
            Objects.requireNonNull(pipeline, "pipeline");
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(orderedDraws, "orderedDraws");
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

    /** The four exact resources conditionally bound by RenderSystem.bindDefaultUniforms. */
    public record DefaultUniformInputs(
            GpuBufferSlice projection,
            GpuBufferSlice fog,
            GpuBuffer globals,
            GpuBufferSlice lighting) {
    }
}
