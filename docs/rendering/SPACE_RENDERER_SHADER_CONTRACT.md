# Space Renderer built-in terrain shader contract

The first built-in terrain shader sources live under
`assets/planet_essentials/shaders/core/`. They are Space Renderer resources,
not entries in the Prism shader-pack catalog, and do not require Prism shader
pack selection or reload state.

## Vertex input contract

The sources consume the existing 36-byte terrain vertex stream:

| Attribute | Type | Meaning |
|---|---|---|
| `Position` | `vec3` | Section-local position |
| `UV0` | `vec2` | Block-atlas texture coordinate |
| `Color` | `vec4` | Vertex tint and ambient-occlusion color |
| `UV2` | `ivec2` | Packed block/sky light coordinates |
| `UV1` | not consumed yet | Overlay coordinates reserved by the vertex format |
| `Normal` | not consumed yet | Packed normal reserved by the vertex format |

The vertex stage adds the camera-relative section origin before applying the
model-view and projection matrices. The block atlas is sampled in the fragment
stage and the lightmap in the vertex stage.

## Uniform and sampler contract

- `ModelViewMat`: camera/model-view matrix.
- `ProjMat`: projection matrix.
- `SpaceRendererSection`: std140 block containing one `vec4 SectionOrigin`.
- `Sampler0`: block texture atlas.
- `Sampler2`: lightmap texture.

## Current boundary

These files are the initial independent shader source contract only. They are
not yet registered as a Minecraft `RenderPipeline`, compiled by the game, or
used for terrain submission. Fog, overlay handling, render-layer blend/depth
state, animated texture handling, and resource-pack compatibility still need
to be integrated and validated before any terrain takeover. Vanilla rendering
remains authoritative.
