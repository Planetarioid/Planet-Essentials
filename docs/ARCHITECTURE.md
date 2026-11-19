# Planet Essentials Architecture

## Role and dependency direction
Planet Essentials is the client-facing control and optimization mod. It owns its settings UI and optimization engines. Planet Lib is the shared API/metadata layer and must not depend on Essentials. Other Planet mods integrate optionally and retain standalone defaults.

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
- An explicit opt-in adapter converts immutable ARGB pixels to NativeImage and registers a Minecraft-managed DynamicTexture under a collision-resistant Planet Essentials identifier. CPU pixel preparation is separate from client-thread DynamicTexture construction/registration. Only adapter-owned identifiers are released on reload/shutdown. It does not intercept vanilla loads, replace atlases, or alter resource-pack priority.
- A bounded CPU mip-chain generator now supports no-mipmap, nearest-sample, and area-weighted linear-light sRGB filtering with premultiplied alpha. Odd source dimensions are area-resampled so edge pixels contribute. Generated levels obey both a maximum-level count and a hard generated-byte budget. GPU upload currently uses only level zero; mip-level allocation/upload and sampler selection remain future integration work.
- Actual GPU allocation, visual correctness, mipmap transitions, and runtime leak checks remain unverified in a launched client; this is not yet a completed rendering optimizer.

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
