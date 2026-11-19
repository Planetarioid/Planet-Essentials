package com.planetstudios.essentials.diagnostics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DiagnosticsEventCollectorTest {
 @Test void retainsNewestEventsWithinConfiguredCapacity(){
  DiagnosticsEventCollector c=new DiagnosticsEventCollector(20);
  for(int i=0;i<25;i++)c.record(DiagnosticEventType.SLOW_CLIENT_TICK,i);
  assertEquals(20,c.eventCount());assertEquals(24,c.latest().durationMillis());
  assertEquals(20,c.snapshot().size());
 }
 @Test void clearRemovesAllEvents(){DiagnosticsEventCollector c=new DiagnosticsEventCollector(20);c.record(DiagnosticEventType.CLIENT_STARTED);c.clear();assertEquals(0,c.eventCount());assertNull(c.latest());}
 @Test void shrinkingCapacityDropsOldestEvents(){DiagnosticsEventCollector c=new DiagnosticsEventCollector(30);for(int i=0;i<25;i++)c.record(DiagnosticEventType.WORLD_JOINED);c.setCapacity(20);assertEquals(20,c.eventCount());}
 @Test void rejectsInvalidCapacityAndNegativeDurations(){
  assertThrows(IllegalArgumentException.class,()->new DiagnosticsEventCollector(19));
  assertThrows(IllegalArgumentException.class,()->new DiagnosticEvent(java.time.Instant.now(),DiagnosticEventType.SLOW_CLIENT_TICK,-1));
 }
}
