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
- Exact per-layer vanilla `RenderPipeline` object, including the debug
  wireframe override when active
- Effective draw ordering: only TRANSLUCENT is reversed by vanilla
- Projection, fog, globals, and lighting resources conditionally supplied by
  `RenderSystem.bindDefaultUniforms`
- A defensive copy of the current model-view matrix
- Maximum sequential index count and the existing chunk-section uniform slices

The draw-order rule is isolated in a pure helper and covered by unit tests.
Captured pipeline objects retain the exact vanilla blend, depth, cull, and
color-write configuration by identity; this is not yet a serialized/rebuilt
pipeline description.

Known remaining state boundary:
- `renderGroup` does not set a custom viewport or scissor. The active pass
  starts with the render target's default full-attachment viewport and no
  explicit scissor command; this still needs confirmation on the runtime GPU
  backend before replacement relies on it.
- Stencil/scissor details and driver/backend behavior are not independently
  introspected.
- Capturing resource references does not prove that Prism can reproduce the
  pass or its timing safely.

This remains observational. Vanilla still owns every terrain draw, and the
fail-closed `PrismTerrainRenderPassState` gate remains mandatory.
