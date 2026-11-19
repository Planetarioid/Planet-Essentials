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

The executor is a cooperating-renderer API, not an automatic vanilla chunk renderer replacement. `PrismTerrainRendererAdapter.register(programId, stage, provider)` now provides an opt-in end-to-end path for providers that can supply immutable V1 section payloads. It extracts payloads at END_EXTRACTION, uploads and caches them per ClientLevel, binds the section-origin and Minecraft default uniforms, and draws indexed QUADS into the main color/depth target at BEFORE_ENTITIES or BEFORE_TRANSLUCENT. It also registers chunk-unload GPU invalidation. No provider is registered by default, and vanilla terrain remains responsible for ordinary world geometry.

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


## Camera-relative section transforms

`PrismTerrainCameraTransform` converts integer section indices to a
camera-relative section origin. It multiplies section indices by 16 and
subtracts the camera's double-precision world position before converting the
result to floats. This avoids first converting large absolute world
coordinates to floats.

The transform is a lightweight per-draw value. Terrain vertex payloads and
cached GPU buffers remain section-local and reusable as the camera moves. A
cooperating renderer should provide the transform as its section-origin
uniform (or equivalent model transform) and keep the existing local vertex
coordinates unchanged.

This utility is unit-tested for negative sections, fractional camera positions,
large world coordinates, local-coordinate bounds, and payload immutability.
It does not yet wire a concrete section-origin uniform into a production
renderer or replace vanilla chunk rendering.


## Section-origin uniform contract

Terrain V1 now has a canonical per-section origin uniform payload:
`PrismTerrainSectionUniform` encodes the camera-relative XYZ origin as a
16-byte std140-compatible vec4 named `PrismSection`. The fourth component is
reserved and set to 1.0. The helper returns a fresh direct, native-order
ByteBuffer suitable for upload.

A renderer adapter must:
1. Compute `PrismTerrainCameraTransform` from the section coordinates and
   current camera world position.
2. Encode it with `PrismTerrainSectionUniform.encode`.
3. Upload all 16 bytes to a uniform buffer using the active Minecraft GPU
   device/command encoder.
4. Bind the resulting buffer slice as `PrismSection` in
   `MinecraftShaderBindings`.
5. Declare `PrismSection` in the terrain program's uniform-buffer bindings.

The helper validates the declaration contract. It does not allocate or own GPU
buffers; adapters retain ownership and must update/recycle the per-draw uniform
allocation according to their GPU synchronization strategy. No built-in
renderer adapter is registered yet.


## Renderer-owned section uniform upload and draw binding

`PrismTerrainSectionUniformAllocation` now provides the concrete
Minecraft 1.21.11 GPU path for the section-origin payload. It creates a
16-byte `GpuBuffer.USAGE_UNIFORM` allocation initialized directly from
`PrismTerrainSectionUniform.encode`. The allocation exposes a bounded
`GpuBufferSlice` and owns the underlying GPU buffer.

Use the section-aware `PrismTerrainDrawExecutor.draw(..., sectionUniform)`
overload for programs that declare `PrismSection`. It verifies the
program declaration, adds the slice to a fresh immutable
`MinecraftShaderBindings` set, validates the full binding set, then
submits the mesh. The original binding set is not mutated.

The allocation is immutable after creation so a renderer cannot accidentally
overwrite a uniform still referenced by queued GPU work. The renderer must
retain each allocation until its frame submissions no longer reference it,
then close it on the render thread. This first implementation favors explicit,
safe ownership over a premature shared ring-buffer strategy; a fenced or
frame-retired uniform arena can replace per-section allocations after its
synchronization behavior is validated on supported backends.

This completes the reusable upload/bind/draw API contract for cooperating
terrain renderers. It still does not register a production renderer, provide
vanilla chunk meshes, or prove visual correctness in-game.


## CPU section frustum culling

PrismTerrainFrustum stores six immutable world-space planes using an inside-positive half-space convention. PrismTerrainCulling.visibleMeshes tests section-sized 16×16×16 AABBs with the positive-vertex method and preserves deterministic frame order. The math is unit-tested for intersection, rejection, ordering, and invalid inputs.

This is a reusable culling primitive, not automatic camera integration. A renderer adapter must supply the current camera frustum planes and invoke the filter before GPU work. Occlusion culling and direct integration with Minecraft's active frustum are still outstanding.


## Section rebuild lifecycle

`PrismTerrainSectionLifecycle` issues revision tokens for section rebuilds, invalidates cached CPU payloads at rebuild start, rejects stale asynchronous results, and invalidates a chunk's section range on unload. The API is synchronized and unit-tested. Callers must provide the actual dimension section range and keep lifecycle state scoped to the active world. Vanilla chunk-builder hooks and render-thread GPU-cache invalidation are not yet connected.


## Opt-in GPU terrain renderer adapter

`PrismTerrainRendererAdapter` joins the V1 payload, GPU cache, section
uniform, terrain shader compiler, and Fabric draw stages. Register it with a
selected-pack program ID, a stage, and a provider callback. Providers emit
immutable `PrismTerrainVertexData` payloads during END_EXTRACTION only.

The adapter publishes a bounded immutable payload list (512 payloads and
1,048,576 vertices maximum), uploads changed payloads through a per-`ClientLevel`
128 MiB LRU cache, and submits indexed QUADS into the main color/depth target.
GPU cache entries compare SHA-256 payload identities so unchanged sections
are not re-uploaded every frame. The draw executor uses Minecraft's
sequential quad index buffer rather than issuing a non-indexed draw for
QUADS.

The selected shader program is recompiled through the terrain V1 vertex format
on demand and cached by shader generation plus source hash. Terrain pipeline
identifiers are distinct from general POSITION-only identifiers, preventing
the two vertex formats from aliasing the same GPU pipeline identity.

A compatible terrain shader declares `PrismSection` and
`DynamicTransforms` as explicit UBOs. It may also declare Minecraft's
`Projection`, `Globals`, and `Fog` defaults, which the adapter binds through
`RenderSystem.bindDefaultUniforms`. Other custom UBOs, texel buffers, and
samplers are rejected by this first adapter rather than silently left unbound.

Per-section uniform payloads are written into a `MappableRingBuffer` with
GPU-fenced rotation, avoiding one new uniform allocation per section per
frame. The adapter is opt-in and consumes provider-supplied terrain. It does
not intercept vanilla section meshing, extract Minecraft's existing chunk
buffers, replace vanilla terrain, or yet construct a frustum from the active
camera.

## Vanilla section rebuild observation

Prism now observes Minecraft 1.21.11's actual `SectionCompiler.compile` entry point. The mixin supplies the active `SectionPos` and callback-scoped `RenderSectionRegion` to `PrismVanillaSectionRebuildEvents` before vanilla meshing begins. The region is the real snapshot used by the vanilla compiler, so CPU-side integrations can inspect block states, fluids, block entities, tint, and lighting inputs without rebuilding a separate region.

Each observed compile begins a new `PrismTerrainSectionLifecycle.BuildToken` and invalidates the prior cached payload for that section. A listener can publish a `PrismTerrainVertexData` result through `RebuildContext.publish`; the lifecycle rejects results whose token has been superseded by a newer rebuild or chunk unload. Listeners execute synchronously on Minecraft's chunk-build worker. They must keep work CPU-only, must not retain the region after the callback returns, and must not call GPU APIs.

This is the first real vanilla rebuild integration point, not yet a vanilla mesh replacement. Prism does not currently copy Minecraft's compiled vertex buffers or change which geometry vanilla draws. No listener is registered by default, and the existing opt-in GPU adapter still requires a provider to submit payloads at extraction time. A future adapter can bridge revision-validated rebuild payloads into the frame queue without blocking Minecraft's compiler.



## Automatic vanilla mesh capture (1.21.11)

Prism now captures the actual output of Minecraft's `SectionCompiler.compile` method, not just the region passed into the compiler. A HEAD hook begins a revision token and preserves a callback-scoped context on the compile worker. A RETURN hook copies each completed `MeshData` layer before Minecraft's upload path owns and closes it.

The capture supports the vanilla `DefaultVertexFormat.BLOCK` layout (32-byte stride): position, RGBA8 color, UV0, packed UV2 light, three-byte normal plus padding. It converts those attributes into Prism V1's 36-byte layout, inserting a neutral zero overlay because vanilla's BLOCK terrain format does not contain UV1. Quad order is preserved. SOLID, CUTOUT, TRANSLUCENT, and TRIPWIRE are assigned stable `planet_essentials:terrain/vanilla/<layer>` material IDs; translucent and tripwire payloads are routed to the translucent stage.

Each layer is copied into an immutable `PrismTerrainVertexData` and published to the existing per-world CPU section cache only if its build token is still current. Unsupported vertex formats, malformed counts, and invalid section-local coordinates are rejected per layer and logged without interrupting vanilla compilation. The source `MeshData` remains owned by Minecraft and is never retained or closed by Prism.

This completes automatic vanilla mesh *capture* and cache population. It does not yet suppress vanilla's own upload/draw, bind these cached vanilla payloads to selected shader programs automatically, or replace vanilla terrain on screen. The next stage is a renderer-owned draw path with a safe vanilla fallback, then active frustum extraction and visibility integration.

## Active camera frustum integration

`LevelRenderer.prepareCullFrustum` is now observed at RETURN. Prism stores a defensive copy of the exact frustum Minecraft prepared for the current frame, rather than relying on the debug-only captured-frustum API. The snapshot is cleared on disconnect. The opt-in terrain adapter tests each section's world-space 16×16×16 AABB against this active frustum before uploading a mesh to the GPU or issuing its draw. If no frame frustum has been published yet, the test fails open so terrain is not accidentally dropped during initialization.

This removes off-screen sections from Prism's upload/draw work. It does not implement occlusion culling or use Minecraft's section visibility graph, so sections inside the camera frustum but hidden behind opaque geometry can still be submitted. Vanilla remains responsible for its own visibility and draw path.
