package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.client.ShaderPackRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fail-closed bridge from Minecraft's synchronous vanilla terrain group to Prism.
 *
 * <p>A selected shader pack opts in by declaring a V1 terrain program named
 * {@code planet:terrain}. Vanilla remains in control unless every draw in the
 * complete group can be prepared and submitted.</p>
 */
public final class PrismTerrainReplacementRuntime {
    public static final String TERRAIN_PROGRAM_ID = "planet:terrain";
    private static PrismTerrainReplacementCoordinator coordinator;

    private PrismTerrainReplacementRuntime() {}

    /**
     * Attempts to replace the current vanilla group. Returns true when vanilla
     * must be cancelled: either Prism completed the whole group, or a GPU draw
     * was already submitted and falling back would duplicate terrain.
     */
    public static boolean tryReplace(PrismTerrainRenderGroupBoundary.Snapshot snapshot) {
        RenderSystem.assertOnRenderThread();
        if (snapshot == null || !ShaderPackRuntime.isEnabled()) return false;
        try {
            return prepareAndSubmit(snapshot);
        } catch (RuntimeException instrumentationFailure) {
            // A Prism observation or preflight failure must never break vanilla's render path.
            PlanetEssentials.LOGGER.warn(
                    "Prism terrain replacement could not prepare group {}; vanilla remains in control",
                    snapshot.group().label(), instrumentationFailure);
            return false;
        }
    }

    private static boolean prepareAndSubmit(PrismTerrainRenderGroupBoundary.Snapshot snapshot) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ShaderPackRuntime.terrainProgram(TERRAIN_PROGRAM_ID) == null) return false;

        PrismTerrainReplacementCoordinator active = coordinator();
        EnumMap<ChunkSectionLayer, PrismTerrainReplacementPlan.Plan> plans =
                new EnumMap<>(ChunkSectionLayer.class);
        EnumMap<ChunkSectionLayer, String> programIds = new EnumMap<>(ChunkSectionLayer.class);

        for (ChunkSectionLayer layer : snapshot.group().layers()) {
            PrismTerrainVanillaPassSnapshot.LayerInput input = snapshot.passInputs().layers().get(layer);
            if (input == null || !input.pipelineApplied() || input.orderedDraws().isEmpty()) continue;
            PrismTerrainReplacementPlan.Plan plan =
                    active.prepare(level, snapshot, layer, TERRAIN_PROGRAM_ID, true);
            if (!plan.mayReplace()) return false;
            plans.put(layer, plan);
            programIds.put(layer, TERRAIN_PROGRAM_ID);
        }
        if (plans.isEmpty()) return false;

        Optional<PrismTerrainPreparedRenderBatch> prepared =
                active.prepareDrawBatch(level, snapshot, plans, programIds);
        if (prepared.isEmpty()) return false;

        PrismTerrainPreparedRenderBatch batch = prepared.get();
        boolean cancelVanilla = false;
        try {
            GpuDevice device = RenderSystem.getDevice();
            if (device == null) return false;
            batch.submitToCapturedPass(device, "Prism terrain replacement: " + snapshot.group().label());
            cancelVanilla = true;
            return true;
        } catch (RuntimeException | Error failure) {
            cancelVanilla = !batch.mayFallBackToVanilla();
            PlanetEssentials.LOGGER.warn(
                    "Prism terrain takeover failed for group {}; {} vanilla fallback",
                    snapshot.group().label(), cancelVanilla ? "suppressing duplicate" : "allowing",
                    failure);
            return cancelVanilla;
        } finally {
            try {
                batch.close();
            } catch (RuntimeException closeFailure) {
                PlanetEssentials.LOGGER.warn("Could not release prepared Prism terrain batch", closeFailure);
            }
        }
    }

    private static PrismTerrainReplacementCoordinator coordinator() {
        if (coordinator == null) coordinator = new PrismTerrainReplacementCoordinator();
        return coordinator;
    }

    /** Called during client shader shutdown on the render thread or queued there. */
    public static void shutdown() {
        PrismTerrainReplacementCoordinator previous = coordinator;
        coordinator = null;
        if (previous != null) previous.close();
    }
}
