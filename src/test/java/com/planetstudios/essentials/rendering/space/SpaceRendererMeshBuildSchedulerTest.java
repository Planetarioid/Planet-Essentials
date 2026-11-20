package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererMeshBuildSchedulerTest {
    private static final java.util.concurrent.ThreadFactory THREADS = task -> {
        Thread thread = new Thread(task, "space-renderer-test-worker");
        thread.setDaemon(true);
        return thread;
    };

    @Test
    void boundsInflightBuildsUntilCompletionsAreDrained() throws Exception {
        var manager = new SpaceRendererSectionManager();
        var token = manager.beginBuild(new SpaceRendererSectionManager.SectionKey(
                "minecraft:overworld", 0, 0, 0));
        try (var scheduler = new SpaceRendererMeshBuildScheduler<String>(1, 1, THREADS)) {
            assertTrue(scheduler.submit(token, 0.0, () -> "mesh"));
            assertFalse(scheduler.submit(token, 1.0, () -> "overflow"));
            var result = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            assertNotNull(result);
            assertTrue(result.succeeded());
            assertEquals("mesh", result.value());
            assertEquals(1, scheduler.availableCapacity());
        }
    }

    @Test
    void executesQueuedBuildsInPriorityOrder() throws Exception {
        var manager = new SpaceRendererSectionManager();
        var token = manager.beginBuild(new SpaceRendererSectionManager.SectionKey(
                "minecraft:overworld", 0, 0, 0));
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var scheduler = new SpaceRendererMeshBuildScheduler<String>(1, 3, THREADS)) {
            assertTrue(scheduler.submit(token, 100.0, () -> {
                started.countDown();
                release.await(2, TimeUnit.SECONDS);
                return "running";
            }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(scheduler.submit(token, 50.0, () -> "lower-priority"));
            assertTrue(scheduler.submit(token, 1.0, () -> "highest-priority"));
            release.countDown();

            var first = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            var second = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            var third = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            assertNotNull(first);
            assertNotNull(second);
            assertNotNull(third);
            assertEquals("running", first.value());
            assertEquals("highest-priority", second.value());
            assertEquals("lower-priority", third.value());
        }
    }

    @Test
    void reportsBuildFailuresWithoutKillingWorker() throws Exception {
        var manager = new SpaceRendererSectionManager();
        var token = manager.beginBuild(new SpaceRendererSectionManager.SectionKey(
                "minecraft:overworld", 1, 0, 0));
        try (var scheduler = new SpaceRendererMeshBuildScheduler<String>(1, 2, THREADS)) {
            assertTrue(scheduler.submit(token, 0.0, () -> { throw new IllegalStateException("broken mesh"); }));
            var failure = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            assertNotNull(failure);
            assertFalse(failure.succeeded());
            assertInstanceOf(IllegalStateException.class, failure.failure());

            assertTrue(scheduler.submit(token, 0.0, () -> "recovered"));
            var success = scheduler.pollCompletion(2, TimeUnit.SECONDS);
            assertNotNull(success);
            assertEquals("recovered", success.value());
        }
    }

    @Test
    void rejectsInvalidConfigurationAndPriorities() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererMeshBuildScheduler<String>(0, 1, THREADS));
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererMeshBuildScheduler<String>(2, 1, THREADS));
        var manager = new SpaceRendererSectionManager();
        var token = manager.beginBuild(new SpaceRendererSectionManager.SectionKey(
                "minecraft:overworld", 0, 0, 0));
        try (var scheduler = new SpaceRendererMeshBuildScheduler<String>(1, 1, THREADS)) {
            assertThrows(IllegalArgumentException.class,
                    () -> scheduler.submit(token, Double.NaN, () -> "bad"));
        }
    }
}
