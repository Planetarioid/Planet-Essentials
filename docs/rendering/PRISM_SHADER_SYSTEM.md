# Prism Shader System

## Status
Prism currently has a backend-neutral program lifecycle and a bounded shader-pack manifest/source loader. These components validate pack metadata, reject unsafe resource paths, cap manifest and source sizes, enforce a total pack-source budget, and produce immutable program specifications.

This is **not yet an Iris-equivalent shader-pack runtime**. The loader accepts a resolver abstraction; it is not yet connected to Minecraft's active ResourceManager. No concrete Minecraft GPU compiler/backend, uniform/buffer/framebuffer binding system, or world-render integration is included yet.

## Pack format (schema 1)
A pack manifest is JSON with:
- `schema`: required integer, currently 1.
- `id`, `name`, `version`: required bounded strings.
- `description`, `author`: optional bounded strings.
- `programs`: 1 to 128 program entries, each with a unique namespaced `id`, and namespaced `vertex` and `fragment` resource identifiers.

Example:

```json
{
  "schema": 1,
  "id": "example:soft_lighting",
  "name": "Soft Lighting",
  "version": "1.0.0",
  "programs": [
    {
      "id": "example:world",
      "vertex": "example:shaders/world.vsh",
      "fragment": "example:shaders/world.fsh"
    }
  ]
}
```

## Resource and memory limits
- Manifest: 64 KiB maximum.
- Each shader stage: 1 MiB maximum UTF-8.
- Programs per pack: 128 maximum.
- Aggregate shader source: 16 MiB maximum.
- Resource identifiers reject traversal segments, duplicate separators, and backslashes.
- Resolver implementations must enforce byte limits during reads, rather than buffering unbounded data before checking.

## Runtime contracts
- Exactly one vertex and one fragment source per program.
- Stable SHA-256 source identity.
- Backend capability snapshots are conservative until populated by a real backend.
- A reload compiles the entire candidate set before publication. Failure closes candidates and preserves the previous active generation.
- Concrete backend implementations must compile on the graphics-context thread and own/dispose native handles.
- Vanilla rendering must remain active until a candidate pipeline is fully compiled and validated.

## Remaining implementation
1. Connect pack discovery and resource resolution to Minecraft's active ResourceManager and resource-pack priority.
2. Concrete Minecraft 1.21.11 GPU shader backend and render-thread scheduler.
3. Uniform, sampler, buffer, framebuffer, and render-target binding model.
4. Actual GPU/driver capability detection and backend negotiation.
5. Defined world-render stages and shader-pack features.
6. Vanilla fallback and compatibility with resource packs, Sodium, Iris, and other renderers.
7. Visual regression, GPU memory, reload/leak, and controlled performance tests.

## Performance claims
Prism does not claim to outperform Iris. Iris is a mature shader-pack ecosystem with extensive Minecraft rendering integration. Comparisons require the same shader packs, scenes, GPU, driver, render distance, and settings. Do not copy Iris code or shader-pack assets.
