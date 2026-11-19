package com.planetstudios.essentials.diagnostics;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
/** Bounded, in-memory diagnostics event history. It never writes or transmits events. */
public final class DiagnosticsEventCollector {
 private final ArrayDeque<DiagnosticEvent> events=new ArrayDeque<>();
 private int capacity;
 public DiagnosticsEventCollector(int capacity){setCapacity(capacity);}
 public synchronized void setCapacity(int capacity){
  if(capacity<20||capacity>1200)throw new IllegalArgumentException("capacity must be 20..1200");
  this.capacity=capacity;while(events.size()>capacity)events.removeFirst();
 }
 public synchronized void record(DiagnosticEventType type){record(type,0);}
 public synchronized void record(DiagnosticEventType type,long durationMillis){
  if(events.size()==capacity)events.removeFirst();
  events.addLast(new DiagnosticEvent(Instant.now(),type,durationMillis));
 }
 public synchronized List<DiagnosticEvent> snapshot(){return List.copyOf(events);}
 public synchronized DiagnosticEvent latest(){return events.peekLast();}
 public synchronized int eventCount(){return events.size();}
 public synchronized void clear(){events.clear();}
}
