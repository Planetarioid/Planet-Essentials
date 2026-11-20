package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * All-or-nothing prepared replacement for one complete vanilla terrain layer
 * group. Every layer is submitted into one shared pass, preserving the
 * attachment clear semantics of vanilla's single renderGroup pass.
 *
 * <p>This class is not wired into vanilla cancellation. Callers must retain
 * vanilla rendering until runtime parity and failure handling are proven.</p>
 */
public final class PrismTerrainPreparedRenderBatch implements AutoCloseable {
    private final ClientLevel level;
    private final PrismTerrainRenderGroupBoundary.Snapshot snapshot;
    private final List<PrismTerrainPreparedRenderGroup> layers;
    private final PrismTerrainSubmissionState submissionState = new PrismTerrainSubmissionState();
    private boolean closed;

    public PrismTerrainPreparedRenderBatch(ClientLevel level,
            PrismTerrainRenderGroupBoundary.Snapshot snapshot,
            List<PrismTerrainPreparedRenderGroup> layers) {
        this.level = Objects.requireNonNull(level, "level");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
        if (this.layers.isEmpty()) throw new IllegalArgumentException("Prepared terrain batch cannot be empty");
        if (Minecraft.getInstance().level != level) {
            throw new IllegalArgumentException("Prepared terrain batch belongs to an inactive world");
        }

        ArrayList<ChunkSectionLayer> expected = new ArrayList<>();
        for (ChunkSectionLayer layer : snapshot.group().layers()) {
            PrismTerrainVanillaPassSnapshot.LayerInput input = snapshot.passInputs().layers().get(layer);
            if (input != null && input.pipelineApplied() && !input.orderedDraws().isEmpty()) expected.add(layer);
        }
        if (expected.size() != this.layers.size()) {
            throw new IllegalArgumentException("Prepared batch does not cover every non-empty vanilla layer");
        }

        Set<String> uniformNames = null;
        for (int i = 0; i < this.layers.size(); i++) {
            PrismTerrainPreparedRenderGroup prepared = Objects.requireNonNull(this.layers.get(i), "prepared layer");
            ChunkSectionLayer expectedLayer = expected.get(i);
            if (prepared.layer() != expectedLayer || !prepared.isValidFor(level, snapshot)) {
                throw new IllegalArgumentException("Prepared terrain layers are stale or not in vanilla order");
            }
            List<?> vanillaDraws = snapshot.passInputs().layers().get(expectedLayer).orderedDraws();
            if (prepared.draws().size() != vanillaDraws.size()) {
                throw new IllegalArgumentException("Prepared layer does not replace the complete vanilla draw list");
            }
            for (int drawIndex = 0; drawIndex < vanillaDraws.size(); drawIndex++) {
                if (prepared.draws().get(drawIndex).vanillaDraw() != vanillaDraws.get(drawIndex)) {
                    throw new IllegalArgumentException("Prepared draw order differs from vanilla layer order");
                }
            }
            if (uniformNames == null) uniformNames = prepared.externallyBoundUniforms();
            else if (!uniformNames.equals(prepared.externallyBoundUniforms())) {
                throw new IllegalArgumentException("Prepared layers disagree on captured default-uniform bindings");
            }
        }
    }

    /**
     * Opens exactly one pass for the complete captured group, binds shared
     * defaults once, and submits every prepared layer in vanilla order.
     */
    public void submitToCapturedPass(GpuDevice device, String debugLabel) {
        RenderSystem.assertOnRenderThread();
        ensureOpen();
        if (Minecraft.getInstance().level != level
                || layers.stream().anyMatch(layer -> !layer.isValidFor(level, snapshot))) {
            throw new IllegalStateException("Prepared terrain batch became stale before pass creation");
        }
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(debugLabel, "debugLabel");

        try (RenderPass pass = snapshot.passInputs().openCapturedTargetPass(device, debugLabel)) {
            Set<String> boundUniforms = snapshot.passInputs().bindCapturedDefaultUniforms(pass);
            if (!boundUniforms.equals(layers.get(0).externallyBoundUniforms())) {
                throw new IllegalStateException("Captured default-uniform bindings changed after batch preflight");
            }
            submissionState.begin();
            try {
                for (PrismTerrainPreparedRenderGroup layer : layers) {
                    layer.submitDrawsInBatch(pass, submissionState);
                }
                submissionState.complete();
            } catch (RuntimeException | Error failure) {
                submissionState.fail();
                throw failure;
            }
        }
    }

    public boolean mayFallBackToVanilla() {
        return submissionState.mayFallBackToVanilla();
    }

    public PrismTerrainSubmissionState.Phase submissionPhase() {
        return submissionState.phase();
    }

    public int drawSubmissionAttempts() {
        return submissionState.drawSubmissionAttempts();
    }

    public List<PrismTerrainPreparedRenderGroup> layers() {
        ensureOpen();
        return layers;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Prepared terrain batch is closed");
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        for (PrismTerrainPreparedRenderGroup layer : layers) {
            try {
                layer.close();
            } catch (RuntimeException closeFailure) {
                if (failure == null) failure = closeFailure;
                else failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) throw failure;
    }
}
