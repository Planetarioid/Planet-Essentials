# Prism Shader System

## Status
Prism now includes bounded pack metadata/source loading, active ResourceManager discovery, a Minecraft 1.21.11 GPU pipeline compiler, typed uniform-buffer/texel-buffer/sampler declarations, and a client resource-reload lifecycle that transactionally compiles discovered programs.

This is **not yet a complete Iris-equivalent shader-pack renderer**. Programs are compiled into Minecraft render-pipeline objects, but Prism does not yet substitute them into the vanilla terrain/entity renderer or provide a full post-processing graph. The current compile adapter is position-only and quad-format.

## Pack format and location
Packs are discovered at:
`assets/<namespace>/prism/shaderpacks/<pack-folder>/prism.json`

Schema 1 requires pack `id`, `name`, `version`, and `programs`. Each program has unique namespaced `id`, vertex and fragment resource IDs, and optional `uniformBuffers`, `texelBuffers`, and `samplers` arrays. Binding names are unique GLSL identifiers, with at most 16 of each kind.

## Safety and limits
- Manifest: 64 KiB.
- Each shader stage: 1 MiB UTF-8.
- Programs per pack: 128.
- Aggregate unique source: 16 MiB.
- Resource paths reject traversal, duplicate separators, and backslashes.
- UTF-8 decoding is strict and source reads are byte-limited.
- ResourceManager resolution respects active resource-pack priority for each resource path.
- Duplicate program IDs across distinct packs are excluded rather than arbitrarily overriding one another.
- A failed compile keeps the previous active compiled generation.

## GPU backend and lifecycle
The client reload listener discovers packs, updates an immutable catalog, then schedules compilation on Minecraft's render-task queue. The backend obtains the active GpuDevice, builds RenderPipeline objects with declared buffer/sampler slots, supplies custom shader source through Minecraft's shader-source callback, and invokes GpuDevice.precompilePipeline. Minecraft owns the compiled pipeline cache; Prism does not clear the global cache when pack handles close.

## Remaining implementation
1. Runtime buffer/texture binding validation and resource ownership.
2. Frame-stage contracts and actual drawing integration.
3. Owned framebuffer/render-target lifecycle and post-process graph.
4. Terrain/entity pipeline integration and shader-pack feature mapping.
5. Actual GPU/driver limit detection, renderer negotiation, and compatibility with vanilla, Sodium, and Iris.
6. Client launch, visual regression, GPU memory/leak tests, and controlled benchmarks.

## Performance claims
Prism does not claim to outperform Iris. Comparisons require identical packs, scenes, GPU, driver, render distance, and settings. Do not copy Iris code or shader-pack assets.
