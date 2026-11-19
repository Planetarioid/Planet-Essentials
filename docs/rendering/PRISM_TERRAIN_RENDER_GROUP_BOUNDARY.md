# Terrain render-group input and vanilla pass capture

Minecraft 1.21.11's `ChunkSectionsToRender.renderGroup` creates its own
`RenderPass` from `ChunkSectionLayerGroup.outputTarget()`. It then binds
Minecraft's default uniforms, binds the lightmap as `Sampler2`, and for each
group layer selects either the debug wireframe pipeline or that layer's vanilla
pipeline, binds the block atlas as `Sampler0`, and submits the layer's draw list.

Planet's HEAD observer now snapshots those inputs without opening another pass
or mutating vanilla state:

- Exact output `RenderTarget`, color attachment, optional depth attachment,
  and target dimensions
- Block atlas view and the exact `Sampler0` sampler
- Lightmap view and the clamp-to-edge linear `Sampler2` sampler
- Exact per-layer vanilla `RenderPipeline` object and copied state: depth test,
  polygon mode, culling, blend function, color/alpha/depth writes, logic op,
  depth bias, vertex format/mode, and shader identifiers, including the debug
  wireframe override when active
- Effective draw ordering: only TRANSLUCENT is reversed by vanilla
- Projection, fog, globals, and lighting resources conditionally supplied by
  `RenderSystem.bindDefaultUniforms`
- A defensive copy of the current model-view matrix
- Maximum sequential index count and QUADS index type, plus the dynamic `ChunkSection` uniform slot. The actual sequential GPU index buffer is resolved during replacement preflight, not during observation, because `getBuffer()` may allocate or grow GPU storage
- Exact render-pass creation options from the pinned 1.21.11 implementation: no color clear, no depth clear, and no stencil attachment
- OpenGL-only viewport and scissor defaults: full attachment viewport at (0,0), scissor disabled. These are guarded against the active `GlDevice` and derived from the 1.21.11 backend implementation, not GPU-driver readback. Unknown backends leave both facets missing
- Whether vanilla actually applies a layer pipeline (empty layer draw lists skip pipeline and atlas binding)

The sequential QUADS buffer is now resolvable against the maximum index
requirement of a Prism replacement draw set, not only vanilla's captured
maximum. `IndexBufferInput.resolveBufferForIndexCount` grows the sequential
buffer during preflight, then returns the actual post-growth index type together
with the buffer and validated byte capacity. The caller must compute the maximum
index count across every planned section before opening a pass and pass that
single requirement to the resolver. Six indices are required per complete
four-vertex quad; the pure conversion helper rejects malformed vertex counts
and integer overflow. This closes the specific gap where Prism's captured
section meshes could require more indices than vanilla's draw group.
`PrismTerrainReplacementCoordinator.prepareSequentialIndexBuffer` now
connects that resolver to the actual all-or-nothing replacement plan. It walks
every planned section, revalidates its CPU/GPU payload identity, computes the
largest quad index requirement from the uploaded mesh vertex counts, and
resolves one shared sequential index buffer sized for the entire group. The
caller must treat an empty result as a preflight failure and leave vanilla in
control. The returned buffer and post-growth index type are shared by all
section draws in that group; no per-draw index-buffer growth is needed after
the replacement pass opens.


The draw-order rule is isolated in a pure helper and covered by unit tests.
Captured pipeline objects retain the exact vanilla blend, depth, cull, and
color-write configuration by identity; this is not yet a serialized/rebuilt
pipeline description.

Known remaining state boundary:
- The OpenGL backend's viewport and scissor defaults are derived from the
  pinned 1.21.11 `GlCommandEncoder` and `GlRenderPass` implementations:
  full attachment viewport, scissor disabled. These are source-backed defaults,
  not GPU-driver readback; runtime confirmation is still required.
- Unknown GPU backends intentionally leave viewport/scissor facets absent.
- Stencil is not attached by this terrain pass. The snapshot records this as
  a known absence rather than inventing a stencil configuration.
- The captured `DefaultVertexFormat.BLOCK` pipeline has no overlay attribute.
  Prism V1 retains its own overlay attribute in its vertex format, initialized
  with the neutral value for vanilla-captured payloads.
- CI and source inspection do not prove visual parity, resource timing, or
  driver behavior.

The snapshot can now be converted into a `PrismTerrainRenderPassState`
evidence record. The replacement coordinator also has a snapshot-native
preparation overload that consumes the exact captured ordered draw list, the
identity draw index, and the evidence record together. Translucent order is not
reversed twice. The snapshot can open a Prism-owned pass against the captured
attachments and replay the four optional default uniforms by their exact
Minecraft names, and build only the declared Sampler0/Sampler2 bindings from
the captured block atlas and lightmap resources. Unknown sampler names fail
closed. The draw executor has a matching overload for section draws
when those defaults were already installed on the pass. The adapter marks only facts represented by the snapshot and
reports missing facets explicitly. It recognizes the captured atlas/lightmap
samplers as `Sampler0` and `Sampler2`, accounts for absent optional default
uniforms, and marks overlay as not used when the captured pipeline uses the
vanilla BLOCK vertex format. Viewport/scissor defaults are marked only for the
known OpenGL backend. Runtime parity and actual Prism pass reproduction are
still unverified, so vanilla rendering remains untouched.

This remains observational. Vanilla still owns every terrain draw, and the
fail-closed `PrismTerrainRenderPassState` gate remains mandatory.

The Prism pass-replay path now has reusable pieces:
- `openCapturedTargetPass` opens a Prism-owned pass using the captured color
  and depth attachments and the same no-clear options
- `bindCapturedDefaultUniforms` replays the exact optional Projection, Fog,
  Globals, and Lighting resources and returns the names actually installed
- `samplerBindingsFor` builds only the declared vanilla Sampler0/Sampler2
  bindings from the captured atlas and lightmap; unknown names fail closed
- `PrismTerrainVanillaPipelineCache` clones a Prism shader pipeline with the
  captured vanilla depth, blend, cull, color-write, polygon, logic-op, and
  depth-bias state while retaining Prism's shader source, definitions, bindings,
  and V1 vertex format. Variants are precompiled with the original shader source
  and cached by pipeline identity
- `PrismTerrainDrawExecutor.drawWithPipeline` can submit a section mesh using
  one of those state-matched pipeline variants

These are tested preparation/replay primitives. They are not yet called from
the vanilla mixin, and no vanilla draw is cancelled or replaced.

## Prepared draw-resource bundle

`PrismTerrainReplacementCoordinator.prepareDrawResources` now creates an
owned `PrismTerrainPreparedRenderGroup` before a Prism pass is opened. For
the complete replacement plan it:

- Resolves and precompiles the pipeline variant matching the captured vanilla
  layer's fixed-function state
- Verifies every uniform buffer required by the program is either the canonical
  `PrismSection` buffer or one of the optional default uniforms actually
  captured from vanilla
- Resolves only declared `Sampler0` and `Sampler2` resources from the
  captured block atlas and lightmap
- Revalidates each CPU payload against its live GPU mesh and quad layout
- Allocates one immutable camera-relative section-origin uniform per draw
- Computes the maximum index requirement across the complete group and resolves
  one shared sequential index buffer before pass creation
- Rechecks world identity, shader generation, and frame state before publishing

The result owns its section uniform allocations and closes them on the render
thread. It borrows GPU meshes from the world's LRU cache, the compiled program,
and the cached pipeline variant. `isValidFor` checks the world, captured
snapshot sequence, and shader generation immediately before use. The result
also carries the exact index buffer/type pair, sampler bindings, and external
default-uniform names needed by the draw executor.

Any preflight failure closes all section uniforms allocated during that attempt
and returns empty. This is still a preparation API only: no pass is opened,
no draw is submitted, and no vanilla draw is cancelled. The caller must keep
the prepared bundle alive through all group submissions and close it afterward.
GPU-driver failure during actual submission remains non-transactional and is
not solved by this preparation work.
