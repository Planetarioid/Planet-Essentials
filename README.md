# Space Client

**The unified Minecraft client experience by Planet Studios.**

Space Client is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It brings settings, integrations, diagnostics, HUD tools, and client optimization systems into one cohesive in-game experience.

Its custom rendering initiative is now named **Space Renderer**. Space Renderer is the engine architecture for terrain section management, visibility, asynchronous mesh work, GPU memory management, shader pipelines, and batched drawing. It is an active development project, not a claim of completed renderer functionality.

## Development status

The repository is in active development. Automated CI status must be checked against the latest commit; a successful build does not establish in-game correctness, GPU performance, or third-party compatibility.

Implemented foundations include typed configuration and profiles, bounded local diagnostics, diagnostics export, mod discovery through Planet Lib, feature-priority controls, searchable settings and keybind screens, a HUD editor, and a route from Minecraft's normal Options entry point to the Space Client settings hub.

- [x] Repository and Fabric build foundation
- [x] Configuration, migrations, profiles, and persistence tests
- [x] Planet Lib API bridge and loaded-mod/capability discovery
- [x] Feature-priority settings and provider lifecycle integration
- [x] Local diagnostics collection and explicit JSON export
- [x] Searchable Space Client settings and keybind screens (runtime test pending)
- [x] HUD editor foundation (runtime test pending)
- [~] Diagnostics runtime-overhead verification
- [~] Space Client settings hub and vanilla Options integration
- [~] Space Renderer shader, texture, and terrain foundations (client runtime and performance validation pending)
- [ ] Complete terrain renderer and visual parity validation
- [ ] Client and dedicated-server launch validation
- [ ] Third-party coexistence validation

See [the development roadmap](docs/ROADMAP.md), [first testing pass](docs/TESTING_GUIDE.md), [compatibility test matrix](docs/COMPATIBILITY_TEST_MATRIX.md), [Space Renderer architecture](docs/rendering/SPACE_RENDERER_ARCHITECTURE.md), [baseline profiling protocol](docs/performance/BASELINE_PROFILING.md), and [benchmark and correctness protocol](docs/performance/BENCHMARK_AND_CORRECTNESS_PROTOCOL.md) for tracked work and verification requirements.

## Space Renderer responsibilities

- World and render-section lifecycle management
- Asynchronous terrain mesh construction and stale-build rejection
- Visibility graph, frustum, and occlusion culling
- Shared GPU region allocation and upload scheduling
- Opaque, cutout, and translucent render lists
- Batched terrain submission and per-frame sorting
- Shader/material management and GPU capability selection
- Safe fallback to Minecraft's renderer when a feature is unavailable

## Design principles

- Correctness and compatibility before performance claims.
- Measure bottlenecks before implementing optimizations.
- Keep client-only systems out of dedicated-server initialization.
- Avoid unnecessary work on tick and render paths.
- Do not silently rewrite third-party configuration or behavior.
- Optional Planet mods must remain optional.
- When Space Client is absent, other Planet mods retain their standalone defaults.
- Settings and feature-priority choices must be reversible.
- Never replace vanilla rendering paths until the optimized path has correctness, fallback, and compatibility coverage.

## Development environment

- Minecraft: 1.21.11
- Fabric Loader: 0.18.1
- Fabric API: 0.141.6+1.21.11
- Java: 21
- Build: Gradle 9.2.0 with Fabric Loom

Build with `gradle clean build` and run tests with `gradle test`.

## Dependency note

Space Client builds against a pinned Planet Lib Git submodule as a sibling Gradle project. Planet Lib is compile-time only in Essentials and remains optional at runtime.

No license has been selected yet. Do not assume permission to redistribute or reuse this project.
