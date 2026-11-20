#version 330

layout(std140) uniform ChunkSection {
    mat4 ModelViewMat;
    float ChunkVisibility;
    ivec2 TextureSize;
    ivec3 ChunkPosition;
};

layout(std140) uniform Fog {
    vec4 FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
};

uniform sampler2D Sampler0;

in vec3 TerrainCameraPosition;
in vec4 TerrainVertexColor;
in vec2 TerrainAtlasUv;

out vec4 fragColor;

float terrainLinearFog(float distanceValue, float fogStart, float fogEnd) {
    if (distanceValue <= fogStart) return 0.0;
    if (distanceValue >= fogEnd) return 1.0;
    float span = max(fogEnd - fogStart, 0.0001);
    return clamp((distanceValue - fogStart) / span, 0.0, 1.0);
}

void main() {
    vec4 atlasColor = texture(Sampler0, TerrainAtlasUv);
    vec4 color = atlasColor * TerrainVertexColor;

    // Minecraft supplies a per-layer alpha threshold as ALPHA_CUTOUT.
    // The pipeline clone carries that exact vanilla define into this shader.
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) discard;
#endif

    color = mix(FogColor * vec4(1.0, 1.0, 1.0, color.a),
                color, clamp(ChunkVisibility, 0.0, 1.0));

    float sphericalDistance = length(TerrainCameraPosition);
    float cylindricalDistance = max(length(TerrainCameraPosition.xz),
                                     abs(TerrainCameraPosition.y));
    float environmentalFog = terrainLinearFog(
            sphericalDistance, FogEnvironmentalStart, FogEnvironmentalEnd);
    float renderDistanceFog = terrainLinearFog(
            cylindricalDistance, FogRenderDistanceStart, FogRenderDistanceEnd);
    float fogAmount = max(environmentalFog, renderDistanceFog);

    color.rgb = mix(color.rgb, FogColor.rgb, clamp(fogAmount * FogColor.a, 0.0, 1.0));
    fragColor = color;
}
