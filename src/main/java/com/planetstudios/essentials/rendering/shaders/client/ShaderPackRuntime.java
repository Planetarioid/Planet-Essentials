package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.EssentialsRuntime;
import com.planetstudios.essentials.PlanetEssentials;
import com.planetstudios.essentials.rendering.shaders.PrismPostProcessGraph;
import com.planetstudios.essentials.rendering.shaders.ShaderBindingLayout;
import com.planetstudios.essentials.rendering.shaders.ShaderPackCatalog;
import com.planetstudios.essentials.rendering.shaders.ShaderPackLoader;
import com.planetstudios.essentials.rendering.shaders.ShaderProgramSpec;
import com.planetstudios.essentials.rendering.shaders.ShaderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.server.packs.resources.ResourceManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.slf4j.Logger;

import java.util.List;

/** Client-side Prism shader pack discovery and transactional GPU compilation lifecycle. */
public final class ShaderPackRuntime {
    private static final Logger LOGGER = PlanetEssentials.LOGGER;
    private static final ShaderPackCatalog CATALOG = new ShaderPackCatalog();
    private static final ShaderPipeline PIPELINE = new ShaderPipeline();
    private static final MinecraftShaderBackend BACKEND = new MinecraftShaderBackend();
    private static volatile ResourceManager lastResourceManager;
    private static volatile String compiledPackId = "";
    private static volatile PrismPostProcessGraph compiledPostProcessGraph;

    private ShaderPackRuntime() {}

    public static void reload(ResourceManager resourceManager) {
        lastResourceManager = resourceManager;
        ShaderPackDiscovery.DiscoveryResult discovered = ShaderPackDiscovery.discover(resourceManager);
        var config = EssentialsRuntime.config();
        String selectedId = config.selectedShaderPack;
        List<ShaderPackLoader.LoadedPack> packs = discovered.packs();
        var snapshot = CATALOG.replace(packs, discovered.errors());

        ShaderPackLoader.LoadedPack selectedPack = packs.stream()
                .filter(pack -> pack.manifest().id().equals(selectedId))
                .findFirst().orElse(null);
        List<ShaderProgramSpec> programs = selectedPack == null ? List.of() : selectedPack.programs();
        PrismPostProcessGraph postProcessGraph = selectedPack == null
                ? null : selectedPack.manifest().resolvedWorldPostProcessGraph();

        if (!selectedId.isBlank() && selectedPack == null) {
            LOGGER.warn("Selected Prism shader pack '{}' was not found in active resources.", selectedId);
        }
        LOGGER.info("Prism discovered {} shader packs and selected {} programs (catalog generation {}).",
                snapshot.packs().size(), programs.size(), snapshot.generation());

        RenderSystem.queueFencedTask(() -> {
            boolean published = PIPELINE.reload(programs, BACKEND);
            if (published) {
                compiledPackId = selectedId;
                compiledPostProcessGraph = postProcessGraph;
                LOGGER.info("Prism compiled and published {} shader programs for pack '{}' (generation {}).",
                        PIPELINE.snapshot().programCount(), compiledPackId, PIPELINE.snapshot().generation());
            } else {
                LOGGER.warn("Prism shader reload failed; the previous compiled generation remains cached: {}",
                        PIPELINE.snapshot().lastFailure());
            }
        });
    }

    public static synchronized void selectPack(String packId) {
        var config = EssentialsRuntime.config();
        config.selectedShaderPack = packId == null ? "" : packId;
        config.validate();
        EssentialsRuntime.save();
        ResourceManager manager = lastResourceManager;
        if (manager != null) reload(manager);
    }

    public static boolean isEnabled() {
        var config = EssentialsRuntime.config();
        return config.shadersEnabled && !config.selectedShaderPack.isBlank()
                && config.selectedShaderPack.equals(compiledPackId)
                && PIPELINE.snapshot().programCount() > 0;
    }

    public static String selectedPackId() {
        return EssentialsRuntime.config().selectedShaderPack;
    }

    public static String compiledPackId() {
        return compiledPackId;
    }

    public static PrismPostProcessGraph worldPostProcessGraph() {
        return isEnabled() ? compiledPostProcessGraph : null;
    }

    public static ShaderBindingLayout programBindings(String programId) {
        if (!isEnabled()) return null;
        var program = PIPELINE.program(programId);
        if (program instanceof MinecraftShaderBackend.MinecraftCompiledProgram minecraftProgram) {
            return minecraftProgram.bindings();
        }
        return null;
    }

    public static RenderPipeline renderPipeline(String programId) {
        if (!isEnabled()) return null;
        var program = PIPELINE.program(programId);
        if (program instanceof MinecraftShaderBackend.MinecraftCompiledProgram minecraftProgram) {
            return minecraftProgram.pipeline();
        }
        return null;
    }

    public static ShaderPackCatalog.Snapshot catalogSnapshot() {
        return CATALOG.snapshot();
    }

    public static ShaderPipeline.Snapshot pipelineSnapshot() {
        return PIPELINE.snapshot();
    }

    public static void shutdown() {
        CATALOG.clear();
        lastResourceManager = null;
        compiledPackId = "";
        compiledPostProcessGraph = null;
        if (RenderSystem.isOnRenderThread()) {
            PIPELINE.close();
        } else {
            RenderSystem.queueFencedTask(PIPELINE::close);
        }
    }
}
