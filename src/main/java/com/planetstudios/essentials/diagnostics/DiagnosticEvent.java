package com.planetstudios.essentials.diagnostics;
import java.time.Instant;
import java.util.Objects;
/** One local diagnostic event. No player, world, server, or account identifiers are stored. */
public record DiagnosticEvent(Instant timestamp, DiagnosticEventType type, long durationMillis) {
 public DiagnosticEvent {
  Objects.requireNonNull(timestamp,"timestamp");Objects.requireNonNull(type,"type");
  if(durationMillis<0)throw new IllegalArgumentException("durationMillis cannot be negative");
 }
}
