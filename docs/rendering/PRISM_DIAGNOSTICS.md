# Prism Diagnostics Snapshot

Prism exposes an immutable diagnostics snapshot for settings screens, support reports, and future automated compatibility checks.

The snapshot includes:

- Whether Prism's selected shader pack is currently enabled.
- Selected and compiled pack IDs, so a pending or failed selection is distinguishable from an active pack.
- Resource discovery generation, pack count, and manifest errors.
- GPU pipeline generation, backend name, compiled program count, source hashes, and last compile failure.
- Detected renderer environment and whether an explicit adapter is available through the compatibility snapshot.
- Number of passes in the active world post-process graph.

The snapshot is read-only and does not trigger resource reloads or GPU compilation. It intentionally does not claim GPU driver support, visual correctness, frame-time impact, or compatibility certification. Those require runtime instrumentation and in-game testing.
