# Memory Allocation and Cache Strategy

## Status and intent

This document establishes implementation constraints for Planet Essentials before optimization engines begin owning large or resource-backed data. It is an architecture decision, not evidence of measured memory savings. No cache or allocation reduction is considered complete until it has correctness tests and repeatable before/after measurements.

## 1. Ownership first

Every retained object must have an explicit owner and end-of-life event.

- Configuration and profile data belong to their stores and are released with the runtime.
- Diagnostics histories belong to their bounded collectors. Their capacity is controlled by configuration and their contents remain in memory unless the user explicitly exports them.
- Texture, shader, model, and other resource-backed caches belong to the subsystem that created them, never to a global utility with unclear ownership.
- A subsystem must not retain objects owned by another mod unless the integration contract explicitly grants that lifetime.
- Avoid static references to Minecraft client, world, renderer, texture-manager, or resource objects beyond the lifecycle in which they are valid.

## 2. Bounded memory

Every cache must declare:
1. A maximum entry count or byte budget.
2. The key and value types it retains.
3. Whether values are immutable or safely published.
4. Its eviction policy and deterministic behavior.
5. Its invalidation events.
6. Its cleanup behavior on resource reload, disconnect, disable, and shutdown as applicable.
7. A metric or diagnostic that can reveal current size and evictions without collecting personal data.

An unbounded map, queue, history, or per-resource accumulation is not an acceptable cache. Prefer byte budgets for variable-sized decoded assets and entry limits only where item sizes are predictably similar. Capacity defaults must be conservative and configurable only when a meaningful user-facing reason exists.

## 3. Hot-path allocation rules

- Do not create temporary collections, streams, iterator pipelines, formatted strings, or diagnostic snapshots on every tick or frame without profiling evidence.
- Reuse immutable constants and stable lookup results where their lifetime is safe.
- Prefer primitive counters and timestamps for lightweight measurements.
- Do not introduce object pools as a default optimization. Pooling is permitted only after profiling proves a meaningful benefit, with strict capacity, reset semantics, thread ownership, and shutdown cleanup.
- Do not retain per-frame samples when diagnostics are disabled.
- Diagnostics-disabled execution should avoid sample/event creation and collector access. Any additional fast-path state must be safely updated at lifecycle boundaries.
- Snapshot creation is an explicit reporting/UI operation, not a hot-path operation.

The existing diagnostics collectors intentionally use bounded deques and immutable snapshots. Their timestamp/sample object creation occurs only on enabled collection paths. This design is bounded but still requires client runtime profiling before its overhead can be characterized.

## 4. Cache keys and correctness

- Keys must contain all inputs that affect the cached result. Never key a resource result only by a display name or a non-unique path fragment.
- Prefer immutable value keys. Do not use mutable Minecraft objects as hash keys.
- Cache values must not expose mutable internal state to callers.
- Cache hits must be behaviorally equivalent to recomputing the value.
- Failed decode, missing resource, invalid metadata, and unsupported formats must not poison a cache with a success-shaped placeholder.
- Cache invalidation must be deterministic and safe if a reload or shutdown occurs while work is pending.

## 5. Resource lifecycle

For any future cache containing resource-pack or renderer-backed data:

- Register invalidation with the appropriate resource reload lifecycle.
- Build replacement state separately, validate it, then publish it atomically; do not expose partially rebuilt caches.
- Release old owned resources after the replacement is safely published.
- Cancel or ignore stale asynchronous work from an earlier reload generation.
- On client shutdown, stop owned workers, clear retained references, and release GPU/native resources through the owning API.
- Never delete, rewrite, or take ownership of another mod's resource/configuration files.

## 6. Concurrency

- Keep ownership single-threaded where practical, especially for Minecraft client and renderer objects.
- If background decoding is introduced, workers may process immutable byte data and metadata, but renderer/texture registration must occur on the required client thread.
- Bound work queues as well as result caches. Define what happens when a queue is full; do not allow unbounded backlog.
- Publish completed immutable results safely. Avoid holding cache locks while decoding, performing I/O, invoking callbacks, or calling into another mod.
- Cancellation and generation checks must prevent stale results from being installed after resource reload or subsystem disable.

## 7. Measurement and acceptance

For each future allocation or cache change, record:

- Scenario, hardware/software environment, Minecraft version, and mod set.
- Warm-up and capture duration, repeated runs, and baseline.
- Heap allocation rate, GC count/pause time, retained heap, and cache size where applicable.
- Frame-time distribution and resource reload duration to detect regressions.
- Correctness checks across reload, resource-pack changes, missing resources, disable/enable, and shutdown.

Accept a change only when it improves a measured target without correctness, compatibility, retained-memory, or frame-time regressions. A lower allocation rate is not automatically a win if retained memory or reload cost grows substantially.

## 8. Initial implementation guardrails

This phase does not add a generic global cache, object pool, custom allocator, or background asset pipeline. Those would be premature before the texture and shader subsystems define their data and lifecycle requirements. The next concrete cache should be owned by the subsystem that needs it and designed against the rules above.

## Related documents

- [Baseline profiling protocol](BASELINE_PROFILING.md)
- [CPU bottleneck investigation](CPU_BOTTLENECK_INVESTIGATION.md)
- [GPU bottleneck investigation](GPU_BOTTLENECK_INVESTIGATION.md)
