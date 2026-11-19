# Prism Shader System

## Current implementation

Prism includes bounded schema-1 shader-pack discovery, source validation, content hashing, transactional Minecraft 1.21.11 GPU compilation, typed resource bindings, shader enable/disable controls, persisted pack selection, a fullscreen draw primitive, owned render targets, resize-safe target management, a validated post-process dependency graph, and a GPU multi-pass executor with deterministic target-slot reuse.

A shader pack can now define its world post-processing graph directly in `prism.json` using `worldPostProcessGraph`. Graphs support up to 32 fullscreen passes, dependency-ordered execution, one output resource per pass, and sampler inputs mapped in the order listed by each pass. The renderer currently exposes exactly one external input, `minecraft:scene_color`. Every pass program must declare the same number of samplers as the pass has inputs and may not declare uniform or texel buffers in this world post-process path.

Example two-pass pack manifest:

```json
{
  "schema": 1,
  "id": "example:cinematic",
  "name": "Cinematic Example",
  "version": "1.0.0",
  "worldPostProcessGraph": {
    "externalInputs": ["minecraft:scene_color"],
    "passes": [
      {
        "id": "example:blur",
        "program": "example:blur",
        "inputs": ["minecraft:scene_color"],
        "output": "example:blurred"
      },
      {
        "id": "example:composite",
        "program": "example:composite",
        "inputs": ["example:blurred", "minecraft:scene_color"],
        "output": "example:final"
      }
    ],
    "output": "example:final"
  },
  "programs": [
    {
      "id": "example:blur",
      "vertex": "example:shaders/fullscreen.vsh",
      "fragment": "example:shaders/blur.fsh",
      "samplers": ["SceneColor"]
    },
    {
      "id": "example:composite",
      "vertex": "example:shaders/fullscreen.vsh",
      "fragment": "example:shaders/composite.fsh",
      "samplers": ["Blurred", "Original"]
    }
  ]
}
```

Pass array order does not determine execution order. Prism topologically sorts dependencies, so the composite above may appear before the blur pass in the JSON and still execute after it. The `inputs` array is positional: its first resource is bound to the first sampler declared by the program, and so on. Resource IDs must be unique, and a pass cannot read the resource it writes.

The previous `worldPostProcessProgram` field remains supported for existing packs and is adapted internally to a one-pass graph. Do not define both fields in the same manifest.

The executor uses last-consumer analysis to reuse physical render-target slots only after an intermediate resource's final pass has consumed it. The final graph output is kept alive until compositing. Cycles, missing producers, duplicate pass IDs/outputs, sampler-count mismatches, feedback loops, unsupported external inputs, and graphs above the pass limit are rejected during manifest validation.

## Pack location and limits

Packs are discovered at:
`assets/<namespace>/prism/shaderpacks/<pack-folder>/prism.json`

- Manifest: 64 KiB.
- Each shader stage: 1 MiB UTF-8.
- Programs per pack: 128.
- Aggregate unique source: 16 MiB.
- Post-process passes per graph: 32.
- Sampler bindings per program: 16.
- Binding names are unique GLSL identifiers.
- Resource paths reject traversal, duplicate separators, and backslashes.
- ResourceManager resolution respects active resource-pack priority.
- Duplicate program IDs across distinct packs are excluded rather than arbitrarily overriding one another.
- A failed compile keeps the previous active compiled generation.

## In-game controls

Open Planet Essentials -> Shader Settings. Prism is disabled by default. The screen allows users to enable or disable shader effects, cycle through discovered packs, and rescan active resource packs. Disabling Prism bypasses the built-in post-process hook immediately; it does not require a restart.

## Compatibility and verification boundary

Fabric's world-render drawing events are used instead of broad LevelRenderer injections. The built-in world post-process hook runs at END_MAIN, after the main terrain/entity/translucency pass and before particles, clouds, weather, hand, and GUI rendering. It does not replace terrain or entity material pipelines.

This is not yet an Iris-equivalent renderer. Terrain/entity shader integration, full pack feature compatibility, renderer negotiation, in-game visual regression, GPU leak testing, and controlled performance benchmarks remain open. CI and unit tests do not prove driver behavior or visual correctness. Prism makes no performance-superiority claim over Iris.
