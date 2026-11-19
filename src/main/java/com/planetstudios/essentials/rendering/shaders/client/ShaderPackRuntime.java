package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.ShaderPackCatalog;
import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.ShaderProgramSpec;
import com.planetstudios.essentials.rendering.shaders.ShaderPipeline;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client-side Prism shader pack discovery and transactional GPU compilation lifecycle. */
public final class ShaderPackRuntime {
    private static final Logger LOGGER = PlanetEssentials.LOGGER;
    private static final ShaderPackCatalog CATALOG = new ShaderPackCatalog();
    private static final ShaderPipeline PIPELINE = new ShaderPipeline();
    private static final MinecraftShaderBackend BACKEND = new MinecraftShaderBackend();

    private ShaderPackRuntime() {}

    public static void reload(ResourceManager resourceManager) {
        ShaderPackDiscovery.DiscoveryResult discovered = ShaderPackDiscovery.discover(resourceManager);
        List<String> errors = new ArrayList<>(discovered.errors());
        List<ShaderProgramSpec> programs = selectUniquePrograms(discovered.packs(), errors);
        var snapshot = CATALOG.replace(discovered.packs(), errors);
        LOGGER.info("Prism discovered {} shader packs and {} unique programs (generation {}).",
                snapshot.packs().size(), programs.size(), snapshot.generation());

        RenderSystem.queueFencedTask(() -> {
            boolean published = PIPELINE.reload(programs, BACKEND);
            if (published) {
                LOGGER.info("Prism compiled and published {} shader programs (generation {}).",
                        PIPELINE.snapshot().programCount(), PIPELINE.snapshot().generation());
            } else {
                LOGGER.warn("Prism shader reload failed; the previous compiled generation remains active: {}",
                        PIPELINE.snapshot().lastFailure());
            }
        });
    }

    private static List<ShaderProgramSpec> selectUniquePrograms(
            List<ShaderPackLoader.LoadedPack> packs, List<String> errors) {
        Map<String, Integer> counts = new HashMap<>();
        for (ShaderPackLoader.LoadedPack pack : packs) {
            for (ShaderProgramSpec program : pack.programs()) counts.merge(program.id(), 1, Integer::sum);
        }
        List<ShaderProgramSpec> result = new ArrayList<>();
        for (ShaderPackLoader.LoadedPack pack : packs) {
            for (ShaderProgramSpec program : pack.programs()) {
                if (counts.getOrDefault(program.id(), 0) == 1) {
                    result.add(program);
                } else {
                    errors.add("Program id appears in multiple packs and was excluded: " + program.id());
                }
            }
        }
        return List.copyOf(result);
    }

    public static ShaderPackCatalog.Snapshot catalogSnapshot() {
        return CATALOG.snapshot();
    }

    public static ShaderPipeline.Snapshot pipelineSnapshot() {
        return PIPELINE.snapshot();
    }

    public static void shutdown() {
        CATALOG.clear();
        if (RenderSystem.isOnRenderThread()) {
            PIPELINE.close();
        } else {
            RenderSystem.queueFencedTask(PIPELINE::close);
        }
    }
}
