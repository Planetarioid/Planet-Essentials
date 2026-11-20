package com.planetstudios.essentials.rendering.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererGpuRegionManagerTest {
 private static SpaceRendererSectionManager.SectionKey key(int x){return new SpaceRendererSectionManager.SectionKey("minecraft:overworld",x,0,0);}
 @Test void replacementCommitsTransactionally(){var a=new SpaceRendererRegionAllocator(1024,64);var m=new SpaceRendererGpuRegionManager(a);var first=m.reserveReplacement(key(0),128);m.commitReplacement(first);var old=m.allocationFor(key(0));var next=m.reserveReplacement(key(0),256);assertEquals(old,m.allocationFor(key(0)));m.commitReplacement(next);assertEquals(next.next(),m.allocationFor(key(0)));assertEquals(256,m.allocatedBytes());}
 @Test void rollbackPreservesOldRange(){var a=new SpaceRendererRegionAllocator(512,64);var m=new SpaceRendererGpuRegionManager(a);var first=m.reserveReplacement(key(0),128);m.commitReplacement(first);var next=m.reserveReplacement(key(0),192);m.rollbackReplacement(next);assertEquals(first.next(),m.allocationFor(key(0)));assertEquals(128,m.allocatedBytes());}
 @Test void failedReserveKeepsOldRange(){var a=new SpaceRendererRegionAllocator(256,64);var m=new SpaceRendererGpuRegionManager(a);var first=m.reserveReplacement(key(0),192);m.commitReplacement(first);assertNull(m.reserveReplacement(key(0),128));assertEquals(first.next(),m.allocationFor(key(0)));}
 @Test void staleCommitCanBeRolledBack(){var a=new SpaceRendererRegionAllocator(512,64);var m=new SpaceRendererGpuRegionManager(a);var first=m.reserveReplacement(key(0),128);m.commitReplacement(first);var stale=m.reserveReplacement(key(0),192);m.releaseSection(key(0));assertThrows(IllegalStateException.class,()->m.commitReplacement(stale));m.rollbackReplacement(stale);assertEquals(0,m.allocatedBytes());}
}
