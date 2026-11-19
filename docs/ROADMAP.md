# Planet Essentials Development Roadmap

Statuses: [ ] not started, [~] in progress / needs runtime validation, [x] implemented and verified at the stated level.

## 0. Foundation
- [x] Fabric 1.21.11 / Java 21 build, pinned Planet Lib dependency, CI build/test and JAR verification
- [ ] Client and dedicated-server launch validation

## 1. Planet Lib
- [x] Typed Essentials API, contract validation, loaded-mod/capability discovery, standalone behavior, integration tests

## 2. Configuration
- [x] Typed settings, validation, migrations, atomic persistence, profiles, import/export, concurrency tests

## 3. Discovery and integration
- [x] Loaded-mod discovery, Planet Studios capability discovery, reversible integration lifecycle

## 4. Feature priorities
- [x] Per-capability provider selection, automatic reset, persistence, lifecycle rollback contract

## 5. Diagnostics
- [x] Bounded local event collection and explicit diagnostics export
- [~] FPS/runtime overhead verification

## 6. Performance architecture
- [x] Profiling methodology and subsystem boundaries documented
- [~] JFR/GPU investigation, runtime compatibility, and benchmarks

## 7. Prism texture subsystem
- [x] Bounded resource discovery, PNG validation, async decode, immutable pixel cache, reload/shutdown lifecycle, CPU mip generation
- [x] Opt-in GpuTexture adapter, mip uploads, pending NativeImage cleanup, vanilla identifier fallback API
- [~] Client GPU validation and production consumer integration
- [ ] Resource-pack coexistence, visual correctness, leak checks, performance benchmarks

## 8. Planet Prism Rendering Engine (PPRE): shader subsystem
- [x] Immutable validated source bundles and stable SHA-256 source identity
- [x] Backend-neutral capabilities, compiler SPI, transactional reload, rollback, and handle lifecycle
- [x] Bounded schema-1 manifest, pack discovery, active resource resolution, path validation, source budgets
- [x] Minecraft 1.21.11 GpuDevice pipeline-precompile backend
- [x] Uniform-buffer, texel-buffer, and sampler declarations
- [x] Opt-in shader enable/disable setting and persisted shader-pack selection UI
- [x] Typed draw executor, reusable fullscreen quad, and shader-program pipeline access
- [x] Client resource-reload discovery and transactional GPU compilation lifecycle
- [x] Typed GPU binding sets with exact declared-versus-supplied validation
- [x] Fabric 1.21.11 world drawing-stage dispatch
- [x] Cooperative Prism material registration and active-pack pipeline resolution API
- [x] Renderer environment detection and explicit renderer-adapter SPI with immutable feature declarations
- [x] Immutable diagnostics snapshot for discovery, compilation, active graph, and renderer state
- [x] Built-in END_MAIN world post-process graph and final composite
- [x] GPU fullscreen multi-pass execution, sampler mapping, target-slot reuse, and final composite
- [x] Shader-pack multi-pass graph authoring and strict graph validation

### Terrain rendering
- [x] Bounded CPU terrain mesh builder and immutable extraction-to-draw frame handoff
- [x] Versioned full-attribute terrain payload and matching Minecraft vertex-format mapping
- [x] Render-thread GPU terrain buffer upload and owned allocation lifecycle
- [x] Bounded LRU GPU terrain buffer cache with section invalidation and teardown
- [x] Terrain-specific shader compilation path and validated quad draw executor
- [x] Fabric END_EXTRACTION frame publication and BEFORE_ENTITIES/BEFORE_TRANSLUCENT scheduling for cooperating providers
- [~] Revision-token rebuild lifecycle, per-ClientLevel CPU caches, chunk load/unload hooks, stale-frame removal, GPU invalidation, and opt-in V1 adapter
- [x] Vanilla SectionCompiler capture for SOLID, CUTOUT, TRANSLUCENT, and TRIPWIRE meshes converted to Prism V1 payloads
- [x] Double-precision camera-relative section-origin transform and 16-byte std140 section uniform contract
- [x] Renderer-owned GPU section-uniform allocation, binding injection, and terrain draw overload
- [x] Active LevelRenderer frame-frustum snapshot and section AABB rejection before Prism upload/draw
- [x] Vanilla SectionOcclusionGraph visible-section snapshot, world-scoped lifecycle, and fail-open filtering
- [x] Vanilla terrain draw identity index mapping Draw records to exact section coordinates, layer, and layer-local order
- [x] Fail-open replacement gate and complete per-layer replacement-plan builder with tests
- [~] Connect replacement-plan readiness to current captured CPU revisions and live GPU cache entries
- [ ] Reproduce vanilla render-pass state: target, sampler, light texture, default uniforms, layer pipelines, and ordering
- [ ] Synchronous ChunkSectionsToRender.renderGroup replacement hook with whole-group fallback
- [ ] Entity and block-entity material pipeline integration
- [ ] Renderer-specific Sodium/Canvas/VulkanMod adapters and compatibility negotiation
- [ ] Actual GPU/driver limit detection
- [ ] In-game visual parity, GPU memory, leak, and performance tests

### Terrain correctness and optimization
- [x] Bounded extraction frame budgets and fail-safe truncation for the opt-in adapter
- [x] Vanilla translucent quad-centroid capture and camera-relative far-to-near index sorting
- [x] GPU-fenced temporary translucent index ring and tripwire-after-translucent grouping
- [~] Runtime verification of translucent ordering and all terrain layer parity

## 9. UI
- [~] Planet client-style settings hub and Prism shader panel; full vanilla settings-category migration, registry-driven pages, accessibility, and localization remain
- [x] Shader enable/disable and discovered pack selection screen
- [ ] Pack search/filter, per-mod panels, reset flows, accessibility/localization

## 10. Compatibility and release
- [ ] Client/server runtime tests, mod coexistence matrix, performance regression suite, license, changelog, version, release artifact

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU driver behavior, visual correctness, renderer compatibility, or performance superiority.
