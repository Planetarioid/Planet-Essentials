package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

/**
 * Submits a preflighted Space Renderer visible-frame plan through the native
 * terrain pipelines. The caller owns the render pass, target, and texture views.
 *
 * <p>This class intentionally does not activate vanilla terrain replacement.
 * It is an explicit submission primitive so the frame hook can be connected
 * only after render-target and texture lifecycle integration is validated.</p>
 */
public final class SpaceRendererDrawExecutor {
    private static final String BLOCK_ATLAS_SAMPLER = "Sampler0";
    private static final String LIGHTMAP_SAMPLER = "Sampler2";

    private SpaceRendererDrawExecutor() {}

    /**
     * Submits every draw in a complete immutable plan. The uniform arena is
     * rotated in a finally block after all attempted submissions, including
     * partial failures, so its next frame never overwrites an in-flight buffer.
     */
    public static SubmissionResult submit(RenderPass pass,
            SpaceRendererDrawPlan plan,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ,
            GpuTextureView blockAtlas, GpuSampler blockAtlasSampler,
            GpuTextureView lightmap, GpuSampler lightmapSampler) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(sectionUniforms, "sectionUniforms");
        Objects.requireNonNull(blockAtlas, "blockAtlas");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        Objects.requireNonNull(lightmap, "lightmap");
        Objects.requireNonNull(lightmapSampler, "lightmapSampler");
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("Camera coordinates must be finite");
        }
        if (!plan.isReady() || plan.drawCount() <= 0) {
            return new SubmissionResult(plan.frameSequence(), 0);
        }
        if (plan.drawCount() > sectionUniforms.maxSectionsPerFrame()) {
            throw new IllegalArgumentException("Visible draw plan exceeds the section uniform frame capacity");
        }

        int submitted = 0;
        sectionUniforms.beginFrame();
        try {
            // DynamicTransforms is shared by every section in this render group.
            // The per-section origin is supplied separately by SpaceRendererSection.
            var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1.0f),
                    new Vector3f(),
                    new Matrix4f());

            for (SpaceRendererDrawPlan.Batch batch : plan.batches()) {
                // RenderPass uniform and sampler bindings are pipeline-scoped. Select
                // the layer pipeline before applying its default and dynamic uniforms.
                pass.setPipeline(pipelineForLayer(batch.layer()));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.bindTexture(BLOCK_ATLAS_SAMPLER, blockAtlas, blockAtlasSampler);
                pass.bindTexture(LIGHTMAP_SAMPLER, lightmap, lightmapSampler);

                for (SpaceRendererDrawPlan.DrawCommand command : batch.draws()) {
                    var draw = command.draw();
                    var mesh = draw.mesh();
                    if (!mesh.isOpen()) {
                        throw new IllegalStateException("Space Renderer draw plan references a retired mesh");
                    }

                    var section = draw.input().section();
                    var uniformSlice = sectionUniforms.upload(
                            section.x(), section.y(), section.z(), cameraX, cameraY, cameraZ);
                    SpaceRendererSectionUniformBuffer.bind(pass, uniformSlice);

                    pass.setVertexBuffer(0, mesh.vertexBuffer());

                    SpaceRendererSortedIndexBuffer sorted = command.sortedIndices();
                    if (batch.translucent()) {
                        if (sorted == null || !sorted.isOpen()
                                || sorted.draw() != draw || sorted.indexCount() != command.indexCount()) {
                            throw new IllegalStateException("Translucent draw has no valid frame-owned sorted index buffer");
                        }
                        pass.setIndexBuffer(sorted.buffer(), VertexFormat.IndexType.SHORT);
                    } else {
                        if (sorted != null) {
                            throw new IllegalStateException("Opaque draw unexpectedly owns sorted indices");
                        }
                        var sequential = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                        pass.setIndexBuffer(sequential.getBuffer(command.indexCount()), sequential.type());
                    }

                    pass.drawIndexed(0, 0, command.indexCount(), 1);
                    submitted++;
                }
            }
            return new SubmissionResult(plan.frameSequence(), submitted);
        } finally {
            sectionUniforms.endFrame();
        }
    }

    /** Maps vanilla chunk layers to the matching independent Space Renderer pipeline. */
    public static com.mojang.blaze3d.pipeline.RenderPipeline pipelineForLayer(ChunkSectionLayer layer) {
        Objects.requireNonNull(layer, "layer");
        if (layer == ChunkSectionLayer.SOLID) return SpaceRendererTerrainPipelines.SOLID;
        if (layer == ChunkSectionLayer.CUTOUT) return SpaceRendererTerrainPipelines.CUTOUT;
        if (layer == ChunkSectionLayer.TRANSLUCENT) return SpaceRendererTerrainPipelines.TRANSLUCENT;
        if (layer == ChunkSectionLayer.TRIPWIRE) return SpaceRendererTerrainPipelines.TRIPWIRE;
        throw new IllegalArgumentException("Unsupported vanilla terrain layer: " + layer);
    }

    /** Immutable report for one attempted frame submission. */
    public record SubmissionResult(long frameSequence, int submittedDraws) {
        public SubmissionResult {
            if (frameSequence <= 0 || submittedDraws < 0) {
                throw new IllegalArgumentException("Invalid Space Renderer submission result");
            }
        }
    }
}
