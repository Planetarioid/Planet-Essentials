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
- [x] Client resource-reload discovery and transactional GPU compilation lifecycle
- [x] Typed GPU binding sets with exact declared-versus-supplied validation for uniform buffers, texel buffers, and samplers
- [~] Frame-stage drawing integration
- [ ] Framebuffer/render-target lifecycle and post-processing graph
- [ ] Terrain/entity pipeline integration and full shader-pack feature compatibility
- [ ] Actual GPU/driver limit detection and renderer negotiation
- [ ] Vanilla fallback and coexistence with Sodium, Iris, and other renderers
- [ ] In-game visual tests, GPU memory tests, benchmarks

## 9. UI
- [~] Main settings hub and registry-driven pages
- [ ] Shader settings and pack selection, search/filter, per-mod panels, reset flows, accessibility/localization

## 10. Compatibility and release
- [ ] Client/server runtime tests, mod coexistence matrix, performance regression suite, license, changelog, version, release artifact

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU driver behavior, visual correctness, renderer compatibility, or performance superiority.
