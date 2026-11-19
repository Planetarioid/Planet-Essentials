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
- Maximum sequential index count, the exact QUADS sequential index-buffer object and index type, and the dynamic `ChunkSection` uniform slot
- Exact render-pass creation options from the pinned 1.21.11 implementation: no color clear, no depth clear, and no stencil attachment
- OpenGL-only viewport and scissor defaults: full attachment viewport at (0,0), scissor disabled. These are guarded against the active `GlDevice` and derived from the 1.21.11 backend implementation, not GPU-driver readback. Unknown backends leave both facets missing
- Whether vanilla actually applies a layer pipeline (empty layer draw lists skip pipeline and atlas binding)

The draw-order rule is isolated in a pure helper and covered by unit tests.
Captured pipeline objects retain the exact vanilla blend, depth, cull, and
color-write configuration by identity; this is not yet a serialized/rebuilt
pipeline description.

Known remaining state boundary:
- `renderGroup` does not set a custom viewport or issue a scissor command.
  The RenderPass API exposes explicit scissor enable/disable methods, neither
  of which vanilla calls here. Backend default viewport and disabled-scissor
  behavior still need runtime confirmation before replacement relies on them.
- Stencil state is not exposed by this vanilla terrain pipeline contract;
  scissor and viewport defaults are inferred from the RenderPass call sequence,
  not read back from the GPU driver.
- Capturing resource references does not prove that Prism can reproduce the
  pass or its timing safely.

The snapshot can now be converted into a `PrismTerrainRenderPassState`
evidence record. The replacement coordinator also has a snapshot-native
preparation overload that consumes the exact captured ordered draw list, the
identity draw index, and the evidence record together. Translucent order is not
reversed twice. The adapter marks only facts represented by the snapshot and
reports missing facets explicitly. It recognizes the captured atlas/lightmap
samplers as `Sampler0` and `Sampler2`, accounts for absent optional default
uniforms, and marks overlay as not used when the captured pipeline uses the
vanilla BLOCK vertex format. Viewport/scissor defaults are marked only for the
known OpenGL backend. Runtime parity and actual Prism pass reproduction are
still unverified, so vanilla rendering remains untouched.

This remains observational. Vanilla still owns every terrain draw, and the
fail-closed `PrismTerrainRenderPassState` gate remains mandatory.
