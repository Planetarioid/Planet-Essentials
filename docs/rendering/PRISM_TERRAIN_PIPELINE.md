# Prism Terrain Pipeline Foundation

## What this adds

Prism terrain currently has:

- A bounded CPU quad mesh builder and immutable extraction-to-draw frame handoff.
- A versioned 36-byte full-attribute V1 payload: XYZ position, UV0, RGBA8 color, packed light, packed overlay, packed normal, and one byte of vertex padding.
- A Minecraft 1.21.11 VertexFormat whose attribute offsets and stride are checked against the serializer.
- Render-thread GPU upload into owned vertex buffers.
- A render-thread-confined, 128 MiB default LRU GPU cache with replacement, eviction, section invalidation, and deterministic close.
- A separate 32 MiB default LRU CPU cache for rebuild payloads.

The V1 serialized order is position, UV0, color, light (UV2), overlay (UV1), normal, one padding byte. Minecraft's normal element occupies three bytes; the final byte is explicit stride padding. This is now an explicit backend mapping rather than an assumption.

## Resource lifecycle

1. CPU extraction builds immutable section payloads away from the GPU.
2. The render thread uploads payloads using GpuDevice.createBuffer with vertex usage.
3. PrismTerrainGpuCache owns the resulting GpuBuffer objects.
4. Rebuild/unload invalidation closes all GPU allocations for the affected section.
5. LRU eviction closes the oldest allocations before accounting them as released.
6. Renderer/device teardown must call cache.close() on the render thread.

Borrowed mesh/buffer handles must not outlive their cache entry. Callers must never close borrowed buffers.

## Drawing boundary

The uploaded buffer uses the full V1 Minecraft vertex format. It must only be bound to a RenderPipeline created with PrismTerrainVertexLayout.minecraftVertexFormat() and a compatible shader interface. Prism's existing general shader backend still compiles POSITION-only pipelines by default. This change deliberately does not bind the full terrain buffer to those pipelines.

No terrain renderer bridge is registered by default. Vanilla chunk compilation, chunk rebuild hooks, culling, actual terrain draw submission, and renderer-specific adapters remain unfinished. Vanilla terrain remains responsible for the world.

## Limits

- Maximum 65,536 vertices per individual payload.
- Maximum 512 meshes and 1,048,576 vertices per published extraction frame.
- CPU cache: 32 MiB default.
- GPU cache: 128 MiB default.
- Section-local positions are constrained to [0, 16].
- GPU allocation and disposal are render-thread-only.
