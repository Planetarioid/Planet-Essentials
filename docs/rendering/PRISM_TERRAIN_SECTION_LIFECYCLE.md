# Prism Terrain Section Lifecycle

## Implemented

- `PrismTerrainSectionLifecycle` issues monotonically increasing revision tokens per section, rejects stale asynchronous results, and invalidates CPU payloads on rebuild or unload.
- `PrismTerrainChunkLifecycleEvents` registers Fabric 1.21.11 client chunk load/unload and disconnect callbacks.
- Lifecycle state and the 32 MiB CPU section cache are isolated by `ClientLevel` object identity. Coordinates from separate dimensions or world sessions therefore cannot share a cache.
- On chunk unload, the bridge invalidates every vertical section from `getMinSectionY()` through `getMaxSectionY()`, removes matching meshes from the currently published frame, and schedules registered GPU invalidators on the render thread.
- On disconnect, per-world CPU caches are cleared and GPU invalidators receive a world-clear callback.
- Renderer adapters can register a `GpuInvalidator` without transferring cache ownership to this module. Callback failures are isolated and logged.

## Deliberate boundaries

- This is actual chunk load/unload integration, not a vanilla chunk rebuild hook. Fabric's client chunk events do not expose the internal section mesher's completed vertex payload or a reliable rebuild token.
- `PrismTerrainRendererAdapter` can register a concrete GPU invalidator for its per-ClientLevel `PrismTerrainGpuCache`. The adapter is opt-in and no provider is registered by default.
- The bridge does not replace vanilla terrain rendering, infer dimension identifiers from coordinates, or upload terrain itself.
- The opt-in adapter registers its invalidator, scopes each GPU cache to the supplied `ClientLevel`, and performs cache destruction on the render thread. Vanilla chunk mesh extraction and rebuild integration remain separate work.

## Verification

Unit tests cover revision supersession, stale-result rejection, CPU cache invalidation, chunk-range invalidation, and invalid input. CI compilation/test results are tracked separately from in-game GPU validation.
