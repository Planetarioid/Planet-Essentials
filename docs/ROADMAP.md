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
- [x] Immutable validated vertex/fragment source bundle and per-stage source limit
- [x] Stable SHA-256 source identity
- [x] Backend-neutral capability snapshot and compiler SPI
- [x] Transactional reload and rollback on compilation failure
- [x] Native handle disposal, invalidation, and close lifecycle
- [x] Unit tests for source validation, hashing, rollback, duplicate IDs, and lifecycle
- [ ] Concrete Minecraft 1.21.11 GPU shader backend
- [ ] Shader-pack discovery, manifest/schema, versioning, pack priority
- [ ] Uniform, sampler, buffer, framebuffer, render-target binding model
- [ ] World render-stage integration and shader-pack features
- [ ] Actual GPU/driver capability detection
- [ ] Vanilla fallback and coexistence with Sodium, Iris, and other renderers
- [ ] In-game shader tests, visual regression, GPU memory tests, benchmarks

## 9. UI
- [~] Main settings hub and registry-driven pages
- [ ] Shader settings and pack selection, search/filter, per-mod panels, reset flows, accessibility/localization

## 10. Compatibility and release
- [ ] Client/server runtime tests, mod coexistence matrix, performance regression suite, license, changelog, version, release artifact

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU shader compilation, visual correctness, renderer compatibility, or performance superiority.
