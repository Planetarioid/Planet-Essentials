package com.planetstudios.essentials.rendering.space;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpaceRendererSectionManagerTest {
    @Test
    void newerBuildSupersedesOlderToken() {
        var manager = new SpaceRendererSectionManager();
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 1, 2, 3);

        var first = manager.beginBuild(key);
        var second = manager.beginBuild(key);

        assertFalse(manager.isCurrent(first));
        assertTrue(manager.isCurrent(second));
        assertEquals(second.revision(), manager.currentRevision(key));
    }

    @Test
    void unloadInvalidatesOutstandingBuild() {
        var manager = new SpaceRendererSectionManager();
        var key = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 4, 5, 6);
        var token = manager.beginBuild(key);

        assertTrue(manager.unload(key));
        assertFalse(manager.isCurrent(token));
        assertFalse(manager.unload(key));
        assertEquals(0, manager.trackedSectionCount());
    }

    @Test
    void dimensionUnloadDoesNotTouchOtherDimensions() {
        var manager = new SpaceRendererSectionManager();
        var overworld = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 0, 0, 0);
        var nether = new SpaceRendererSectionManager.SectionKey("minecraft:the_nether", 0, 0, 0);
        var overworldToken = manager.beginBuild(overworld);
        var netherToken = manager.beginBuild(nether);

        assertEquals(1, manager.unloadDimension("minecraft:overworld"));
        assertFalse(manager.isCurrent(overworldToken));
        assertTrue(manager.isCurrent(netherToken));
    }

    @Test
    void dimensionIsPartOfSectionIdentity() {
        var manager = new SpaceRendererSectionManager();
        var overworld = new SpaceRendererSectionManager.SectionKey("minecraft:overworld", 3, 4, 5);
        var nether = new SpaceRendererSectionManager.SectionKey("minecraft:the_nether", 3, 4, 5);

        var a = manager.beginBuild(overworld);
        var b = manager.beginBuild(nether);

        assertTrue(manager.isCurrent(a));
        assertTrue(manager.isCurrent(b));
        assertEquals(2, manager.trackedSectionCount());
    }

    @Test
    void rejectsBlankDimension() {
        assertThrows(IllegalArgumentException.class,
                () -> new SpaceRendererSectionManager.SectionKey(" ", 0, 0, 0));
    }
}
