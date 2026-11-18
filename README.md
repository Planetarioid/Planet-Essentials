# Planet Essentials

**The central hub for the Planet Studios Minecraft mod ecosystem.**

Planet Essentials is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It is designed to become the main in-game settings hub, integration coordinator, diagnostics center, and client optimization suite for Planet Studios projects.

## Development status

The repository is in active development. The current implementation includes a validated configuration model, atomic config persistence, bounded FPS history, basic diagnostics UI, and a client-side replacement route for Minecraft's vanilla Options screen.

- [x] Repository and Fabric build foundation
- [x] Initial configuration and diagnostics foundation
- [~] Planet Essentials settings hub replaces vanilla Options entry point
- [ ] Planet Lib API integration
- [ ] Mod discovery and integration coordination
- [ ] Feature priority UI
- [ ] Complete diagnostics interface
- [ ] Performance systems
- [ ] Texture optimization engine
- [ ] Shader system
- [ ] Full settings navigation and profiles
- [ ] Client and dedicated-server validation

See [the development roadmap](docs/ROADMAP.md) for subsystem checklists.

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

Planet Essentials is designed to use Planet Lib as its shared API foundation. Direct API usage will be wired once the library has a stable, consumable development artifact and the dependency can be resolved reproducibly in both local and CI builds.

No license has been selected yet. Do not assume permission to redistribute or reuse this project.
