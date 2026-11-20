# Planet Essentials

**The central hub for the Planet Studios Minecraft mod ecosystem.**

Planet Essentials is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It is designed to become the main in-game settings hub, integration coordinator, diagnostics center, and client optimization suite for Planet Studios projects.

Its custom rendering initiative is named the **Planet Prism Rendering Engine (PPRE)**. Prism is the umbrella architecture for texture processing, GPU resource management, shader systems, rendering capabilities, and compatibility. It is an active development project, not a claim of completed renderer functionality.

## Development status

The repository is in active development. The latest CI build and test workflow passed for commit `7c1e554204b20e585e8befe3d324c4927f706c15`: 238 automated tests, 0 failures, 0 ignored tests, plus distributable JAR verification. This is automated verification only; Minecraft runtime, GPU behavior, multiplayer behavior, and third-party compatibility remain unverified until tested.

Implemented foundations include typed configuration and profiles, bounded local diagnostics, explicit diagnostics export, mod discovery through Planet Lib, feature-priority controls, searchable settings and keybind screens, a HUD editor, and a route from Minecraft's normal Options entry point to the Essentials settings hub.

- [x] Repository and Fabric build foundation
- [x] Configuration, migrations, profiles, and persistence tests
- [x] Planet Lib API bridge and loaded-mod/capability discovery
- [x] Feature-priority settings and provider lifecycle integration
- [x] Local diagnostics collection and explicit JSON export
- [x] Searchable Planet settings and keybind screens (automated build verified; runtime test pending)
- [x] HUD editor foundation (automated build verified; runtime test pending)
- [~] Diagnostics runtime-overhead verification
- [~] Planet Essentials settings hub and vanilla Options integration
- [~] Prism shader and texture pipelines: source implementation and CI verification; client runtime and consumer integration pending
- [ ] Prism terrain takeover and visual parity validation
- [ ] Client and dedicated-server launch validation
- [ ] Third-party coexistence validation

See [the development roadmap](docs/ROADMAP.md), [first testing pass](docs/TESTING_GUIDE.md), [compatibility test matrix](docs/COMPATIBILITY_TEST_MATRIX.md), [Prism architecture](docs/rendering/PRISM_RENDERING_ENGINE.md), [baseline profiling protocol](docs/performance/BASELINE_PROFILING.md), and [benchmark and correctness protocol](docs/performance/BENCHMARK_AND_CORRECTNESS_PROTOCOL.md) for tracked work and verification requirements.

## Planned responsibilities

- Main in-game settings hub, entered through Minecraft's normal Options button
- Access to vanilla Minecraft settings through the hub
- Central settings UI and configuration profiles
- Integration with installed Planet Studios mods
- Per-feature implementation priority controls
- Diagnostics and performance visibility
- CPU, GPU, memory, rendering, audio, particle, and animation optimization systems
- **Planet Prism Rendering Engine (PPRE)** for texture processing, shader management, GPU resource lifecycle, and rendering optimizations

## Design principles

- Correctness and compatibility before performance claims.
- Measure bottlenecks before implementing optimizations.
- Keep client-only systems out of dedicated-server initialization.
- Avoid unnecessary work on tick and render paths.
- Do not silently rewrite third-party configuration or behavior.
- Optional Planet mods must remain optional.
- When Planet Essentials is absent, other Planet mods retain their standalone defaults.
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

Planet Essentials builds against a pinned Planet Lib Git submodule as a sibling Gradle project. Planet Lib is compile-time only in Essentials and remains optional at runtime.

No license has been selected yet. Do not assume permission to redistribute or reuse this project.
