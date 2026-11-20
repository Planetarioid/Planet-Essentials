# Space Renderer

Space Renderer is Planet Studios' independent terrain and graphics-rendering engine for Space Client on Minecraft Java 1.21.11 and Fabric.

It takes inspiration from publicly documented renderer engineering patterns used by high-performance Minecraft renderers, including section-oriented world representation, asynchronous work, visibility graphs, shared GPU storage, and batched draw submission. Its implementation is original. It does not copy Sodium source code or depend on Sodium.

## Why this is a renderer rewrite

The existing terrain work is a foundation, not a finished replacement. It can capture vanilla section meshes, maintain revision-aware CPU caches, upload per-section GPU meshes, prepare render-group plans, and submit shader-backed draws. Its current replacement path is opt-in and tied to the shader runtime. It does not yet own the complete world terrain lifecycle or replace vanilla chunk meshing by default.

A full renderer needs to own the whole path from world changes to GPU submission. Optimizing only the final draw call cannot close a large CPU-side gap.

## Target architecture

1. **World and section manager**
   - Track loaded render sections by dimension and section coordinates.
   - Handle load, unload, block changes, rebuild invalidation, and world transitions.
   - Keep stable section identities and revision numbers so stale work cannot publish.

2. **Asynchronous mesh builder**
   - Build immutable section snapshots away from the render thread.
   - Use bounded worker queues, cancellation, priority, and back-pressure.
   - Separate opaque, cutout, and translucent geometry.
   - Preserve lighting, tint, biome color, UVs, overlay, normals, and material data.

3. **Visibility engine**
   - Maintain section adjacency and connectivity.
   - Perform frustum tests and conservative occlusion traversal.
   - Reuse valid visibility results when the camera is stable.
   - Fail open when visibility information is stale or incomplete.

4. **GPU region manager**
   - Store many section meshes in shared GPU allocations instead of one buffer per section.
   - Allocate aligned ranges, free and coalesce ranges, and track fragmentation.
   - Upload only changed ranges and retire old allocations safely.
   - Keep all GPU creation, mutation, and disposal on the render thread.

5. **Render-list and batching system**
   - Build per-layer visible lists.
   - Group compatible draws by material, layer, and pipeline.
   - Reduce pipeline changes and draw submission overhead.
   - Sort translucent geometry correctly and independently of opaque geometry.

6. **Pipeline and compatibility layer**
   - Use Minecraft's supported Blaze3D device and render-pass abstractions.
   - Detect required GPU capabilities instead of assuming them.
   - Keep a reliable vanilla fallback until the full replacement passes correctness checks.
   - Treat shader packs and terrain rendering as separate concerns.

7. **Profiling and diagnostics**
   - Record CPU time for section extraction, mesh building, culling, uploads, and submission.
   - Track visible sections, rebuilt sections, queue depth, upload bytes, GPU-region occupancy, and draw counts.
   - Compare frame-time distributions in repeatable scenes, not just peak FPS.

## Implementation sequence

| Stage | Deliverable | Status |
|---|---|---|
| 0 | Existing mesh capture, revision-safe cache, draw planning, and GPU mesh foundation | Partial implementation |
| 1 | Shared GPU region allocator and allocator tests | Initial implementation |
| 2 | Section manager and bounded asynchronous mesh-build scheduler | Not started |
| 3 | Visibility graph and reusable culling results | Partial foundations |
| 4 | Shared GPU region buffers and upload lifecycle | Not started |
| 5 | Layered render lists and batched terrain submission | Not started |
| 6 | Complete vanilla terrain takeover with safe fallback | Not started |
| 7 | Visual parity, compatibility, and controlled benchmarks | Not started |

## Correctness requirements

- A stale build must never overwrite a newer section revision.
- Chunk unload and dimension changes must release CPU and GPU state.
- GPU resources must only be mutated or destroyed on the render thread.
- Missing chunks, unknown visibility, unsupported pipelines, or incomplete draw coverage must not make terrain disappear.
- Translucent sorting must preserve vanilla ordering expectations.
- Resource packs, block entities, animated textures, foliage, fluids, weather, particles, and shader packs must be tested before declaring parity.
- Performance claims require reproducible before-and-after frame-time measurements.

## License boundary

Sodium's repository is publicly readable, but its current source is distributed under PolyForm Shield 1.0.0, not an unrestricted permissive license. Space Renderer therefore uses independent implementations of general rendering concepts rather than copying or repackaging Sodium code. See the [official Sodium repository](https://github.com/CaffeineMC/sodium) and its [license](https://github.com/CaffeineMC/sodium/blob/dev/LICENSE.md).

## Current verification

Automated compilation and unit tests are necessary but not sufficient. No stage is considered runtime-verified until tested in a launched Minecraft client with visual correctness checks and measured frame times.
