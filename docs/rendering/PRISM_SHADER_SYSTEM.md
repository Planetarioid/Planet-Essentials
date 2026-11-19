# Prism Shader System

## Status
Prism has a bounded schema-1 pack loader, Minecraft resource-manager discovery, a backend-neutral transactional registry, a Minecraft 1.21.11 GPU pipeline compilation adapter, and validated uniform-buffer/texel-buffer/sampler declarations.

This is **not yet a complete Iris-equivalent shader-pack renderer**. The backend compiles custom shader pairs into Minecraft render pipelines and registers declared bindings, but it does not yet replace vanilla terrain/entity pipelines, execute a full post-processing graph, or render a complete shader pack across the world.

## Pack format and location
Packs are discovered at:
`assets/<namespace>/prism/shaderpacks/<pack-folder>/prism.json`

Example program:
```json
{
  "id": "example:world",
  "vertex": "example:shaders/world.vsh",
  "fragment": "example:shaders/world.fsh",
  "uniformBuffers": ["PrismGlobals"],
  "texelBuffers": ["LightData"],
  "samplers": ["DiffuseSampler", "DepthSampler"]
}
```

Manifest schema 1 also requires pack `id`, `name`, `version`, and a `programs` array. Binding arrays are optional. Binding names must be unique GLSL identifiers; at most 16 of each kind are accepted.

## Limits and resolution
- Manifest: 64 KiB.
- Each shader stage: 1 MiB UTF-8.
- Programs per pack: 128.
- Aggregate unique source: 16 MiB.
- Binding declarations: up to 16 uniform buffers, 16 texel buffers, and 16 samplers per program.
- Resource paths reject traversal, duplicate separators, and backslashes.
- UTF-8 decoding is strict; source reads enforce limits before decoding.
- Active Minecraft ResourceManager resolution respects selected resource-pack priority.

## GPU backend
The Minecraft backend requires the render thread, obtains the active GpuDevice, constructs a RenderPipeline, declares its buffer/sampler bindings, and calls GpuDevice.precompilePipeline with a custom shader-source callback. Minecraft owns the compiled pipeline cache. Prism does not clear the shared cache when a program is closed.

Current compile adapter uses a position-only quad vertex format. It is a concrete pipeline compiler integration, not yet a general arbitrary vertex-format or complete world shader renderer. Device limits not exposed by supported APIs remain unknown rather than guessed.

## Render model and remaining work
Fabric 1.21.11 splits extraction from drawing. Prism must prepare immutable frame state during extraction and perform GPU work only in drawing callbacks. Its intended world integration uses Fabric world-render events rather than broad LevelRenderer injections.

Still required:
1. Typed runtime buffer/texture binding validation and resource ownership.
2. Frame-stage contracts and safe drawing integration.
3. Owned framebuffer/render-target lifecycle and post-process graph.
4. Terrain/entity pipeline integration and shader-pack feature mapping.
5. Actual GPU limits, renderer negotiation, and compatibility with vanilla, Sodium, and Iris.
6. Reload invalidation, client launch, visual regression, GPU memory/leak tests, and controlled benchmarks.

## Performance claims
Prism does not claim to outperform Iris. Comparisons require identical packs, scenes, GPU, driver, render distance, and settings. Do not copy Iris code or shader-pack assets.
