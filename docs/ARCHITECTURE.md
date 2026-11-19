# Planet Essentials Architecture

## Role
Planet Essentials is the central client-facing control and optimization mod in the Planet Studios ecosystem. It owns its settings UI and optimization engines. Planet Lib remains the shared API and metadata layer.

## Dependency direction
- Planet Essentials depends on Planet Lib.
- Planet Lib must not depend on Planet Essentials.
- Other Planet mods may integrate with Essentials through the optional API bridge in Planet Lib.
- Other Planet mods must retain standalone defaults when Essentials is absent.

## Mod discovery
- Discovery uses Planet Lib's immutable snapshot of mods loaded by Fabric Loader.
- Essentials filters Planet Studios mods using explicit identity metadata.
- Declared capabilities are read from the shared capability registry; unknown capabilities remain unknown.
- If Planet Lib is absent, discovery returns an empty standalone snapshot and does not load Planet Lib API classes.
- No mods-directory scanning, archive inspection, or arbitrary third-party configuration access is permitted.

## Planet Lib integration
- Planet Lib is pinned as a Git submodule and built as a sibling Gradle project.
- Essentials compiles against Planet Lib's named-elements API but does not package it or require it at runtime.
- Essentials exposes the typed planet_essentials_api Fabric entrypoint at API version 1.
- Integration descriptors are immutable, validated, deduplicated, and exposed as read-only snapshots.
- Missing Planet Lib leaves Essentials in standalone mode; bridge failures are soft.

## System controls
- The Diagnostics master switch gates all Essentials-owned diagnostics collection.
- FPS monitoring and lifecycle/slow-tick event collection have independent persisted switches.
- A disabled subsystem must stop creating new samples/events; existing bounded history is retained until explicitly cleared or evicted.
- Settings are registry-backed, validated, profile-persisted, and changed through the Essentials UI.
- Future optimization engines register real settings only when implemented and wired to actual activation/deactivation. No placeholder toggles.
- A system toggle must not silently disable a whole mod or override a provider selected through Planet Lib. Feature-priority selection remains separate and per-capability.

## Lifecycle rules
- Common initialization remains safe on a dedicated server.
- Client-only classes are isolated behind Fabric client entrypoints or client-only lifecycle registration.
- No expensive scanning, shader compilation, texture decoding, or profiling should occur on every tick.
- Diagnostics are bounded in-memory observations only. No player names, world identifiers, server addresses, account data, or telemetry transmission.
- Diagnostics export is initiated only by explicit user action and contains bounded FPS/event snapshots.
- Resource reload and shutdown release owned resources.
- Optional integrations fail softly and expose diagnostic reasons.

## Performance and correctness rules
- Profile before optimizing; every optimization needs correctness checks and before/after benchmarks.
- Baseline environment control and capture steps are in docs/performance/BASELINE_PROFILING.md.
- Benchmark and correctness acceptance gates are in docs/performance/BENCHMARK_AND_CORRECTNESS_PROTOCOL.md. They require repeated controlled A/B runs, a predeclared primary metric, guardrail metrics, automated/runtime/visual/compatibility checks, and an explicit Accepted/Rejected/Inconclusive outcome.
- Compatibility combinations and result vocabulary are in docs/COMPATIBILITY_TEST_MATRIX.md. A matrix row remains unknown until exact versions and runtime scenarios have been tested.
- A protocol document is not a benchmark result or compatibility pass.
- CPU and GPU investigations remain deferred until real recordings/telemetry are available.
- Memory/cache constraints are in docs/performance/MEMORY_ALLOCATION_AND_CACHE_STRATEGY.md.
- Audio, particle, animation, and network boundaries are documented in their respective review files; those reviews do not claim optimizations.
- Network behavior remains owned by Minecraft; Essentials must not modify packets, keepalives, timeouts, or server-authoritative behavior.
- Avoid per-frame allocations and premature object pooling.
- Resource-backed caches must be invalidated on reload and released on shutdown.

## Configuration rules
- Planet Essentials owns its own configuration; other Planet mods own their settings unless they explicitly register an integration.
- Never silently rewrite third-party configuration files.
- Feature-priority overrides are per capability, persistent, and reversible.
- Runtime provider changes occur only through Planet Lib's provider-owned lifecycle contract.
- Missing or invalid settings fall back to documented defaults.

## Verification levels
Track separately: source implementation, unit tests, CI build verification, client runtime verification, dedicated-server verification, compatibility verification, and benchmark verification.
