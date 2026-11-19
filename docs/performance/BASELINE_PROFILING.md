# Planet Essentials Baseline Profiling Protocol

This protocol defines how to measure the unmodified Minecraft client before adding performance optimizations. It is a repeatable procedure, not a claim that any benchmark has already been run.

## 1. Record the environment

For every run, record:
- Minecraft version, Fabric Loader version, Fabric API version, Java runtime/vendor
- Planet Essentials commit and Planet Lib submodule commit
- Full mod list and versions
- CPU, GPU, RAM, storage, operating system, graphics driver
- Display resolution, refresh rate, fullscreen/windowed mode
- Graphics preset, render distance, simulation distance, entity distance, particles, VSync, frame cap
- Resource pack, shader pack, world identifier kept local to the test notes (never included in exported diagnostics)

Do not compare runs if these settings or the mod set changed. Record any unavoidable difference.

## 2. Control the test conditions

1. Use the same machine, power mode, display mode, game settings, world, player position, camera direction, and test route.
2. Close unrelated heavy applications and wait for background updates to finish.
3. Use a fixed world and repeatable scene. Keep the save unchanged between runs; restore a clean copy if the scenario modifies it.
4. Launch the client and allow a two-minute warm-up before capturing data.
5. Capture each scenario for five minutes. Repeat at least three times and compare medians, not the single best run.
6. Change one variable per comparison. Keep the same Fabric mods and settings when comparing Essentials diagnostics enabled versus disabled.
7. Note crashes, loading interruptions, shader compilation, resource reloads, and other anomalies. Do not silently discard an inconvenient run.

## 3. Required scenarios

| Scenario | Purpose | Procedure |
| --- | --- | --- |
| Main menu idle | Baseline idle cost | Remain at the main menu without opening menus |
| Static world | World/render baseline | Stand still in a fixed location and direction |
| Repeatable traversal | Streaming and chunk workload | Follow the same route at the same pace |
| Busy scene | Entity and particle workload | Use a fixed scene with repeatable entity/particle activity |
| Resource reload | Loading and allocation behavior | Trigger one resource reload, then measure recovery separately |

Do not use a multiplayer server as the only benchmark scene. Server tick rate and network conditions can hide or imitate client-side changes.

## 4. Metrics to capture

### Frame pacing
- Average FPS, but never use it alone
- Frame-time median, p95, and p99
- 1% low FPS where the capture tool provides a defensible calculation
- Stutter count and duration for clearly defined frame-time thresholds

### CPU and JVM
- Process CPU utilization and per-thread samples
- Client tick duration distribution
- Java Flight Recorder allocation and garbage-collection events
- Heap use before and after the capture, plus GC pause duration

### GPU and memory
- GPU utilization, clock, temperature, and dedicated memory
- System memory use
- GPU frame time when the capture tool exposes it reliably

Keep CPU frame time and GPU frame time separate. A high GPU utilization reading alone does not prove the GPU is the bottleneck.

## 5. Suggested tools

- Java Flight Recorder (JFR) with the Java 21 runtime for JVM CPU, allocation, and GC investigation
- PresentMon or an equivalent frame-presentation capture tool for frame-time distributions
- GPU vendor telemetry or an equivalent monitor for GPU utilization, clocks, temperature, and memory
- Minecraft/Fabric logs for startup, resource reload, and crash context

Use the same tool versions and capture settings for all compared runs. External profilers add overhead; record which tools were active and do not compare profiled and unprofiled captures as if they were identical.

## 6. Diagnostics overhead comparison

To evaluate Planet Essentials diagnostics:
1. Use the same client build, world, route, graphics settings, and capture tools.
2. Run one set with diagnostics disabled.
3. Run one set with diagnostics enabled but the Essentials settings screen closed.
4. Run one set with diagnostics enabled and the diagnostics screen open only if measuring UI cost.
5. Repeat each condition at least three times, alternating the order to reduce thermal or background-load bias.
6. Compare frame-time percentiles, client tick percentiles, CPU samples, allocation rate, and GC pauses.
7. Report the median per run and the spread. Treat differences smaller than normal run-to-run variation as inconclusive.

The disabled code path is designed to avoid sampling and collector work after its single configuration check. This protocol is required to establish its real runtime cost.

## 7. Optimization acceptance criteria

An optimization is not accepted solely because a metric improved once. It must:
- Preserve vanilla-visible behavior and game correctness
- Pass unit and integration tests relevant to the change
- Improve the target metric across repeated controlled runs
- Avoid meaningful regressions in other metrics or scenarios
- Include the exact environment, commits, settings, capture files, and analysis notes
- Be reversible through its setting or feature-priority control when applicable

If the measured change is within noise, label it inconclusive and do not advertise a performance gain.

## 8. Reporting template

For every comparison, retain a local report containing:
- Test date and machine/software environment
- Exact commit hashes and mod list
- Scenario and settings
- Raw capture file references stored locally
- Three or more run results and median/spread
- Frame-time, CPU, GPU, allocation, and GC observations
- Correctness/visual checks
- Known confounders and conclusion: improvement, regression, or inconclusive

Do not upload raw profiler recordings automatically. Share them only after reviewing them for private paths, usernames, world names, server addresses, or other identifying details.
