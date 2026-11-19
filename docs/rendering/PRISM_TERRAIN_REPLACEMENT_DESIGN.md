# Prism terrain replacement boundary

## Minecraft 1.21.11 render flow

LevelRenderer prepares one ChunkSectionsToRender value from visibleSections.
For each visible section it adds a draw record for every available
ChunkSectionLayer buffer and stores the corresponding section uniform data.
It submits the OPAQUE group during terrain setup, before entity submission.
Later in the same world-render pass it submits TRANSLUCENT and then TRIPWIRE
groups. ChunkSectionsToRender.renderGroup(group, sampler) opens the render
pass, binds Minecraft's default uniforms and light texture, then submits the
group's layer draw lists.

This is the correct replacement boundary: the group call has the complete
vanilla draw list for one ordered stage. Fabric's general BEFORE_ENTITIES and
BEFORE_TRANSLUCENT events are useful extension points, but are not sufficient
to decide whether an earlier vanilla group can be skipped. A takeover must
draw synchronously at the group boundary or prove that its replacement draw
has already completed before that boundary.

## Fail-open replacement contract

A future hook at ChunkSectionsToRender.renderGroup must leave vanilla
untouched unless Prism can prove all of the following for the exact group:

1. Every vanilla draw record in every layer has a matching, current Prism
   payload. Coverage is per draw record, not merely per section.
2. Every matching payload passed format conversion and revision checks.
3. All required Prism GPU buffers are uploaded and valid for the active device.
4. The selected terrain program is compiled for the active backend and has
   every required binding.
5. The replacement pass preserves the group's target, layer order,
   translucent reversal, tripwire ordering, light texture, and default uniforms.
6. The replacement draw is submitted synchronously before vanilla's method
   returns, with no dependency on a later Fabric event.

Any unknown, missing, stale, unsupported, or failed item means vanilla runs.
There must be no partial suppression: replacing only some draws in a group
would create holes. A successful takeover must suppress the vanilla group
exactly once to avoid duplicate geometry and z-fighting.

PrismTerrainReplacementGate is the pure fail-open policy primitive for this
contract. It requires non-empty complete draw coverage plus a ready program,
compatible device, and ready frame. It is not wired to a mixin and does not
suppress vanilla by itself.

## Current status

Prism captures vanilla section meshes, maintains revision-scoped CPU payloads,
uploads through an opt-in adapter, and filters against vanilla visibility.
It does not yet own the synchronous ChunkSectionsToRender group draw path.
Vanilla rendering remains authoritative. The next implementation step is to
map each vanilla RenderPass.Draw back to its section/layer payload and build
a complete per-group replacement plan before adding any cancellable hook.
