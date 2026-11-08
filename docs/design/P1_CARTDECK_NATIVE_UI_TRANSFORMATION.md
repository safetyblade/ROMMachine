# CARTDECK — P1 Native UI Transformation | Controlled Engineering Handoff

**Status:** DESIGN READY / IMPLEMENTATION GATE: P0-B independent acceptance still open (#9). No application code in this documentation PR.  
**Source:** Jonathan's 8 Nov 2026 physical-device screenshots of Home empty/filled, touch layout and GBA gameplay.  
**Design:** Sienna Vale | **Product:** Cassia Rowan | **Engineering:** Elliot Shaw | **Architecture:** Daniel Mercer | **Independent QA:** Marcus Reid.

## 1. Locked product principle

The CARTDECK device is the app. Original graphite/teal and Pink are approved first finishes. C-slot logo, cartridge journey and signature boot are locked *in direction*. Device family geometry, controls, mark and UX rhythm are permanent; edition colour/material/button finish is swappable. Do not copy manufacturer trade dress. Do not bundle ROMs, protected cover artwork, logos or splash screens from imported games.

Player journey: **Cartridge Shelf → Insert → Boot (nonblocking) → Play → Suspend → Resume**.

## 2. Current phone findings, 8 Nov 2026

| Existing phone screen | Evidence | Correction |
| --- | --- | --- |
| Home empty | Two large notification/directory permission cards obscure play action, generic Home header | Compact CARTDECK device welcome, single prominent Add Cartridges action, retain contextual permissions in settings |
| Home populated | Generic Discover and large half-empty cover tile, initials placeholder | Cartridge Shelf using original fallback cartridge labels; Continue when recent exists; clear game/system metadata |
| GBA game running | Black unused frame region, emulator video detached from four large circular directional buttons and flat A/B discs | Default responsive chassis and inset screen; compact chamfered cross D-pad, tactile A/B; correct game aspect |
| Game menus | Top utility bubbles and separate menu affordance | Consistent hardware-positioned shoulder/start/select/pause, preserve inputs and existing game menu actions |
| All screens | Generic system-colour Material palette and Lemuroid branding | Fixed CARTDECK Original semantic theme by default, user-editable edition appearance; preserve source attribution in About |

**A running GBA title is functional evidence for launch/display, not independent P0 acceptance for saves and updates.** P0-B #9 remains open.

## 3. Source map (inspected master; do not replace emulator engine)

| Surface | Files | Implementation contract |
| --- | --- | --- |
| Home | `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/home/HomeScreen.kt` + `HomeViewModel.kt` | Reuse `recentGames`, `favoritesGames`, `discoveryGames`, `indexInProgress`, permission flags and existing StorageFrameworkPickerLauncher; restyle only, no DB/import rewrite |
| Entry and nav | `.../feature/main/MainActivity.kt`, `MainTopBar.kt`, `MainNavigationBar.kt`, `MainNavigationRoutes.kt` | Retain route semantics, settings/help and search; adaptive branded header / dock |
| Cards/theme | `.../mobile/shared/compose/ui/LemuroidGameCard.kt`, `LemuroidTheme.kt` | Compose original cartridge tile; override Android dynamic colours with CARTDECK semantic tokens by default |
| Game layout | `.../feature/game/MobileGameScreen.kt`, `GameScreenLayout.kt` | Continue same `AndroidView`/RetroGameView viewport and same PadKit, only change arrangement/decorative overlay |
| Input art | `lemuroid-touchinput/src/main/java/com/swordfish/touchinput/radial/controls/LemuroidControlCross.kt`, `LemuroidControlFaceButtons.kt`; `.../ui/LemuroidCrossForeground.kt`, `LemuroidControlBackground.kt`; `.../LemuroidPadTheme.kt` | Keep ControlCross, ControlFaceButtons, pointer ownership and haptic feedback; replace visuals using corrected v0.3.1 SVG. Do **not** reintroduce circular 4-button D-pad. |
| System input layouts | `.../radial/layouts/GB.kt`, `GBA.kt` | Never remap keys L/R, Start/Select, D-pad diagonals or A+B |
| Pause/settings | `.../feature/gamemenu/GameMenuHomeScreen.kt` and existing settings routes | Preserve all save/load/restart/mute/edit-control commands |
| App identity | `lemuroid-app/src/main/AndroidManifest.xml`, `res/values/strings.xml`, mipmap assets | Public brand CARTDECK; legal provenance and upstream credits remain. Package ID/signing migration separately gated. |
| Art source | `design/cartdeck-v03/` after PR #10 | One master brand/control path set, Original/Pink manifests, codegen, icon candidate; Green/Mono not shipping-approved |

## 4. Screen designs

**Empty shelf:** CARTDECK header with C-slot; small "YOUR DEVICE IS READY"; single Add Cartridges primary action launches existing Android folder picker; one explanatory line; scanning status and cancel/error; notification permission in unobtrusive contextual slot.

**Populated shelf:** Continue last-played game as a single action at top, then horizontally browsable original cartridge tiles. Long press context menu retained. Favourites, Systems, Search reachable from restrained lower navigation. Unlicensed/missing cover falls back to a custom CARTDECK cartridge surface with title abbreviation, system and original non-infringing device marks; do not auto-fetch game box art.

**Play / portrait GB & GBC:** 10:9 unclipped game image in inset frame, compact cross left, A/B right, Start/Select and Pause accessible, shoulder controls reachable. Portrait GBA: 3:2 game image and wide aspect-friendly inset; allow layout to use available vertical space, minimise black dead zones. Native renderer not replaced.

**Play / landscape:** responsive horizontal device treatment with correct game aspect. Standard portrait version should not be rotated into unusable proportions. Android TV retains controller-first fullscreen.

**Three display modes:** (1) Console Shell **default phone/tablet**; (2) Fullscreen game unobstructed with physical controller; (3) Floating game-first controls with adjustable scale, position, opacity. Maintain Pause/Back access. Persist preference outside ROM and save state. Keep existing edit-controls scale/margin/rotation support; prevent controls clipping safe insets and restore defaults on invalid config.

**Edition preferences:** Original and Pink first. Theme tokens use `design/cartdeck-v03/editions/` as controlled source, converted to native Compose colours and vector appearance. Identity/shape/input geometries are not editor parameters. Invalid edition falls back to Original while current game continues. Green and Mono are proof editions only.

**Boot:** Wake 0–120ms → Slot 120–280ms → Mark 280–550ms → actual game when ready; never insert extra wait. Tap/Back/controller skip, mute and reduced-motion static variant.

## 5. Sequencing and evidence

1. **P1-A / #12:** branded app icon/title, fixed semantic theme, guided empty state, Cartridge Shelf and Continue. Debug APK screenshot comparisons and no regressions in importer/db/search.
2. **P1-B / #13:** shell with existing PadKit and unchanged screen viewport. First apply corrected cross-shaped D-pad and tactile buttons. Android GBA/GB/GBC + landscape screenshots; diagonal/A+B/shoulder/controller tests.
3. **P1-C / #14:** Console/Fullscreen/Floating presentation preference and Original/Pink theme selector. Settings, persistence, resizing and accessibility. Separate theme manifest validation.
4. **P1-D / #15:** branded nonblocking boot, pause UI hierarchy, finishing, muted/reduced-motion and TV menu treatment.

No massive UI rewrite PR. Each phase on one reviewable branch/PR, testable build and screenshot comparison. Report PASS/FAIL/NOT RUN; new visuals alone are not gameplay acceptance.

### Mandatory gates

Before first app implementation: Marcus physical GB/GBC/GBA/upgrade/save evidence in #9, Daniel architecture and Cassia acceptance. Public release separately requires verified signing migration, core licensing/attribution and 16KB compatibility. After each P1 PR: screenshot alignment, small device/landscape/tablet, touch hit zones >=48dp, TalkBack/focus, native input latency and persistence. Marcus independently checks regressions; do not merge broken PRs.

## 6. Asset authority and source-of-truth

Owner-approved direction in controlled Drive `03_CARTDECK_Console_Experience_and_UX_Specification_v1.0_ACTIVE`; brand vector source `design/cartdeck-v03/` (including merged D-pad correction PR #10); GitHub issues #12–#15 are the implementation queues. ChatGPT visual concept studies are **not** production export geometry or licensed game-cover assets.

**Disposition:** return to Cassia for experience sign-off, Daniel for architecture review and Elliot for gated implementation when P0-B truly passes.
