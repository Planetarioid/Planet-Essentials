# Prism Shader System

## Current implementation
Prism includes bounded schema-1 shader-pack discovery, source validation, content hashing, transactional Minecraft 1.21.11 GPU compilation, typed resource bindings, shader enable/disable controls, persisted pack selection, a fullscreen draw primitive, owned render targets, resize-safe target management, and a validated post-process dependency graph.

The built-in world post-process hook is opt-in. A pack may declare a `worldPostProcessProgram` at the manifest root. That program must declare exactly one sampler named `SceneColor`, no custom uniform buffers or texel buffers, and use the position-only quad contract. Prism renders it into a private target while sampling Minecraft's main render target, then composites the result using Minecraft's render-target blit path. This keeps source and destination textures separate.

Example manifest shape:

```json
{
  "schema": 1,
  "id": "example:cinematic",
  "name": "Cinematic Example",
  "version": "1.0.0",
  "worldPostProcessProgram": "example:post",
  "programs": [
    {
      "id": "example:post",
      "vertex": "example:shaders/post.vsh",
      "fragment": "example:shaders/post.fsh",
      "samplers": ["SceneColor"]
    }
  ]
}
```

The program's vertex shader receives the `Position` attribute as clip-space quad coordinates. It should derive UV coordinates from that position and sample `SceneColor` in its fragment shader. The pack can omit `worldPostProcessProgram` if it only wants to provide compiled programs for other integrations.

## Pack location and limits
Packs are discovered at:
`assets/<namespace>/prism/shaderpacks/<pack-folder>/prism.json`

- Manifest: 64 KiB.
- Each shader stage: 1 MiB UTF-8.
- Programs per pack: 128.
- Aggregate unique source: 16 MiB.
- Binding names are unique GLSL identifiers, with at most 16 of each kind.
- Resource paths reject traversal, duplicate separators, and backslashes.
- ResourceManager resolution respects active resource-pack priority.
- Duplicate program IDs across distinct packs are excluded rather than arbitrarily overriding one another.
- A failed compile keeps the previous active compiled generation.

## In-game controls
Open Planet Essentials -> Shader Settings. Prism is disabled by default. The screen allows users to enable or disable shader effects, cycle through discovered packs, and rescan active resource packs. Disabling Prism bypasses the built-in post-process hook immediately; it does not require a restart.

## Compatibility and verification boundary
Fabric's world-render drawing events are used instead of broad LevelRenderer injections. The built-in post-process hook runs at END_MAIN, after the main terrain/entity/translucency pass and before particles, clouds, weather, hand, and GUI rendering. It does not replace terrain or entity material pipelines.

This is not yet an Iris-equivalent renderer. Terrain/entity shader integration, multi-pass graph resource allocation and execution, full pack feature compatibility, renderer negotiation, in-game visual regression, GPU leak testing, and controlled performance benchmarks remain open. CI and unit tests do not prove driver behavior or visual correctness. Prism makes no performance-superiority claim over Iris.
