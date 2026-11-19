# Prism terrain replacement planning

`PrismTerrainReplacementPlan` builds an immutable, all-or-nothing plan for one vanilla terrain layer draw list. It joins identity-indexed vanilla Draw objects to section/layer descriptors, validates the expected layer, rejects duplicate section/layer entries, and asks the caller to verify that each descriptor has a current captured payload and usable uploaded GPU mesh.

The caller also supplies program, device, and frame readiness. A plan is replaceable only when every draw is indexed and ready and all global conditions pass. For translucent groups the caller can request reversed ordering to preserve vanilla's layer-local list order. A failed plan contains no replacement entries, preventing partial submission.

This is a pure planning primitive. It does not upload buffers, submit draws, change render targets, install a cancellable mixin, or suppress vanilla. The next step is connecting its readiness predicate to revision-safe CPU payloads and GPU cache entries, then validating the actual ChunkSectionsToRender render-pass state before attempting a synchronous replacement hook.
