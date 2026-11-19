# Animation Processing Review

## Status and scope

This is a source-level engineering review and a set of constraints for future animation work. It is not an animation optimization implementation, a measured performance result, or evidence that animation is currently a bottleneck.

Planet Essentials does not currently own a general-purpose animation engine. Minecraft, the active renderer, resource packs, and other mods remain authoritative for their respective entity, block-entity, model, and texture animations. Do not add global animation hooks or replace renderer behavior simply to create an optimization surface.

For this review, animation work includes:
- Entity and living-model pose animation.
- Block-entity and other world-object animation.
- Animated texture frame advancement and upload/resource handling.
- UI animation owned by Essentials itself.

These categories have different owners and costs. Measure and review them separately rather than treating all animation as one subsystem.

## Ownership and correctness boundaries

- Minecraft and the active renderer own vanilla entity/model animation evaluation, interpolation, render submission, and animated texture handling.
- Resource packs and mods own the definitions and intended appearance of their custom models, animations, and animated textures.
- Essentials may optimize only resources and UI behavior it explicitly owns, or use a documented optional integration that leaves the provider in control.
- Do not replace global model, entity-renderer, block-entity-renderer, or texture-animation managers as a broad optimization.
- Do not change animation speed, frame order, loop behavior, pose, interpolation, visibility, or timing without an explicit user-facing feature and correctness tests.
- Keep simulation/gameplay state separate from render interpolation. A visual optimization must not change entity state, hitboxes, collision, server-authoritative behavior, or timing of gameplay events.
- Preserve pause behavior, world-time semantics, partial-tick interpolation, and expected behavior when the game loses or regains focus.
- Any optional optimization must be client-only, independently disableable, fail softly, and leave vanilla behavior intact when absent or disabled.

## Investigation targets

Investigate only when repeatable profiling or reproducible reports identify animation-related cost:

1. **Pose evaluation:** determine whether model/pose calculations consume meaningful client CPU time in controlled entity-heavy scenes. Separate animation evaluation from general entity ticking and renderer submission.
2. **Interpolation:** inspect repeated interpolation or transform work, while preserving partial-tick smoothness and frame-rate-independent visual behavior. Do not move gameplay updates into render callbacks.
3. **Animated textures:** distinguish frame selection, image decoding, texture upload, and GPU sampling costs. Resource reload and upload stalls must be measured separately from steady-state rendering.
4. **Repeated lookups:** identify repeated immutable animation metadata or resource lookups that could safely be cached. Every cache must be bounded, have explicit ownership, and invalidate on resource reload.
5. **Allocation and garbage collection:** inspect allocation rate and GC pauses in controlled scenes. Avoid per-entity/per-frame temporary collections, formatted strings, streams, or object pooling without evidence.
6. **Visibility and distance:** document existing renderer behavior before considering any visibility or distance policy. Do not silently freeze, skip, or reduce animation updates for distant or off-screen objects; that can create visible snapping or mod incompatibility.
7. **UI animation:** keep Essentials-owned UI animation lightweight, respect reduced-motion/accessibility preferences where available, and avoid scheduling work when a screen is inactive.
8. **Lifecycle cleanup:** release Essentials-owned animation resources on resource reload, screen close, and client shutdown as appropriate.
9. **Diagnostics overhead:** investigation must be opt-in and low overhead. Do not record player activity, world identifiers, server addresses, or transmit telemetry.

These are investigation categories, not claims that any of these problems currently exist.

## Changes that are out of scope for an optimization pass

- Replacing Minecraft's animation, model, renderer, or texture-animation managers.
- Globally skipping entity or block-entity animation updates.
- Changing animation timing, speed, frame order, pose, interpolation, or loop semantics.
- Modifying third-party model or animation definitions.
- Moving gameplay-state updates into render callbacks or changing server-authoritative state.
- Adding permanent per-entity or per-frame hooks without measured need and compatibility review.
- Introducing unbounded caches or object pools to reduce allocations.

## Verification protocol for a future implementation

1. Establish a repeatable baseline using docs/performance/BASELINE_PROFILING.md. Record Minecraft version, Java runtime, operating system, renderer/mod list, resource packs, graphics settings, and relevant entity/texture animation settings.
2. Use separate repeatable scenarios for entity-heavy scenes, block-entity-heavy scenes, animated-texture-heavy scenes, and Essentials UI screens. Avoid mixing categories when identifying a cost.
3. Record frame-time distribution and variance, CPU thread activity, GPU frame/presentation data where available, memory use, allocation rate, GC pauses, and resource reload duration.
4. Separate animation evaluation, interpolation, texture upload, render submission, and GPU execution where tools permit. Do not infer a bottleneck from entity count or animation count alone.
5. Change one variable at a time and repeat each scenario under identical conditions.
6. Verify animation speed and loop behavior, partial-tick smoothness, pause/focus transitions, resource-pack overrides, modded models, renderer compatibility, and screen lifecycle.
7. Test with Essentials enabled and disabled, and with the proposed feature independently enabled and disabled.
8. Reject changes that cause snapping, timing drift, visual differences, stale resource handles, leaks, reload failures, compatibility regressions, increased frame-time variance, or no repeatable benefit.

## Acceptance criteria

An animation optimization may be implemented only when repeatable measurements identify a specific cost and the proposed change has a narrow ownership boundary. It must preserve animation semantics and gameplay state, respect resource-pack and mod ownership, use bounded resources with correct invalidation and cleanup, fail back to vanilla behavior, and demonstrate a measurable improvement without visual or compatibility regressions.

## Current conclusion

No animation bottleneck has been measured or established. No animation optimization is justified yet. This review documents investigation targets and compatibility boundaries only. The next performance-architecture item is safe client-side network work.
