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

## 1. Planet Lib integration
- [ ] Resolve Planet Lib as a real build dependency
- [ ] Validate API contract version at startup
- [ ] Register Planet Essentials integration metadata
- [ ] Handle missing/incompatible bridge without crashing
- [ ] Add integration tests against the actual Planet Lib artifact

## 2. Core configuration and settings
- [x] Typed setting definitions with stable IDs, categories, defaults, and validation
- [x] Central settings registry and category lookup
- [x] Configuration schema migration path (0/1 to 2)
- [x] Atomic persistence with malformed-file preservation
- [x] Named profile create/save/load/delete storage
- [x] Profile import/export validation and safe profile names
- [x] Tests for registry, migration, profile persistence, and malformed imports
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
- [~] Diagnostics toggle and configurable history capacity
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
- [x] Main navigation for diagnostics and profiles
- [x] Registry-driven diagnostics settings page
- [x] Profile management screen for create/save/load/delete
- [x] Vanilla Minecraft settings remain accessible
- [ ] Search/filter settings
- [ ] Per-mod settings panels
- [ ] Feature-priority controls
- [ ] Dedicated diagnostics view
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

CI verifies compilation, unit tests, JAR contents, and artifact creation. In-game behavior remains unverified until tested in Minecraft.

### docs/ARCHITECTURE.md
# Planet Essentials Architecture

## Role
Planet Essentials is the central client-facing control and optimization mod in the Planet Studios ecosystem. It owns its own settings UI and optimization engines. Planet Lib remains the shared API and metadata layer.

## Dependency direction
- Planet Essentials depends on Planet Lib.
- Planet Lib must not depend on Planet Essentials.
- Other Planet mods may integrate with Essentials through the optional API bridge in Planet Lib.
- Other Planet mods must retain standalone defaults when Essentials is absent.

## Module boundaries
- `config`: typed settings registry, persistence, profiles, migrations
- `integration`: Planet Lib bridge and per-mod integration lifecycle
- `features`: feature/provider coordination and preference presentation
- `diagnostics`: bounded diagnostics and user-visible reporting
- `performance`: measured optimization systems and benchmark support
- `texture`: dedicated texture optimization engine
- `shader`: shader lifecycle, compilation, capability checks, fallback
- `client`: client-only screens and lifecycle integration

## Lifecycle rules
- Common initialization must remain safe on a dedicated server.
- Client-only classes must be isolated behind Fabric client entrypoints or client-only lifecycle registration.
- No expensive scanning, shader compilation, texture decoding, or profiling should occur on every tick.
- Resource reload and shutdown must release owned resources.
- Optional integrations must fail softly and expose a diagnostic reason.

## Performance rules
- Profile before optimizing.
- Avoid per-frame allocations and repeated lookups in hot paths.
- Cache only when invalidation and resource ownership are explicit.
- Do not trade correctness, server validation, or compatibility for responsiveness.
- Every optimization needs a correctness check and a before/after benchmark.

## Configuration rules
- Planet Essentials owns its own configuration.
- Other Planet mods own their settings unless they explicitly register an integration.
- Never silently rewrite another mod's configuration files.
- Setting IDs are stable, namespaced by subsystem, and validated centrally.
- Profiles snapshot only Planet Essentials-owned settings.
- Profile import is validated before the managed profile file is changed.
- Feature priority overrides are per capability, persistent, and reversible.
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
