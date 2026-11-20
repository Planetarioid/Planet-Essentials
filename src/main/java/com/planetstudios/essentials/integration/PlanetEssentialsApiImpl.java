package com.planetstudios.essentials.integration;

import com.planetstudios.planetlib.essentials.PlanetEssentialsApi;
import com.planetstudios.planetlib.essentials.PlanetIntegration;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Common-side implementation of the optional Planet Lib API entrypoint.
 * This class deliberately has no client-only dependencies.
 */
public final class PlanetEssentialsApiImpl implements PlanetEssentialsApi {
    private final IntegrationRegistry registry;
    private final Predicate<String> loadedModCheck;

    /** Fabric entrypoint constructor. */
    public PlanetEssentialsApiImpl() {
        this(IntegrationRegistry.getInstance(),
                modId -> FabricLoader.getInstance().isModLoaded(modId));
    }

    PlanetEssentialsApiImpl(IntegrationRegistry registry, Predicate<String> loadedModCheck) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.loadedModCheck = Objects.requireNonNull(loadedModCheck, "loadedModCheck");
    }

    @Override
    public int apiVersion() {
        return PlanetEssentialsApi.API_VERSION;
    }

    @Override
    public void registerIntegration(PlanetIntegration integration) {
        Objects.requireNonNull(integration, "integration");
        if (!integration.modId().startsWith("planet_")) {
            throw new IllegalArgumentException("Only Planet Studios mod IDs may register with Essentials.");
        }
        if (!loadedModCheck.test(integration.modId())) {
            throw new IllegalArgumentException("Cannot register an integration for an unloaded mod: "
                    + integration.modId());
        }
        if (!registry.register(integration)) {
            throw new IllegalStateException("Conflicting integration metadata for mod: "
                    + integration.modId());
        }
    }
}
