#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

layout(std140) uniform SpaceRendererSection {
    vec4 SectionOrigin;
};

out vec4 vertexColor;
out vec2 texCoord0;

vec4 sampleLightmap(sampler2D lightMap, ivec2 packedLight) {
    vec2 uv = (vec2(packedLight) / 256.0) + vec2(0.5 / 16.0);
    uv = clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    return texture(lightMap, uv);
}

void main() {
    vec3 cameraRelativePosition = Position + SectionOrigin.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(cameraRelativePosition, 1.0);
    vertexColor = Color * sampleLightmap(Sampler2, UV2);
    texCoord0 = UV0;
}
