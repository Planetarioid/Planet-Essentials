# Planet Essentials Development Roadmap

Statuses: [ ] not started, [~] in progress / needs runtime validation, [x] implemented and verified at the stated level.

## 0. Foundation
- [x] Fabric 1.21.11 / Java 21 foundation, pinned Planet Lib dependency, CI build/test and JAR verification
- [ ] Client and dedicated-server launch validation

## 1. Planet Lib and core systems
- [x] Typed Essentials API, contract validation, loaded-mod/capability discovery, standalone behavior, integration tests
- [x] Typed configuration, validation, migrations, atomic persistence, profiles, import/export
- [x] Mod discovery, capability selection, reversible integration lifecycle
- [x] Bounded diagnostics event collection and explicit export
- [~] FPS/runtime overhead, JFR/GPU profiling, and benchmark verification
- [ ] Unified feature/provider registry organized by player-facing function rather than source mod
- [ ] Unified settings, HUD-widget, keybind, lifecycle, and configuration adapter contracts
- [ ] Versioned full-client profiles covering Planet settings and supported integration settings without silently rewriting provider-owned files
- [ ] Deterministic provider selection and duplicate-feature handling across native and integrated providers
- [ ] Optional dependency detection, graceful degradation, and actionable compatibility diagnostics

## 2. Planet Prism shader engine
- [x] Immutable validated shader source bundles and SHA-256 identity
- [x] Bounded schema-1 manifest, shader pack discovery, path validation, and source budgets
- [x] Minecraft 1.21.11 GpuDevice pipeline compilation backend
- [x] Transactional shader reload, rollback, and resource lifecycle
- [x] Uniform-buffer, texel-buffer, and sampler declarations and typed binding validation
- [x] World render-stage dispatch, fullscreen draw executor, and render targets
- [x] Multi-pass post-processing graph, validation, target reuse, and final composite
- [x] Shader enable/disable and pack selection UI
- [ ] Actual GPU/driver capability detection
- [ ] In-game shader correctness and performance validation

## 3. Prism terrain rendering

### Implemented architecture
- [x] Bounded CPU terrain mesh builder and immutable extraction-to-draw handoff
- [x] Versioned full-attribute terrain payload and Minecraft vertex-format mapping
- [x] Render-thread GPU terrain upload, owned allocation lifecycle, and bounded LRU GPU cache
- [x] Bounded CPU section cache and terrain-specific shader/draw executor
- [x] Frame extraction and BEFORE_ENTITIES / BEFORE_TRANSLUCENT scheduling for cooperating providers
- [x] Revision-token section rebuild lifecycle and per-world CPU caches
- [x] Vanilla SOLID, CUTOUT, TRANSLUCENT, and TRIPWIRE SectionCompiler mesh capture
- [x] Camera-relative section transforms and section-origin GPU uniforms
- [x] Active frame-frustum snapshot and section AABB filtering
- [x] Vanilla SectionOcclusionGraph visible-section snapshot
- [x] Vanilla draw identity index mapping to section coordinates, layer, and layer-local order
- [x] Fail-open replacement gate and all-or-nothing per-layer replacement plans
- [x] Live CPU-payload to GPU-allocation readiness check, including source-hash and size parity
- [x] Per-ClientLevel replacement coordinator, GPU cache preparation, shader-generation consistency check, and chunk/world invalidation

### Still required for vanilla terrain takeover
- [x] Define and test a fail-closed vanilla render-pass state completeness contract
- [x] Instrument the exact ChunkSectionsToRender.renderGroup boundary and capture group, sampler/atlas identity, ordered draw-list snapshots, index budget, and chunk-section uniform slices
- [x] Snapshot vanilla output target/attachments, block atlas and lightmap bindings, default uniform resources, model-view matrix, per-layer pipeline identities/state, and exact vanilla draw order
- [x] Convert observed vanilla snapshot facts into fail-closed render-state evidence and report missing facets
- [x] Connect captured render-group snapshots directly to replacement-plan preparation without suppressing vanilla draws
- [x] Capture source-backed OpenGL viewport/scissor defaults, no-stencil attachment state, and BLOCK-format overlay absence; unknown backends remain fail-closed
- [x] Add Prism-owned pass opening and exact replay of captured default uniforms and declared vanilla samplers
- [x] Clone and precompile Prism shader pipelines with captured vanilla fixed-function layer state while retaining the Prism V1 vertex format
- [x] Add draw-executor support for state-matched terrain pipeline variants
- [ ] Runtime-validate viewport/scissor defaults and attachment behavior against the active GPU backend
- [ ] Integrate pass replay and pipeline variants at the synchronous vanilla render-group boundary
- [ ] Validate draw index mapping against actual 1.21.11 runtime behavior
- [ ] Synchronous replacement hook at the vanilla render-group boundary calling the coordinator
- [ ] Whole-group vanilla fallback on any missing payload, GPU allocation, shader, state, or frame
- [ ] Runtime visual parity for opaque, cutout, translucent, and tripwire geometry
- [ ] GPU memory, resource lifetime, and performance benchmarks

### Terrain correctness and optimization
- [x] Bounded extraction frame budgets and fail-safe truncation
- [x] Vanilla translucent quad-centroid capture and camera-relative far-to-near index sorting
- [x] GPU-fenced temporary translucent index ring and tripwire-after-translucent grouping
- [~] In-game verification of translucent sorting and terrain-layer parity

## 4. Prism texture subsystem
- [x] Bounded resource discovery, PNG validation, async decode, immutable pixel cache, reload/shutdown lifecycle, CPU mip generation
- [x] Opt-in GPU texture adapter, mip uploads, pending NativeImage cleanup, vanilla identifier fallback API
- [~] Client GPU validation and production consumer integration
- [ ] Resource-pack coexistence, visual correctness, leak checks, and performance benchmarks

## 5. Planet client experience and settings
- [~] Main settings hub and Prism shader panel
- [ ] Migrate every supported vanilla settings category into Planet-owned pages while preserving native Minecraft option behavior and persistence
- [ ] Redesign every incorporated vanilla setting control using reusable Planet UI components; no final vanilla-looking controls in the finished experience
- [ ] Keep vanilla settings screens as a recovery route until complete parity and in-game validation
- [ ] Registry-driven settings pages, search/filter, reset flows, accessibility, and localization
- [ ] Full custom main menu and cohesive Planet visual system, deferred until underlying features stabilize

## 6. Unified feature and third-party integration experience
- [ ] Unified feature directory organized by capability/function, not by originating mod
- [ ] Native, deep, and compatibility integration levels with truthful per-version status
- [ ] Unified HUD editor for Planet widgets and supported third-party HUD providers
- [ ] Unified settings pages for exposed Planet and third-party settings
- [ ] Unified keybind manager for supported providers
- [ ] Unified profile support for Planet-owned and explicitly supported integration settings
- [ ] Optional dependency detection and graceful behavior when integrations are absent, outdated, or incompatible
- [ ] Flashback and compatible add-on integration; Planet does not build a competing replay/recording system
- [ ] Simple Voice Chat integration and unified voice interface, subject to its supported API
- [ ] Uku ecosystem integration as optional providers; do not independently duplicate features owned by Uku mods
- [ ] Respect third-party licenses, independence, and provider-owned configuration boundaries; no redistribution without permission

## 7. New client features

### Hardware and performance HUD
- [ ] CPU utilization and temperature where supported by the operating system/hardware
- [ ] GPU utilization and temperature where supported by the driver/platform
- [ ] System RAM and Minecraft Java heap usage
- [ ] Optional live performance graphs for FPS, frame time, CPU/GPU, memory, and network latency
- [ ] Clear unavailable-sensor states; never invent or imply unsupported sensor readings
- [ ] HUD widget visibility, positioning, scale, opacity, colors, formatting, and compact/graph modes

### Main menu, identity, and customization
- [ ] Full skin management: upload/import, preview, Classic/Slim model selection, saved skins, apply/change, and export
- [ ] Account-authenticated skin changes through supported Minecraft services; never collect or store Microsoft passwords/tokens in Planet
- [ ] Additional cosmetics only where supported by Minecraft services or an explicitly compatible provider
- [ ] Customizable menu backgrounds, optional animation, themes, accent colors, fonts, transitions, and menu audio controls
- [ ] Recently played worlds and servers

### Server discovery and multiplayer
- [ ] Separate My Servers and Discover Servers views
- [ ] Search and filter discoverable servers
- [ ] Server detail cards with available description, version, player count, and status/latency information
- [ ] Add a discovered server to My Servers or join directly without saving
- [ ] Favorites and organization for saved servers
- [ ] Discovery backed by a documented, reliable directory/service; no fabricated server listings or availability claims

### Client utilities and organization
- [ ] Planet Command Center/searchable action palette for client features and settings
- [ ] Screenshot manager with browsing, preview, and organization
- [ ] Resource-pack manager with search, sorting, and preview where possible
- [ ] Waypoint manager and world information, respecting server authority and information legitimately available to the client
- [ ] Configurable item tooltip enhancements that do not duplicate an installed provider's feature
- [ ] Session statistics for locally observable/tracked data
- [ ] Central notification center for client, server, voice, and system notifications
- [ ] Chat customization: tabs, filters, timestamps, highlighting, and appearance controls, with provider ownership respected
- [ ] Profile manager for complete Planet setups and supported integration settings

### Voice chat
- [ ] Simple Voice Chat capability discovery and optional integration
- [ ] Planet-styled voice interface and settings where the provider API permits
- [ ] Voice status, speaking indicators, nearby-player indicators, and microphone mute controls
- [ ] Unified keybinds and HUD registration for supported voice features
- [ ] No replacement voice transport or server-authoritative behavior changes

## 8. Uku feature ownership and optional ecosystem
- [ ] Treat Uku mods as optional, independently installable providers that enrich the Planet experience
- [ ] Do not recreate or ship duplicate implementations of features already owned by the approved Uku ecosystem
- [ ] Integrate supported Uku capabilities into Planet's feature directory, HUD editor, settings, keybinds, and profiles only where documented APIs permit
- [ ] Approved targets include Armor HUD, TotemCounter, BetterHurtCam, Tier Tagger, PotionCounter, BetterShields, NoWheel, HitRange, HealthIndicator, ChatHighlighter, FractionalGui, OldInput, BlessBottleRNG, Uku's PvP modpack, and ukulib
- [ ] When an Uku provider is absent, Planet continues to run normally and does not pretend the corresponding feature is present
- [ ] When a provider conflicts with another implementation, resolve ownership per capability without silently disabling unrelated features
- [ ] Verify every exact Minecraft/mod version before making compatibility claims

## 9. Testing, final UI overhaul, and publication
- [ ] Implement and stabilize feature backends, APIs, data models, and non-final functional screens before the major visual overhaul
- [ ] Unit/integration tests for registries, providers, profiles, optional dependencies, skin workflows, server directory responses, and sensor availability
- [ ] In-game testing for every settings page, HUD widget, menu flow, multiplayer action, voice integration, and profile round trip
- [ ] Compatibility matrix testing with optional Uku mods, Flashback/add-ons, Simple Voice Chat, and other explicitly supported providers
- [ ] Performance, memory, resource-lifecycle, accessibility, localization, and failure/fallback testing
- [ ] Resolve duplicate ownership and verify that absent/incompatible optional mods never prevent normal startup
- [ ] Freeze feature behavior and interaction contracts before final UI design
- [ ] Complete the full Planet visual/UI overhaul across the main menu, all settings, HUD editor, skin manager, server browser, voice interface, and utility screens
- [ ] Regression-test the finished UI and all navigation/persistence after the overhaul
- [ ] Select and verify project license and third-party notices/attributions; confirm redistribution permissions for every bundled component
- [ ] Prepare changelog, versioning, documentation, support/issue process, and release artifacts
- [ ] Final clean-install and upgrade tests; publish only after release gates pass

## 10. Compatibility and platform constraints
- [x] Primary target: vanilla Minecraft 1.21.11 with Fabric
- [x] No Sodium, Iris, VulkanMod, or Canvas compatibility targets or adapters planned
- [ ] Reconcile documented Fabric Loader baseline with the user's current 0.20.1 test environment before release claims
- [ ] Ordinary Fabric mod coexistence testing
- [ ] Client/server runtime validation and performance regression suite

## Verification boundary
CI and unit tests do not prove graphics-context integration, real GPU driver behavior, visual correctness, server-directory quality, account skin workflows, voice interoperability, runtime performance, or third-party mod compatibility.
