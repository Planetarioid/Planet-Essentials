# Planet Prism Rendering Engine (PPRE)

**Planet Prism Rendering Engine**, shortened to **Prism**, is the custom rendering architecture developed inside Planet Essentials.

Prism is an umbrella for multiple cooperating systems. It is not a shader pack, a replacement graphics API, or a claim that every planned renderer feature is already implemented.

## Goals

- Build a measurable, modular rendering system for Minecraft Java 1.21.11 on Fabric.
- Preserve Minecraft's resource ownership, pack priority, and fallback behavior.
- Separate CPU asset processing from GPU resource work.
- Keep optional features independently switchable and reversible.
- Integrate only through narrow, tested consumers rather than globally replacing vanilla rendering.
- Require visual correctness, lifecycle safety, compatibility evidence, and controlled benchmarks before accepting an optimization.

## Architecture

### 1. Asset processing
Owns resource discovery, metadata validation, bounded decoding, immutable CPU pixel data, mip generation, and bounded caches.

Current state: a source-implemented texture pipeline with unit-test and CI coverage. Client runtime behavior, resource-pack coexistence, consumer integration, and measured performance remain open.

### 2. GPU resource management
Owns GPU texture allocation, upload scheduling, sampler configuration, Minecraft-managed texture registration, generation invalidation, and disposal.

Current state: an explicit opt-in GpuTexture-backed adapter with mip uploads and lifecycle cleanup. GPU correctness and memory behavior still require a launched Minecraft client.

### 3. Shader management
Will own shader definitions, resource lifecycle, capability checks, compilation, error diagnostics, configuration, reload, and fallback.

Current state: not implemented. No shader compilation or shader injection is currently performed by Prism.

### 4. Render integration
Will expose narrow adapters to real consumers. Initial integrations must be opt-in and must not intercept all vanilla texture loads or replace atlas behavior before a complete compatibility test strategy exists.

Current state: the texture registration API is exposed, but no production rendering consumer uses it yet.

### 5. Capability and compatibility
Will identify supported GPU/backend features and renderer constraints, then select a safe supported path. Unknown capabilities must fail closed to vanilla behavior rather than guessing.

Current state: compatibility principles are documented; runtime detection and renderer matrix testing remain open.

### 6. Measurement
Every accepted optimization must have:
- A reproducible baseline and test scene.
- Visual and functional correctness checks.
- CPU time and allocation measurements where relevant.
- GPU memory and frame-time measurements where relevant.
- Reload, shutdown, and failure-path checks.
- Compatibility results for the exact Minecraft, loader, driver, and renderer combination.

No performance improvement is claimed until these measurements exist.

## Non-goals and guardrails

- Prism does not replace Vulkan, OpenGL, or Minecraft's graphics API.
- Prism does not claim to outperform Sodium, OptiFine, or other renderers without controlled comparisons.
- Prism does not globally rewrite vanilla rendering paths as a shortcut.
- Prism does not alter server-authoritative gameplay, packet behavior, or player input.
- A failed Prism path must preserve access to Minecraft's ordinary resource/render path.

## Verification gates

A Prism subsystem progresses through these gates independently:

1. Design and API contract.
2. Source implementation.
3. Unit and lifecycle tests.
4. CI build and distributable artifact verification.
5. Launched-client correctness.
6. Resource-pack and renderer compatibility.
7. Controlled performance benchmarks.

Passing an earlier gate never implies that a later gate has passed.
