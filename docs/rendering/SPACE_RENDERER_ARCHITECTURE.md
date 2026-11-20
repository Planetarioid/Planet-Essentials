# Space Renderer architecture

Space Renderer is Planet Studios' independent terrain renderer for Space Client on Minecraft Java 1.21.11 and Fabric. It studies general high-performance rendering techniques, including section-oriented world representation, asynchronous mesh work, visibility traversal, shared GPU storage, and batched submissions. Its implementation is original and does not copy Sodium source code.

## Current foundations

- **Section identity and revisions:** dimension-aware section keys and monotonic build tokens reject stale worker results.
- **Mesh scheduling:** bounded in-flight priority queue for CPU-only immutable mesh construction. Completed results are polled by the client/render coordination layer.
- **Visibility graph:** section connectivity traversal crosses only mutually open faces. Unknown data or visit-limit truncation requests a safe frustum-only fallback.
- **GPU region allocation:** aligned best-fit allocation with range coalescing.
- **GPU region ownership:** transactional replacement reservations preserve old section data until a new upload is committed.

These are standalone foundations. They are not yet connected to Minecraft's complete chunk rebuild, visibility, or GPU upload lifecycle.

## Required architecture

1. World and section manager integrated with chunk load, unload, block changes, and dimension changes.
2. Immutable chunk snapshots and parallel mesh building with bounded work queues and cancellation.
3. Correct visibility graph generation from section occlusion data, plus frustum and conservative occlusion culling.
4. Shared GPU vertex/index storage, render-thread uploads, fences or safe retirement, and fragmentation management.
5. Opaque, cutout, and translucent render lists with material-aware batching.
6. Full terrain takeover with safe vanilla fallback and correct lighting, biome tint, fluids, animated textures, block entities, and resource packs.
7. Profiling of CPU mesh build, culling, uploads, draw submission, GPU time, and frame-time percentiles.

## Safety and correctness rules

- Worker threads only consume immutable snapshots and produce CPU mesh data.
- All GPU resource creation, mutation, upload, and disposal stays on the render thread.
- Every result must be checked against its section revision before upload.
- Unknown visibility or incomplete graph state must not hide terrain.
- Replaced GPU ranges remain allocated until replacement uploads have succeeded.
- Translucent sorting and render-layer semantics must match Minecraft's expectations.
- Vanilla remains the fallback until visual parity and compatibility tests pass.

## Implementation stages

| Stage | Deliverable | Status |
|---|---|---|
| 1 | Section identity and revision manager | Implemented, CI pending |
| 2 | Bounded asynchronous mesh-build scheduler | Implemented, CI pending |
| 3 | Conservative visibility graph | Implemented, CI pending |
| 4 | Aligned GPU-region allocator and ownership manager | Implemented, CI pending |
| 5 | Minecraft chunk lifecycle and mesh-builder integration | Not started |
| 6 | Shared GPU buffers and render-thread upload pipeline | Not started |
| 7 | Layered render lists and batched draw submission | Partial foundations |
| 8 | Complete terrain takeover, fallback, and visual parity | Not started |
| 9 | Compatibility and repeatable performance benchmarks | Not started |

## License boundary

Sodium's repository is public, but its current source is distributed under PolyForm Shield 1.0.0, not an unrestricted permissive license. Space Renderer therefore implements general rendering concepts independently rather than copying or repackaging Sodium code. Review the official [Sodium repository](https://github.com/CaffeineMC/sodium) and [license](https://github.com/CaffeineMC/sodium/blob/dev/LICENSE.md) before any source reuse.
