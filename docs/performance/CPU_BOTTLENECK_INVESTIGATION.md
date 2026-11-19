# CPU Bottleneck Investigation

Status: awaiting a real Minecraft client recording. This document defines the first investigation pass and records source-level areas to inspect; it does not claim that any bottleneck has been measured.

## Why a recording is required

Source inspection can identify work that deserves attention, but cannot establish whether it is a meaningful CPU bottleneck. JVM compilation, thread scheduling, driver behavior, other mods, world content, and hardware all affect the result. Do not optimize a subsystem based only on a suspicious-looking method.

## First-pass investigation targets

These are measurement targets, not confirmed problems:

1. **Client tick diagnostics path**
   - PlanetEssentialsClient reads the diagnostics-enabled configuration once at the start of each client tick.
   - With diagnostics disabled, it should skip clock reads, FPS sampling, event creation, and collector operations.
   - With diagnostics enabled, inspect the cost of FPS sampling and slow-tick event recording.
2. **Configuration and profile persistence**
   - Persistence is synchronized and uses file I/O, so verify that saves/loads remain on user-driven lifecycle paths and do not occur on the render or client tick thread.
3. **Settings and feature-priority screens**
   - Inspect screen initialization and refresh paths for repeated registry snapshots, avoidable allocations, or work performed every frame.
4. **Mod discovery and integration**
   - Discovery should be lifecycle-driven, not repeated every tick or frame. Confirm this in a recording and call-path review.

No changes should be made to these paths until evidence identifies a measurable cost.

## Capture a five-minute Java Flight Recording

Use the Java 21 runtime configured by the Minecraft launcher. In the launcher's JVM arguments, temporarily add:

```text
-XX:StartFlightRecording=filename=planet-essentials-cpu.jfr,settings=profile,duration=5m,dumponexit=true
```

Launch the same Minecraft profile and mod set used for the baseline protocol. Join a repeatable test world, wait for the warm-up period, and perform the same scenario for the capture duration. Let the game exit normally so the recording is finalized. Remove the JVM argument after capturing.

If the recording does not start, check the launcher log for JVM argument parsing errors. Do not add multiple recording options or change heap settings between comparison runs.

## What to inspect in JDK Mission Control

Open the .jfr recording in JDK Mission Control and inspect:
- Hot Methods and Method Profiling
- Execution Samples grouped by thread
- Java Application and Minecraft client thread activity
- Allocation in TLAB and Outside TLAB
- Garbage Collection and pause durations
- Lock Instances / Java Monitor Blocked events, where available

Focus first on the Minecraft client thread and distinguish CPU execution from waiting, sleeping, or blocked time. A frequently sampled method is not automatically a performance problem: determine its inclusive/exclusive cost, call frequency, and whether it runs in the measured scenario.

## Evidence required before selecting an optimization

Provide:
- The .jfr recording, or screenshots/exported summaries of the relevant JMC views
- Minecraft/Java versions and the Essentials + Planet Lib commit hashes
- Mod list and test scenario
- Whether diagnostics were enabled, with the settings screen closed
- At least one repeatable capture and preferably three runs for a suspected regression or improvement

Before sharing a recording, review it for local file paths, usernames, world names, server addresses, and other private details. A recording may contain more environment information than a diagnostics JSON export.

## Decision record

For each confirmed candidate, record the hot method/thread, sample percentage or measured duration, scenario, repeatability, suspected cause, and a correctness-preserving experiment. If no stable hotspot appears, record that result and move on without inventing an optimization.
