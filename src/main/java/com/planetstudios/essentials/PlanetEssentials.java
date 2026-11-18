package com.planetstudios.essentials;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Planet Essentials.
 *
 * <p>Initialization stays deliberately small. Expensive client systems,
 * render hooks, and optional integrations must be initialized only by their
 * owning lifecycle and only when required.</p>
 */
public final class PlanetEssentials implements ModInitializer {
    public static final String MOD_ID = "planet_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger("Planet Essentials");

    @Override
    public void onInitialize() {
        LOGGER.info("Planet Essentials initialized.");
    }
}
