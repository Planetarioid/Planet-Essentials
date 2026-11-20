# Per-world terrain replacement coordinator

`PrismTerrainReplacementCoordinator` is the render-thread orchestration layer
between the vanilla draw identity index, the per-`ClientLevel` CPU section
cache, the per-world GPU cache, and the shader runtime.

For each non-empty vanilla layer group it:

1. Verifies the current client world and caller-provided frame-ready flag.
2. Requires a complete identity mapping for every vanilla draw.
3. Resolves the selected Prism terrain program and requires the V1 terrain
   vertex format.
4. Captures the active shader generation.
5. Uploads each unique captured section/layer payload to that world's GPU LRU.
6. Builds the all-or-nothing plan using the exact CPU payload and matching
   live GPU allocation readiness predicate.
7. Rechecks the shader generation before returning the plan.

Chunk unloads invalidate that world's matching GPU allocations through the
existing chunk lifecycle invalidator. Disconnect/world teardown closes and
removes the corresponding GPU cache. Cache ownership is keyed by
`ClientLevel` object identity, not dimension name or coordinates.

A missing payload, failed upload, incomplete draw index, unavailable shader,
world mismatch, or generation change produces an empty fallback plan. The
caller must continue vanilla rendering for that entire group.

## Deliberate boundary

This coordinator does not hook the vanilla render-group method, reproduce
vanilla render-pass state, or suppress vanilla draws. Its `prepare` method is
the intended preparation call for a future synchronous replacement hook.
It currently requires render-thread invocation and a truthful
`frameReady` value from that future hook. Graphics-context and in-game
validation remain outstanding.
