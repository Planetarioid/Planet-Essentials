# Planet Essentials

**The central hub for the Planet Studios Minecraft mod ecosystem.**

Planet Essentials is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It will provide the shared settings experience, diagnostics, integration coordination, and client optimization systems used across Planet Studios projects.

## Development status

This repository is at the initial project-foundation stage.

- [x] Repository initialized
- [x] Fabric Loom and Java 21 build configuration
- [x] Minecraft 1.21.11 target
- [x] Fabric mod metadata and entrypoint
- [x] Continuous integration baseline
- [ ] Planet Lib API integration
- [ ] Core configuration and settings architecture
- [ ] Mod discovery and integration coordination
- [ ] Feature priority UI
- [ ] Diagnostics interface
- [ ] Performance systems
- [ ] Texture optimization engine
- [ ] Shader system
- [ ] In-game settings interface
- [ ] Client and dedicated-server validation

A checklist for each subsystem is maintained in [the development roadmap](docs/ROADMAP.md).

## Planned responsibilities

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
- When Planet Essentials is absent, other Planet mods should retain their standalone defaults.
- Settings and feature-priority choices must be reversible.

## Development environment

- Minecraft: 1.21.11
- Mod loader: Fabric
- Fabric Loader: 0.18.1
- Java: 21
- Build: Gradle with Fabric Loom

Build with `gradle clean build` and run tests with `gradle test`.

## Dependency note

Planet Essentials is designed to use Planet Lib as its shared API foundation. The initial repository foundation declares the runtime mod dependency. Direct API usage will be wired once the library has a stable, consumable development artifact and the dependency can be resolved reproducibly in both local and CI builds.

No license has been selected yet. Do not assume permission to redistribute or reuse this project.
