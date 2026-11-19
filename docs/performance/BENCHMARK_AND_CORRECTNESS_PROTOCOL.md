# Benchmark and Correctness Verification Protocol

## Purpose and current status

This document defines the evidence required before a Planet Essentials optimization can be described as correct or faster. It complements the environment and capture procedure in [BASELINE_PROFILING.md](BASELINE_PROFILING.md).

This protocol is implemented as engineering guidance only. No optimization engine has yet produced a benchmark result, no performance gain is claimed, and no runtime benchmark has been executed by CI. The current diagnostics and configuration foundations are not evidence of a Minecraft performance improvement.

## 1. Required evidence for every optimization

Every optimization proposal must identify:

- **Target:** the exact client-side operation or resource cost being addressed.
- **Hypothesis:** why the change should reduce that cost.
- **Ownership:** which component owns the behavior and which API/lifecycle boundary is respected.
- **Correctness invariants:** observable behavior that must remain unchanged.
- **Baseline:** the unmodified or previous implementation measured under the same scenario.
- **Candidate:** the exact commit containing the proposed implementation.
- **Metrics:** the primary metric, guardrail metrics, and expected direction of change.
- **Reversal:** the setting or provider-selection path that returns control to the prior implementation, where applicable.

If the target and primary metric cannot be stated precisely, do not implement a speculative optimization.

## 2. A/B benchmark procedure

1. Build the baseline and candidate from clean checkouts with the same Java 21 runtime, Minecraft version, loader, Fabric API, mod list, resource packs, shader packs, world/save, graphics settings, and launch arguments.
2. Record both Planet Essentials and Planet Lib commit hashes.
3. Use the same machine, power mode, display mode, resolution, refresh rate, frame cap, and test route.
4. Allow a two-minute warm-up before capture. If shader or resource compilation occurs, finish the warm-up before starting the measured interval.
5. Run each scenario for at least five minutes, with at least five independent repetitions per variant for a performance claim. Alternate baseline/candidate order where practical to reduce thermal and background-load bias.
6. Restart the client between runs when the tested system has startup, resource reload, cache warm-up, or lifecycle effects. State explicitly when a restart is not appropriate.
7. Capture raw measurements for each run. Do not average all frames across runs and present them as independent repetitions.
8. Compare the median of the per-run results and report the range or interquartile range. Keep outliers visible and explain exclusions before analyzing results.
9. Repeat the comparison if environmental drift, background work, thermal throttling, or inconsistent workload makes the result inconclusive.
10. Retain the exact commands, capture-tool versions, settings, scenario description, and raw local capture references in the report.

The five-run minimum is a project acceptance rule, not a guarantee of statistical significance. If run-to-run spread is large relative to the observed difference, classify the result as inconclusive and collect more evidence.

## 3. Metrics and guardrails

Choose one primary metric before running the comparison. Track relevant guardrails so an apparent improvement does not conceal a regression.

| Area | Primary or supporting measurements | Guardrail |
| --- | --- | --- |
| Frame pacing | Frame-time median, p95, p99; 1% lows where defensibly calculated | Stutter count/duration; no worse tail latency |
| Client CPU | Client-thread and relevant worker-thread CPU time | No new main-thread stalls or busy-waiting |
| Tick work | Client tick duration distribution | No missed lifecycle work or changed game behavior |
| Memory | Allocation rate, retained heap, peak memory | No unbounded cache growth or retained resources |
| Garbage collection | Collection frequency and pause duration | No material increase in GC pressure |
| GPU/render | GPU frame time and presentation intervals where available | No visual corruption, flicker, or render regressions |
| Loading | Resource reload and startup duration | No stale resources, deadlocks, or failed reloads |
| Responsiveness | Input-to-visible-response measurements only where safely measurable | No changed input semantics or server-authoritative behavior |

Average FPS is supporting context, never the sole acceptance metric. GPU utilization, ping, heap occupancy, or a single best run must not be used alone as proof of a bottleneck or improvement.

## 4. Correctness gates

A candidate must pass all applicable gates before a performance result can be accepted.

### A. Automated verification
- Clean Gradle build and unit tests pass.
- Relevant integration/contract tests pass.
- Configuration defaults, migration, validation, persistence, and independent enable/disable behavior are tested.
- Failure and fallback paths are tested, including malformed resources/configuration and optional dependencies where applicable.
- No new warnings or errors are ignored without a documented reason.

### B. Runtime behavior
- Launches on the declared Minecraft, Fabric Loader, Fabric API, and Java versions.
- Enable, disable, reload, disconnect, reconnect, and shutdown lifecycle paths work as relevant.
- The feature remains independently reversible.
- No resource leaks, stale handles, stuck tasks, or unbounded queues are observed.
- Dedicated-server safety is checked for any code reachable from common initialization.

### C. Minecraft behavior and visual correctness
- Vanilla-visible behavior remains equivalent unless the feature explicitly documents an intended user-facing option.
- Resource-pack and shader interactions are checked when rendering or assets are affected.
- Screenshots or controlled visual comparisons are retained for visual changes.
- No gameplay authority, packet semantics, timing, or server validation is changed by a client optimization.
- No audio, particle, animation, or other presentation behavior is silently removed or altered.

### D. Compatibility
- Test with Essentials alone and with the relevant Planet mod integration.
- Test the feature both enabled and disabled.
- Test representative third-party mods that touch the same subsystem.
- Record untested combinations as unknown; do not label them compatible by inference.

## 5. Acceptance decision

Use one of these outcomes:

- **Accepted:** all applicable correctness gates pass, the primary metric improves consistently across repeated controlled runs, and no material guardrail regression appears.
- **Rejected:** correctness fails, the primary metric regresses, or a material guardrail regresses.
- **Inconclusive:** the result is within normal run-to-run variation, capture quality is insufficient, or the workload does not isolate the target.
- **Needs compatibility work:** core correctness passes but one or more relevant mod/runtime combinations fail or remain unresolved.

Do not merge a performance claim into release notes until the result is accepted. An optimization may still be retained as an experimental, disabled-by-default option only when its risks and limitations are explicit and correctness is not compromised.

## 6. Benchmark report template

Copy this template for each optimization and keep the report with its implementation or release evidence.

```text
Optimization:
Target subsystem:
Hypothesis:
Correctness invariants:
Baseline commit:
Candidate commit:
Planet Lib commit:
Minecraft / Loader / Fabric API / Java:
OS / CPU / GPU / RAM / driver:
Mods and versions:
Resource packs / shaders:
Graphics and launch settings:
Scenario and route:
Primary metric:
Guardrail metrics:
Capture tools and versions:

Baseline runs:
  Run 1:
  Run 2:
  Run 3:
  Run 4:
  Run 5:
  Median / spread:

Candidate runs:
  Run 1:
  Run 2:
  Run 3:
  Run 4:
  Run 5:
  Median / spread:

Correctness gates:
  Automated:
  Runtime lifecycle:
  Visual / behavioral:
  Compatibility:
  Resource cleanup:

Raw capture locations (local, reviewed for private data):
Known confounders:
Outcome: Accepted / Rejected / Inconclusive / Needs compatibility work
Conclusion:
Reviewer / date:
```

Keep raw JFR, frame-presentation, and GPU capture files local by default. Review them for usernames, local paths, world names, server addresses, and other private details before sharing. Never add raw recordings to diagnostics export or transmit them automatically.

## 7. Current project application

- The existing baseline protocol is the required environment-control procedure.
- CPU and GPU bottleneck investigations remain deferred until the user supplies the relevant runtime evidence; this protocol does not replace those measurements.
- The current diagnostics collectors and UI are not performance optimizations and must not be used to claim higher FPS.
- Texture and shader engines do not yet exist, so there are no valid texture/shader benchmark results to report.
- Future subsystem controls must be connected to real activation/deactivation behavior before they can be benchmarked as controls.
- The benchmark correctness/performance roadmap item remains in progress until at least one implemented optimization completes the applicable correctness gates and an actual controlled benchmark report is reviewed.

## Current conclusion

The benchmark and correctness acceptance process is now specified. No optimization has passed it yet, and no performance gain is claimed.
