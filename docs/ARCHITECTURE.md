# Planet Essentials Architecture

## Role and dependency direction
Planet Essentials is the client-facing control and optimization mod. It owns its settings UI, diagnostics, integration coordination, and optimization engines. Planet Lib is the shared API/metadata layer and must not depend on Essentials. Other Planet mods integrate optionally and retain standalone defaults.

## Planet Prism Rendering Engine (PPRE)
**Planet Prism Rendering Engine**, abbreviated **Prism** or **PPRE**, is the internal name for Planet Essentials' custom rendering architecture. It is an umbrella system, not a claim that every planned backend is implemented.

Prism is divided into independently testable layers:
- **Asset layer:** resource discovery, metadata validation, bounded decode, immutable CPU pixels, and cache policy.
- **GPU resource layer:** explicit GPU allocation, uploads, sampler configuration, registration, ownership, and disposal.
- **Shader layer:** shader definitions, capability checks, compilation, diagnostics, reload, and safe fallback.
- **Render integration layer:** narrow consumer adapters that opt into Prism without globally replacing vanilla rendering or atlas behavior.
- **Compatibility layer:** resource-pack precedence, renderer coexistence, capability negotiation, and fallback decisions.
- **Measurement layer:** correctness fixtures, lifecycle tests, allocation accounting, and controlled performance benchmarks.

Layer boundaries are intentional. CPU decode must not access GPU state. GPU work must run on the supported client/render execution context. A failed optimized path must return control to Minecraft's ordinary resource/render path. Prism must not synthesize player input, alter server-authoritative behavior, or claim performance gains without measurements.

## Discovery and integration
- Mod discovery uses Planet Lib's immutable snapshot of Fabric-loaded mods and declared capabilities.
- If Planet Lib is absent, Essentials remains standalone without loading Lib API classes.
- No mods-folder scanning, archive inspection, or arbitrary third-party config access.
- The typed Essentials bridge uses API version 1; integration descriptors are validated immutable snapshots.

## Configuration and feature coordination
- Diagnostics master switch gates Essentials diagnostics. FPS and event collection have independent persisted switches.
- Disabled systems stop creating new samples/events while retaining existing bounded history.
- Settings are validated, registry-backed, profile-persisted, and changed through the Essentials UI.
- Future engine controls are added only when wired to real activation/deactivation.
- Feature priority is per capability, reversible, and applied only through provider-owned lifecycle callbacks.

## Texture pipeline
- Discovery uses Minecraft's active client ResourceManager; Minecraft retains resource-pack priority and lookup authority.
- The metadata catalog is bounded, deterministic, immutable to readers, atomically replaced, and cleared at client shutdown.
- Texture identifiers validate namespace, resource ID, normalized path, textures/ root, and PNG format.
- PNG signature, IHDR CRC, legal bit-depth/color-type combinations, encoding methods, dimensions, and pixel count are checked before ImageIO decode.
- Encoded bytes are hard-capped before decode. The decoder uses a bounded worker pool (2 workers and a 64-job queue) and produces immutable CPU-side ARGB pixels only.
- The decoded-pixel LRU is bounded by retained bytes (128 MiB default). Reload increments a generation and invalidates stale work/cache entries; client shutdown stops workers and clears pixels.
- Effective texture streams are resolved through Minecraft's active ResourceManager, retaining resource-pack priority and vanilla resource ownership.
- An explicit opt-in adapter converts immutable ARGB pixels and generated mip levels to NativeImage, allocates an RGBA8 GpuTexture with the corresponding mip count, uploads each level through CommandEncoder, creates a clamped linear sampler, and registers a Minecraft-managed AbstractTexture view under a collision-resistant Planet Essentials identifier. CPU preparation is separate from GPU work. Only adapter-owned identifiers are released on reload/shutdown. It does not intercept vanilla loads, replace atlases, or alter resource-pack priority.
- A bounded CPU mip-chain generator supports no-mipmap, nearest-sample, and area-weighted linear-light sRGB filtering with premultiplied alpha. Generated levels obey both a maximum-level count and a hard generated-byte budget.
- Source implementation, unit tests, and CI build verification exist. Actual GPU allocation, visual correctness, mip transitions, consumer integration, runtime resource cleanup, and performance remain unverified in a launched client.

## Lifecycle and safety
- Common initialization remains dedicated-server safe; client-only work stays behind client entrypoints.
- No texture decode, shader compile, or profiling on each tick.
- Diagnostics are bounded local observations only; no telemetry or player/server/account identifiers.
- Resource-backed caches are invalidated on reload and released on shutdown.
- Minecraft retains rendering and fallback ownership until an explicit, separately tested integration stage exists.

## Performance and correctness
- Profile before optimizing. Every optimization requires correctness checks and controlled before/after benchmarks.
- Baseline and benchmark protocols define capture conditions and acceptance gates; documentation is not a measured result.
- CPU/GPU investigations remain deferred until actual recordings are available.
- Memory/cache constraints and audio/particle/animation/network review boundaries are documented separately.
- Network behavior remains Minecraft-owned; no packet manipulation or server-authoritative behavior changes.
- Compatibility rows remain unknown until exact runtime combinations are tested.

## Verification levels
Track source implementation, unit tests, CI build, client runtime, dedicated-server runtime, compatibility, and benchmark verification separately.
