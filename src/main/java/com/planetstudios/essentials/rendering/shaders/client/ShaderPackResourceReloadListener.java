package com.planetstudios.essentials.rendering.shaders.client;

import com.planetstudios.essentials.PlanetEssentials;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/** Reload hook for shader pack discovery and transactional GPU program compilation. */
public final class ShaderPackResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(
            PlanetEssentials.MOD_ID, "prism_shader_packs");
    private static final Logger LOGGER = PlanetEssentials.LOGGER;

    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new ShaderPackResourceReloadListener());
    }

    @Override
    public Identifier getFabricId() {
        return LISTENER_ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        try {
            ShaderPackRuntime.reload(resourceManager);
        } catch (RuntimeException exception) {
            LOGGER.warn("Prism shader pack discovery failed; keeping the last published pipeline.", exception);
        }
    }
}
