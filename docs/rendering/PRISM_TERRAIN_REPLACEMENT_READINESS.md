# Live Prism terrain replacement readiness

`PrismTerrainReplacementReadiness` joins a vanilla draw descriptor to the exact current
CPU payload and corresponding GPU cache allocation. It derives the frame stage
and material ID using the same mapping as vanilla mesh capture, then requires:

1. The CPU section cache contains the exact section/layer payload.
2. The GPU cache contains an allocation under the same section/material/stage key.
3. The GPU allocation's source hash matches the current CPU payload.
4. Vertex counts and uploaded byte counts agree.

The method accesses the render-thread-confined GPU cache and must only be called
on the render thread. It does not upload missing meshes: a cache miss returns
false and leaves the caller to use vanilla fallback.

This closes the readiness predicate's data path, but not the entire replacement
path. The caller still has to supply the correct per-world caches and current
shader/frame/device readiness, and vanilla render-pass state must be reproduced
before any draw suppression can be safe.
