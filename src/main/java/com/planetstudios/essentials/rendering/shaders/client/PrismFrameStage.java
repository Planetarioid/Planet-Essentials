package com.planetstudios.essentials.rendering.shaders.client;

/** Supported Minecraft 1.21.11 world-render drawing phases. */
public enum PrismFrameStage {
    /** After solid/cutout terrain has drawn and before vanilla entities are drawn. */
    BEFORE_ENTITIES,
    /** After vanilla entities and block entities, before translucent terrain. */
    AFTER_ENTITIES,
    /** Immediately before vanilla translucent terrain is drawn. */
    BEFORE_TRANSLUCENT,
    /** At the end of the main world pass, before particles, clouds, weather, and hand. */
    END_MAIN
}
