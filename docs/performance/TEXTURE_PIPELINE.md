# Texture Pipeline

## Current implementation

The client texture pipeline now has four distinct stages:

1. **Discovery:** Minecraft's active client ResourceManager enumerates effective PNG identifiers. Pack priority remains owned by Minecraft. Discovery is bounded and stores metadata only.
2. **Preflight and input bounds:** The PNG signature and IHDR CRC/fields are checked before image decoding. Width, height, pixel count, bit depth, and PNG methods are validated. The encoded stream is capped by `maxEncodedBytes`; the decoder never buffers more than that configured limit.
3. **CPU decode:** PNG decoding runs through a bounded two-thread executor with a bounded 64-task queue. Decoding produces immutable CPU-side ARGB pixels. It does not touch OpenGL or the render thread.
4. **Decoded pixel cache:** An access-order LRU cache is bounded by retained pixel bytes (128 MiB by default). A resource reload advances a generation, clears cached pixels, and makes stale queued/in-flight work unable to publish into the new generation.

## Ownership and safety

- Minecraft remains the source of truth for resource-pack priority and resource identifiers.
- Input streams are closed by the decode worker after use.
- Encoded input is bounded before ImageIO sees it; dimensions/pixel count are validated before image allocation.
- Worker count, queue length, encoded byte size, dimensions, pixels, and cache bytes all have hard limits.
- Decoded pixel data is immutable to callers. Cache operations are synchronized and bounded.
- No per-frame work, filesystem scans, archive scans, packet behavior, or telemetry is added.
- The decoder never uploads textures or creates GPU objects. Vanilla rendering remains authoritative until a version-verified client texture registration/reload integration is implemented and tested.

## Explicit remaining work

- Connect lazy decoding to Minecraft's actual texture lookup/registration path without changing resource-pack priority or vanilla fallback.
- Verify the exact 1.21.11 NativeImage/DynamicTexture/render-thread APIs and add GPU upload/disposal ownership.
- Add mipmap/filtering policy only after upload integration and visual tests.
- Test resource packs, animated textures, atlases, shaders, reload races, and GPU memory cleanup in a launched client.
- Benchmark decode time, allocation, retained heap, resource reload time, and frame pacing against vanilla.
