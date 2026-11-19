package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.rendertype.OutputTarget;

import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.PrismPostProcessGraph;

/** Optional world post-process graph integration at the END_MAIN drawing stage. */
public final class PrismWorldPostProcessor {
    private static final PrismRenderTargetManager TARGETS = new PrismRenderTargetManager();
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static GpuDevice ownerDevice;
    private static GpuSampler sceneSampler;
    private static PrismFullscreenQuad fullscreenQuad;
    private static long failedGeneration = -1;

    private PrismWorldPostProcessor() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        PrismFrameStageRegistry.register(PrismFrameStage.END_MAIN, context -> render());
    }

    private static void render() {
        if (!ShaderPackRuntime.isEnabled()) return;
        String programId = ShaderPackRuntime.worldPostProcessProgramId();
        if (programId.isBlank()) return;
        long generation = ShaderPackRuntime.pipelineSnapshot().generation();
        if (failedGeneration == generation) return;

        try {
            RenderSystem.assertOnRenderThread();
            GpuDevice device = RenderSystem.tryGetDevice();
            RenderTarget main = OutputTarget.MAIN_TARGET.getRenderTarget();
            if (device == null || main == null || main.getColorTextureView() == null) return;
            ensureDeviceResources(device);

            PrismPostProcessGraph graph = PrismPostProcessGraph.builder()
                    .externalInput(PrismPostProcessExecutor.SCENE_COLOR)
                    .pass(new PrismPostProcessGraph.Pass(
                            "planet:world_post", programId,
                            List.of(PrismPostProcessExecutor.SCENE_COLOR),
                            "planet:world_post_output"))
                    .output("planet:world_post_output")
                    .build();

            if (!PrismPostProcessExecutor.execute(
                    graph, main, device, sceneSampler, fullscreenQuad, TARGETS)) {
                return;
            }
        } catch (RuntimeException failure) {
            failedGeneration = generation;
            PlanetEssentials.LOGGER.error(
                    "Prism disabled world post-processing for shader generation {} after a render error",
                    generation, failure);
        }
    }

    private static void ensureDeviceResources(GpuDevice device) {
        if (ownerDevice == device && sceneSampler != null && fullscreenQuad != null) return;
        if (ownerDevice != null && ownerDevice != device) TARGETS.clear();
        releaseDeviceResources();
        ownerDevice = device;
        sceneSampler = device.createSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        fullscreenQuad = PrismFullscreenQuad.create(device);
    }

    public static void shutdown() {
        if (RenderSystem.isOnRenderThread()) {
            releaseDeviceResources();
            TARGETS.clear();
        } else {
            RenderSystem.queueFencedTask(() -> {
                releaseDeviceResources();
                TARGETS.clear();
            });
        }
    }

    private static void releaseDeviceResources() {
        if (sceneSampler != null) {
            try { sceneSampler.close(); } catch (RuntimeException ignored) {}
            sceneSampler = null;
        }
        if (fullscreenQuad != null) {
            try { fullscreenQuad.close(); } catch (RuntimeException ignored) {}
            fullscreenQuad = null;
        }
        ownerDevice = null;
    }
}
