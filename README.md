# Planet Essentials

**The central hub for the Planet Studios Minecraft mod ecosystem.**

Planet Essentials is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It is designed to become the main in-game settings hub, integration coordinator, diagnostics center, and client optimization suite for Planet Studios projects.

## Development status

The repository is in active development. Implemented foundations include typed configuration and profiles, bounded local diagnostics, explicit diagnostics export, mod discovery through Planet Lib, feature-priority controls, and a route from Minecraft's normal Options entry point to the Essentials settings hub.

- [x] Repository and Fabric build foundation
- [x] Configuration, migrations, profiles, and persistence tests
- [x] Planet Lib API bridge and loaded-mod/capability discovery
- [x] Feature-priority settings and provider lifecycle integration
- [x] Local diagnostics collection and explicit JSON export
- [~] Diagnostics runtime-overhead verification
- [~] Planet Essentials settings hub replaces vanilla Options entry point
- [ ] Performance bottleneck investigation and optimization systems
- [ ] Texture optimization engine
- [ ] Shader system
- [ ] Full settings navigation and per-mod panels
- [ ] Client and dedicated-server validation

See [the development roadmap](docs/ROADMAP.md), [baseline profiling protocol](docs/performance/BASELINE_PROFILING.md), [benchmark and correctness protocol](docs/performance/BENCHMARK_AND_CORRECTNESS_PROTOCOL.md), and [compatibility test matrix](docs/COMPATIBILITY_TEST_MATRIX.md) for tracked work and verification requirements.

## Planned responsibilities

- Main in-game settings hub, entered through Minecraft's normal Options button
- Access to vanilla Minecraft settings through the hub
- Central settings UI and configuration profiles
- Integration with installed Planet Studios mods
- Per-feature implementation priority controls
- Diagnostics and performance visibility
- CPU, GPU, memory, rendering, audio, particle, and animation optimization systems
- Dedicated texture optimization engine
- Shader management and rendering system

## Design principles

- Correctness and compatibility before performance claims.
- Measure bottlenecks before implementing optimizations.
- Keep client-only systems out of dedicated-server initialization.
- Avoid unnecessary work on tick and render paths.
- Do not silently rewrite third-party configuration or behavior.
- Optional Planet mods must remain optional.
- When Planet Essentials is absent, other Planet mods retain their standalone defaults.
- Settings and feature-priority choices must be reversible.

## Development environment

- Minecraft: 1.21.11
- Mod loader: Fabric
- Fabric Loader: 0.18.1
- Java: 21
- Build: Gradle with Fabric Loom

Build with `gradle clean build` and run tests with `gradle test`.

## Dependency note

Planet Essentials builds against a pinned Planet Lib Git submodule as a sibling Gradle project. Planet Lib is compile-time only in Essentials and remains optional at runtime.

No license has been selected yet. Do not assume permission to redistribute or reuse this project.
