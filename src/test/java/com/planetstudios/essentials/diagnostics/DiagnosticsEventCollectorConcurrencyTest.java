package com.planetstudios.essentials.diagnostics;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class DiagnosticsEventCollectorConcurrencyTest {
 @Test void concurrentRecordingAndSnapshotsRemainBounded()throws Exception{
  DiagnosticsEventCollector c=new DiagnosticsEventCollector(100);ExecutorService pool=Executors.newFixedThreadPool(6);
  try{
   List<Future<?>> tasks=new ArrayList<>();
   for(int w=0;w<6;w++)tasks.add(pool.submit(()->{for(int i=0;i<300;i++){c.record(DiagnosticEventType.SLOW_CLIENT_TICK,i);assertTrue(c.snapshot().size()<=100);}}));
   for(Future<?> task:tasks)task.get(10,TimeUnit.SECONDS);
   assertEquals(100,c.eventCount());
  }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
 }
}
