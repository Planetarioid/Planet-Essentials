package com.planetstudios.essentials.rendering.shaders.client.terrain;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrismTerrainFrameQueueTest {
    private static PrismTerrainMesh mesh(int x, String material) {
        return new PrismTerrainMeshBuilder(x,0,0,material,PrismFrameStage.BEFORE_ENTITIES)
                .addQuad(0,0,0, 1,0,0, 1,1,0, 0,1,0).build();
    }

    @Test
    void publishesDeterministicallyOrderedImmutableFrame() {
        var queue = new PrismTerrainFrameQueue();
        queue.beginFrame();
        queue.submit(mesh(2,"example:z"));
        queue.submit(mesh(1,"example:a"));
        var frame = queue.publish();
        assertEquals(8, frame.vertexCount());
        assertEquals(1, frame.meshes().get(0).sectionX());
        assertEquals(2, frame.meshes().get(1).sectionX());
        assertThrows(UnsupportedOperationException.class, () -> frame.meshes().clear());
        assertThrows(IllegalStateException.class, () -> queue.submit(mesh(3,"example:c")));
    }

    @Test
    void rejectsDuplicateSectionMaterialAndStage() {
        var queue = new PrismTerrainFrameQueue();
        queue.beginFrame();
        queue.submit(mesh(0,"example:stone"));
        assertThrows(IllegalArgumentException.class, () -> queue.submit(mesh(0,"example:stone")));
    }

    @Test
    void newFrameInvalidatesUnpublishedPreviousData() {
        var queue = new PrismTerrainFrameQueue();
        long first = queue.beginFrame();
        queue.submit(mesh(0,"example:stone"));
        long second = queue.beginFrame();
        assertTrue(second > first);
        assertTrue(queue.publish().meshes().isEmpty());
    }

    @Test
    void requiresExplicitFrameLifecycle() {
        var queue = new PrismTerrainFrameQueue();
        assertThrows(IllegalStateException.class, queue::publish);
        assertThrows(IllegalStateException.class, () -> queue.submit(mesh(0,"example:stone")));
    }
}
