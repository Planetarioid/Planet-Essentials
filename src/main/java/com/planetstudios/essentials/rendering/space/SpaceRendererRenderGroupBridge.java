package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;

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

        EnumMap<ChunkSectionLayer, List<Object>> plannedDrawsByLayer =
                new EnumMap<>(ChunkSectionLayer.class);
        for (SpaceRendererDrawPlan.Batch batch : groupPlan.batches()) {
            List<Object> plannedDraws = plannedDrawsByLayer.computeIfAbsent(
                    batch.layer(), ignored -> new java.util.ArrayList<>());
            for (SpaceRendererDrawPlan.DrawCommand command : batch.draws()) {
                plannedDraws.add(command.draw().input().vanillaDraw());
            }
        }
        if (!hasCompleteGroupCoverage(List.of(group.layers()),
                input.drawsPerLayer(), plannedDrawsByLayer)) {
            return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                    FallbackReason.INCOMPLETE_GROUP_COVERAGE, false));
        }

        try {
            SpaceRendererVanillaPassSnapshot snapshot =
                    SpaceRendererVanillaPassSnapshot.capture(input, group, blockAtlasSampler);
            snapshot.validateCurrent(RenderSystem.getDevice(), input, blockAtlasSampler);
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

    /**
     * Verifies exact vanilla draw identity coverage for every layer in this
     * native group. Count-only checks can accept a plan containing different
     * draws, so both sides are compared with identity semantics and duplicates
     * are rejected. Planned layers outside the group also fail closed.
     */
    static boolean hasCompleteGroupCoverage(List<ChunkSectionLayer> groupLayers,
            Map<?, ?> vanillaDrawsByLayer,
            Map<ChunkSectionLayer, List<Object>> plannedDrawsByLayer) {
        Objects.requireNonNull(groupLayers, "groupLayers");
        Objects.requireNonNull(vanillaDrawsByLayer, "vanillaDrawsByLayer");
        Objects.requireNonNull(plannedDrawsByLayer, "plannedDrawsByLayer");
        EnumSet<ChunkSectionLayer> expectedLayers =
                EnumSet.noneOf(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : groupLayers) {
            if (layer == null || !expectedLayers.add(layer)) return false;
        }
        if (!expectedLayers.containsAll(plannedDrawsByLayer.keySet())) return false;

        for (ChunkSectionLayer layer : expectedLayers) {
            Object vanillaValue = vanillaDrawsByLayer.get(layer);
            if (!(vanillaValue instanceof List<?> vanillaDraws)) return false;
            List<Object> plannedDraws = plannedDrawsByLayer.getOrDefault(layer, List.of());
            Set<Object> vanillaIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object draw : vanillaDraws) {
                if (draw == null || !vanillaIdentities.add(draw)) return false;
            }
            Set<Object> plannedIdentities = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object draw : plannedDraws) {
                if (draw == null || !plannedIdentities.add(draw)) return false;
            }
            if (vanillaIdentities.size() != plannedIdentities.size()
                    || !vanillaIdentities.containsAll(plannedIdentities)) return false;
        }
        return true;
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
