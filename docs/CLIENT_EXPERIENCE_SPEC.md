# Planet Client Experience Specification

This document records the agreed product direction for the Planet Essentials client experience. It is a planning specification, not a statement that these features are implemented or compatible today.

## Product principle

Planet Essentials owns the cohesive client experience. Planet-native systems and optional third-party providers contribute capabilities through explicit integration contracts. The player should encounter one coherent product, not a collection of unrelated mod screens.

The development sequence is deliberate:
1. Build and stabilize the functional systems, APIs, registries, data models, and service integrations.
2. Test each feature and its failure/fallback behavior in Minecraft.
3. Freeze the interaction contracts and feature set.
4. Perform the comprehensive Planet UI/visual overhaul across every screen and setting.
5. Regression-test the finished interface, then prepare and publish a release only after all release gates pass.

The current UI remains developmental until the fixed Planet visual system is completed and regression-tested. The design is fixed rather than player-theme-customizable. FancyMenu is the optional path for users who want to customize the Minecraft main menu.

## Integration ownership

Planet should provide shared infrastructure for:
- Feature discovery and organization by player-facing function.
- Typed settings pages and reusable controls.
- HUD widget registration and a single HUD editor.
- Keybind registration and a single keybind manager.
- Profiles and configuration adapters.
- Lifecycle, optional dependency detection, compatibility diagnostics, and graceful fallback.

Integration levels:
- **Native integration:** feature is implemented by Planet and uses Planet's systems.
- **Deep integration:** a third-party provider supplies the capability while Planet presents supported settings, HUD, keybind, profile, and lifecycle integration.
- **Compatibility integration:** provider coexists with Planet but is not fully absorbed into the unified experience.

Only claim the level verified for the exact versions tested. Prefer documented public APIs and versioned adapters. Avoid fragile mixins/reflection unless justified and guarded. Do not silently rewrite provider-owned configuration. Respect licenses and keep third-party mods independently installable. Do not bundle or redistribute third-party code/assets without permission.

## Uku ecosystem: explicit non-duplication rule

Uku mods are optional providers that enrich the Planet experience. Planet Essentials must launch and operate normally without them. Planet must not independently recreate features owned by the approved Uku ecosystem merely to make the base installation look more complete.

Approved integration targets:
- Armor HUD
- TotemCounter
- BetterHurtCam
- Tier Tagger
- PotionCounter
- BetterShields
- NoWheel
- HitRange
- HealthIndicator
- ChatHighlighter
- FractionalGui
- OldInput
- BlessBottleRNG
- Uku's PvP modpack
- ukulib

Where APIs permit, expose provider capabilities through Planet's feature directory, shared HUD editor, settings pages, keybind manager, and supported profiles. If the provider is absent, report the feature as unavailable/optional rather than showing a fake implementation. Resolve duplicates per capability, not by disabling an entire mod. Exact versions and APIs remain subject to verification.

Planet's own HUD roadmap should therefore focus on original client/system information and utilities, not on recreating Uku's PvP HUD elements.

## Unified HUD and hardware monitoring

One editor should manage Planet-native widgets and integrated third-party widgets that explicitly support the adapter contract. Common editor capabilities should include position, alignment, scale, opacity, visibility, colors, formatting, grouping, and preview.

Planned Planet-native information widgets:
- CPU utilization and CPU temperature where sensors are exposed.
- GPU utilization and GPU temperature where drivers/platforms expose them.
- System RAM usage and Minecraft Java heap usage.
- FPS, frame time, ping/network latency.
- Optional live graphs and compact display modes.

Sensor readings are platform-dependent. Missing or inaccessible sensors must be reported as unavailable; never substitute guessed values. Performance monitoring must be bounded and benchmarked so the overlay does not become a source of avoidable overhead.

## Skin management and FancyMenu compatibility

### Skin system
- Import/upload supported Minecraft skin files.
- Preview the player model in 3D.
- Select Classic or Slim model geometry.
- Manage multiple saved skins; apply, switch, and export them.
- Additional cosmetic management only where supported by official Minecraft account services or an explicitly compatible provider.

Changing an online Minecraft profile skin requires appropriate account authentication and service support. Planet must not ask users to paste Microsoft passwords or collect/store account credentials or tokens. Clearly distinguish local preview/import from a successfully applied account skin.

### Main menu personalization
- Planet uses a fixed, carefully art-directed visual identity. Do not build a deep player-facing editor for Planet UI themes, colors, fonts, panel opacity, animation, or home-screen layout.
- FancyMenu is the optional, user-facing tool for main-menu layout/background customization. Planet should coexist with FancyMenu rather than implement a competing main-menu editor.
- Keep Planet-owned screens visually consistent and fixed; functional settings such as HUD widget placement/visibility remain configurable.
- FancyMenu is optional, never a hard dependency. Do not bundle or redistribute it. Target its Fabric build for Minecraft 1.21.11 and verify coexistence with Planet screens in-game before making a tested compatibility claim.
- Recently played worlds and servers remain a Planet utility, not a theme-customization feature.

## Server discovery

The multiplayer experience should have two primary views:
- **My Servers:** saved servers, add/edit/remove, favorites, and join.
- **Discover Servers:** browse, search, filter, inspect details, add to My Servers, or join immediately without saving.

Server cards may show only information the directory or a live query actually provides, such as description, version, player count, status, and latency. Discovery requires a documented, reliable directory/service and appropriate moderation/abuse controls. Do not fabricate listings, promise uptime, or imply that a directory query is a definitive availability guarantee. Joining a discovered server must still respect normal Minecraft connection and server rules.

## Voice chat

Prioritize optional integration with Simple Voice Chat. The provider remains responsible for voice transport and server-side behavior. Planet may unify the interface where supported APIs permit:
- Voice status and speaking indicators.
- Nearby-player voice indicators.
- Microphone mute controls.
- Voice settings and keybinds.
- HUD widgets and notifications.

No competing voice transport should be built as part of this integration. Keep the integration modular for possible additional providers later.

## Client utilities and personalization

Planned features:
- Planet Command Center: searchable actions and settings.
- Screenshot manager: browse, preview, and organize screenshots.
- Resource-pack manager: search, sort, and preview where supported.
- Waypoint manager and client-available world information, respecting server authority.
- Item tooltip enhancements only when they do not duplicate an installed provider.
- Session statistics limited to data legitimately observable or tracked locally.
- Central notification center.
- Chat tabs, filters, timestamps, highlighting, and appearance controls, with feature-provider ownership respected.
- Full-client profiles for Planet settings and explicitly supported integration settings. Functional HUD placement and visibility remain customizable; Planet UI themes and home-screen layout do not.

## Vanilla Minecraft settings

Every vanilla setting incorporated into Planet must use Planet's redesigned reusable UI controls and layouts in the finished client. Preserve the underlying native Minecraft option behavior, values, validation, and persistence wherever possible. The temporary vanilla settings route remains a recovery path until every supported category has parity and has passed in-game testing. Do not remove that fallback early.

## Release gates

A public release is not simply the first successful build. Before publishing:
- All advertised core features must be implemented and tested at their stated level.
- Optional dependencies must fail gracefully when missing, outdated, or incompatible.
- Profiles, settings persistence, server discovery, skin workflows, HUD behavior, and voice integration must be tested in real client sessions.
- Compatibility claims must name exact versions and test outcomes.
- Performance, memory use, shutdown/reload behavior, accessibility, and fallback paths must be checked.
- The final UI overhaul must cover every Planet screen and every incorporated vanilla setting.
- The finished UI must pass a full regression pass.
- Project license, third-party licenses, notices, and redistribution permissions must be resolved.
- Changelog, versioning, documentation, and release artifacts must be ready.

Until these gates pass, mark features as planned, in progress, or unverified rather than complete.
