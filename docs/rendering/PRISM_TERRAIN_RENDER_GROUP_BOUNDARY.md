# Terrain render-group input capture

Minecraft 1.21.11 routes terrain batches through
`ChunkSectionsToRender.renderGroup(ChunkSectionLayerGroup, GpuSampler)`.
Planet observes that exact method at HEAD and RETURN.

At HEAD the thread-local snapshot now retains:
- The exact opaque/translucent/tripwire group enum
- The exact sampler object and block-atlas texture-view object by identity
- An immutable copy of the draw lists for every layer belonging to that group
- Per-layer and total draw counts
- The maximum index count requested by vanilla
- A defensive copy of the chunk-section uniform-buffer slices
- A monotonically increasing invocation sequence

The snapshot is available only during the synchronous vanilla method call.
Draw objects and GPU resources are referenced, not recreated or modified.
The copied collection structure prevents later list mutation from changing the
captured ordering, while retaining the exact draw object identities.

This remains observational. It does not open/close a RenderPass, change GPU
state, replace a pipeline, or cancel vanilla rendering. It does not yet capture
the active render target, viewport/scissor, depth/stencil, blend/cull/color
mask, matrices, fog/lightmap/overlay uniforms, or every pipeline binding.
The fail-closed `PrismTerrainRenderPassState` contract therefore remains
mandatory and incomplete until those live values can be captured and reproduced.
