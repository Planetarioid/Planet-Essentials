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
- [x] Configuration schema migration path (0/1/2/3 to 4)
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
- [x] Persist preferences safely (validated per-capability overrides)
- [x] Apply changes only through provider lifecycle contracts (provider-owned callbacks with rollback on transition failure)
- [x] Ensure choices are scoped to one capability; no whole-mod enable/disable
- [x] Deterministic resolution and reversible override behavior covered by Planet Lib tests

## 5. Diagnostics
- [~] Bounded in-memory FPS history
- [~] Diagnostics toggle and configurable history capacity
- [x] Unit tests for bounded history and configuration validation
- [x] Diagnostics event collection integration (local lifecycle and slow-tick events; bounded in-memory collector)
- [x] Export diagnostics only on explicit user action (manual button; local JSON file; bounded snapshot; no background export)
- [x] No background telemetry or personal-data collection
- [~] Verify low overhead when diagnostics are not open (disabled subsystem paths avoid sampling and event creation; runtime profiling still required)

## 6. Performance architecture
- [x] Baseline profiling methodology documented; no actual baseline measurements
- [~] CPU bottleneck investigation deferred by user; requires real JFR before identifying or changing bottlenecks
- [~] GPU/render bottleneck investigation deferred pending client telemetry; no bottleneck is claimed
- [x] Memory allocation/cache strategy documented; no measured allocation reduction
- [x] Audio processing review documented; no audio optimization implemented
- [x] Particle processing review documented; no particle optimization implemented
- [x] Animation processing review documented; no animation optimization implemented
- [x] Network processing review documented; no packet behavior changes or network optimization implemented
- [x] Per-system enable/disable controls (Diagnostics master switch plus independent FPS-monitoring and event-collection switches; persisted in schema 4; future optimization engines will register their own controls when implemented)
- [~] Benchmark correctness and performance impact protocol documented in docs/performance/BENCHMARK_AND_CORRECTNESS_PROTOCOL.md; no optimization has been benchmarked or accepted yet
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
- [x] Main navigation for diagnostics, profiles, feature priority, and system controls
- [x] Registry-driven settings pages
- [x] Profile management screen for create/save/load/delete
- [x] Vanilla Minecraft settings remain accessible
- [ ] Search/filter settings
- [ ] Per-mod settings panels
- [~] Feature-priority controls (runtime lifecycle application and usability review remain)
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
