# Vanilla terrain render-pass state contract

`PrismTerrainRenderPassState` defines the minimum evidence a future synchronous
vanilla render-group hook must collect before Prism can replace that group.
It is intentionally a completeness contract, not a state synthesizer.

Every capture must explicitly account for:

- Render target, viewport, and scissor
- Depth/stencil, blending, culling, and color mask
- Lightmap, overlay, and fog resources/state
- Model-view and projection matrices
- Minecraft default uniforms
- The exact vanilla layer pipeline and draw ordering
- Every sampler declared by the selected Prism terrain program

The coordinator now requires a complete snapshot for the exact layer before
it uploads or publishes a replacement plan. Missing state or a layer mismatch
returns `RENDER_STATE_NOT_READY`, leaving the whole vanilla group untouched.

The current work establishes and tests the gate. It does not claim that the
mixin currently captures these values, nor that Prism can reproduce the
vanilla pass yet. The next implementation step is to instrument the actual
Minecraft 1.21.11 render-group boundary and populate this snapshot from live
pass state, then reproduce each captured category in Prism's draw executor.
