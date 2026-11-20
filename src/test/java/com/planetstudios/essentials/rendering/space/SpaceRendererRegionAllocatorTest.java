package com.planetstudios.essentials.rendering.space;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererRegionAllocatorTest {
 @Test void alignsAndTracks(){var a=new SpaceRendererRegionAllocator(1024,256);var x=a.allocate(300);var y=a.allocate(1);assertEquals(512,x.reservedBytes());assertEquals(512,y.offsetBytes());assertEquals(768,a.allocatedBytes());}
 @Test void coalesces(){var a=new SpaceRendererRegionAllocator(1024,64);var x=a.allocate(128);var y=a.allocate(128);var z=a.allocate(128);a.release(y);a.release(x);a.release(z);assertEquals(1024,a.largestFreeRangeBytes());}
 @Test void rejectsDoubleRelease(){var a=new SpaceRendererRegionAllocator(256,64);var x=a.allocate(64);a.release(x);assertThrows(IllegalArgumentException.class,()->a.release(x));}
 @Test void returnsNullWhenFull(){var a=new SpaceRendererRegionAllocator(64,16);assertNull(a.allocate(65));assertNotNull(a.allocate(64));assertNull(a.allocate(1));}
}
