package com.planetstudios.essentials.integration;

import com.planetstudios.planetlib.essentials.PlanetEssentialsApi;
import com.planetstudios.planetlib.essentials.PlanetIntegration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlanetEssentialsApiImplTest {
    @Test
    void exposesSupportedApiVersion() {
        PlanetEssentialsApi api = new PlanetEssentialsApiImpl(new IntegrationRegistry(), ignored -> true);
        assertEquals(1, api.apiVersion());
        assertEquals(PlanetEssentialsApi.API_VERSION, api.apiVersion());
    }

    @Test
    void acceptsOnlyLoadedPlanetMods() {
        IntegrationRegistry registry = new IntegrationRegistry();
        PlanetEssentialsApiImpl api = new PlanetEssentialsApiImpl(registry,
                modId -> modId.equals("planet_projectile_optimizations"));
        PlanetIntegration loaded = new PlanetIntegration(
                "planet_projectile_optimizations", "Planet Projectile Optimizations",
                Set.of("projectile:vanilla"));
        PlanetIntegration missing = new PlanetIntegration(
                "planet_missing", "Missing Planet Mod", Set.of());

        assertDoesNotThrow(() -> api.registerIntegration(loaded));
        assertThrows(IllegalArgumentException.class, () -> api.registerIntegration(missing));
        assertEquals(1, registry.size());
    }

    @Test
    void rejectsNonPlanetIdsAndConflictingMetadata() {
        IntegrationRegistry registry = new IntegrationRegistry();
        PlanetEssentialsApiImpl api = new PlanetEssentialsApiImpl(registry, ignored -> true);
        PlanetIntegration external = new PlanetIntegration("external_mod", "External", Set.of());
        PlanetIntegration first = new PlanetIntegration("planet_one", "Planet One", Set.of());
        PlanetIntegration conflict = new PlanetIntegration("planet_one", "Other Name", Set.of());

        assertThrows(IllegalArgumentException.class, () -> api.registerIntegration(external));
        api.registerIntegration(first);
        assertThrows(IllegalStateException.class, () -> api.registerIntegration(conflict));
    }
}
