# Prism Terrain Frustum Culling

PrismTerrainFrustum is an immutable six-plane, world-space frustum. Plane normals use the inside-positive convention and may be unnormalized. PrismTerrainCulling.visibleMeshes tests each section's 16×16×16 world-space AABB using the positive-vertex plane test and returns an immutable, order-preserving visible list.

This is CPU-side frustum rejection for cooperating Prism terrain renderers. It can prevent unnecessary upload/draw work when called before cache lookup or submission. It is not wired to Minecraft's active camera/frustum yet, does not replace the renderer's own culling, and does not implement occlusion queries or depth-pyramid occlusion. The renderer adapter must supply six planes from its current camera/frustum and should continue to respect the active renderer's visibility decisions.
