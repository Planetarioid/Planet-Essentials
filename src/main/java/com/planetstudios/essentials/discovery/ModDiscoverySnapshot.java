package com.planetstudios.essentials.discovery;

import com.planetstudios.planetlib.capability.CapabilityDescriptor;
import com.planetstudios.planetlib.essentials.PlanetIntegration;
import com.planetstudios.planetlib.registry.ModDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Immutable point-in-time view of optional Planet ecosystem integrations. */
public record ModDiscoverySnapshot(
        boolean planetLibAvailable,
        List<ModDescriptor> planetMods,
        List<CapabilityDescriptor> capabilities,
        Map<String, PlanetIntegration> integrations
) {
    public ModDiscoverySnapshot {
        planetMods = List.copyOf(planetMods);
        capabilities = List.copyOf(capabilities);
        integrations = Collections.unmodifiableMap(new TreeMap<>(integrations));
        if (!planetLibAvailable && (!planetMods.isEmpty() || !capabilities.isEmpty() || !integrations.isEmpty())) {
            throw new IllegalArgumentException("A standalone snapshot cannot contain Planet Lib data.");
        }
    }

    public static ModDiscoverySnapshot standalone() {
        return new ModDiscoverySnapshot(false, List.of(), List.of(), Map.of());
    }
}
