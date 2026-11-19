# Network Processing Review

## Status and scope

This is a source-level engineering review and a set of constraints for future network-related work. It is not a networking optimization implementation, a measured performance result, or evidence that the network is currently a bottleneck.

Planet Essentials currently observes client connection lifecycle events for local diagnostics. It does not own Minecraft's connection, packet protocol, packet scheduling, or server networking. Future work in this area is limited to safe client-side observation, presentation, and local resource handling. Do not add packet hooks or networking transformations merely to create an optimization surface.

## Ownership and correctness boundaries

- Minecraft and the active networking stack own connection establishment, packet encoding/decoding, dispatch, ordering, acknowledgement, keepalive, timeout, compression, encryption, and disconnect behavior.
- The server remains authoritative for gameplay state, validation, inventory, movement, combat, and world changes.
- Essentials must not cancel, rewrite, delay, duplicate, reorder, synthesize, or suppress gameplay packets.
- Do not change keepalive behavior, timeout thresholds, retry behavior, connection protocols, compression settings, or encryption.
- Do not introduce client-side prediction or reconciliation that changes gameplay semantics.
- Do not attempt to hide latency, manufacture lower ping readings, or present estimates as authoritative measurements.
- Do not inspect packet payloads or collect chat, player identifiers, server addresses, account data, or world identifiers for diagnostics.
- Any future network-related feature must be client-only, independently disableable, opt-in where it collects diagnostics, fail softly, and leave vanilla networking unchanged when absent or disabled.
- Optional integrations must use documented APIs and must not require a server-side mod or configuration unless the feature explicitly states that requirement.

## Safe client-side investigation and improvement areas

Investigate only when repeatable measurements or reproducible reports identify a specific client-side cost:

1. **Connection lifecycle visibility:** use existing client lifecycle callbacks to report connected/disconnected state and local transition timing. Do not infer server health or connection quality from a lifecycle event alone.
2. **Existing latency presentation:** where Minecraft already exposes a ping measurement, Essentials may present that existing value with its source and age clearly identified. Do not send extra probes, change ping collection, or label stale values as live.
3. **Client packet-processing cost:** if profiling points to client-side packet decode/dispatch cost, inspect the relevant client thread with profiling tools first. Do not add a global per-packet callback as a speculative optimization.
4. **Main-thread stalls:** distinguish network waiting from client-thread work, resource loading, rendering, and garbage collection. A frozen frame is not proof of a network bottleneck.
5. **Diagnostics overhead:** keep observation bounded and opt-in. The diagnostics-disabled path should not add packet listeners, per-packet allocations, clock reads, or extra synchronization.
6. **UI responsiveness:** keep network status presentation non-blocking. Never wait synchronously for network activity while drawing a screen or handling input.
7. **Local resource handling:** any Essentials-owned queues or snapshots must be bounded, have clear ownership, and be released on disconnect and client shutdown. Do not queue or replay gameplay packets.
8. **Failure reporting:** surface actionable local errors without exposing private server details. Avoid repeated log spam during reconnect or unstable connections.

These are investigation categories, not claims that any of these problems currently exist.

## Changes that are out of scope

- Modifying, cancelling, delaying, duplicating, reordering, or synthesizing gameplay packets.
- Altering keepalive, timeout, retry, compression, encryption, protocol negotiation, or connection behavior.
- Changing client movement, combat, inventory, block interaction, or other gameplay authority.
- Adding artificial lag, latency masking, ping manipulation, or packet-rate manipulation.
- Sending extra probes or telemetry to servers.
- Inspecting or exporting chat content, packet payloads, player identities, server addresses, account information, or world identifiers.
- Blocking the render or input thread while waiting for network work.
- Adding permanent global packet hooks without measured need and a compatibility review.

## Verification protocol for a future implementation

1. Establish a repeatable baseline using docs/performance/BASELINE_PROFILING.md. Record Minecraft version, Java runtime, operating system, loader, networking-related mods, server type, and connection conditions without recording the server address.
2. Compare a local/integrated world and a remote test server where practical. Keep gameplay activity and client settings consistent.
3. Record client frame-time distribution, client-thread activity, memory use, allocation rate, and GC pauses. Use existing network measurements only when their source and age are known.
4. Separate network waiting, client packet processing, server response time, rendering, and unrelated client-thread work. Do not treat ping alone as a measure of overall responsiveness.
5. Change one client-side variable at a time and repeat each scenario under equivalent conditions.
6. Verify connect/disconnect, reconnect, dimension changes, server transfers where supported, temporary connection loss, and clean client shutdown.
7. Test with Essentials enabled and disabled, with the proposed feature independently enabled and disabled, and alongside representative networking and gameplay mods.
8. Confirm that packet contents and ordering, server validation, keepalive behavior, timeouts, and gameplay outcomes remain unchanged.
9. Reject changes that alter protocol behavior, block the client thread, expose private connection details, increase frame-time variance, leak resources across disconnects, or show no repeatable benefit.

## Acceptance criteria

A network-related improvement may be implemented only when measurements identify a specific client-side cost and the proposed change does not modify protocol or gameplay behavior. It must remain client-only, preserve server authority and vanilla connection semantics, use bounded resources, clean up on disconnect and shutdown, fail softly, and demonstrate a repeatable improvement without compatibility regressions.

## Current conclusion

No network bottleneck has been measured or established. No packet-processing optimization is justified yet. This review documents safe client-side investigation boundaries only. The next performance-architecture item is per-system enable/disable controls.
