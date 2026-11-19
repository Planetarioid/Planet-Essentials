package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.rendertype.OutputTarget;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import com.planetstudios.essentials.PlanetEssentials;

/**
 * Optional full-screen world post-process pass. It renders into a private target
 * while sampling the main render target, then uses Minecraft's render-target blit
 * path to composite the result back. It never samples from the texture currently
 * attached as its render destination.
 */
public final class PrismWorldPostProcessor {
    public static final String SCENE_COLOR_SAMPLER = "SceneColor";
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
            if (device == null || main == null) return;
            if (main.getColorTexture() == null || main.getColorTextureView() == null) return;
            ensureDeviceResources(device);

            PrismRenderTarget scratch = TARGETS.ensureTarget(
                    device, "planet:world_post_source",
                    main.width, main.height, false);
            var layout = ShaderPackRuntime.programBindings(programId);
            if (layout == null || !layout.uniformBuffers().isEmpty()
                    || !layout.texelBuffers().isEmpty()
                    || !layout.samplers().equals(java.util.List.of(SCENE_COLOR_SAMPLER))) {
                throw new IllegalStateException("World post-process program must declare only the SceneColor sampler");
            }

            MinecraftShaderBindings bindings = MinecraftShaderBindings.builder()
                    .sampler(SCENE_COLOR_SAMPLER, main.getColorTextureView(), sceneSampler)
                    .build();
            var encoder = device.createCommandEncoder();
            try (RenderPass pass = scratch.beginPass(encoder, "Prism world post-process",
                    OptionalInt.of(0), OptionalDouble.empty())) {
                if (!PrismDrawExecutor.draw(pass, programId, fullscreenQuad.buffer(),
                        0, 4, bindings, layout)) return;
            }
            // Minecraft owns the main render target and performs the final composite.
            main.blitAndBlendToTexture(scratch.colorView());
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
            TARGETS.close();
        } else {
            RenderSystem.queueFencedTask(() -> {
                releaseDeviceResources();
                TARGETS.close();
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
