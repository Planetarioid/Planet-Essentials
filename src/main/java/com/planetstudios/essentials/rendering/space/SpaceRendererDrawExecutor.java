package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
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
     * Selects only the layers belonging to the supplied native Minecraft
     * render group, then submits that immutable slice. This prevents callers
     * from accidentally submitting layers owned by another group.
     */
    public static SubmissionResult submitGroup(RenderPass pass,
            SpaceRendererDrawPlan framePlan,
            ChunkSectionLayerGroup group,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ,
            GpuTextureView blockAtlas, GpuSampler blockAtlasSampler,
            GpuTextureView lightmap, GpuSampler lightmapSampler) {
        Objects.requireNonNull(framePlan, "framePlan");
        Objects.requireNonNull(group, "group");
        return submit(pass, framePlan.forGroup(group), sectionUniforms,
                cameraX, cameraY, cameraZ, blockAtlas, blockAtlasSampler, lightmap, lightmapSampler);
    }

    /**
     * Submits one native group using only the resources captured at that
     * group's boundary. The caller must open the pass from the same snapshot.
     */
    public static SubmissionResult submitGroup(RenderPass pass,
            SpaceRendererDrawPlan framePlan,
            SpaceRendererVanillaPassSnapshot snapshot,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(snapshot, "snapshot");
        snapshot.validateCurrent(RenderSystem.getDevice());
        return submitInternal(pass, framePlan.forGroup(snapshot.group()), sectionUniforms,
                cameraX, cameraY, cameraZ,
                snapshot.blockAtlas(), snapshot.blockAtlasSampler(),
                snapshot.lightmap(), snapshot.lightmapSampler(),
                snapshot.defaultUniforms(), snapshot.modelView());
    }

    /**
     * Owns the complete native-group pass lifecycle: validate the captured
     * resources, select only this group's layers, skip empty groups without
     * opening a pass, and close the single no-clear pass even if submission
     * fails. This is an explicit primitive; the live renderer hook remains
     * responsible for choosing vanilla fallback before calling it.
     */
    public static SubmissionResult submitCapturedGroup(
            SpaceRendererDrawPlan framePlan,
            SpaceRendererVanillaPassSnapshot snapshot,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(framePlan, "framePlan");
        Objects.requireNonNull(snapshot, "snapshot");
        snapshot.validateCurrent(RenderSystem.getDevice());
        SpaceRendererDrawPlan groupPlan = framePlan.forGroup(snapshot.group());
        if (!groupPlan.isReady() || groupPlan.drawCount() <= 0) {
            return new SubmissionResult(groupPlan.frameSequence(), 0);
        }
        validatePlanBeforeSubmission(groupPlan, sectionUniforms,
                cameraX, cameraY, cameraZ);
        try (RenderPass pass = snapshot.openPass(
                RenderSystem.getDevice(), "Planet Essentials Space Renderer " + snapshot.group())) {
            return submitGroup(pass, groupPlan, snapshot, sectionUniforms,
                    cameraX, cameraY, cameraZ);
        }
    }

    /**
     * Attempts one captured native-group submission and reports whether a
     * caller may still safely fall back to vanilla.
     *
     * <p>All validation and pass creation happen before the guard enters
     * SUBMITTING. Once draw submission begins, any exception is treated as
     * indeterminate: some GPU work may already have been queued, so rendering
     * vanilla as well could duplicate terrain.</p>
     */
    public static SubmissionAttempt attemptCapturedGroup(
            SpaceRendererDrawPlan framePlan,
            SpaceRendererVanillaPassSnapshot snapshot,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(framePlan, "framePlan");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(sectionUniforms, "sectionUniforms");

        SpaceRendererDrawPlan groupPlan;
        try {
            snapshot.validateCurrent(RenderSystem.getDevice());
            groupPlan = framePlan.forGroup(snapshot.group());
            if (!groupPlan.isReady() || groupPlan.drawCount() <= 0) {
                return new SubmissionAttempt(framePlan.frameSequence(),
                        SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION,
                        0, true, null, null);
            }
            validatePlanBeforeSubmission(groupPlan, sectionUniforms,
                    cameraX, cameraY, cameraZ);
        } catch (RuntimeException failure) {
            return new SubmissionAttempt(framePlan.frameSequence(),
                    SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION,
                    0, true, failure.getClass().getName(), failure.getMessage());
        }

        SpaceRendererSubmissionGuard guard =
                new SpaceRendererSubmissionGuard(groupPlan.frameSequence(), groupPlan.drawCount());
        RenderPass pass;
        try {
            pass = snapshot.openPass(RenderSystem.getDevice(),
                    "Planet Essentials Space Renderer " + snapshot.group());
        } catch (RuntimeException failure) {
            guard.failBeforeSubmission();
            return attemptFromFailure(guard, failure);
        }

        SubmissionResult submitted;
        guard.beginSubmission();
        try (pass) {
            submitted = submitGroup(pass, groupPlan, snapshot, sectionUniforms,
                    cameraX, cameraY, cameraZ);
        } catch (RuntimeException failure) {
            guard.failAfterSubmission();
            return attemptFromFailure(guard, failure);
        }

        guard.complete(submitted.submittedDraws());
        return new SubmissionAttempt(submitted.frameSequence(), guard.state(),
                submitted.submittedDraws(), false, null, null);
    }

    private static SubmissionAttempt attemptFromFailure(
            SpaceRendererSubmissionGuard guard, RuntimeException failure) {
        return new SubmissionAttempt(guard.frameSequence(), guard.state(),
                -1, guard.mayUseVanillaFallback(),
                failure.getClass().getName(), failure.getMessage());
    }

    /** Immutable result of an all-or-nothing group attempt. */
    public record SubmissionAttempt(long frameSequence,
            SpaceRendererSubmissionGuard.State state, int submittedDraws,
            boolean vanillaFallbackSafe, String failureType, String failureMessage) {
        public SubmissionAttempt {
            if (frameSequence <= 0 || submittedDraws < -1) {
                throw new IllegalArgumentException("Invalid Space Renderer submission attempt");
            }
            Objects.requireNonNull(state, "state");
            if (vanillaFallbackSafe != (state == SpaceRendererSubmissionGuard.State.FAILED_BEFORE_SUBMISSION)) {
                throw new IllegalArgumentException("Vanilla fallback safety does not match submission state");
            }
            if ((failureType == null) != (failureMessage == null)) {
                throw new IllegalArgumentException("Failure type and message must be supplied together");
            }
        }

        public boolean submitted() {
            return state == SpaceRendererSubmissionGuard.State.COMPLETED;
        }
    }

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
        return submitInternal(pass, plan, sectionUniforms, cameraX, cameraY, cameraZ,
                blockAtlas, blockAtlasSampler, lightmap, lightmapSampler, null, null);
    }

    private static SubmissionResult submitInternal(RenderPass pass,
            SpaceRendererDrawPlan plan,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ,
            GpuTextureView blockAtlas, GpuSampler blockAtlasSampler,
            GpuTextureView lightmap, GpuSampler lightmapSampler,
            SpaceRendererVanillaPassSnapshot.DefaultUniformInputs capturedUniforms,
            Matrix4f capturedModelView) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(pass, "pass");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(sectionUniforms, "sectionUniforms");
        Objects.requireNonNull(blockAtlas, "blockAtlas");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");
        Objects.requireNonNull(lightmap, "lightmap");
        Objects.requireNonNull(lightmapSampler, "lightmapSampler");
        if (!plan.isReady() || plan.drawCount() <= 0) {
            return new SubmissionResult(plan.frameSequence(), 0);
        }
        validatePlanBeforeSubmission(plan, sectionUniforms, cameraX, cameraY, cameraZ);

        int submitted = 0;
        sectionUniforms.beginFrame();
        try {
            // DynamicTransforms is shared by every section in this render group.
            // The per-section origin is supplied separately by SpaceRendererSection.
            var dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                    capturedModelView == null ? RenderSystem.getModelViewMatrix() : capturedModelView,
                    new Vector4f(1.0f),
                    new Vector3f(),
                    new Matrix4f());

            for (SpaceRendererDrawPlan.Batch batch : plan.batches()) {
                // RenderPass uniform and sampler bindings are pipeline-scoped. Select
                // the layer pipeline before applying its default and dynamic uniforms.
                pass.setPipeline(pipelineForLayer(batch.layer()));
                if (capturedUniforms == null) {
                    RenderSystem.bindDefaultUniforms(pass);
                } else {
                    capturedUniforms.bindTo(pass);
                }
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

    /**
     * Performs every currently CPU-checkable validation before opening a
     * captured render pass or beginning the section-uniform frame. This keeps
     * stale meshes, invalid translucent indices, unsupported layers, and
     * capacity errors from surfacing halfway through a group submission.
     *
     * <p>GPU/driver failures can still occur after the first draw. The live
     * renderer must therefore retain vanilla until a separate all-or-nothing
     * takeover protocol has been runtime-verified.</p>
     */
    static void validatePlanBeforeSubmission(SpaceRendererDrawPlan plan,
            SpaceRendererSectionUniformBuffer sectionUniforms,
            double cameraX, double cameraY, double cameraZ) {
        Objects.requireNonNull(sectionUniforms, "sectionUniforms");
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)) {
            throw new IllegalArgumentException("Camera coordinates must be finite");
        }
        validatePlanResources(plan, sectionUniforms.maxSectionsPerFrame());
    }

    /**
     * Checks plan-owned GPU resources and structural invariants without
     * requiring a live uniform arena. The live render-group bridge uses this
     * before attempting to capture pass state; the executor repeats it
     * immediately before submission to close the time-of-check gap.
     */
    static void validatePlanResources(SpaceRendererDrawPlan plan, int maxSectionsPerFrame) {
        Objects.requireNonNull(plan, "plan");
        if (maxSectionsPerFrame <= 0) {
            throw new IllegalArgumentException("Section uniform capacity must be positive");
        }
        if (!plan.isReady() || plan.drawCount() <= 0) return;
        if (plan.drawCount() > maxSectionsPerFrame) {
            throw new IllegalArgumentException("Visible draw plan exceeds the section uniform frame capacity");
        }

        int validatedDraws = 0;
        for (SpaceRendererDrawPlan.Batch batch : plan.batches()) {
            pipelineForLayer(batch.layer());
            if (batch.translucent() != SpaceRendererDrawPlan.requiresSortedIndices(batch.layer())) {
                throw new IllegalArgumentException("Terrain batch translucency does not match its vanilla layer");
            }
            for (SpaceRendererDrawPlan.DrawCommand command : batch.draws()) {
                var draw = command.draw();
                var mesh = draw.mesh();
                if (!mesh.isOpen() || mesh.vertexCount() <= 0 || mesh.vertexCount() % 4 != 0
                        || command.indexCount() != SpaceRendererDrawPlan.indexCount(mesh.vertexCount())) {
                    throw new IllegalStateException("Space Renderer draw plan contains an invalid or retired mesh");
                }
                SpaceRendererSortedIndexBuffer sorted = command.sortedIndices();
                if (batch.translucent()) {
                    if (sorted == null || !sorted.isOpen()
                            || sorted.draw() != draw || sorted.indexCount() != command.indexCount()) {
                        throw new IllegalStateException(
                                "Translucent draw has no valid frame-owned sorted index buffer");
                    }
                } else if (sorted != null) {
                    throw new IllegalStateException("Opaque draw unexpectedly owns sorted indices");
                }
                validatedDraws = Math.addExact(validatedDraws, 1);
            }
        }
        if (validatedDraws != plan.drawCount()) {
            throw new IllegalStateException("Space Renderer draw plan count does not match its batches");
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
