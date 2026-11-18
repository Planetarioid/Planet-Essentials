package com.planetstudios.essentials;

import com.planetstudios.essentials.discovery.ModDiscoveryService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Planet Essentials.
 *
 * <p>Initialization stays deliberately small. Planet Lib is an optional
 * runtime integration; its API is accessed only when the mod is present.</p>
 */
public final class PlanetEssentials implements ModInitializer {
    public static final String MOD_ID = "planet_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger("Planet Essentials");

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("planet_lib")) {
            var snapshot = new ModDiscoveryService().refresh();
            LOGGER.info("Planet Essentials initialized with Planet Lib: {} Planet mods and {} declared capabilities discovered.",
                    snapshot.planetMods().size(), snapshot.capabilities().size());
        } else {
            LOGGER.info("Planet Essentials initialized in standalone mode; Planet Lib is not installed.");
        }
    }
}
