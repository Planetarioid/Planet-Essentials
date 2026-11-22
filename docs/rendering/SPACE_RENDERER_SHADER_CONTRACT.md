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

Both vanilla-captured terrain and the independent baked-model mesher preserve
section-local vertex coordinates. The per-section std140 UBO contains one
16-byte `vec4 SectionOrigin`, calculated as
`(sectionX * 16 - cameraX, sectionY * 16 - cameraY, sectionZ * 16 - cameraZ, 0)`.
The vertex stage adds this origin before applying Minecraft's dynamic model-view
and projection uniform buffers. It samples the lightmap and passes atlas
coordinates and lit vertex color to the fragment stage.

`SpaceRendererSectionUniformData` owns CPU-side encoding. The
`SpaceRendererSectionUniformBuffer` uploads each vec4 into an aligned slice
of a triple-buffered mapped GPU ring. Its per-frame capacity is bounded
(default 4096 sections), and the ring rotates after submitted draws so in-flight
uniform data is not overwritten. A draw binds the returned slice under
`SpaceRendererSection`.

The arena is implemented and unit-tested, but is not yet connected to the
visible-frame draw executor.

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
