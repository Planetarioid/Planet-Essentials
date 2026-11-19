# Prism Terrain Section Rebuild Lifecycle

`PrismTerrainSectionLifecycle` coordinates CPU-side section payload caching with asynchronous rebuild work. `beginRebuild` issues a monotonically unique revision token and invalidates the previous cached payloads for that section. A completed worker result must call `publish(token, data)`; the result is accepted only if its token still matches the current section revision. A newer rebuild, explicit invalidation, or chunk unload makes older results stale.

`unloadChunk` invalidates all section payloads across a caller-supplied inclusive vertical section range. The caller must use the dimension's actual section bounds. This API is thread-safe for CPU-side rebuild publication and cache invalidation.

This is the lifecycle contract for future chunk hooks, not an automatic hook into Minecraft's chunk builder yet. It does not infer world/dimension identity, listen to vanilla chunk events, or own GPU cache destruction. Renderer adapters must scope lifecycle instances to the relevant world and schedule GPU invalidation on the render thread.
