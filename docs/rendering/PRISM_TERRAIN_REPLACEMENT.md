# Prism vanilla terrain replacement

A shader pack opts into vanilla terrain replacement by declaring a shader program
with the exact identifier `planet:terrain`. The program must compile through Prism's Terrain V1 path and declare the
required `PrismSection` uniform buffer. Prism V1 vertices are section-local;
the per-draw `PrismSection` vec4 supplies the camera-relative section origin,
so a vanilla `DynamicTransforms` binding is not required. A program may also
declare any captured vanilla default uniform (`Projection`, `Fog`, `Globals`,
or `Lighting`) and the supported `Sampler0` block-atlas and `Sampler2`
lightmap bindings. Unsupported or unavailable bindings fail closed.

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


## Current safety gate

The replacement hook executes before vanilla creates its RenderPass. The
viewport and scissor values are captured from the verified Minecraft 1.21.11
pass-creation behavior: the OpenGL command encoder binds the target and sets
the viewport to the color attachment dimensions, while each new GlRenderPass
starts with scissoring disabled. Vanilla's terrain renderGroup does not alter
that per-pass scissor state. The replacement pass is created through the same
command-encoder API, so these defaults are reproduced rather than inferred from
the previously active global GL state.

This closes the viewport/scissor capture gap for the supported vanilla OpenGL
backend. It does not establish visual parity by itself; the remaining shader,
pipeline-state, and in-game validation gates below still apply.
