# Prism Shader System

## Status
Prism has a bounded schema-1 pack loader, a Minecraft resource-manager resolver/discovery adapter, a backend-neutral transactional program registry, and a Minecraft 1.21.11 GPU pipeline compilation adapter.

This is a meaningful compiler/discovery milestone, **not yet a complete Iris-equivalent shader-pack renderer**. The backend compiles custom shader pairs into Minecraft render pipelines using the device's pipeline precompiler. It does not yet replace vanilla terrain/entity pipelines, implement full post-processing, or render a full-world shader pack. Minecraft owns its compiled-pipeline cache; Prism deliberately does not clear that global cache during pack unload.

## Pack format and location
Packs are discovered at:
`assets/<namespace>/prism/shaderpacks/<pack-folder>/prism.json`

Manifest schema 1 contains:
- `schema`: required integer, currently 1.
- `id`, `name`, `version`: required bounded strings.
- `description`, `author`: optional bounded strings.
- `programs`: 1 to 128 program entries, each with a unique namespaced `id`, and namespaced `vertex` and `fragment` resource identifiers.

Resource identifiers resolve through Minecraft's active ResourceManager. For a given resource path, Minecraft's selected resource-pack priority determines the effective file.

## Resource and memory limits
- Manifest: 64 KiB maximum.
- Each shader stage: 1 MiB maximum UTF-8.
- Programs per pack: 128 maximum.
- Aggregate unique shader source: 16 MiB maximum.
- Resource identifiers reject traversal segments, duplicate separators, and backslashes.
- Resource reads are byte-limited before decoding and malformed UTF-8 is rejected.
- Shared source identifiers are cached once per pack load.

## GPU backend
The Minecraft backend:
- Requires Minecraft's render thread.
- Obtains the active `GpuDevice` through `RenderSystem.tryGetDevice()`.
- Builds a Minecraft `RenderPipeline` for each validated source pair.
- Supplies source text through Minecraft's `ShaderSourceGetter` callback.
- Calls `GpuDevice.precompilePipeline` and captures diagnostics on failure.
- Does not clear Minecraft's shared compiled-pipeline cache when a Prism program is closed.

This is currently a position-only, quad-format compile adapter. It proves a concrete path into Minecraft's GPU pipeline compiler, but it is not yet a general arbitrary-vertex-format shader runtime. Device limits not exposed by the supported public API remain reported as unknown (zero), not guessed.

## Render integration
Fabric 1.21.11 separates extraction from drawing. Prism should prepare immutable frame state during extraction and issue GPU work only during drawing callbacks. The selected integration points are Fabric's world-render events, not broad injections into LevelRenderer. The initial integration remains intentionally non-invasive until a defined render-stage contract and framebuffer ownership model are implemented.

## Remaining implementation
1. Frame-stage contracts and safe drawing integration.
2. Uniform/sampler schemas, typed bindings, and per-pass data.
3. Owned framebuffer/render-target lifecycle and post-process graph.
4. Actual capability limits and backend feature negotiation.
5. World terrain/entity pipeline replacement and shader-pack feature mapping.
6. Reload invalidation, compatibility tests with Sodium/Iris/vanilla, and failure fallback.
7. Client launch, visual regression, GPU memory/leak, and controlled performance tests.

## Performance claims
Prism does not claim to outperform Iris. Comparisons require identical packs, scenes, GPU, driver, render distance, and settings. Do not copy Iris code or shader-pack assets.
