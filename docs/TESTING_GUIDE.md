# Planet Essentials First Testing Pass

This guide is for the first manual runtime pass of the exact build artifact linked from the GitHub Actions run. It is a test checklist, not a claim that runtime behavior has already passed.

## Build under test

- Minecraft Java: 1.21.11
- Fabric Loader: 0.18.1
- Java runtime: 21
- Fabric API: 0.141.6+1.21.11
- Planet Essentials: 0.1.0-alpha.1
- Source commit: 7c1e554204b20e585e8befe3d324c4927f706c15
- Planet Lib: optional at runtime; the CI build verifies the pinned submodule checkout.

The CI run for this commit completed successfully. Its test report contains 238 tests, 0 failures, and 0 ignored tests. CI also verified the distributable JAR contains fabric.mod.json and the main mod entrypoint. CI does not launch Minecraft or prove graphics-driver, multiplayer, or third-party-mod behavior.

## Install an isolated test profile

1. Create a separate Minecraft launcher installation/profile for Minecraft 1.21.11 and Fabric Loader 0.18.1.
2. Use Java 21 for that profile.
3. Install the matching Fabric API build.
4. Put the Planet Essentials JAR in that profile's mods folder.
5. First launch with Essentials and Fabric API only. Do not add other mods, shaders, or resource packs in the first pass.
6. Keep your normal instance and worlds untouched. Use a new disposable test world for gameplay checks.
7. Launch once, close normally, and confirm the config was created. Relaunch to check persistence.

Planet Lib is optional at runtime. The first pass should test Essentials without it. A separate follow-up pass can add the exact pinned Planet Lib build and compare startup/discovery behavior.

## Pass A: startup and navigation

- [ ] Client reaches the title screen without a crash.
- [ ] Open Options from the title screen and verify the Planet settings route.
- [ ] Open and close the Planet hub repeatedly.
- [ ] Open the hub from the pause menu and confirm closing returns to the pause menu.
- [ ] Enter vanilla Minecraft settings from the hub, then return.
- [ ] Open every hub page and verify Back/Close returns to the correct parent screen.
- [ ] Test at the normal resolution and one smaller window size. Check for clipped buttons or inaccessible controls.

## Pass B: menu shortcut

- [ ] Enter a single-player test world.
- [ ] Press 0 during active, unpaused gameplay. The Planet hub should open.
- [ ] Close it and verify gameplay resumes normally.
- [ ] Open inventory, a container, chat, and the pause menu. Press 0 in each. It should not open another Planet hub over those screens.
- [ ] Pause and resume, then test 0 again.
- [ ] Change the keybind in Controls and verify the new key works and the old key no longer opens the hub.

## Pass C: settings and keybind search

- [ ] Open Search Settings from the Planet hub.
- [ ] Search by setting name, category, description, and an alias such as "combo" or "crosshair".
- [ ] Try a search with multiple words and a search with no results.
- [ ] Toggle a boolean setting and verify the displayed value changes.
- [ ] Change history capacity and verify it remains within the documented 20–1200 range.
- [ ] Close and reopen the screen; confirm the changed value persists.
- [ ] Open Search Keybinds from the hub.
- [ ] Search by action name, category, and assigned key.
- [ ] Rebind a harmless test action, restart the client, and verify persistence.
- [ ] Test Escape cancellation, Backspace unbinding, and mouse-button assignment.
- [ ] Confirm duplicate assignments are flagged and that normal Minecraft keybind behavior still works.

## Pass D: HUD and combat indicators

- [ ] Open HUD Editor.
- [ ] Toggle FPS and combo widgets independently.
- [ ] Change each widget's anchor and X/Y offsets.
- [ ] Reset one widget, then reset all widgets.
- [ ] Close and reopen the editor; verify settings persist.
- [ ] In a test world, verify the FPS widget appears only when enabled.
- [ ] In a controlled multiplayer test with another consenting player, verify the crosshair indicator only highlights a directly targeted player within melee range. Check through walls and at longer range; it should not highlight.
- [ ] In a controlled multiplayer test, land a confirmed hit and observe the outgoing combo count. Then receive confirmed hits and observe the negative incoming count.
- [ ] Check misses, blocked hits, non-player damage, disconnect, and reconnect. Record any unexpected counter changes.
- [ ] Do not use a public competitive server for first-pass debugging.

## Pass E: configuration, profiles, and diagnostics

- [ ] Toggle diagnostics master, FPS monitoring, and event collection separately. Confirm one switch does not unexpectedly disable unrelated settings.
- [ ] Change settings, close the client normally, relaunch, and confirm persistence.
- [ ] Create a disposable profile, switch to it, restart, and verify the selected profile.
- [ ] Test profile export/import using a disposable file. Verify invalid input is rejected safely.
- [ ] Check diagnostics history remains bounded at the selected capacity.
- [ ] Export diagnostics only when explicitly requested and inspect the file for unintended private information before sharing it.
- [ ] Check session elapsed time and travel distance in a normal walk. Teleport and confirm the distance tracker does not count the discontinuity as ordinary travel.

## Pass F: Prism and resource lifecycle

Prism is experimental. Keep it disabled for the baseline launch.

- [ ] Confirm Prism starts disabled by default.
- [ ] Open the Prism settings page without enabling shaders.
- [ ] If testing a shader pack, use a disposable world and record the GPU, driver, pack ID, and exact steps.
- [ ] Test enable, disable, resource reload, world exit, re-entry, and client shutdown.
- [ ] Record visual corruption, missing terrain, flicker, crashes, stale resources, or major frame-time changes.
- [ ] Do not treat a successful launch with Prism disabled as proof that terrain replacement or shader rendering works.

## Pass G: dedicated server and optional dependencies

This is a separate validation pass.

- [ ] Start a dedicated server with the mod present and confirm common initialization does not load client-only classes.
- [ ] Start a client without Planet Lib and confirm Essentials remains usable.
- [ ] Add the pinned Planet Lib build and compare startup and capability discovery.
- [ ] Test third-party mods only after the clean baseline passes. Add one mod at a time and record exact versions.

## What to capture when something fails

For each issue, record:

- Test pass and checkbox name.
- Minecraft, Loader, Fabric API, Java, Essentials, and Planet Lib versions.
- Other mods and versions, if any.
- Exact reproduction steps and expected versus observed behavior.
- Latest relevant section of `logs/latest.log` or the crash report.
- Screenshot or short capture for visual/layout problems.

Before sharing logs, remove usernames, local file paths, world names, server addresses, and other private information. Keep the failing profile intact until the issue is understood.

## Current acceptance boundary

A clean CI build and passing unit tests make this artifact suitable for the first manual test pass. They do not mean the features above have passed in-game. Mark each checkbox only after observing it on the exact environment listed above.
