package com.planetstudios.essentials;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Planet Essentials.
 *
 * <p>Initialization stays deliberately small. Planet Lib is an optional
 * runtime integration; the API entrypoint is exposed only for installations
 * where Planet Lib is present.</p>
 */
public final class PlanetEssentials implements ModInitializer {
    public static final String MOD_ID = "planet_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger("Planet Essentials");

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isModLoaded("planet_lib")) {
            LOGGER.info("Planet Essentials initialized with Planet Lib integration available.");
        } else {
            LOGGER.info("Planet Essentials initialized in standalone mode; Planet Lib is not installed.");
        }
    }
}
