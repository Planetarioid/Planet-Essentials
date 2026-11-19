# Prism Terrain Pipeline Foundation

## What this adds

Prism has two bounded CPU-side terrain geometry contracts for cooperating renderer integrations:

- A compact POSITION-only procedural quad mesh for shader-generated geometry.
- A versioned full-attribute V1 vertex payload: XYZ position, UV, RGBA8 color, packed light, packed overlay, and packed normal.
- A fixed 36-byte vertex stride with explicit offsets and a stable layout fingerprint.
- Immutable payloads with defensive copies and a read-only little-endian byte view.
- Section-local coordinates, finite UV validation, namespaced material IDs, and solid/cutout or translucent draw-stage classification.
- A synchronized extraction queue with deterministic ordering, duplicate rejection, and per-frame mesh/vertex limits.

The full-attribute V1 layout is a CPU serialization contract. It does not yet claim byte-for-byte compatibility with Minecraft's internal VertexFormat, nor does it imply that the current position-only RenderPipeline can consume this payload. A future renderer bridge must explicitly map the layout to the active backend's vertex format and validate packed light, overlay, color, and normal semantics.

## Intended lifecycle

1. A terrain integration starts a frame with PrismTerrainFrameQueue.beginFrame().
2. Extraction workers build independent immutable mesh or vertex-data payloads.
3. The integration publishes its extraction frame after workers finish.
4. A renderer bridge consumes the immutable data at the matching draw stage and resolves the Prism material pipeline.
5. Section rebuilds or unloads invalidate the corresponding cached GPU allocation in the eventual renderer adapter.

## Critical boundary

This is a terrain mesh extraction and handoff foundation, not a replacement for Minecraft's vanilla chunk mesher or GPU chunk storage. Fabric 1.21.11's public WorldRenderEvents provide extraction and drawing extension points, but the documented drawing events occur around vanilla terrain rather than replacing its chunk compilation and upload path. Prism does not cancel or replace vanilla chunk rendering here.

No terrain renderer bridge is registered by default. Until one is implemented and tested, vanilla terrain remains responsible for the world.

## Limits

- Maximum 65,536 vertices per individual mesh or full-attribute payload.
- Maximum 512 meshes and 1,048,576 vertices per published frame queue.
- Position coordinates are section-local and constrained to [0, 16].
- Full-attribute V1 stride is 36 bytes, encoded little-endian.
- CPU payloads are immutable. GPU upload/cache, frustum/occlusion culling, chunk rebuild invalidation, draw submission, and GPU resource lifetime remain future work.
