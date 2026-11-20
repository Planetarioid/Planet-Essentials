# Prism vanilla terrain replacement

The bundled **Planet Default** shader pack declares the Terrain V1 program
`planet:terrain` at
`assets/planet_essentials/prism/shaderpacks/default/prism.json`. It is
discovered through Minecraft's active ResourceManager like other Prism packs.
New configurations preselect `planet:default`, but shader execution remains
opt-in because `shadersEnabled` defaults to false.

## Terrain V1 shader contract

The pack manifest declares these uniform buffers:

- `PrismSection`: one std140 vec4. XYZ is the section's world origin relative
  to the camera; W is reserved and currently 1.
- `ChunkSection`: Minecraft 1.21.11's original per-draw block containing
  `ModelViewMat`, `ChunkVisibility`, `TextureSize`, and `ChunkPosition`.
  Prism replays the original per-draw uploader, so the matrix and section
  metadata remain associated with the correct draw.
- `Projection`: Minecraft's captured projection matrix.
- `Fog`: Minecraft's captured fog color and distance parameters.

The shader declares `Sampler0` for the block atlas and `Sampler2` for the
lightmap. These are rebound from the captured vanilla pass. It consumes the
Prism V1 36-byte vertex format: Position, UV0, RGBA color, packed light (UV2),
overlay (UV1), normal, and one padding byte. The shader uses position, UV0,
color, and packed light; the unused overlay and normal attributes remain in
the stable vertex layout for future shader versions.

The vertex transform adds the camera-relative section origin to section-local
positions, then applies the captured ChunkSection model-view matrix and
Projection matrix. Fragment processing samples the atlas and lightmap, applies
the captured chunk-visibility fade, then applies spherical environmental fog
and cylindrical render-distance fog.

Vanilla terrain pipelines supply different `ALPHA_CUTOUT` values for solid,
cutout, translucent, and tripwire layers. The pipeline clone merges the exact
vanilla shader defines with the selected Prism program before compiling each
layer variant, preserving those thresholds instead of hard-coding one value.

A selected pack opts into replacement only by declaring the exact program ID
`planet:terrain`. That program must be Terrain V1 and declare the required
`PrismSection`, `ChunkSection`, and `Projection` blocks. It may use captured
vanilla default blocks and the supported `Sampler0` and `Sampler2` samplers.
Unknown or unavailable bindings fail closed.

## Render-group behavior

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

This is an opt-in execution path, not a claim of visual parity on all devices.
The shader sources are bundled and compiled through the normal pack reload
transaction, but actual GPU compilation, graphics-backend behavior, and
in-world visual parity still require Minecraft runtime testing.

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
backend. It does not establish visual parity by itself; in-game validation is
still required.
