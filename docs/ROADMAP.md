# Planet Essentials Development Roadmap

Statuses: [ ] not started, [~] in progress / needs runtime validation, [x] implemented and verified at the stated level.

## 0. Foundation
- [x] Fabric 1.21.11 / Java 21 foundation, pinned Planet Lib dependency, CI build/test and JAR verification
- [ ] Client and dedicated-server launch validation

## 1. Planet Lib and core systems
- [x] Typed Essentials API, contract validation, loaded-mod/capability discovery, standalone behavior, integration tests
- [x] Typed configuration, validation, migrations, atomic persistence, profiles, import/export
- [x] Mod discovery, capability selection, reversible integration lifecycle
- [x] Bounded diagnostics event collection and explicit export
- [~] FPS/runtime overhead, JFR/GPU profiling, and benchmark verification

## 2. Planet Prism shader engine
- [x] Immutable validated shader source bundles and SHA-256 identity
- [x] Bounded schema-1 manifest, shader pack discovery, path validation, and source budgets
- [x] Minecraft 1.21.11 GpuDevice pipeline compilation backend
- [x] Transactional shader reload, rollback, and resource lifecycle
- [x] Uniform-buffer, texel-buffer, and sampler declarations and typed binding validation
- [x] World render-stage dispatch, fullscreen draw executor, and render targets
- [x] Multi-pass post-processing graph, validation, target reuse, and final composite
- [x] Shader enable/disable and pack selection UI
- [ ] Actual GPU/driver capability detection
- [ ] In-game shader correctness and performance validation

## 3. Prism terrain rendering

### Implemented architecture
- [x] Bounded CPU terrain mesh builder and immutable extraction-to-draw handoff
- [x] Versioned full-attribute terrain payload and Minecraft vertex-format mapping
- [x] Render-thread GPU terrain upload, owned allocation lifecycle, and bounded LRU GPU cache
- [x] Bounded CPU section cache and terrain-specific shader/draw executor
- [x] Frame extraction and BEFORE_ENTITIES / BEFORE_TRANSLUCENT scheduling for cooperating providers
- [x] Revision-token section rebuild lifecycle and per-world CPU caches
- [x] Vanilla SOLID, CUTOUT, TRANSLUCENT, and TRIPWIRE SectionCompiler mesh capture
- [x] Camera-relative section transforms and section-origin GPU uniforms
- [x] Active frame-frustum snapshot and section AABB filtering
- [x] Vanilla SectionOcclusionGraph visible-section snapshot
- [x] Vanilla draw identity index mapping to section coordinates, layer, and layer-local order
- [x] Fail-open replacement gate and all-or-nothing per-layer replacement plans
- [x] Live CPU-payload to GPU-allocation readiness check, including source-hash and size parity
- [x] Per-ClientLevel replacement coordinator, GPU cache preparation, shader-generation consistency check, and chunk/world invalidation

### Still required for vanilla terrain takeover
- [ ] Reproduce vanilla render-pass state: render target, samplers, light texture, default uniforms, layer pipelines, and exact ordering
- [ ] Validate draw index mapping against actual 1.21.11 runtime behavior
- [ ] Synchronous replacement hook at the vanilla render-group boundary calling the coordinator
- [ ] Whole-group vanilla fallback on any missing payload, GPU allocation, shader, state, or frame
- [ ] Runtime visual parity for opaque, cutout, translucent, and tripwire geometry
- [ ] GPU memory, resource lifetime, and performance benchmarks

### Terrain correctness and optimization
- [x] Bounded extraction frame budgets and fail-safe truncation
- [x] Vanilla translucent quad-centroid capture and camera-relative far-to-near index sorting
- [x] GPU-fenced temporary translucent index ring and tripwire-after-translucent grouping
- [~] In-game verification of translucent sorting and terrain-layer parity

## 4. Prism texture subsystem
- [x] Bounded resource discovery, PNG validation, async decode, immutable pixel cache, reload/shutdown lifecycle, CPU mip generation
- [x] Opt-in GPU texture adapter, mip uploads, pending NativeImage cleanup, vanilla identifier fallback API
- [~] Client GPU validation and production consumer integration
- [ ] Resource-pack coexistence, visual correctness, leak checks, and performance benchmarks

## 5. Planet client-style UI
- [~] Main settings hub and Prism shader panel
- [ ] Migrate remaining vanilla settings categories without removing unvalidated settings
- [ ] Registry-driven settings pages, search/filter, per-mod panels, reset flows
- [ ] Accessibility and localization

## 6. Compatibility and release
- [x] Primary target: vanilla Minecraft 1.21.11 with Fabric
- [x] No Sodium, Iris, VulkanMod, or Canvas compatibility targets or adapters planned
- [ ] Ordinary Fabric mod coexistence testing
- [ ] Client/server runtime validation and performance regression suite
- [ ] License, changelog, versioning, and release artifact

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU driver behavior, visual correctness, or runtime performance.
