# Compatibility Test Matrix

## Purpose and status

This matrix defines the combinations and behaviors that must be checked before Planet Essentials can claim compatibility. It is a test plan and reporting template, not evidence that the listed combinations have already been tested.

**Current verified compatibility results: none beyond the automated build/test contract.** CI currently verifies compilation, unit tests, distributable JAR contents, and artifact creation. It does not launch Minecraft or validate third-party mod coexistence. All runtime combinations below begin as **Not tested** until a real test report records the exact versions and outcome.

## 1. Result vocabulary

Use only these result states:

- **Not tested:** no test has been run for this exact combination.
- **Blocked:** the combination cannot currently be tested; record why.
- **Pass:** the exact versions and scenarios passed the defined checks.
- **Pass with limitations:** the combination works, but a reproducible limitation is documented.
- **Fail:** a reproducible incompatibility or regression exists.
- **Retest required:** a relevant dependency, mixin, rendering pipeline, or integration changed after the last pass.

A result applies only to the exact Minecraft, Loader, Fabric API, Essentials, Planet Lib, and companion-mod versions recorded. Do not generalize one passing version to an entire mod family or later releases.

## 2. Core platform matrix

| ID | Environment | Required checks | Initial status |
| --- | --- | --- | --- |
| P-01 | Minecraft 1.21.11 + Fabric Loader 0.18.1 + Java 21, Essentials only | Clean launch, main menu, Options interception, Essentials navigation, save/restart | Not tested in-game |
| P-02 | P-01 plus pinned Planet Lib | Bridge discovery, API version, capability snapshot, normal shutdown | Not tested in-game |
| P-03 | P-01 with Planet Lib absent | Essentials loads standalone; no missing-class crash; no accidental hard dependency | Not tested in-game |
| P-04 | P-01 plus an unsupported or incompatible Planet Lib API fixture | Soft failure, clear diagnostics, no partial integration state | Not tested in-game |
| P-05 | Dedicated server with Essentials present | Common initialization remains safe; no client-class loading | Not tested |
| P-06 | Client and dedicated server with Essentials absent | No changes to vanilla behavior from Essentials | Not tested |

The declared project target remains Minecraft 1.21.11, Fabric Loader 0.18.1, and Java 21. Other versions are separate matrix rows and must not be implied compatible.

## 3. Planet Studios integration matrix

Only add a row when the corresponding mod has a real build that can be tested. Planned concepts are not installable compatibility targets.

| ID | Combination | Required checks | Initial status |
| --- | --- | --- | --- |
| S-01 | Essentials + Planet Lib only | API bridge and discovery snapshot | Automated contract tests exist; in-game test not run |
| S-02 | Essentials + each released Planet mod, one at a time | Discovery, declared capabilities, settings ownership, feature lifecycle, standalone defaults | Not tested; dependent mods not yet available |
| S-03 | Essentials + multiple released Planet mods | Duplicate capability resolution, deterministic provider selection, reversible overrides | Not tested; dependent mods not yet available |
| S-04 | Planet mod without Essentials | Mod retains its standalone defaults and does not require Essentials | Not tested; dependent mods not yet available |
| S-05 | Planet mod + Essentials removed after prior use | No stale integration state or destructive config changes | Not tested; dependent mods not yet available |

For each mod, record its repository commit and release version, declared capabilities, provider IDs, settings/API version, and whether the integration is native, tested coexistence, conditional, or detect-only. Never infer integration support from the mod being discoverable.

## 4. Third-party coexistence matrix

Test only real, current mod builds on the target Minecraft/Fabric versions. Select representative mods from each category when available.

| Category | Interaction surface | Required checks | Initial status |
| --- | --- | --- | --- |
| Settings/UI mods | Options screen, screen replacement, menu navigation | No redirect loops, lost buttons, broken return path, or screen crashes | Not tested |
| Rendering/performance mods | Renderer hooks, render events, resource reload | Clean launch, stable rendering, no duplicate ownership, no visual corruption | Not tested |
| Shader pipeline mods | Shader loading, pipeline ownership, GPU capability paths | Shader enable/disable, reload, fallback, error reporting | Not tested; shader system not implemented |
| Resource-pack/content mods | Resource manager, texture/model/sound assets | Pack enable/disable, reload, fallback, no stale cached assets | Not tested |
| Configuration/profile mods | Config files, options persistence | No file overwrite, schema collision, or profile data loss | Not tested |
| Diagnostics/profiling mods | Tick/render instrumentation | No recursion, duplicate sampling assumptions, or significant overhead | Not tested |
| Client/server utility mods | Common initialization and lifecycle | No dedicated-server classloading failure; no altered server authority | Not tested |

Do not publish a named compatibility claim until the exact mod version, test setup, and result are recorded. A mod not tested should be reported as unknown, not incompatible.

## 5. Essentials feature regression matrix

Run applicable rows with the feature enabled and disabled. For settings that persist, repeat after a full client restart.

| Area | Scenarios | Pass criteria |
| --- | --- | --- |
| Options-screen integration | Open Options from title screen and in-world; enter vanilla settings; return; close; reopen | Correct screen each time; no redirect loop or lost navigation |
| Configuration | Fresh config, existing config, malformed config, migration from schemas 0-3 | Defaults and migrations correct; malformed data preserved; no silent overwrite |
| Profiles | Create, save, load, delete, import, export, restart | Correct profile restored; invalid imports rejected safely |
| Diagnostics | Master switch, FPS switch, event switch, history clear, export | Each switch gates only its own collector; bounded history; export only on explicit action |
| Mod discovery | Planet Lib absent/present; empty and populated registries | Standalone behavior without Lib; immutable and accurate snapshot with Lib |
| Feature priority | Automatic selection, manual override, reset, provider failure | Per-capability selection; provider lifecycle called; rollback works |
| Resource lifecycle | Reload, disconnect, reconnect, shutdown | No stale references, leaked tasks, or unbounded retained resources |
| Dedicated-server safety | Dedicated server startup with Essentials | No client-only class loading or client UI initialization |

## 6. Test execution order

1. Build and unit-test the exact commit in CI.
2. Test clean client launch with Essentials only.
3. Test Planet Lib present and absent.
4. Test the dedicated-server classloading path.
5. Exercise settings, profiles, diagnostics, and provider-priority lifecycle.
6. Add each available Planet mod individually, then test supported combinations.
7. Add representative third-party mods by interaction category.
8. Run relevant performance A/B tests only after correctness passes.
9. Record failures, logs, exact versions, and reproduction steps; fix and retest affected rows.

When a test fails, preserve the failing environment and logs before changing versions. Change one variable at a time. Do not resolve a conflict by silently disabling another mod's feature or changing server-authoritative behavior.

## 7. Compatibility claim levels

Use these terms consistently in documentation and UI:

- **Native integration verified:** Essentials and the other Planet mod exchange the documented API/capability data and the integrated feature behavior was tested.
- **Tested coexistence:** both mods work together in the recorded environment, but do not exchange a dedicated integration.
- **Conditional:** works only with a documented setting, dependency, or configuration.
- **Detect-only:** Essentials can identify the mod or its declared metadata, but no integrated behavior has been verified.
- **Unknown:** no applicable runtime test has been completed.
- **Known issue:** a reproducible failure exists for the recorded versions.

A successful build, metadata discovery, or dependency resolution alone is not a runtime compatibility pass.

## 8. Test report template

```text
Test ID:
Date:
Result:
Minecraft:
Fabric Loader:
Fabric API:
Java runtime/vendor:
Planet Essentials commit/version:
Planet Lib commit/version or absent:
Other mods and exact versions:
Operating system:
GPU/driver (if rendering-related):
Resource packs / shaders:
Client or dedicated server:
Fresh profile or existing profile:
Steps performed:
Expected result:
Observed result:
Relevant logs / crash report (reviewed for private data):
Known limitations:
Retest trigger:
Tester:
```

Review logs before sharing. Remove usernames, local paths, world names, server addresses, account identifiers, and other private information.

## Current conclusion

The compatibility test matrix and reporting rules are defined. Runtime compatibility is still unverified. Keep every row at Not tested until the exact combination has been exercised and its result recorded.
