package com.planetstudios.essentials.rendering.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererSectionManagerTest {
 @Test void newerBuildSupersedes(){var m=new SpaceRendererSectionManager();var k=new SpaceRendererSectionManager.SectionKey("minecraft:overworld",1,2,3);var a=m.beginBuild(k);var b=m.beginBuild(k);assertFalse(m.isCurrent(a));assertTrue(m.isCurrent(b));}
 @Test void unloadInvalidates(){var m=new SpaceRendererSectionManager();var k=new SpaceRendererSectionManager.SectionKey("minecraft:overworld",0,0,0);var t=m.beginBuild(k);assertTrue(m.unload(k));assertFalse(m.isCurrent(t));}
 @Test void dimensionsAreDistinct(){var m=new SpaceRendererSectionManager();var a=m.beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:overworld",0,0,0));var b=m.beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:the_nether",0,0,0));assertTrue(m.isCurrent(a));assertTrue(m.isCurrent(b));}
 @Test void unloadDimensionScoped(){var m=new SpaceRendererSectionManager();m.beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:overworld",0,0,0));m.beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:the_nether",0,0,0));assertEquals(1,m.unloadDimension("minecraft:overworld"));assertEquals(1,m.trackedSectionCount());}
}
