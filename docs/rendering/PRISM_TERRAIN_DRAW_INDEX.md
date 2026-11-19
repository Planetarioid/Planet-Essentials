# Vanilla terrain draw-record indexing

An identity index is captured at the RETURN of LevelRenderer.prepareChunkRenders.
That method traverses visibleSections and appends one RenderPass.Draw per
ChunkSectionLayer exactly when SectionMesh.getBuffers(layer) is non-null.

The mixin repeats that same traversal after the result is built, pairing each
per-layer Draw object by identity with its section-node coordinates, layer,
and layer-local ordinal. If a layer is absent, counts disagree, or a draw is
not a RenderPass.Draw, the entire snapshot is cleared. No partial mapping or
guessed association is published.

This provides a lookup from a vanilla draw record to its exact captured Prism
payload key (section XYZ plus layer/material). The snapshot exposes complete
coverage checks and an ordered descriptor view that can reverse a layer when
vanilla does so.

The index is planning data only. It does not alter or cancel
ChunkSectionsToRender.renderGroup. Vanilla remains authoritative. Next is
building complete group plans, matching capture revisions and GPU readiness,
then implementing a synchronous replacement pass preserving target, sampler,
light texture, pipelines, and per-section uniforms.
