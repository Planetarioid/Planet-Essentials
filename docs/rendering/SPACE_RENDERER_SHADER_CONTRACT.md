# Space Renderer built-in terrain shader contract

The independent terrain shaders are registered as native Minecraft 1.21.11
`RenderPipeline` instances by `SpaceRendererTerrainPipelines`. Registration
uses Minecraft's pipeline registry and does not use Prism's shader-pack
catalog or shader runtime.

## Vertex input contract

The sources consume the existing 36-byte terrain vertex stream:

| Attribute | Type | Meaning |
|---|---|---|
| `Position` | `vec3` | Section-local position |
| `UV0` | `vec2` | Block-atlas texture coordinate |
| `Color` | `vec4` | Vertex tint and ambient-occlusion color |
| `UV2` | `ivec2` | Packed block/sky light coordinates |
| `UV1` | reserved | Overlay coordinates |
| `Normal` | `vec3` | Reserved for future directional lighting |

The vertex stage adds a camera-relative section origin before applying
Minecraft's dynamic model-view and projection uniform buffers. It samples the
lightmap and passes atlas coordinates and lit vertex color to the fragment
stage.

## Pipeline variants

- Solid: depth test/write, no blending.
- Cutout: alpha discard threshold 0.5.
- Translucent: blending enabled, depth writes disabled, threshold 0.01.
- Tripwire: blending enabled, threshold 0.1.

All variants use the same vertex layout, block atlas sampler, lightmap sampler,
and per-section uniform buffer. The draw executor must bind the section UBO
before issuing a draw.

## Current boundary

The pipelines are registered during client initialization and are discoverable
through Minecraft's static pipeline registry. The shader sources and Java
pipeline descriptions are covered by unit tests. This does not yet prove that
the game has compiled the GLSL or that a GPU draw works. Fog, exact vanilla
overlay behavior, animated texture handling, resource-pack compatibility, and
render-pass integration remain outstanding. Vanilla terrain rendering remains
authoritative until complete-frame submission is ready.
