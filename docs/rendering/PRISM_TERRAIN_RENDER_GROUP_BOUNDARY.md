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
- Maximum sequential index count and the existing chunk-section uniform slices

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

This remains observational. Vanilla still owns every terrain draw, and the
fail-closed `PrismTerrainRenderPassState` gate remains mandatory.
