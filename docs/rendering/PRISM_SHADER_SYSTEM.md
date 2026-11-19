# Prism Shader System

## Current implementation
Prism includes bounded schema-1 shader-pack discovery, source validation, content hashing, transactional Minecraft 1.21.11 GPU compilation, typed resource bindings, shader enable/disable controls, persisted pack selection, a fullscreen draw primitive, owned render targets, resize-safe target management, a validated post-process dependency graph, and a GPU multi-pass executor with deterministic target-slot reuse.

The built-in world post-process hook is opt-in. A pack may declare a `worldPostProcessProgram` at the manifest root. That program must declare exactly one sampler named `SceneColor`, no custom uniform buffers or texel buffers, and use the position-only quad contract. Prism routes this legacy single-pass entry through the same multi-pass executor used by graph passes. The executor validates dependencies, maps sampler declarations to graph inputs, renders into private targets, reuses target slots only after a resource's last consumer, and composites the graph output using Minecraft's render-target blit path. Source and destination textures are kept separate.

The graph execution API currently supports up to 32 ordered fullscreen passes per graph. Each pass declares a program, input resource IDs, and one output resource ID. Cycles, missing producers, duplicate outputs, read/write feedback, and graphs over the pass limit are rejected. The schema-1 pack manifest still exposes only the legacy single-pass `worldPostProcessProgram` field; authoring multi-pass graphs directly in shader-pack JSON and visual in-game validation remain follow-up work.

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

This is not yet an Iris-equivalent renderer. Terrain/entity shader integration, multi-pass graph execution is implemented as a renderer-side API, but multi-pass graph authoring in pack manifests, full pack feature compatibility, renderer negotiation, in-game visual regression, GPU leak testing, and controlled performance benchmarks remain open. CI and unit tests do not prove driver behavior or visual correctness. Prism makes no performance-superiority claim over Iris.
