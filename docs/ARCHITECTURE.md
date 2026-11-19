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

## Texture discovery foundation
- Discovery uses Minecraft's active client ResourceManager during client resource reload.
- ResourceManager remains authoritative for pack priority; no filesystem or archive scanning.
- The catalog stores validated identifiers and lightweight metadata only. It does not decode image pixels or intercept rendering.
- Entries are bounded, immutable to readers, atomically replaced on successful reload, and cleared on shutdown.
- A failed discovery retains the last known-good snapshot.
- Texture identities validate namespace syntax, exact namespace/path/resource-ID agreement, normalized resource paths, the textures/ root, and PNG extension/format.
- The initial decode preflight reads only the fixed PNG signature and IHDR, validates its CRC and PNG encoding fields, and enforces configurable dimension/pixel/encoded-size policy values before a future decoder is allowed to run. It does not decode pixels or claim to enforce the encoded-size limit on a stream yet.
- Coarse categories (GUI, block, entity, item, particle, environment, mob effect, other) are inferred from resource paths for diagnostics and later policy selection only; they do not imply rendering behavior.
- This is discovery infrastructure, not a texture optimizer, decoder, cache, mipmap policy, or rendering engine.

## Lifecycle and safety
- Common initialization remains dedicated-server safe; client-only work stays behind client entrypoints.
- No texture decode, shader compile, or profiling on each tick.
- Diagnostics are bounded local observations only; no telemetry or player/server/account identifiers.
- Resource-backed caches must be invalidated on reload and released on shutdown.
- Minecraft retains texture upload, rendering, and resource fallback ownership until an explicit, separately tested integration stage exists.

## Performance and correctness
- Profile before optimizing. Every optimization requires correctness checks and controlled before/after benchmarks.
- Baseline and benchmark protocols define capture conditions and acceptance gates; documentation is not a measured result.
- CPU/GPU investigations remain deferred until actual recordings are available.
- Memory/cache constraints and audio/particle/animation/network review boundaries are documented separately.
- Network behavior remains Minecraft-owned; no packet manipulation or server-authoritative behavior changes.
- Compatibility rows remain unknown until exact runtime combinations are tested.

## Verification levels
Track source implementation, unit tests, CI build, client runtime, dedicated-server runtime, compatibility, and benchmark verification separately.
