package com.planetstudios.essentials.discovery;

import com.planetstudios.essentials.integration.IntegrationRegistry;
import com.planetstudios.planetlib.capability.CapabilityDescriptor;
import com.planetstudios.planetlib.capability.CapabilityRegistry;
import com.planetstudios.planetlib.essentials.PlanetIntegration;
import com.planetstudios.planetlib.registry.ModDescriptor;
import com.planetstudios.planetlib.registry.ModRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit, initialization-time discovery of Planet ecosystem metadata.
 * Never scans directories or inspects arbitrary mod archives.
 */
public final class ModDiscoveryService {
    private final DiscoverySource source;
    private final IntegrationRegistry integrationRegistry;

    public ModDiscoveryService() {
        this(new FabricDiscoverySource(), IntegrationRegistry.getInstance());
    }

    ModDiscoveryService(DiscoverySource source, IntegrationRegistry integrationRegistry) {
        this.source = Objects.requireNonNull(source, "source");
        this.integrationRegistry = Objects.requireNonNull(integrationRegistry, "integrationRegistry");
    }

    public ModDiscoverySnapshot refresh() {
        if (!source.isPlanetLibLoaded()) {
            return ModDiscoverySnapshot.standalone();
        }

        List<ModDescriptor> planetMods = List.copyOf(source.refreshPlanetMods());
        List<CapabilityDescriptor> capabilities = List.copyOf(source.capabilities());
        Map<String, PlanetIntegration> integrations = integrationRegistry.snapshot();
        return new ModDiscoverySnapshot(true, planetMods, capabilities, integrations);
    }

    interface DiscoverySource {
        boolean isPlanetLibLoaded();
        List<ModDescriptor> refreshPlanetMods();
        List<CapabilityDescriptor> capabilities();
    }

    private static final class FabricDiscoverySource implements DiscoverySource {
        @Override
        public boolean isPlanetLibLoaded() {
            return FabricLoader.getInstance().isModLoaded("planet_lib");
        }

        @Override
        public List<ModDescriptor> refreshPlanetMods() {
            return ModRegistry.refresh().getPlanetMods();
        }

        @Override
        public List<CapabilityDescriptor> capabilities() {
            return CapabilityRegistry.getInstance().snapshot().getCapabilities();
        }
    }
}
