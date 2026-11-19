# Prism Terrain Pipeline Foundation

## What this adds

Prism now has a bounded CPU-side terrain mesh extraction contract for cooperating renderer integrations:

- Section-local procedural quad meshes using the current Prism POSITION-only vertex contract.
- Immutable mesh snapshots with defensive copies.
- Solid/cutout (BEFORE_ENTITIES) and translucent (BEFORE_TRANSLUCENT) draw-stage classification.
- A synchronized extraction queue that transfers immutable data to the drawing phase.
- Deterministic section/material ordering.
- Per-mesh and per-frame limits to prevent unbounded memory growth.
- Duplicate section/material/stage submissions are rejected.

The intended lifecycle is:

1. A terrain integration starts a frame with PrismTerrainFrameQueue.beginFrame().
2. Its extraction workers build independent PrismTerrainMeshBuilder instances and submit completed immutable meshes.
3. The integration calls publish() once extraction is complete.
4. Its renderer bridge consumes the published frame at the appropriate drawing stage and uses the registered Prism material pipeline when the vertex contract matches.
5. The next frame begins with beginFrame(), releasing previous queued mesh references.

## Critical boundary

This is a terrain mesh extraction and handoff foundation, not a replacement for Minecraft's vanilla chunk mesher or GPU chunk storage. Fabric 1.21.11's public WorldRenderEvents provide extraction and drawing extension points, but the documented drawing events occur around vanilla terrain rather than replacing its chunk compilation and upload path. Prism does not cancel or replace vanilla chunk rendering here.

The current POSITION-only shader pipeline is sufficient for procedural geometry whose appearance is derived from position and shader uniforms. It is not yet a complete block vertex format: UVs, per-vertex colors, packed light, overlay, normals, and vanilla block render-layer semantics still need an explicit versioned vertex-layout design before Prism can claim full chunk-material parity.

No terrain renderer bridge is registered by default. Until one is implemented and tested, vanilla terrain remains responsible for the world.

## Limits

- Maximum 65,536 vertices per mesh.
- Maximum 512 meshes per frame.
- Maximum 1,048,576 vertices per frame.
- Meshes are quads with four XYZ vertices and section-local coordinates in [0, 16].
- Mesh snapshots contain CPU data only; GPU upload, frustum/occlusion culling, chunk rebuild invalidation, draw submission, and resource lifetime are future work.
