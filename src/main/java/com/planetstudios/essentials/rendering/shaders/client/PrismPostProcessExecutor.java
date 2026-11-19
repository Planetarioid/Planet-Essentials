package com.planetstudios.essentials.rendering.shaders.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.planetstudios.essentials.rendering.shaders.PrismPostProcessGraph;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Executes a validated multi-pass graph using reusable, non-aliasing GPU targets.
 * All passes are fullscreen draws. Sampler declarations map positionally to the
 * pass input list, making binding order deterministic and keeping graph metadata
 * independent from Minecraft GPU objects.
 */
public final class PrismPostProcessExecutor {
    public static final String SCENE_COLOR = "minecraft:scene_color";

    private PrismPostProcessExecutor() {}

    public static boolean execute(
            PrismPostProcessGraph graph,
            RenderTarget mainTarget,
            GpuDevice device,
            GpuSampler sampler,
            PrismFullscreenQuad quad,
            PrismRenderTargetManager targets) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(mainTarget, "mainTarget");
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(sampler, "sampler");
        Objects.requireNonNull(quad, "quad");
        Objects.requireNonNull(targets, "targets");

        if (!graph.externalInputs().contains(SCENE_COLOR)) {
            throw new IllegalArgumentException("A world post-process graph must declare minecraft:scene_color");
        }
        if (mainTarget.getColorTextureView() == null) return false;

        Map<String, Integer> slots = allocateSlots(graph);
        Map<String, PrismRenderTarget> produced = new HashMap<>();
        Map<String, GpuTextureView> external = Map.of(SCENE_COLOR, mainTarget.getColorTextureView());
        int width = mainTarget.width;
        int height = mainTarget.height;

        for (int index = 0; index < graph.passes().size(); index++) {
            PrismPostProcessGraph.Pass passSpec = graph.passes().get(index);
            ShaderBindingLayout layout = ShaderPackRuntime.programBindings(passSpec.programId());
            if (layout == null) return false;
            if (!layout.uniformBuffers().isEmpty() || !layout.texelBuffers().isEmpty()) {
                throw new IllegalStateException("Fullscreen graph passes currently accept sampler bindings only");
            }
            if (layout.samplers().size() != passSpec.inputs().size()) {
                throw new IllegalStateException("Pass " + passSpec.id() + " declares "
                        + layout.samplers().size() + " samplers for " + passSpec.inputs().size() + " inputs");
            }

            int slot = slots.get(passSpec.output());
            PrismRenderTarget output = targets.ensureTarget(device,
                    "planet:postprocess_slot_" + slot, width, height, false);
            MinecraftShaderBindings.Builder bindingBuilder = MinecraftShaderBindings.builder();
            for (int bindingIndex = 0; bindingIndex < passSpec.inputs().size(); bindingIndex++) {
                String resource = passSpec.inputs().get(bindingIndex);
                GpuTextureView view = external.get(resource);
                if (view == null) {
                    PrismRenderTarget inputTarget = produced.get(resource);
                    if (inputTarget == null) {
                        throw new IllegalStateException("Graph input was not produced: " + resource);
                    }
                    view = inputTarget.colorView();
                }
                bindingBuilder.sampler(layout.samplers().get(bindingIndex), view, sampler);
            }

            try (RenderPass renderPass = output.beginPass(device.createCommandEncoder(),
                    "Prism pass " + passSpec.id(), OptionalInt.of(0), OptionalDouble.empty())) {
                if (!PrismDrawExecutor.draw(renderPass, passSpec.programId(), quad.buffer(),
                        0, 4, bindingBuilder.build(), layout)) {
                    return false;
                }
            }
            produced.put(passSpec.output(), output);
        }

        PrismRenderTarget result = produced.get(graph.output());
        if (result == null) throw new IllegalStateException("Graph output was not produced");
        mainTarget.blitAndBlendToTexture(result.colorView());
        return true;
    }

    /**
     * Linear-scan allocation: resources whose last consumer has completed may share
     * a physical target slot. The graph output remains live through final composite.
     */
    static Map<String, Integer> allocateSlots(PrismPostProcessGraph graph) {
        List<PrismPostProcessGraph.Pass> passes = graph.passes();
        Map<String, Integer> producer = new HashMap<>();
        Map<String, Integer> lastUse = new HashMap<>();
        for (int i = 0; i < passes.size(); i++) {
            PrismPostProcessGraph.Pass pass = passes.get(i);
            producer.put(pass.output(), i);
            lastUse.putIfAbsent(pass.output(), i);
            for (String input : pass.inputs()) {
                if (producer.containsKey(input)) lastUse.put(input, i);
            }
        }
        lastUse.put(graph.output(), passes.size());

        record Slot(int id, int availableAfter) {}
        List<Slot> slots = new ArrayList<>();
        Map<String, Integer> assignment = new HashMap<>();
        for (int i = 0; i < passes.size(); i++) {
            String resource = passes.get(i).output();
            int chosen = -1;
            for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                if (slots.get(slotIndex).availableAfter() < i) {
                    chosen = slotIndex;
                    break;
                }
            }
            if (chosen < 0) {
                chosen = slots.size();
                slots.add(new Slot(chosen, lastUse.getOrDefault(resource, i)));
            } else {
                slots.set(chosen, new Slot(chosen, lastUse.getOrDefault(resource, i)));
            }
            assignment.put(resource, chosen);
        }
        return Map.copyOf(assignment);
    }
}
