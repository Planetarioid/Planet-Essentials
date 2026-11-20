# Prism vanilla terrain replacement

A shader pack opts into vanilla terrain replacement by declaring a shader program
with the exact identifier `planet:terrain`. The program must compile through
Prism's Terrain V1 path and declare the required `PrismSection` and
`DynamicTransforms` uniform buffers. It may use the captured vanilla
`Sampler0` block atlas and `Sampler2` lightmap bindings.

The replacement bridge runs at the HEAD of Minecraft 1.21.11's
`ChunkSectionsToRender.renderGroup`. It prepares every non-empty layer in the
group, resolves one shared QUADS index allocation, prepares every draw resource,
and submits the group into one pass using the captured vanilla attachments and
layer order. This is especially important for OPAQUE, which contains both SOLID
and CUTOUT.

Vanilla remains in control if any preparation step fails. If a draw submission
has already started and then fails, vanilla is suppressed to avoid drawing the
already-submitted terrain a second time. That failure is logged and must be
treated as an incomplete frame requiring investigation.

This integration is an opt-in execution path, not a claim of visual parity on
all devices. Runtime validation is still required for each supported graphics
backend and terrain layer.
