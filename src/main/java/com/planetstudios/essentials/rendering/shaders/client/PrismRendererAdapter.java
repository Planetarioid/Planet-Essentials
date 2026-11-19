package com.planetstudios.essentials.rendering.shaders.client;

import java.util.Set;

/**
 * Optional bridge implemented by a dedicated renderer integration module.
 * Implementations must only advertise hooks they actually own and have tested.
 */
public interface PrismRendererAdapter {
    String rendererId();

    /** Human-readable tested renderer version or range, for diagnostics. */
    String testedVersion();

    Set<Feature> supportedFeatures();

    enum Feature {
        CHUNK_TERRAIN_PIPELINE,
        BLOCK_ENTITY_PIPELINE,
        ENTITY_PIPELINE,
        EXTRACTION_STATE_TRANSFER,
        WORLD_POST_PROCESS_INTEROP
    }
}
