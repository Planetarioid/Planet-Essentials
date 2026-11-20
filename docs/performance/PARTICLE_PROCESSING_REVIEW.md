# Particle Processing Review

## Status and scope

This is a source-level engineering review and a set of constraints for future particle work. It is not a particle optimization implementation, a measured performance result, or proof that particles are currently a bottleneck.

Planet Essentials currently has no dedicated particle-processing engine and no particle hot-path mixins. Do not add one merely to create an optimization surface. First establish whether particle simulation, submission, rendering, or resource handling causes a repeatable CPU, GPU, memory, or frame-pacing problem.

## Ownership and compatibility boundaries

- Minecraft and the active renderer own particle creation, simulation, lifetime, visibility, render submission, and resource usage.
- Essentials must not replace the particle manager or renderer, or take ownership of particles created by vanilla or other mods.
- Do not intercept every particle spawn or tick as a general optimization. A global hook can add work to the busiest path and alter mod compatibility.
- Do not silently drop, merge, delay, recolor, resize, or change the lifetime of particles. Particle effects are part of gameplay feedback and visual presentation.
- Do not change vanilla particle settings, user-selected particle density, transparency, lighting, or rendering order behind the user's back.
- Resource-pack and mod-provided particle definitions remain authoritative.
- Any future optional integration must be client-only, independently disableable, fail softly, and leave vanilla behavior intact when absent or disabled.
- Server-authoritative gameplay state and validation must never depend on a client-side particle optimization.

## Safe investigation targets

Investigate only when profiling or reproducible reports point to particle-related cost:

1. **Simulation cost:** determine whether particle ticking or per-particle state updates consume meaningful client CPU time in repeatable particle-heavy scenes.
2. **Render submission:** distinguish particle draw/submission cost from general world rendering and GPU execution. Do not infer a GPU bottleneck from particle count or utilization alone.
3. **Resource loading:** determine whether particle textures, definitions, or resource reload work cause measurable stalls. Keep expensive preparation out of tick and render hot paths where the platform permits safe asynchronous work.
4. **Repeated resource work:** check for repeated metadata or resource lookups that could safely reuse immutable data. Any cache must be bounded and invalidated on resource reload.
5. **Allocation and garbage collection:** inspect allocation rate and GC pauses during controlled particle-heavy scenes. Avoid per-particle temporary collections, formatted strings, streams, or object pooling without evidence.
6. **Visibility and distance behavior:** document vanilla renderer behavior and identify whether any proposed visibility change would alter expected visuals. Do not introduce custom culling or distance cutoffs without an explicit feature decision and visual compatibility tests.
7. **Lifecycle cleanup:** ensure any future Essentials-owned resources are released on resource reload and client shutdown.
8. **Diagnostics overhead:** particle investigation must be opt-in and low overhead. Do not record player activity, world identifiers, server addresses, or transmit telemetry.

These are investigation categories, not claims that any of these problems currently exist.

## Changes that are out of scope for an optimization pass

- Replacing or modifying Minecraft's particle manager or active renderer.
- Globally suppressing particle spawn events or reducing particle density without user control.
- Changing particle lifetime, motion, size, color, lighting, transparency, or render order.
- Changing gameplay feedback effects to improve benchmark numbers.
- Rewriting third-party particle definitions or resource-pack assets.
- Adding a permanent per-particle event hook without measured need and compatibility review.
- Using unbounded caches or object pools to avoid allocations.

## Verification protocol for a future implementation

1. Establish a repeatable baseline using the project baseline profiling protocol. Record Minecraft version, Java runtime, operating system, renderer/mod list, resource packs, graphics settings, and particle setting.
2. Compare a quiet scene, ordinary gameplay, and a repeatable particle-heavy scene. Use the same camera position, view distance, graphics settings, and activity for each run.
3. Record frame-time distribution and variance, CPU thread activity, GPU frame/presentation data where available, memory use, allocation rate, GC pauses, and resource reload duration.
4. Separate simulation cost, render submission cost, and GPU execution cost where the available tools permit. Do not treat particle count or GPU utilization as proof of a bottleneck.
5. Change one variable at a time and repeat each scenario. Compare identical conditions before and after.
6. Verify vanilla particle settings, critical gameplay indicators, resource-pack particle overrides, mod-provided particles, transparency, lighting, and renderer compatibility.
7. Test with Essentials enabled and disabled, and with the proposed particle feature independently enabled and disabled.
8. Reject a change if it removes expected visual feedback, creates rendering artifacts, increases frame-time variance, leaks resources, breaks resource reload, or lacks a repeatable benefit.

## Acceptance criteria

A particle optimization may be implemented only when repeatable measurements identify a specific cost and the proposed change has a narrow ownership boundary. It must preserve expected vanilla and modded visuals, respect user settings, have bounded resource use, clean up correctly, fail back to vanilla behavior, and demonstrate a measurable improvement without visual or compatibility regressions.

## Current conclusion

No particle bottleneck has been measured or established. No particle optimization is justified yet. The review establishes investigation and compatibility boundaries only. The next performance-architecture review is animation processing.
