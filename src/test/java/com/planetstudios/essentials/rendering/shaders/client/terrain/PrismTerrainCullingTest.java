package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainCullingTest {
    private static PrismTerrainFrustum boxFrustum(double min, double max) {
        return new PrismTerrainFrustum(List.of(
                new PrismTerrainFrustum.Plane(1, 0, 0, -min),
                new PrismTerrainFrustum.Plane(-1, 0, 0, max),
                new PrismTerrainFrustum.Plane(0, 1, 0, -min),
                new PrismTerrainFrustum.Plane(0, -1, 0, max),
                new PrismTerrainFrustum.Plane(0, 0, 1, -min),
                new PrismTerrainFrustum.Plane(0, 0, -1, max)));
    }
    private static PrismTerrainMesh mesh(int x, int y, int z) {
        return new PrismTerrainMeshBuilder(x, y, z, "planet:test", PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(0,0,0, 1,0,0, 1,1,0, 0,1,0).build();
    }
    @Test void keepsIntersectingAndRejectsOutsideSections() {
        var frustum = boxFrustum(0, 32);
        assertTrue(frustum.intersects(16, 0, 0, 32, 16, 16));
        assertFalse(frustum.intersects(48, 0, 0, 64, 16, 16));
    }
    @Test void filtersFrameWithoutChangingVisibleOrder() {
        var frame = new PrismTerrainFrame(1, List.of(mesh(0,0,0), mesh(4,0,0), mesh(1,0,0)), 12);
        var visible = PrismTerrainCulling.visibleMeshes(frame, boxFrustum(0, 32));
        assertEquals(2, visible.size());
        assertEquals(0, visible.get(0).sectionX());
        assertEquals(1, visible.get(1).sectionX());
        assertEquals(3, frame.meshes().size());
        PrismTerrainFrame culled = PrismTerrainCulling.visibleFrame(frame, boxFrustum(0, 32));
        assertEquals(2, culled.meshes().size());
        assertEquals(8, culled.vertexCount());
        assertEquals(frame.generation(), culled.generation());
    }
    @Test void rejectsInvalidPlanesAndBounds() {
        assertThrows(IllegalArgumentException.class, () -> new PrismTerrainFrustum(List.of(new PrismTerrainFrustum.Plane(0,0,0,0))));
        assertThrows(IllegalArgumentException.class, () -> boxFrustum(0, 32).intersects(2,0,0,1,1,1));
    }
}