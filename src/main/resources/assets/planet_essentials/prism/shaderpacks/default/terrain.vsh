#version 330

layout(std140) uniform PrismSection {
    vec4 SectionOrigin;
};

layout(std140) uniform ChunkSection {
    mat4 ModelViewMat;
    float ChunkVisibility;
    ivec2 TextureSize;
    ivec3 ChunkPosition;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

uniform sampler2D Sampler2;

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

out vec3 TerrainCameraPosition;
out vec4 TerrainVertexColor;
out vec2 TerrainAtlasUv;

vec4 sampleTerrainLightmap(sampler2D lightmap, ivec2 packedLightUv) {
    vec2 uv = vec2(packedLightUv) / 256.0 + vec2(0.5 / 16.0);
    uv = clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    return texture(lightmap, uv);
}

void main() {
    // PrismSection is the section's world origin relative to the camera.
    // The captured ChunkSection matrix supplies the same camera rotation used
    // by Minecraft's terrain pipeline, without reintroducing world translation.
    vec3 cameraRelativePosition = SectionOrigin.xyz + Position;
    vec4 viewPosition = ModelViewMat * vec4(cameraRelativePosition, 1.0);

    gl_Position = ProjMat * viewPosition;
    TerrainCameraPosition = cameraRelativePosition;
    TerrainVertexColor = Color * sampleTerrainLightmap(Sampler2, UV2);
    TerrainAtlasUv = UV0;
}
