package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainCameraTransformTest {
    @Test
    void convertsSectionIndexToCameraRelativeOrigin() {
        var transform = PrismTerrainCameraTransform.forSection(
                10, -2, 3, 161.5, -17.0, 40.25);

        assertEquals(-1.5f, transform.originX(), 0.0001f);
        assertEquals(-15.0f, transform.originY(), 0.0001f);
        assertEquals(7.75f, transform.originZ(), 0.0001f);
        assertEquals(14.5f, transform.x(16.0f), 0.0001f);
    }

    @Test
    void subtractsCameraInDoublePrecisionBeforeFloatConversion() {
        double cameraX = 299_999_999.5;
        var transform = PrismTerrainCameraTransform.forSection(
                18_750_000, 0, 0, cameraX, 0.0, 0.0);

        assertEquals(0.5f, transform.originX(), 0.0001f);
    }

    @Test
    void supportsNegativeSectionCoordinates() {
        var transform = PrismTerrainCameraTransform.forSection(
                -1, -4, 2, -8.0, -64.0, 32.0);

        assertEquals(-8.0f, transform.originX(), 0.0001f);
        assertEquals(0.0f, transform.originY(), 0.0001f);
        assertEquals(0.0f, transform.originZ(), 0.0001f);
    }

    @Test
    void derivesTransformFromMeshWithoutChangingItsLocalPayload() {
        var mesh = new PrismTerrainMeshBuilder(
                4, 5, 6, "example:stone", PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0)
                .build();

        var transform = PrismTerrainCameraTransform.forMesh(mesh, 64.0, 80.0, 96.0);

        assertEquals(0.0f, transform.originX(), 0.0001f);
        assertEquals(0.0f, transform.originY(), 0.0001f);
        assertEquals(0.0f, transform.originZ(), 0.0001f);
        assertEquals(0.0f, mesh.positions()[0], 0.0001f);
    }

    @Test
    void rejectsNonFiniteCameraAndInvalidLocalCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
                PrismTerrainCameraTransform.forSection(0, 0, 0, Double.NaN, 0, 0));
        var transform = PrismTerrainCameraTransform.forSection(0, 0, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> transform.x(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> transform.y(-0.01f));
        assertThrows(IllegalArgumentException.class, () -> transform.z(16.01f));
    }
}
