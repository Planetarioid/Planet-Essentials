# Space Client

**The unified Minecraft client experience by Planet Studios.**

Space Client is a Fabric mod for Minecraft Java Edition **1.21.11**, targeting Java 21. It combines settings, HUD tools, diagnostics, profiles, and client optimization systems.

Its custom terrain and graphics initiative is named **Space Renderer**. Space Renderer is an independent rendering engine project, not a Sodium fork and not a completed renderer yet.

## Development status

The repository is in active development. CI success confirms compilation, automated tests, and distributable JAR checks only. It does not establish in-game rendering correctness, GPU performance, or third-party compatibility.

## Space Renderer

The renderer is being developed around these independently implemented systems:

- World and render-section identity with revision-safe rebuild tokens
- Bounded priority scheduling for CPU-only mesh construction
- Conservative section-connectivity traversal with frustum fallback
- Aligned shared GPU-region allocation and transactional section ownership
- Layered render lists and batched terrain submission
- Render-thread GPU uploads and safe resource retirement
- Vanilla fallback until visual parity and compatibility are demonstrated

The implementation is informed by general public rendering-engine techniques. It does not copy Sodium's restricted source code or require Sodium as a dependency.

See [Space Renderer architecture](docs/rendering/SPACE_RENDERER_ARCHITECTURE.md) for implementation stages and correctness requirements.

## Development environment

- Minecraft: 1.21.11
- Fabric Loader: 0.18.1
- Fabric API: 0.141.6+1.21.11
- Java: 21
- Gradle: 9.2.0 with Fabric Loom

Build with `gradle clean build` and run tests with `gradle test`.

## Design principles

- Correctness and compatibility before performance claims.
- Measure frame-time distributions, not just peak FPS.
- Keep client-only systems out of dedicated-server initialization.
- Avoid unnecessary work on tick and render paths.
- Preserve a safe vanilla rendering fallback.
- Never touch GPU resources from worker threads.
- Reject stale asynchronous mesh results.
- Treat unknown visibility data conservatively.
- Do not copy code whose license does not permit the intended use.
