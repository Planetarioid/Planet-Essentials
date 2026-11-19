# Planet Essentials Architecture

## Role

Planet Essentials is the central client-facing control and optimization mod in the Planet Studios ecosystem. It owns its own settings UI and optimization engines. Planet Lib remains the shared API and metadata layer.

## Dependency direction

- Planet Essentials depends on Planet Lib.
- Planet Lib must not depend on Planet Essentials.
- Other Planet mods may integrate with Essentials through the optional API bridge in Planet Lib.
- Other Planet mods must retain standalone defaults when Essentials is absent.

## Mod discovery
- Discovery uses Planet Lib's immutable snapshot of mods loaded by Fabric Loader.
- Planet Essentials filters Planet Studios mods using the library's explicit identity metadata.
- Declared capabilities are read from the shared capability registry; unknown capabilities remain unknown.
- If Planet Lib is absent, discovery returns an empty standalone snapshot and does not load Planet Lib API classes.
- No mods-directory scanning, archive inspection, or arbitrary third-party configuration access is permitted.

## Planet Lib integration
- Planet Lib is pinned as a Git submodule and built as a sibling Gradle project.
- Essentials compiles against Planet Lib's named-elements API but does not package it or require it at runtime; Planet Lib remains optional.
- Essentials exposes the typed `planet_essentials_api` Fabric entrypoint at API version 1.
- Integration descriptors are immutable, validated, deduplicated by mod ID, and exposed as read-only snapshots.
- Missing Planet Lib leaves Essentials in standalone mode. Planet Lib owns API discovery and version negotiation, and must treat unavailable or incompatible Essentials APIs as soft failures.

## Module boundaries

Planned internal areas:

- `config`: settings models, persistence, profiles, migrations
- `integration`: Planet Lib bridge and per-mod integration lifecycle
- `features`: feature/provider coordination and preference presentation
- `diagnostics`: bounded diagnostics and user-visible reporting
- `performance`: measured optimization systems and benchmark support
- `texture`: dedicated texture optimization engine
- `shader`: shader lifecycle, compilation, capability checks, fallback
- `client`: client-only screens and lifecycle integration

These are architectural boundaries, not a commitment to create empty packages before they are needed.

## Lifecycle rules

- Common initialization must remain safe on a dedicated server.
- Client-only classes must be isolated behind Fabric client entrypoints or client-only lifecycle registration.
- No expensive scanning, shader compilation, texture decoding, or profiling should occur on every tick.
- Resource reload and shutdown must release owned resources.
- Optional integrations must fail softly and expose a diagnostic reason.

## Performance rules

- Profile before optimizing.
- Avoid per-frame allocations and repeated lookups in hot paths.
- Cache only when invalidation and resource ownership are explicit.
- Do not trade correctness, server validation, or compatibility for responsiveness.
- Every optimization needs a correctness check and a before/after benchmark.

## Configuration rules

- Planet Essentials owns its own configuration.
- Other Planet mods own their settings unless they explicitly register an integration.
- Never silently rewrite another mod's configuration files.
- Feature priority overrides are per capability, persistent, and reversible.
- Runtime provider changes occur only through the provider's explicit Planet Lib lifecycle contract. Essentials never disables a mod or takes ownership of provider resources.
- Missing or invalid settings must fall back to documented defaults.

## Verification levels

Track separately:
1. Source implementation
2. Unit-test verification
3. CI build verification
4. Client runtime verification
5. Dedicated-server verification
6. Compatibility verification
7. Benchmark verification
