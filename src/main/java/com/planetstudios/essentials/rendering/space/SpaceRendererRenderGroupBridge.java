package com.planetstudios.essentials.rendering.space;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

import java.util.Objects;

/**
 * Live, fail-closed preflight at Minecraft's native render-group boundary.
 *
 * <p>This bridge deliberately observes and validates the Space Renderer path
 * without cancelling vanilla. Replacement stays disabled until every captured
 * vanilla state facet has been implemented and runtime-verified.</p>
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

        SpaceRendererDrawPlan groupPlan = SpaceRendererCaptureRuntime.currentDrawPlanForGroup(group);
        if (!groupPlan.isReady() || groupPlan.drawCount() == 0) {
            return publish(new Preflight(groupPlan.frameSequence(), 0,
                    FallbackReason.FRAME_NOT_READY, false));
        }

        int vanillaDrawCount = 0;
        for (ChunkSectionLayer layer : group.layers()) {
            var draws = input.drawsPerLayer().get(layer);
            if (draws == null) {
                return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                        FallbackReason.INCOMPLETE_GROUP_COVERAGE, false));
            }
            vanillaDrawCount = Math.addExact(vanillaDrawCount, draws.size());
        }
        if (vanillaDrawCount != groupPlan.drawCount()) {
            return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                    FallbackReason.INCOMPLETE_GROUP_COVERAGE, false));
        }

        try {
            SpaceRendererVanillaPassSnapshot snapshot =
                    SpaceRendererVanillaPassSnapshot.capture(input, group, blockAtlasSampler);
            snapshot.validateCurrent(RenderSystem.getDevice());
            if (!snapshot.layers().equals(java.util.List.of(group.layers()))) {
                return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                        FallbackReason.INCOMPLETE_GROUP_COVERAGE, false));
            }

            // Fog is captured, but overlay, animated-texture and special-model
            // parity are not yet complete or verified. Never take over on
            // capture completeness alone.
            return publish(new Preflight(groupPlan.frameSequence(), groupPlan.drawCount(),
                    FallbackReason.VANILLA_STATE_PARITY_UNVERIFIED, true));
        } catch (RuntimeException captureFailure) {
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
        VANILLA_STATE_PARITY_UNVERIFIED
    }

    public record Preflight(long frameSequence, int plannedDraws,
            FallbackReason fallbackReason, boolean snapshotValidated) {
        public Preflight {
            if (frameSequence < 0 || plannedDraws < 0) {
                throw new IllegalArgumentException("Invalid Space Renderer group preflight");
            }
            Objects.requireNonNull(fallbackReason, "fallbackReason");
        }

        /** Intentionally false until full vanilla parity is runtime-verified. */
        public boolean mayReplaceVanilla() {
            return false;
        }
    }
}
