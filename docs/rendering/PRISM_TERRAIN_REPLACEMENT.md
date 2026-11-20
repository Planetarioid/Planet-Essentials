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
- `Globals`: Minecraft's global block, including `UseRgss`. Camera block
  position and fractional offset are not needed by this vertex shader because
  PrismSection already carries the equivalent camera-relative section origin.

The shader declares `Sampler0` for the block atlas and `Sampler2` for the
lightmap. These are rebound from the captured vanilla pass. It consumes the
Prism V1 36-byte vertex format: Position, UV0, RGBA color, packed light (UV2),
overlay (UV1), normal, and one padding byte. Overlay and normal remain present
for stable format compatibility; vanilla terrain's shader does not consume
them.

## Minecraft 1.21.11 terrain shading

The vertex stage uses the camera-relative section origin plus section-local
vertex position, then applies the original per-draw ModelViewMat and Projection
matrix. It computes spherical and cylindrical fog distances from the same
camera-relative position and applies the vanilla lightmap coordinate transform.

The fragment stage reproduces Minecraft's atlas sampling path: nearest
sampling with screen derivatives and the optional four-tap rotated-grid
super-sampling path selected by the captured `Globals.UseRgss` value. It uses
the captured atlas dimensions from `ChunkSection.TextureSize`, applies the
vanilla chunk-visibility fade, preserves the exact per-layer
`ALPHA_CUTOUT` define, and applies Minecraft's spherical/environmental and
cylindrical/render-distance fog calculation.

The Prism vertex format retains overlay and normal attributes even though the
vanilla terrain shader does not consume them. This leaves the V1 mesh contract
stable for future shader variants.

A selected pack opts into replacement only by declaring the exact program ID
`planet:terrain`. That program must be Terrain V1 and declare the required
`PrismSection`, `ChunkSection`, and `Projection` blocks. The bundled pack
also declares `Fog` and `Globals`, plus the supported `Sampler0` and
`Sampler2` samplers. Unknown or unavailable bindings fail closed.

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

The shader source is matched to the Minecraft 1.21.11 terrain shader behavior,
but actual GPU compilation and in-world visual parity still require testing in
Minecraft on the target graphics backend.
