# Texture Pipeline

## Current implementation

The client texture pipeline has six distinct stages:

1. **Discovery:** Minecraft's active client ResourceManager enumerates effective PNG identifiers. Pack priority remains owned by Minecraft. Discovery is bounded and stores metadata only.
2. **Preflight and input bounds:** The PNG signature and IHDR CRC/fields are checked before image decoding. Width, height, pixel count, bit depth, and PNG methods are validated. The encoded stream is capped by `maxEncodedBytes`; the decoder never buffers more than that configured limit.
3. **CPU decode:** PNG decoding runs through a bounded two-thread executor with a bounded 64-task queue. Decoding produces immutable CPU-side ARGB pixels. It does not touch GPU objects or the render thread.
4. **Decoded pixel cache:** An access-order LRU cache is bounded by retained pixel bytes (128 MiB by default). A resource reload advances a generation, clears cached pixels, and makes stale queued/in-flight work unable to publish into the new generation.
5. **Minecraft resource resolution:** The public client decode entry point accepts a Minecraft Identifier, validates it as a catalog-compatible PNG texture identity, and opens the effective resource through the active ResourceManager. The resource stream is owned and closed by the worker. Minecraft's resource-pack priority remains authoritative.
6. **Opt-in GPU registration adapter:** Decoded ARGB pixels can be explicitly registered as a Minecraft-managed AbstractTexture backed by a GpuTexture. CPU mip generation and NativeImage preparation happen before dispatch to Minecraft's client executor. On the client/render thread, the adapter allocates an RGBA8 GPU texture with the generated mip count, uploads each level through CommandEncoder.writeToTexture, creates a clamped linear sampler with a bounded LOD range, and exposes the result through an AbstractTexture texture view. Generated identifiers live in the Planet Essentials namespace and are derived from a stable SHA-256 of the source identifier, so this path never overwrites vanilla/resource-pack identifiers. The TextureManager owns registered texture wrappers; the adapter tracks and releases only identifiers it created. This remains an explicit opt-in API, not a replacement for vanilla texture lookup or atlas loading.

## Mipmap generation

An opt-in mip policy can produce conventional floor-halved levels, with a strict generated-byte budget and level cap. The high-quality mode uses area-weighted filtering in linear-light sRGB with premultiplied alpha, preventing transparent-color fringes and reducing gamma-space darkening. A nearest-sample mode is available for pixel-art/data textures; mipmaps can also be disabled.

The GPU adapter allocates and uploads all generated CPU levels and configures a matching LOD range. This is source/build verified only: actual backend mip sampling, transitions, and visual output still require a launched-client test. Do not infer trilinear filtering or a performance gain from the presence of mip levels alone.

## Ownership and safety

- Minecraft remains the source of truth for resource-pack priority and resource identifiers.
- Input streams are closed by the decode worker after use.
- Encoded input is bounded before ImageIO sees it; dimensions/pixel count are validated before image allocation.
- Worker count, queue length, encoded byte size, dimensions, pixels, and cache bytes all have hard limits.
- Decoded pixel data is immutable to callers. Cache operations are synchronized and bounded.
- No per-frame work, filesystem scans, archive scans, packet behavior, or telemetry is added.
- The CPU decoder never uploads textures or creates GPU objects. The separate adapter creates Minecraft-managed GpuTexture-backed AbstractTexture objects only when explicitly requested; it does not intercept vanilla texture loads or atlas creation.
- GPU allocation, mip upload, sampler creation, and texture registration run through the Minecraft client executor and assert render-thread ownership before touching the GPU.
- Queued registrations are tracked. Resource reload invalidates the generation, cancels pending registrations, closes their NativeImages exactly once, and releases only adapter-owned identifiers. Shutdown permanently closes the registrar and rejects later registrations.
- Registration failures close partially allocated GPU resources and CPU upload images. Cancellation is synchronized with upload execution so NativeImages are not closed while the upload loop is reading them.
- Actual GPU allocation, visual correctness, thread scheduling, reload behavior, and leak behavior still require a launched-client test.

## Explicit remaining work

- Integrate the opt-in adapter into an actual texture consumer only after in-game validation. Atlas interception remains intentionally out of scope for this first adapter.
- Launch Minecraft 1.21.11 and verify NativeImage pixel colors, texture binding, reload disposal, GPU resource cleanup, and mip-level sampling.
- Add automated tests for resource-pack priority/coexistence and vanilla fallback behavior using real ResourceManager fixtures or a suitable client test harness.
- Test resource packs, animated textures, atlases, shaders, reload races, mip transitions, and GPU memory cleanup in a launched client.
- Benchmark decode time, allocation, retained heap, resource reload time, and frame pacing against vanilla.

## Verification boundary

CI currently verifies compilation, unit tests, distributable JAR contents, and artifact creation. It does not launch Minecraft, exercise the GPU backend, prove resource-pack compatibility, or measure performance. No FPS or memory improvement is claimed until controlled benchmarks are run.
