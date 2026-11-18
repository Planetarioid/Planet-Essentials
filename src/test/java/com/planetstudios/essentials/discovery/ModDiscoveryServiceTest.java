package com.planetstudios.essentials.discovery;

import com.planetstudios.essentials.integration.IntegrationRegistry;
import com.planetstudios.planetlib.capability.CapabilityDescriptor;
import com.planetstudios.planetlib.capability.CapabilityId;
import com.planetstudios.planetlib.registry.ModDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ModDiscoveryServiceTest {
    @Test
    void missingPlanetLibProducesEmptyStandaloneSnapshotWithoutScanning() {
        ModDiscoveryService.DiscoverySource source = new ModDiscoveryService.DiscoverySource() {
            @Override public boolean isPlanetLibLoaded() { return false; }
            @Override public List<ModDescriptor> refreshPlanetMods() {
                fail("Must not query Planet Lib when it is absent");
                return List.of();
            }
            @Override public List<CapabilityDescriptor> capabilities() {
                fail("Must not query capabilities when Planet Lib is absent");
                return List.of();
            }
        };

        ModDiscoverySnapshot snapshot = new ModDiscoveryService(source, new IntegrationRegistry()).refresh();
        assertFalse(snapshot.planetLibAvailable());
        assertTrue(snapshot.planetMods().isEmpty());
        assertTrue(snapshot.capabilities().isEmpty());
        assertTrue(snapshot.integrations().isEmpty());
    }

    @Test
    void loadedPlanetLibPublishesImmutableMetadataSnapshot() {
        ModDescriptor mod = new ModDescriptor(
                "planet_projectile_optimizations", "Planet Projectile Optimizations",
                "0.1.0", "Projectile processing optimizations", "fabric.mod",
                Set.of(), null, true);
        CapabilityDescriptor capability = new CapabilityDescriptor(
                new CapabilityId("planet_projectile_optimizations", "vanilla_projectiles"),
                "Vanilla projectile processing capability");
        ModDiscoveryService.DiscoverySource source = new ModDiscoveryService.DiscoverySource() {
            @Override public boolean isPlanetLibLoaded() { return true; }
            @Override public List<ModDescriptor> refreshPlanetMods() { return List.of(mod); }
            @Override public List<CapabilityDescriptor> capabilities() { return List.of(capability); }
        };

        ModDiscoverySnapshot snapshot = new ModDiscoveryService(source, new IntegrationRegistry()).refresh();
        assertTrue(snapshot.planetLibAvailable());
        assertEquals(List.of(mod), snapshot.planetMods());
        assertEquals(List.of(capability), snapshot.capabilities());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.planetMods().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.capabilities().clear());
    }
}
