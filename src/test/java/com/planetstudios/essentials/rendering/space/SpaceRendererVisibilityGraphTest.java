package com.planetstudios.essentials.rendering.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererVisibilityGraphTest {
 private static SpaceRendererSectionManager.SectionKey k(int x){return new SpaceRendererSectionManager.SectionKey("minecraft:overworld",x,0,0);}
 @Test void traversesOnlyMutuallyOpenFaces(){var g=new SpaceRendererVisibilityGraph();g.update(k(0),SpaceRendererVisibilityGraph.POSITIVE_X);g.update(k(1),SpaceRendererVisibilityGraph.NEGATIVE_X);g.update(k(2),SpaceRendererVisibilityGraph.ALL_FACES);var r=g.visibleFrom(k(0),32);assertTrue(r.visibleSections().contains(k(1)));assertFalse(r.visibleSections().contains(k(2)));assertTrue(r.isComplete());}
 @Test void unknownNeighborRequiresFallback(){var g=new SpaceRendererVisibilityGraph();g.update(k(0),SpaceRendererVisibilityGraph.ALL_FACES);assertTrue(g.visibleFrom(k(0),32).requiresFrustumFallback());}
 @Test void unknownOriginFailsOpen(){var g=new SpaceRendererVisibilityGraph();assertTrue(g.visibleFrom(k(0),32).requiresFrustumFallback());}
 @Test void limitRequiresFallback(){var g=new SpaceRendererVisibilityGraph();g.update(k(0),SpaceRendererVisibilityGraph.POSITIVE_X);g.update(k(1),SpaceRendererVisibilityGraph.NEGATIVE_X);assertTrue(g.visibleFrom(k(0),1).requiresFrustumFallback());}
 @Test void rejectsUnknownBits(){var g=new SpaceRendererVisibilityGraph();assertThrows(IllegalArgumentException.class,()->g.update(k(0),1<<7));}
}
