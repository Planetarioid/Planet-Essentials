# Space Renderer architecture

Space Renderer is Planet Studios' independent terrain renderer for Planet Essentials on Minecraft Java 1.21.11 and Fabric. It is an original implementation of a section-based terrain pipeline, not a Sodium fork and not yet a complete replacement for Minecraft's renderer.

## Implemented foundations

- Revision-safe, dimension-aware section identity and lifecycle management.
- Immutable block-state and model snapshots with bounded memory accounting.
- Bounded asynchronous CPU meshing, including baked block models, biome tint, packed light, ambient occlusion, and fluids.
- Conservative visibility traversal with safe frustum fallback.
- GPU mesh ownership/cache, bounded upload handoff, render-thread uploads, and fenced retirement.
- Immutable visible-frame capture, native render-group draw plans, material-aware batching, and camera-sorted translucent index buffers.
- Native terrain shader pipelines, section-uniform uploads, and a draw executor.
- Native 1.21.11 fog uniform integration and spherical/cylindrical fog evaluation in the Space Renderer terrain shaders; runtime visual parity still requires in-game verification.
- Vanilla render-group snapshots for attachments, atlas/lightmap, default uniforms, per-layer pipelines, explicit color/depth clear options, and viewport/scissor state revalidation at submission.
- Per-layer fixed-function pipeline parity diagnostics comparing depth, blend, culling, write masks, polygon mode, logic ops, and polygon offset; mismatches are reported by native layer and must retain vanilla fallback.
- Captured-state group submission API and a live, fail-closed render-group preflight hook that now captures the active vanilla pass, validates target identity plus unchanged viewport/scissor state, checks exact vanilla draw identity coverage within every group layer (not just matching counts), revalidates atlas/lightmap texture and sampler identity against live vanilla inputs, and publishes missing-resource and fixed-function mismatch diagnostics. CI validates compilation and tests, not actual GPU rendering.

## Remaining work before replacement

1. Complete backend-state parity, including viewport/scissor lifecycle and any state not represented by the public RenderPass API.
2. Upgrade the live preflight hook into a submission hook only after every vanilla state facet is implemented and runtime-verified. It now captures and assesses the current group, but deliberately never cancels vanilla.
3. Connect section snapshot creation to every supported chunk rebuild/change path and verify stale-result invalidation under world transitions.
4. Preserve all vanilla terrain semantics: fog, overlays, lightmap behavior, animated textures, model layers, fluids, resource packs, and special blocks.
5. Integrate the independent draw path at the synchronous native render-group boundary behind a fail-closed readiness gate.
6. Validate visual parity on supported GPU drivers and with ordinary Fabric mods before enabling replacement by default.
7. Benchmark frame-time distributions, CPU meshing, upload pressure, GPU memory, and translucent sorting against vanilla.

## Correctness rules

- Worker threads consume immutable snapshots and produce CPU-only mesh data.
- GPU creation, mutation, upload, and disposal remain on the render thread.
- Every asynchronous result is checked against its current section revision before upload.
- Unknown or incomplete visibility data must not hide terrain.
- Replaced GPU allocations remain alive until prior commands have passed a GPU fence.
- Translucent sorting and native layer ordering must match Minecraft's behavior.
- Vanilla remains authoritative until the full render group can be replaced without missing draws or state.

## Current stage

| Component | Status |
|---|---|
| Section manager and bounded mesh scheduler | Implemented, CI passed |
| Block-state snapshots and baked-model mesher | Implemented, CI passed |
| Lighting, AO, biome tint and fluid geometry | Implemented, CI passed |
| Visibility graph and conservative fallback | Implemented, CI passed |
| GPU cache, upload queue and fenced retirement | Implemented, CI passed |
| Native terrain pipelines and section uniforms | Implemented, CI passed |
| Shader-side vanilla fog integration | Implemented, CI passed; runtime visual parity pending |
| Frame snapshots and native group draw plans | Implemented, CI passed |
| Draw executor and captured-state group submission | Implemented, CI passed |
| Vanilla target, atlas/lightmap texture and sampler identity validation, uniforms, layer pipeline, color/depth clear options wired into pass creation and unsupported stencil rejection and scissor capture | Implemented, CI pending; runtime parity pending |
| Exact viewport capture/replay | Implemented, CI passed |
| Fixed-function pipeline parity diagnostics | Implemented, CI passed; runtime backend parity pending |
| Shared render-pass owner for captured groups | Implemented, CI passed |
| Live render-group preflight with exact draw-identity and live texture-binding validation, captured-state diagnostics and fail-closed fallback | Implemented, CI passed; runtime parity pending |
| Live Space Renderer submission and whole-group vanilla takeover | Not started |
| Animated fluid textures and full overlay/fog parity | Not started |
| Full visual parity, compatibility and performance validation | Not started |

## License boundary

Sodium's public repository currently uses PolyForm Shield 1.0.0 rather than an unrestricted permissive license. Space Renderer therefore implements general rendering concepts independently instead of copying or repackaging Sodium source code. Review the official [Sodium repository](https://github.com/CaffeineMC/sodium) and [license](https://github.com/CaffeineMC/sodium/blob/dev/LICENSE.md) before any source reuse.
