# Planet Essentials Development Roadmap

Statuses:
- [ ] Not started
- [~] In progress
- [x] Implemented and verified at the stated level

A feature is not marked complete merely because its class exists. Build verification, tests, and runtime validation are tracked separately.

## 0. Project foundation
- [x] Repository and default branch
- [x] Fabric Loom project structure
- [x] Minecraft 1.21.11 and Java 21 targets
- [x] Fabric metadata and common entrypoint
- [x] CI build/test baseline
- [x] Reproducible Planet Lib development dependency (pinned Git submodule + Gradle multi-project build)
- [ ] Client launch configuration
- [ ] Dedicated-server launch configuration

## 1. Planet Lib integration
- [x] Resolve Planet Lib as a real build dependency (pinned source project dependency; compile-only because runtime integration is optional)
- [x] Validate API contract version at startup (Planet Lib bridge negotiates API v1)
- [x] Expose the typed Planet Essentials API entrypoint and accept validated integration metadata
- [x] Handle missing/incompatible bridge without crashing (standalone Essentials mode; bridge failure is soft)
- [x] Add integration tests against the pinned Planet Lib source project

## 2. Core configuration and settings
- [x] Typed setting definitions with stable IDs, categories, defaults, and validation
- [x] Central settings registry and category lookup
- [x] Configuration schema migration path (0/1/2 to 3)
- [x] Atomic persistence with malformed-file preservation
- [x] Named profile create/save/load/delete storage
- [x] Profile import/export validation and safe profile names
- [x] Tests for registry, migration, profile persistence, and malformed imports
- [x] Thread-safety and lifecycle tests (serialized config/profile persistence; concurrent diagnostics and profile-store tests)

## 3. Mod discovery and integration coordination
- [x] Read loaded mods from Planet Lib registry
- [x] Identify installed Planet Studios mods
- [x] Integration registration lifecycle
- [x] Optional-mod capability discovery
- [x] Graceful behavior for absent integrations
- [x] Avoid scanning or modifying arbitrary third-party files (uses Fabric Loader and Planet Lib registries only)

## 4. Feature priority management
- [x] Read feature/provider metadata from Planet Lib (shared FeaturePriorityManager snapshot and FeatureRegistry descriptors)
- [x] Present competing implementations per capability
- [x] Display selected provider and automatic/manual selection reason
- [x] User override controls (cycle registered implementations per capability)
- [x] Reset to automatic selection
- [x] Persist preferences safely (schema 3, validated per-capability overrides; stale entries retained)
- [x] Apply changes only through provider lifecycle contracts (provider-owned onSelected/onDeselected callbacks with rollback on transition failure)
- [x] Ensure choices are scoped to one capability; no whole-mod enable/disable
- [x] Deterministic resolution and reversible override behavior covered by Planet Lib tests

## 5. Diagnostics
- [~] Bounded in-memory FPS history
- [~] Diagnostics toggle and configurable history capacity
- [x] Unit tests for bounded history and configuration validation
- [x] Diagnostics event collection integration (local lifecycle and slow-tick events; bounded in-memory collector)
- [x] Export diagnostics only on explicit user action (manual button; local JSON file; bounded snapshot; no background export)
- [x] No background telemetry or personal-data collection (export contains only timestamps, FPS values, event types, and durations)
- [~] Verify low overhead when diagnostics are not open (disabled tick path avoids clock reads, sampling, event creation, and collector locking after one cached config read; runtime profiling still required)

## 6. Performance architecture
- [x] Establish baseline profiling methodology (repeatable scenarios, environment controls, frame-time/CPU/GPU/JVM metrics, diagnostics A/B procedure, acceptance criteria documented in docs/performance/BASELINE_PROFILING.md)
- [~] Identify measurable CPU bottlenecks (deferred by user; source-level targets documented in docs/performance/CPU_BOTTLENECK_INVESTIGATION.md; still requires a real Java Flight Recording before identifying or changing any bottleneck)
- [~] Identify measurable GPU/render bottlenecks (capture protocol documented in docs/performance/GPU_BOTTLENECK_INVESTIGATION.md; deferred pending client telemetry; no bottleneck is claimed or optimized)
- [x] Memory allocation and cache strategy documented (bounded ownership, invalidation, reload/shutdown cleanup, allocation-aware hot-path rules in docs/performance/MEMORY_ALLOCATION_AND_CACHE_STRATEGY.md; runtime profiling and implementation review remain separate)
- [x] Audio processing review (source-level boundaries and verification protocol documented in docs/performance/AUDIO_PROCESSING_REVIEW.md; no bottleneck measured and no audio optimization implemented)
- [ ] Particle processing review
- [ ] Animation processing review
- [ ] Network-related work limited to safe client-side improvements
- [ ] Per-system enable/disable controls
- [ ] Benchmark correctness and performance impact
- [ ] Compatibility test matrix

## 7. Texture optimization engine
- [ ] Asset discovery and lifecycle design
- [ ] Texture metadata and format validation
- [ ] Safe decode/processing pipeline
- [ ] Memory-aware caching
- [ ] Resource reload integration
- [ ] Mipmap and filtering policy
- [ ] Resource-pack compatibility
- [ ] Failure fallback to vanilla resource behavior
- [ ] Benchmark memory, load time, and frame-time impact
- [ ] Verify no visual corruption or resource leaks

## 8. Shader system
- [ ] Shader architecture and supported pipeline boundaries
- [ ] Shader resource lifecycle
- [ ] Shader configuration model
- [ ] Shader compilation and error reporting
- [ ] GPU capability detection
- [ ] Graceful fallback paths
- [ ] Resource-pack and other-renderer compatibility review
- [ ] Visual correctness tests
- [ ] GPU performance benchmarks

## 9. User interface
- [~] Replace vanilla Options entry point with Planet Essentials hub
- [x] Main navigation for diagnostics and profiles
- [x] Registry-driven diagnostics settings page
- [x] Profile management screen for create/save/load/delete
- [x] Vanilla Minecraft settings remain accessible
- [ ] Search/filter settings
- [ ] Per-mod settings panels
- [~] Feature-priority controls (initial provider selector screen; runtime lifecycle application and usability review remain)
- [~] Dedicated diagnostics view
- [ ] Reset and confirmation flows
- [ ] Accessibility, scaling, and localization review

## 10. Compatibility and release
- [ ] Clean client startup test
- [ ] Dedicated-server startup test
- [ ] No-Planet-mod standalone test
- [ ] Each available Planet mod integration test
- [ ] Third-party mod coexistence test matrix
- [ ] Performance regression suite
- [ ] License decision
- [ ] Release checklist and changelog
- [ ] Distribution decision
- [ ] Version tag and release artifact

## Verification record

CI verifies compilation, unit tests, JAR contents, and artifact creation. In-game behavior and runtime profiling remain unverified until tested in Minecraft.
