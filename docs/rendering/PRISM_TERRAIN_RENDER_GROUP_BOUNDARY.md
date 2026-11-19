# Terrain render-group boundary capture

Minecraft 1.21.11 routes terrain batches through
`ChunkSectionsToRender.renderGroup(ChunkSectionLayerGroup, GpuSampler)`.
Planet now observes that exact method at HEAD and RETURN.

The observer records:
- The exact opaque/translucent/tripwire group enum passed by vanilla
- The sampler object passed by vanilla, by identity
- A monotonically increasing invocation sequence
- A thread-local active snapshot available only during the synchronous call

This is deliberately observational: it does not change the draw lists, open or
close a RenderPass, alter GPU state, replace a pipeline, or cancel vanilla
rendering. It also does not claim to capture the render target, matrices,
lightmap, fog, blend/depth state, or all samplers. The full
`PrismTerrainRenderPassState` gate remains closed until those values can be
captured and reproduced truthfully.

The replacement coordinator now accepts the required render-state snapshot
explicitly. Callers cannot silently omit it, and incomplete state still
produces a vanilla fallback.
