# Prism Terrain and Entity Renderer Integration

## What this integration provides

Prism now exposes a cooperative material registry and drawing-stage dispatcher for mods that own their terrain-like or entity-like geometry. A renderer can register a material ID and the Prism program it expects, resolve that material against the currently compiled shader pack, and fall back to its normal Minecraft material whenever Prism is disabled or the requested program is unavailable.

The API deliberately does not claim to replace Minecraft's built-in chunk, entity, or block-entity material pipelines. Fabric's world-render events are phase callbacks, not per-vertex or per-material interception points. Existing vanilla terrain and entity geometry therefore continues to use the renderer selected by Minecraft or another installed renderer.

## Frame stages

- `BEFORE_ENTITIES`: after solid/cutout terrain has been drawn and before vanilla entities.
- `AFTER_ENTITIES`: after vanilla entities and block entities, before translucent terrain.
- `BEFORE_TRANSLUCENT`: immediately before translucent terrain.
- `END_MAIN`: after the main terrain/entity/translucency pass and before particles, clouds, and weather.

These are drawing callbacks. Geometry extraction and CPU preparation should happen earlier in the owning mod's extraction/update phase. Do not retain a `WorldRenderContext` beyond the callback.

## Cooperative material registration

A client renderer can register a material once during client initialization:

```java
PrismMaterialRegistry.Registration registration =
    PrismMaterialRegistry.register(new PrismMaterial(
        "example:custom_terrain",
        "example:terrain_program",
        PrismFrameStage.BEFORE_ENTITIES
    ));
```

At draw time, resolve the material:

```java
PrismMaterialRegistry.resolve("example:custom_terrain").ifPresentOrElse(
    resolved -> {
        // Use resolved.pipeline() and resolved.bindings() in the renderer's
        // own RenderPass, with geometry and resource bindings it owns.
    },
    () -> {
        // Draw using the renderer's normal Minecraft material.
    }
);
```

The registry does not own geometry buffers, textures, samplers, or render passes. The registering renderer owns those resources and must release them on its normal lifecycle. Close the returned registration when the integration unloads.

## Current boundary and next work

This is the renderer extension contract and stage dispatch foundation, not automatic shader replacement for vanilla chunk meshes or vanilla entity models. Direct integration into those pipelines requires renderer-specific hooks and compatibility work for each supported renderer. Prism must negotiate with Sodium/Iris/other renderers rather than assume their internal draw paths.

Remaining work includes built-in terrain chunk pipeline integration, entity model/material integration, extraction-to-draw state transfer, renderer negotiation, vanilla fallback validation, in-game visuals, GPU leak checks, and performance measurements.
