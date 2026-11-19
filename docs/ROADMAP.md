# Planet Essentials Development Roadmap

Statuses: [ ] not started, [~] in progress, [x] implemented and verified at the stated level.

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
- [x] Bounded CPU terrain mesh builder and immutable extraction-to-draw frame handoff
- [x] Versioned full-attribute terrain payload and matching Minecraft vertex-format mapping
- [x] Render-thread GPU terrain buffer upload and owned allocation lifecycle
- [x] Bounded LRU GPU terrain buffer cache with section invalidation and teardown
- [x] Terrain-specific shader compilation path and validated quad draw executor
- [x] Fabric END_EXTRACTION frame publication and BEFORE_ENTITIES/BEFORE_TRANSLUCENT terrain scheduling bridge for cooperating providers
- [~] Revision-token rebuild lifecycle, per-ClientLevel CPU caches, Fabric chunk load/unload hooks, stale-frame removal, render-thread GPU invalidation, and opt-in V1 GPU terrain adapter
- [x] Vanilla SectionCompiler result capture: copy SOLID, CUTOUT, TRANSLUCENT, and TRIPWIRE BLOCK-format quad buffers into revision-validated Prism V1 CPU payloads, retaining vanilla translucent quad centroids for per-frame sorting
- [~] Vanilla terrain GPU draw replacement, live camera-frustum wiring, occlusion culling, and renderer-specific compatibility adapters remain
- [x] Double-precision camera-relative section-origin transform utility with unit coverage
- [x] Canonical 16-byte std140 camera-relative section-origin uniform payload and declaration contract
- [x] Renderer-owned immutable GPU section-uniform allocation, binding injection, and terrain draw overload
- [x] Active LevelRenderer frame frustum snapshot and per-section AABB rejection before Prism terrain GPU upload/draw
- [~] CPU section-AABB frustum culling utility and unit tests; occlusion culling and vanilla visibility-graph integration remain
- [ ] Automatic vanilla entity/block-entity material pipeline replacement
- [~] Opt-in full-attribute V1 terrain renderer adapter and per-world GPU cache integration, with active-frustum filtering, graceful per-frame budget truncation, and vanilla-compatible translucent quad sorting; renderer-specific vanilla/Sodium/Canvas adapters and safe fallback negotiation remain
- [x] Vanilla render-distance and simulation-distance option bounds extended to 2–64 chunks (server-imposed render-distance caps still apply)
- [ ] Actual GPU/driver limit detection
- [ ] In-game visual tests, GPU memory tests, benchmarks

## 9. UI
- [~] Planet client-style settings hub and Prism shader panel; full vanilla settings-category migration, registry-driven pages, accessibility, and localization remain
- [x] Shader enable/disable and discovered pack selection screen
- [ ] Pack search/filter, per-mod panels, reset flows, accessibility/localization

## 10. Compatibility and release
- [ ] Client/server runtime tests, mod coexistence matrix, performance regression suite, license, changelog, version, release artifact

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU driver behavior, visual correctness, renderer compatibility, or performance superiority.
