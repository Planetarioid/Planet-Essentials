# Planet chunk-distance options

Planet Essentials extends the vanilla render-distance and simulation-distance integer option validators to the inclusive range **2–64 chunks**.

Implementation details:
- The client Options constructor replaces each option's OptionInstance.IntRange value set with a 2–64 range.
- The associated codec is replaced at the same time, so values in the extended range can be serialized and loaded.
- Existing values are clamped into the new supported range.
- The standard vanilla option widgets consume the updated value set, so their sliders use the expanded bounds.
- Multiplayer servers may impose a lower view-distance cap. Planet Essentials does not override server policy or promise that a client can render beyond the distance the server sends.

Verification boundary:
- This change is source-implemented for Minecraft 1.21.11 and must pass the project's Gradle/CI build.
- Client launch and visual slider verification are still required.
- A 64-chunk setting can have substantial CPU, memory, network, and GPU costs; it is a selectable upper bound, not a performance guarantee.
