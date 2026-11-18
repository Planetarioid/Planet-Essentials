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
- [ ] Reproducible Planet Lib development dependency
- [ ] Client launch configuration
- [ ] Dedicated-server launch configuration
- [ ] Initial clean-build verification

## 1. Planet Lib integration
- [ ] Resolve Planet Lib as a real build dependency
- [ ] Validate API contract version at startup
- [ ] Register Planet Essentials integration metadata
- [ ] Handle missing/incompatible bridge without crashing
- [ ] Add integration tests against the actual Planet Lib artifact

## 2. Core configuration and settings
- [x] Initial typed configuration model and defaults
- [x] Schema version field and validation
- [x] Atomic persistence with malformed-file preservation
- [~] Profile model and import/export validation
- [ ] Settings migration tests
- [ ] Thread-safety and lifecycle tests

## 3. Mod discovery and integration coordination
- [ ] Read loaded mods from Planet Lib registry
- [ ] Identify installed Planet Studios mods
- [ ] Integration registration lifecycle
- [ ] Optional-mod capability discovery
- [ ] Graceful behavior for absent integrations
- [ ] Avoid scanning or modifying arbitrary third-party files

## 4. Feature priority management
- [ ] Read feature/provider metadata from Planet Lib
- [ ] Present competing implementations per capability
- [ ] Display selected provider and selection reason
- [ ] User override controls
- [ ] Reset to automatic selection
- [ ] Persist preferences safely
- [ ] Apply changes only through provider lifecycle contracts
- [ ] Ensure only duplicate features are affected, not entire mods
- [ ] Test deterministic resolution and reversibility

## 5. Diagnostics
- [~] Bounded in-memory FPS history
- [~] Diagnostics toggle and basic performance display
- [x] Unit tests for bounded history and configuration validation
- [ ] Diagnostics event collection integration
- [ ] Export diagnostics only on explicit user action
- [ ] No background telemetry or personal-data collection
- [ ] Verify low overhead when diagnostics are not open

## 6. Performance architecture
- [ ] Establish baseline profiling methodology
- [ ] Identify measurable CPU bottlenecks
- [ ] Identify measurable GPU/render bottlenecks
- [ ] Memory allocation and cache strategy
- [ ] Audio processing review
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
- [~] Retain access to vanilla Minecraft settings from the hub
- [ ] Main Planet Essentials navigation and subsystem sections
- [ ] Search/filter settings
- [ ] Per-mod settings panels
- [ ] Feature-priority controls
- [ ] Diagnostics view
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

Do not claim in-game validation based solely on CI compilation. Record each tested Minecraft version, Fabric Loader version, environment, and result here.
