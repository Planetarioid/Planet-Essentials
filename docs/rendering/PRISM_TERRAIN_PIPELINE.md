# Prism Terrain Pipeline Foundation

## What this adds

Prism terrain currently has:

- A bounded CPU quad mesh builder and immutable extraction-to-draw frame handoff.
- A versioned 36-byte full-attribute V1 payload: XYZ position, UV0, RGBA8 color, packed light, packed overlay, packed normal, and one byte of vertex padding.
- A Minecraft 1.21.11 VertexFormat whose attribute offsets and stride are checked against the serializer.
- A dedicated shader compilation path for terrain V1 programs, separate from Prism's general POSITION-only shader path.
- Render-thread GPU upload into owned vertex buffers.
- A render-thread-confined, 128 MiB default LRU GPU cache with replacement, eviction, section invalidation, and deterministic close.
- A separate 32 MiB default LRU CPU cache for rebuild payloads.
- A terrain draw executor that refuses POSITION-only programs and submits complete quad meshes through the matching terrain pipeline.

The V1 serialized order is position, UV0, color, light (UV2), overlay (UV1), normal, one padding byte. Minecraft's normal element occupies three bytes; the final byte is explicit stride padding. This is an explicit backend mapping rather than an assumption.

## Resource lifecycle

1. CPU extraction builds immutable section payloads away from the GPU.
2. The render thread uploads payloads using GpuDevice.createBuffer with vertex usage.
3. PrismTerrainGpuCache owns the resulting GpuBuffer objects.
4. Rebuild/unload invalidation closes all GPU allocations for the affected section.
5. LRU eviction closes the oldest allocations before accounting them as released.
6. Renderer/device teardown must call cache.close() on the render thread.

Borrowed mesh/buffer handles must not outlive their cache entry. Callers must never close borrowed buffers.

## Drawing boundary

Terrain programs are compiled through MinecraftShaderBackend.compileTerrain(), which declares the V1 Minecraft vertex format and QUADS mode. PrismTerrainDrawExecutor accepts only a program compiled through this terrain path, validates all shader bindings before changing pass state, binds the uploaded vertex buffer, and submits the complete quad vertex range.

The executor is a cooperating-renderer API, not an automatic vanilla chunk renderer replacement. No terrain renderer bridge is registered by default. Vanilla chunk compilation, chunk rebuild hooks, culling, world-stage scheduling, camera-relative transforms, and renderer-specific adapters remain unfinished. Vanilla terrain remains responsible for the world.

## Limits

- Maximum 65,536 vertices per individual payload.
- Maximum 512 meshes and 1,048,576 vertices per published extraction frame.
- CPU cache: 32 MiB default.
- GPU cache: 128 MiB default.
- Section-local positions are constrained to [0, 16].
- GPU allocation, draw submission, and disposal are render-thread-only.


## Fabric frame lifecycle bridge

`PrismTerrainFrameCoordinator` connects the bounded frame queue to Fabric API
0.141.6's Minecraft 1.21.11 world-render lifecycle:

1. Registered extraction providers run at `WorldRenderEvents.END_EXTRACTION`,
   after vanilla render-state extraction and before drawing begins.
2. Providers submit immutable `PrismTerrainMesh` instances through
   `PrismTerrainFrameCoordinator.submit`. Submission is rejected outside the
   extraction callback window.
3. The coordinator publishes one immutable, deterministically ordered frame.
   A failed provider is disabled without preventing other providers from running.
4. Registered terrain renderers receive the published frame at
   `BEFORE_ENTITIES` or `BEFORE_TRANSLUCENT`, filtered to their requested stage.
   Renderer failures disable only the failing renderer.

The coordinator is a lifecycle and extension-point bridge for cooperating
terrain integrations. It does not obtain vanilla chunk meshes, alter vanilla
chunk rebuilds, or replace vanilla terrain rendering. Camera-relative transforms,
GPU cache upload policy, frustum/occlusion culling, and actual renderer adapters
remain separate work. Consumers must perform GPU operations on the render thread.
