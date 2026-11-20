# GPU / Render Bottleneck Investigation

Status: investigation protocol prepared; awaiting a real client capture. This document identifies what to measure and how to distinguish GPU-bound rendering from CPU, frame-pacing, thermal, and synchronization limits. It does not claim that a GPU bottleneck has been measured.

## Why a capture is required

A high GPU-utilization percentage by itself does not prove that the GPU is limiting frame delivery. Conversely, low utilization does not prove that rendering is inexpensive: the GPU may be waiting on CPU command submission, presentation, synchronization, or a frame cap. Measurements must be collected in a repeatable scene and interpreted alongside frame times, clocks, temperatures, and CPU activity.

Do not change render code, shader behavior, texture handling, or frame scheduling based only on a single utilization reading.

## First-pass measurement targets

These are hypotheses to investigate, not confirmed problems:

1. **GPU saturation**
   - GPU busy time and utilization during a repeatable scene.
   - GPU clock, power state, and temperature to identify throttling or power limits.
   - Dedicated GPU memory use and shared-memory pressure.
2. **Frame pacing and presentation**
   - Frame-time median, p95, p99, stutter count, and presentation intervals.
   - Compare uncapped and capped runs only as separate scenarios; record VSync and frame-cap settings.
   - Separate GPU execution time from time waiting for presentation or a frame limiter.
3. **CPU submission versus GPU execution**
   - Compare GPU busy time with total frame time and CPU utilization.
   - If the GPU is frequently underutilized while frame times are poor, investigate CPU-side submission, synchronization, or external limits rather than assuming a GPU workload problem.
4. **Render workload sensitivity**
   - Change one setting at a time: render distance, graphics quality, particles, resolution, or shader state.
   - A repeatable frame-time response to a controlled change can help isolate the workload class.
5. **Memory and resource transitions**
   - Observe dedicated/shared GPU memory during normal play and separately during resource reloads or shader changes.
   - Note spikes, stutters, driver resets, and visual corruption. Do not conflate loading spikes with steady-state rendering.

## Capture procedure (Windows)

Use the same Minecraft profile, mod set, world, player position, camera direction, and graphics settings as the baseline protocol.

1. Record the environment listed in `docs/performance/BASELINE_PROFILING.md`, including GPU model, driver version, display resolution/refresh rate, render distance, VSync, frame cap, and whether shaders/resource packs are active.
2. Open Windows Task Manager (Ctrl+Shift+Esc), select Performance, and observe the GPU graphs while Minecraft is running. If available, select the 3D engine graph rather than relying on the aggregate number alone. Record dedicated and shared GPU memory.
3. Use PresentMon or an equivalent frame-presentation capture tool to capture frame-time/presentation data for a five-minute repeatable scenario. Keep the tool version and capture settings identical between comparison runs.
4. If your GPU vendor provides a performance overlay, record GPU utilization, clock, temperature, power, and memory. Do not enable multiple overlapping overlays if they materially alter performance.
5. Warm up the client for two minutes, then capture the scenario for five minutes. Repeat at least three times and compare medians and spread.
6. Keep diagnostics state, Minecraft settings, and background applications consistent. Change only one workload variable per comparison.
7. Record anomalies such as shader compilation, resource reloads, chunk generation, alt-tabbing, thermal throttling, or background downloads.

If a capture tool is unavailable, Task Manager observations and screenshots can provide preliminary context, but they are not equivalent to frame-time traces or GPU execution-time measurements. Label the evidence accordingly.

## Required evidence

Provide whichever of the following you can safely share:

- PresentMon capture or exported frame-time summary
- Screenshots of Task Manager GPU 3D, dedicated memory, and shared memory graphs during the test
- GPU overlay readings for utilization, clock, temperature, power, and memory
- Minecraft version, Java version, Planet Essentials and Planet Lib commit hashes, mod list, graphics settings, resolution, frame cap, VSync, and test scenario
- Whether the capture used shaders or a resource pack

Review files and screenshots for usernames, local paths, world names, server addresses, and other private information before sharing. Do not upload captures automatically.

## Interpretation rules

- High GPU utilization is only a clue. Correlate it with GPU busy/frame time and frame-presentation time.
- A frame cap or VSync can intentionally leave GPU headroom; that is not an optimization defect.
- Low GPU utilization with poor frame times may indicate CPU submission, synchronization, a limiter, or a capture interpretation issue.
- High memory use alone is not proof of harmful pressure. Look for shared-memory growth, eviction/stutter evidence, or repeatable failures.
- Temperature and clock drops must be interpreted with power limits, cooling, and workload consistency.
- Compare at least three runs and report median and spread. If results overlap normal run-to-run variation, mark them inconclusive.

## Decision record

For each confirmed candidate, record the metric, capture source, scene, GPU/driver, settings, repeatability, and plausible cause. Propose one correctness-preserving experiment that changes only one variable. Verify visual correctness and compare frame-time distributions before and after.

If evidence does not identify a stable GPU/render bottleneck, record that result and do not invent an optimization. CPU and GPU investigations are separate: a GPU capture does not complete the deferred CPU investigation.
