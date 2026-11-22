#version 150

// Space Renderer built-in terrain vertex shader.
// This shader is intentionally independent of the Prism shader-pack runtime.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

layout(std140) uniform SpaceRendererSection {
    vec4 SectionOrigin;
};

uniform sampler2D Sampler2;

out vec4 vertexColor;
out vec2 texCoord0;

vec4 sampleLightmap(ivec2 packedLight) {
    vec2 uv = (vec2(packedLight) / 256.0) + vec2(0.5 / 16.0);
    uv = clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
    return texture(Sampler2, uv);
}

void main() {
    vec3 cameraRelativePosition = Position + SectionOrigin.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(cameraRelativePosition, 1.0);
    vertexColor = Color * sampleLightmap(UV2);
    texCoord0 = UV0;
}
