# Prism Renderer Compatibility Layer

## Purpose

Prism must not assume that every Fabric installation uses Minecraft's vanilla renderer. Sodium, Iris, Canvas, VulkanMod, and other renderer projects can own or alter different parts of the rendering path. Their presence is detected without linking Planet Essentials against their implementation classes.

## Current detection

The compatibility probe records known renderer mod IDs and classifies the primary environment:

- Vanilla
- Sodium (Iris is tracked separately as a shader/renderer integration signal)
- Canvas
- VulkanMod
- Unknown (for legacy OptiFabric detection)

This is environment detection only. It is not a compatibility certification.

## Safety policy

No automatic terrain or entity pipeline replacement is enabled for any renderer today. The current Prism world post-process and cooperative material APIs remain separate integration paths. A renderer is considered supported for pipeline replacement only after a dedicated adapter has been implemented, tested against that renderer's supported version, and registered explicitly.

Prism must not reflect into renderer internals, guess mixin targets, or silently patch a renderer based only on its mod ID. When an adapter is absent, renderers keep their normal pipeline. This is the required fallback, not an error.

## Adapter contract required before implementation

A future adapter must declare:

1. Stable renderer ID and tested version range.
2. Which hooks it owns: chunk terrain, block entities, entities, extraction, or draw.
3. Thread and lifetime rules for extracted render state.
4. Whether it can coexist with Iris and other installed renderer extensions.
5. How it declines unsupported shader packs and returns to vanilla rendering.
6. Integration tests and an in-game smoke test for each supported version.

## Current limitations

- No Sodium, Iris, Canvas, or VulkanMod adapter is shipped.
- No built-in chunk mesh or vanilla entity pipeline replacement is shipped.
- Detection does not imply that Prism post-processing is visually compatible with another renderer.
- The renderer report is informational; it does not yet automatically disable shader features.
