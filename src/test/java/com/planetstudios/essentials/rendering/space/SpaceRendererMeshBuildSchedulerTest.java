package com.planetstudios.essentials.rendering.space;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class SpaceRendererMeshBuildSchedulerTest {
 private static final java.util.concurrent.ThreadFactory FACTORY=r->{Thread t=new Thread(r,"space-renderer-test");t.setDaemon(true);return t;};
 private static SpaceRendererSectionManager.BuildToken token(){return new SpaceRendererSectionManager().beginBuild(new SpaceRendererSectionManager.SectionKey("minecraft:overworld",0,0,0));}
 @Test void boundsInflightUntilDrain()throws Exception{try(var s=new SpaceRendererMeshBuildScheduler<String>(1,1,FACTORY)){assertTrue(s.submit(token(),0,()->"mesh"));assertFalse(s.submit(token(),1,()->"overflow"));var r=s.pollCompletion(2,TimeUnit.SECONDS);assertNotNull(r);assertEquals("mesh",r.value());assertEquals(1,s.availableCapacity());}}
 @Test void prioritizesQueuedTasks()throws Exception{var started=new CountDownLatch(1);var release=new CountDownLatch(1);try(var s=new SpaceRendererMeshBuildScheduler<String>(1,3,FACTORY)){assertTrue(s.submit(token(),100,()->{started.countDown();release.await();return "running";}));assertTrue(started.await(2,TimeUnit.SECONDS));assertTrue(s.submit(token(),50,()->"low"));assertTrue(s.submit(token(),1,()->"high"));release.countDown();assertEquals("running",s.pollCompletion(2,TimeUnit.SECONDS).value());assertEquals("high",s.pollCompletion(2,TimeUnit.SECONDS).value());assertEquals("low",s.pollCompletion(2,TimeUnit.SECONDS).value());}}
 @Test void capturesFailureAndKeepsWorkerAlive()throws Exception{try(var s=new SpaceRendererMeshBuildScheduler<String>(1,2,FACTORY)){assertTrue(s.submit(token(),0,()->{throw new IllegalStateException("broken");}));assertFalse(s.pollCompletion(2,TimeUnit.SECONDS).succeeded());assertTrue(s.submit(token(),0,()->"ok"));assertEquals("ok",s.pollCompletion(2,TimeUnit.SECONDS).value());}}
 @Test void rejectsInvalidConfig(){assertThrows(IllegalArgumentException.class,()->new SpaceRendererMeshBuildScheduler<String>(0,1,FACTORY));}
}
