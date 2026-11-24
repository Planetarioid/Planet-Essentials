package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Live, fail-closed preflight at Minecraft's native render-group boundary.
 *
 * <p>Captures the active vanilla state and compares the Space Renderer pipeline
 * against it, but deliberately never cancels vanilla. A matching snapshot is
 * necessary, not sufficient, for replacement.</p>
 */
public final class SpaceRendererRenderGroupBridge {
    private static volatile Preflight lastPreflight =
            new Preflight(0, 0, FallbackReason.NOT_OBSERVED, false);

    private SpaceRendererRenderGroupBridge() {}

    /** Called synchronously from ChunkSectionsToRender.renderGroup HEAD. */
    public static Preflight preflight(ChunkSectionsToRender input,
            ChunkSectionLayerGroup group, GpuSampler blockAtlasSampler) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(blockAtlasSampler, "blockAtlasSampler");

        SpaceRendererDrawPlan groupPlan =
                SpaceRendererCaptureRuntime.currentDrawPlanForGroup(group);
        if (!groupPlan.isReady() || groupPlan.drawCount() == 0) {
            return publish(new Preflight(groupPlan.frameSequence(), 0,
                    FallbackReason.FRAME_NOT_READY, false));
        }

        Map<ChunkSectionLayer, Integer> plannedByLayer = new EnumMap<>(ChunkSectionLayer.class);
        for (SpaceRendererDrawPlan.Batch batch : groupPlan.batches()) {
            plannedByLayer.merge(batch.layer(), batch.draws().size(), Math::addExact);
        }
        for (ChunkSectionLayer layer : group.layers()) {
            List<?> vanillaDraws = input.drawsPerLayer().get(layer);
            if (vanillaDraws == null
                    || vanillaDraws.size() != plannedByLayer.getOrDefault(layer, 0)) {
                return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                        FallbackReason.INCOMPLETE_GROUP_COVERAGE, false));
            }
        }

        try {
            SpaceRendererVanillaPassSnapshot snapshot =
                    SpaceRendererVanillaPassSnapshot.capture(input, group, blockAtlasSampler);
            snapshot.validateCurrent(RenderSystem.getDevice());
            SpaceRendererVanillaPassSnapshot.CapturedStateReadinessReport report =
                    snapshot.assessCapturedStateReadiness();
            FallbackReason reason = report.capturedStateMatches()
                    ? FallbackReason.RUNTIME_PARITY_UNVERIFIED
                    : FallbackReason.CAPTURED_STATE_MISMATCH;
            return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                    reason, true, report.missingResources(), report.pipelineDifferences(),
                    report.wireframe()));
        } catch (RuntimeException failure) {
            return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                    FallbackReason.SNAPSHOT_UNAVAILABLE, false));
        }
    }

    private static Preflight publish(Preflight result) {
        lastPreflight = result;
        return result;
    }

    /** Last immutable result for diagnostics; does not own GPU resources. */
    public static Preflight lastPreflight() {
        return lastPreflight;
    }

    public enum FallbackReason {
        NOT_OBSERVED,
        FRAME_NOT_READY,
        INCOMPLETE_GROUP_COVERAGE,
        SNAPSHOT_UNAVAILABLE,
        CAPTURED_STATE_MISMATCH,
        RUNTIME_PARITY_UNVERIFIED
    }

    public record Preflight(long frameSequence, int plannedDraws,
            FallbackReason fallbackReason, boolean snapshotValidated,
            Set<String> missingResources,
            Map<ChunkSectionLayer, List<String>> pipelineDifferences,
            boolean wireframe) {
        public Preflight(long frameSequence, int plannedDraws,
                FallbackReason fallbackReason, boolean snapshotValidated) {
            this(frameSequence, plannedDraws, fallbackReason, snapshotValidated,
                    Set.of(), Map.of(), false);
        }

        public Preflight {
            if (frameSequence < 0 || plannedDraws < 0) {
                throw new IllegalArgumentException("Invalid Space Renderer group preflight");
            }
            Objects.requireNonNull(fallbackReason, "fallbackReason");
            Objects.requireNonNull(missingResources, "missingResources");
            Objects.requireNonNull(pipelineDifferences, "pipelineDifferences");
            missingResources = Set.copyOf(missingResources);
            EnumMap<ChunkSectionLayer, List<String>> copy =
                    new EnumMap<>(ChunkSectionLayer.class);
            pipelineDifferences.forEach((layer, values) ->
                    copy.put(Objects.requireNonNull(layer, "layer"),
                            List.copyOf(Objects.requireNonNull(values, "differences"))));
            pipelineDifferences = Map.copyOf(copy);
        }

        /** Intentionally false until full vanilla parity is runtime-verified. */
        public boolean mayReplaceVanilla() {
            return false;
        }
    }
}
