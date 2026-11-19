# Prism Shader System

## Status
This implementation establishes the backend-neutral shader program lifecycle: validated immutable vertex/fragment sources, stable content hashes, backend capability snapshots, a compiler/backend SPI, transactional program-set reload, failure diagnostics, and deterministic handle disposal.

This is **not yet an Iris-equivalent shader-pack runtime**. No concrete Minecraft GPU compiler/backend, shader-pack format, world-render integration, uniform/buffer/framebuffer binding system, or visual/runtime validation is included yet. The SPI is the seam for those components, not proof they already exist.

## Contracts
- Exactly one vertex and one fragment source per program.
- Namespaced resource identifiers and a 1 MiB UTF-8 limit per stage.
- Immutable source maps and SHA-256 content identity.
- Concrete backend reports actual device capabilities and compiles on its required graphics-context thread.
- A reload compiles the entire candidate set before publication. Failure closes candidates and preserves the previous active generation.
- Explicit invalidation and shutdown close active handles. One disposal failure does not prevent attempts to close remaining handles.
- Unknown capabilities remain conservative; callers may not infer support.
- Vanilla rendering must remain active until a candidate pipeline is fully compiled and validated.

## Threading
Invoke reload only on the graphics backend's required context thread. A Minecraft implementation must prove the correct scheduling boundary rather than assuming a generic client executor is a render-thread callback.

## Remaining implementation
1. Concrete Minecraft 1.21.11 graphics backend and render-thread scheduler.
2. Bounded shader resource-pack discovery, manifests, versioning, pack priority, and validation.
3. Uniform, sampler, buffer, framebuffer, and render-target binding model.
4. Real GPU/driver capability detection and backend negotiation.
5. Defined world-render stages and narrow integration points.
6. Vanilla fallback and compatibility with resource packs, Sodium, Iris, and other renderers.
7. Visual regression, GPU memory, reload/leak, and controlled performance tests.

## Performance claims
Prism does not claim to outperform Iris. Iris is a mature shader-pack ecosystem with extensive Minecraft rendering integration. Comparisons require the same shader packs, scenes, GPU, driver, render distance, and settings. Do not copy Iris code or shader-pack assets.
