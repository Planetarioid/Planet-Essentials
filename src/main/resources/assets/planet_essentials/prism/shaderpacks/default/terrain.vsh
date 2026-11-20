#version 330

// Minecraft 1.21.11 RenderPipeline does not inject vertex attributes.
// Declare every input used by the Terrain V1 shader explicitly.
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in ivec2 UV1;
in vec3 Normal;

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

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, clamp((vec2(uv) / 256.0) + 0.5 / 16.0,
            vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main() {
    // SectionOrigin is already the section's world origin relative to the camera.
    // This is equivalent to vanilla's Position + (ChunkPosition - CameraBlockPos)
    // + CameraOffset, while avoiding a second source of camera-position state.
    vec3 pos = Position + SectionOrigin.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    sphericalVertexDistance = length(pos);
    cylindricalVertexDistance = max(length(pos.xz), abs(pos.y));
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
