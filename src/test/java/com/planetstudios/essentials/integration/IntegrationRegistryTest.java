package com.planetstudios.essentials.integration;

import com.planetstudios.planetlib.essentials.PlanetIntegration;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationRegistryTest {
    @Test
    void registersAndReturnsImmutableSnapshot() {
        IntegrationRegistry registry = new IntegrationRegistry();
        PlanetIntegration integration = new PlanetIntegration(
                "planet_projectile_optimizations", "Planet Projectile Optimizations",
                Set.of("projectile:vanilla"));

        assertTrue(registry.register(integration));
        Map<String, PlanetIntegration> snapshot = registry.snapshot();
        assertEquals(integration, snapshot.get(integration.modId()));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put("planet_other", integration));
    }

    @Test
    void identicalRegistrationIsIdempotentAndConflictsAreRejected() {
        IntegrationRegistry registry = new IntegrationRegistry();
        PlanetIntegration first = new PlanetIntegration(
                "planet_texture", "Planet Textures", Set.of("resource:texture"));
        PlanetIntegration conflicting = new PlanetIntegration(
                "planet_texture", "Different Display Name", Set.of("resource:texture"));

        assertTrue(registry.register(first));
        assertTrue(registry.register(first));
        assertFalse(registry.register(conflicting));
        assertEquals(1, registry.size());
        assertEquals(first, registry.find("planet_texture").orElseThrow());
    }
}
