# Texture Pipeline

## Current implementation

The client texture pipeline now has five distinct stages:

1. **Discovery:** Minecraft's active client ResourceManager enumerates effective PNG identifiers. Pack priority remains owned by Minecraft. Discovery is bounded and stores metadata only.
2. **Preflight and input bounds:** The PNG signature and IHDR CRC/fields are checked before image decoding. Width, height, pixel count, bit depth, and PNG methods are validated. The encoded stream is capped by `maxEncodedBytes`; the decoder never buffers more than that configured limit.
3. **CPU decode:** PNG decoding runs through a bounded two-thread executor with a bounded 64-task queue. Decoding produces immutable CPU-side ARGB pixels. It does not touch OpenGL or the render thread.
4. **Decoded pixel cache:** An access-order LRU cache is bounded by retained pixel bytes (128 MiB by default). A resource reload advances a generation, clears cached pixels, and makes stale queued/in-flight work unable to publish into the new generation.
5. **Minecraft resource resolution:** The public client decode entry point accepts a Minecraft Identifier, validates it as a catalog-compatible PNG texture identity, and opens the effective resource through the active ResourceManager. The resource stream is owned and closed by the worker. Minecraft's resource-pack priority remains authoritative.

6. **Opt-in GPU registration adapter:** Decoded ARGB pixels can be explicitly registered as a Minecraft-managed DynamicTexture. Native pixel allocation/fill is performed before dispatching DynamicTexture creation and TextureManager registration to the client thread. Generated identifiers live in the Planet Essentials namespace and are derived from a stable SHA-256 of the source identifier, so this path never overwrites vanilla/resource-pack identifiers. The adapter tracks only textures it owns and releases them on resource reload and client shutdown. This is an integration API, not a replacement for vanilla texture lookup or atlas loading.

7. **Bounded CPU mip-chain generation:** An opt-in mip policy can produce conventional floor-halved levels, with a strict generated-byte budget and level cap. The high-quality mode uses area-weighted filtering in linear-light sRGB with premultiplied alpha, preventing transparent-color fringes and reducing gamma-space darkening. A nearest-sample mode is available for pixel-art/data textures; mipmaps can also be disabled. This currently produces immutable CPU levels only. The DynamicTexture adapter still registers level zero, so GPU mip-level upload and sampler selection are not yet wired.

## Ownership and safety

- Minecraft remains the source of truth for resource-pack priority and resource identifiers.
- Input streams are closed by the decode worker after use.
- Encoded input is bounded before ImageIO sees it; dimensions/pixel count are validated before image allocation.
- Worker count, queue length, encoded byte size, dimensions, pixels, and cache bytes all have hard limits.
- Decoded pixel data is immutable to callers. Cache operations are synchronized and bounded.
- No per-frame work, filesystem scans, archive scans, packet behavior, or telemetry is added.
- The CPU decoder never uploads textures or creates GPU objects. The separate adapter creates Minecraft-managed DynamicTexture objects only when explicitly requested; it does not intercept vanilla texture loads or atlas creation.
- DynamicTexture construction/registration and release are dispatched to the Minecraft client thread. The TextureManager owns registered texture objects; the adapter releases only its own generated identifiers. A monotonic lifecycle generation invalidates queued registrations on reload, and permanent shutdown rejects any later registration. Failed registration closes the allocated texture/image.
- GPU allocation, visual correctness, and reload behavior still require a launched-client test.

## Explicit remaining work

- Integrate the explicit DynamicTexture adapter into an actual consumer only after in-game validation. Atlas interception remains intentionally out of scope for this first adapter.
- Launch Minecraft 1.21.11 and verify NativeImage pixel colors, texture binding, reload disposal, and GPU resource cleanup. The lifecycle race guard is covered by pure unit tests, but Minecraft-thread scheduling and actual GPU cleanup still require a launched-client test.
- Wire the generated CPU mip chain into a verified GPU texture allocation/upload path and sampler policy; current DynamicTexture registration still uploads only level zero.
- Test resource packs, animated textures, atlases, shaders, reload races, mip transitions, and GPU memory cleanup in a launched client.
- Benchmark decode time, allocation, retained heap, resource reload time, and frame pacing against vanilla.
