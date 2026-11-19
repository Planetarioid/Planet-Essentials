# Planet Essentials Architecture

## Role

Planet Essentials is the central client-facing control and optimization mod in the Planet Studios ecosystem. It owns its own settings UI and optimization engines. Planet Lib remains the shared API and metadata layer.

## Dependency direction

- Planet Essentials depends on Planet Lib.
- Planet Lib must not depend on Planet Essentials.
- Other Planet mods may integrate with Essentials through the optional API bridge in Planet Lib.
- Other Planet mods must retain standalone defaults when Essentials is absent.

## Mod discovery

- Discovery uses Planet Lib's immutable snapshot of mods loaded by Fabric Loader.
- Planet Essentials filters Planet Studios mods using the library's explicit identity metadata.
- Declared capabilities are read from the shared capability registry; unknown capabilities remain unknown.
- If Planet Lib is absent, discovery returns an empty standalone snapshot and does not load Planet Lib API classes.
- No mods-directory scanning, archive inspection, or arbitrary third-party configuration access is permitted.

## Planet Lib integration

- Planet Lib is pinned as a Git submodule and built as a sibling Gradle project.
- Essentials compiles against Planet Lib's named-elements API but does not package it or require it at runtime; Planet Lib remains optional at runtime.
- Essentials exposes the typed planet_essentials_api Fabric entrypoint at API version 1.
- Integration descriptors are immutable, validated, deduplicated by mod ID, and exposed as read-only snapshots.
- Missing Planet Lib leaves Essentials in standalone mode. Planet Lib owns API discovery and version negotiation, and must treat unavailable or incompatible Essentials APIs as soft failures.

## Module boundaries

Planned internal areas:

- config: settings models, persistence, profiles, migrations
- integration: Planet Lib bridge and per-mod integration lifecycle
- features: feature/provider coordination and preference presentation
- diagnostics: bounded FPS samples, local lifecycle/slow-tick events, explicit local JSON export, and user-visible reporting
- performance: measured optimization systems and benchmark support
- texture: dedicated texture optimization engine
- shader: shader lifecycle, compilation, capability checks, fallback
- client: client-only screens and lifecycle integration

These are architectural boundaries, not a commitment to create empty packages before they are needed.

## Lifecycle rules

- Common initialization must remain safe on a dedicated server.
- Client-only classes must be isolated behind Fabric client entrypoints or client-only lifecycle registration.
- No expensive scanning, shader compilation, texture decoding, or profiling should occur on every tick.
- Diagnostics events are bounded in-memory observations only. Do not collect player names, world identifiers, server addresses, account data, or transmit telemetry.
- Diagnostics export is initiated only by an explicit user action and contains only the bounded FPS/event snapshots.
- Resource reload and shutdown must release owned resources.
- Optional integrations must fail softly and expose a diagnostic reason.

## Performance rules

- Profile before optimizing.
- Avoid per-frame allocations and repeated lookups in hot paths.
- Cache only when invalidation and resource ownership are explicit.
- Do not trade correctness, server validation, or compatibility for responsiveness.
- Every optimization needs a correctness check and a before/after benchmark.
- Baseline measurements must follow docs/performance/BASELINE_PROFILING.md; methodology completion does not imply any measured baseline or verified performance gain.
- CPU candidates are only hypotheses until confirmed by a Java Flight Recording using docs/performance/CPU_BOTTLENECK_INVESTIGATION.md. The CPU investigation is deferred; do not implement speculative hot-path changes.
- GPU/render candidates are only hypotheses until confirmed by repeatable frame-presentation and GPU telemetry using docs/performance/GPU_BOTTLENECK_INVESTIGATION.md. The GPU investigation is deferred pending client telemetry; do not infer a GPU bottleneck from utilization alone or implement speculative render changes.
- Memory ownership, cache bounds, invalidation, and cleanup follow docs/performance/MEMORY_ALLOCATION_AND_CACHE_STRATEGY.md. The document defines engineering constraints, not measured allocation reductions or an implementation of a texture cache.
- Audio ownership and investigation constraints follow docs/performance/AUDIO_PROCESSING_REVIEW.md. Minecraft and its audio backend retain playback/device ownership; do not change sound semantics, suppress sounds, or add per-sound hot-path hooks without measured need and compatibility validation. The review is documentation, not an audio optimization implementation.
- Particle ownership and investigation constraints follow docs/performance/PARTICLE_PROCESSING_REVIEW.md. Minecraft and the active renderer retain particle lifecycle and rendering ownership; do not suppress, alter, or globally intercept particles without measured need and visual/compatibility validation. The review is documentation, not a particle optimization implementation.
- Animation ownership and investigation constraints follow docs/performance/ANIMATION_PROCESSING_REVIEW.md. Minecraft, renderers, resource packs, and mods retain their animation ownership; do not globally alter animation timing, interpolation, or update behavior without measured need and visual/compatibility validation. The review is documentation, not an animation optimization implementation.
- Avoid object pooling unless profiling demonstrates a material benefit and the pool has a strict capacity and lifecycle. Pools can retain memory and add synchronization or reset costs.
- Hot-path code must not allocate temporary collections, streams, lambdas, formatted strings, or snapshots per tick/frame without evidence that the cost is acceptable.
- Any cache of resource-backed objects must be invalidated on resource reload and released on client shutdown; stale resource handles must never be reused.

## Configuration rules

- Planet Essentials owns its own configuration.
- Other Planet mods own their settings unless they explicitly register an integration.
- Never silently rewrite another mod's configuration files.
- Feature priority overrides are per capability, persistent, and reversible.
- Runtime provider changes occur only through the provider's explicit Planet Lib lifecycle contract. Essentials never disables a mod or takes ownership of provider resources.
- Missing or invalid settings must fall back to documented defaults.

## Verification levels

Track separately:
1. Source implementation
2. Unit-test verification
3. CI build verification
4. Client runtime verification
5. Dedicated-server verification
6. Compatibility verification
7. Benchmark verification
