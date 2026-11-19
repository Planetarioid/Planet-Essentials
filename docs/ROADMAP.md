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
- [x] Resolve Planet Lib as a real build dependency (pinned source project dependency; runtime integration remains optional)
- [x] Validate API contract version at startup
- [x] Expose typed Planet Essentials API entrypoint and accept validated integration metadata
- [x] Handle missing/incompatible bridge without crashing
- [x] Add integration tests against pinned Planet Lib

## 2. Core configuration and settings
- [x] Typed setting definitions, central registry, validation
- [x] Configuration schema migration path (0/1/2/3 to 4)
- [x] Atomic persistence with malformed-file preservation
- [x] Named profile create/save/load/delete
- [x] Profile import/export validation and safe names
- [x] Persistence, migration, and concurrency tests

## 3. Mod discovery and integration coordination
- [x] Loaded-mod discovery through Planet Lib
- [x] Planet Studios mod identity and capability discovery
- [x] Integration registration lifecycle
- [x] Graceful standalone behavior when Planet Lib is absent
- [x] No arbitrary mods-folder scanning or third-party config modification

## 4. Feature priority management
- [x] Provider metadata and competing implementation UI
- [x] Per-capability manual selection and automatic reset
- [x] Persisted reversible preferences
- [x] Provider-owned lifecycle callbacks and rollback contract
- [x] Deterministic resolution covered by Planet Lib tests

## 5. Diagnostics
- [~] Bounded FPS history and diagnostics settings (runtime verification pending)
- [x] Bounded in-memory lifecycle and slow-tick event collection
- [x] Explicit local diagnostics export; no telemetry
- [~] Verify disabled-path overhead with runtime profiling

## 6. Performance architecture
- [x] Baseline profiling methodology documented; no actual baseline
- [~] CPU investigation deferred by user pending JFR
- [~] GPU/render investigation deferred pending measurements
- [x] Memory/cache strategy documented; no measured gains
- [x] Audio, particle, animation, and network boundaries reviewed; no optimizations claimed
- [x] Independent diagnostics subsystem controls
- [~] Benchmark protocol documented; no optimization accepted yet
- [~] Compatibility matrix documented; runtime combinations untested

## 7. Texture optimization engine
- [x] Effective resource discovery through Minecraft ResourceManager, bounded metadata catalog, deterministic immutable snapshots
- [x] Texture metadata and format validation (namespace, resource identity, path safety, PNG format, and path-based category validation)
- [x] PNG signature, IHDR CRC, dimensions, pixel-count, bit-depth, and method preflight
- [x] Encoded PNG stream hard limit enforced before ImageIO decode
- [x] Bounded asynchronous CPU decode (2 workers, 64 queued jobs, immutable decoded pixels)
- [x] Memory-aware decoded-pixel LRU cache (128 MiB default hard budget)
- [x] Resource reload generation invalidation and client-shutdown cleanup
- [x] Effective texture resource stream resolution through Minecraft ResourceManager
- [~] Lazy decoder-to-Minecraft texture registration/render integration (CPU pipeline exists; GPU upload path not connected)
- [ ] Mipmap and filtering policy
- [ ] Resource-pack coexistence tests
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
- [x] Registry-driven settings pages and profile management
- [x] Vanilla Minecraft settings remain accessible
- [ ] Search/filter settings
- [ ] Per-mod settings panels
- [~] Feature-priority usability and runtime review
- [~] Dedicated diagnostics view
- [ ] Reset and confirmation flows
- [ ] Accessibility, scaling, and localization review

## 10. Compatibility and release
- [ ] Clean client startup test
- [ ] Dedicated-server startup test
- [ ] Standalone test without Planet Lib
- [ ] Available Planet mod integration tests
- [ ] Third-party mod coexistence matrix execution
- [ ] Performance regression suite
- [ ] License decision
- [ ] Release checklist and changelog
- [ ] Distribution decision
- [ ] Version tag and release artifact

## Verification record
CI verifies compilation, unit tests, JAR contents, and artifact creation. In-game behavior, dedicated-server behavior, runtime compatibility, and performance gains remain unverified until tested.
