# Daniel and Sienna — source-backed gameplay architecture and native UX design
**Draft for actual specialist review; NO approval is claimed.** 15 Nov 2026. Priority 1 after active WiiU lane.

## Inspected foundation
- docs/design/CARTDECK_PHASE3BB_CONTROLLER_ACTIVITY.md explicitly documents GameViewModelInput.initializeGamePadKeysFlow -> GLRetroView.sendKeyEvent(action,mappedCode,port); virtual axis-derived digital transitions; GameViewModelTouchControls.handleVirtualInputButton -> GLRetroView; PivotArc capture before dispatch. Dedicated SDL/PS2/WiiU/Xbox routes bypass those handlers; no global new collector is allowed.
- #191 and PR #200 describe current Compose GameMenuActivity and native Xbox grey AlertDialog inconsistency. Exact current paths must be resolved against master before editing (historic design names are not definitive path proofs).
- #162 #167 and master roadmap document LibretroDroid host image aspect and that AndroidView.fillMaxSize alone cannot guarantee actual fullscreen pixels; native C++/JNI/aspect math needs a precise host inventory.
- #145/PR #201 and successors cover stable UUID, frozen save aliases and Room migrations; do not change schema to implement menu/display/controller work.
- WIIU PR #263 currently repairs Vulkan swapchain during controller resize, so menu/surface and device hotplug changes MUST NOT race that runtime.

## Daniel's proposed contract decisions — approval explicitly pending
D1 Menu is app-owned CARTDECK UI. Single CartdeckGameplayMenu state machine (RUNNING, PAUSE_REQUESTED, PAUSED, ACTION_PENDING, ERROR, RESUME_REQUESTED, EXITING). Foreground app owns visible menu, stable Back/DPAD/touch focus; suppress user-facing engine dialogs. No Libretro-brand presentation, no duplicate nav stack.
D2 EngineCapabilityAdapter lists supportsPause, saveState, loadState, nativeSave, perEngineVideo, inputConfig and special options with reason if unavailable; never claim a capability just because another core supports it. Distinguish automatic SRAM, manual save state and memory card.
D3 EngineCommandAdapter has async acknowledged request/response (pause/resume/save/load/exit/openConfig) with cancellation and failure, explicit thread ownership. Native host operations run only on their required engine thread. No direct UI socket to emulator internal state and no blocking UI on SDL/Vulkan locks.
D4 Existing Android input -> existing GameViewModel route -> GLRetroView stays authoritative for Libretro, and existing SDL/JNI native paths stay authoritative for native engines. Normalise physical semantic position/profile at an approved boundary. Menu interception releases held gameplay keys and prevents repeated down dispatch; preserves chords/diagonals, triggers and multiple ports. Restore touch on true physical gameplay pad removal. TV remote counts as navigation, not gameplay pad.
D5 ViewportPolicy fits native frame inside measured safe rect preserving original effective aspect ratio (including pixel aspect where applicable), no crop/stretch. Compose shell and glass trim are separate layers outside video; touch hit boxes use same transforms. DS 2-screen retained, no accidental second-screen cropping. TV overscan and gesture cutouts must be measured, not hard-coded. Hotplug must not force needless Vulkan surface replacement on WiiU.
D6 Persistence existing cartridge UUID+SaveBinding controls save ownership. Per-device controller profile and per-game overrides add under stable keys only; X360 source account/profile system is engine-owned but linked durably to CARTDECK cartridge/profile, no speculative new save schema.
D7 Rollback via isolated reviewable adapters/feature flag where appropriate and exact APK/source snapshot, without app uninstall or untested schema down-migration. Preserve original engine paths.

## Sienna native UX interaction definition — review pending
Navigation IA: open Pause -> Resume / Save & Load (supported slots and clear 'unavailable' state) / Controls (Connected device, Calibrate, Remap, Test, Reset, Save, Player port) / Display (Aspect Fit, native pixel/integer option only when feasible; safe shell and touch controls) / Advanced engine settings [capability-gated native CARTDECK screen] / Exit to cartridge shelf.
Entry paths: touch menu affordance; physical controller MENU/START chord when not consumed by title; Android Back; TV DPAD remote. When pause unsupported, show truthful overlay semantics without fake freeze; no duplicate menu.
Focus states: touch/keyboard/DPAD, tab/traversal and large text; background/rotation/crash; suspended/resuming and action-progress; controller hotplug; responsive portrait/landscape/tablet/TV. Save feedback must acknowledge actual engine result.
Touch treatment: phone/tablet physical GAMEPAD active in fullscreen -> hide shell and gameplay buttons, maintain discoverable menu; disconnect -> restore usable on-screen controls. TV displays no touch overlay but remote menu always reachable. Portrait shell remains unless user chooses fullscreen.
Safe layout: no crop/stretch, top-line bleed eliminated, minimum accessible target and gesture clearances, no button overlap, PivotArc + GripArc must preserve original per-console button inventory.
Appearance: match approved CARTDECK Original/Pink/Green and personal appearance tokens without divergent per-engine menu layouts; never add gameplay-affecting lighting overlays.
Native controller UI: real-world SOUTH/EAST/WEST/NORTH capture labels and action test feedback; preserve Console Authentic/Modern Function/Custom distinct; unknown device guided first-use.

## Required code evidence before Daniel signs
- Exact Kotlin/Java/C++/JNI/Compose files for each host; function and thread call trace with code links and SHAs.
- Back/PAUSE dispatch and duplicate interruption matrix; native SDL/Xenia/ARMSX2/Cemu engine limitations with proof.
- Pixel viewport math and screenshot expected vs actual on known devices.
- Input event ownership down/up/held/axes/multiplayer, OS focus and controller disconnect.
- Data ownership and versioned install-over/rollback contract.
- Explicit Daniel architecture sign-off + Sienna inspected UI/wireframe state sign-off (neither performed by this artifact).
**Gate: SOURCE-BASED DESIGN CANDIDATE PREPARED; REVIEWS AND APPROVALS PENDING.**

---

# CONTROLLED HANDOFF TO DANIEL MERCER + SIENNA VALE — 15 NOV 2026

**Handoff type:** Actual code-path preflight and executable specialist review specification. **Not a claim of completed Daniel or Sienna sign-off.** Cassia scope #264 complete. Issue #265 stays OPEN until reviewers record their architecture/UX decisions and evidence. Emulation/Wii U remains live Codex lane; design only here.

## Verified master source snapshot
Master tree fetched at `7362f442f88dc2de2260030ab11fc08e24fb8271`, after merge PR #263. The repository tree was searched directly (GitHub indexed text search returned no results, so use verified paths rather than assuming unindexed code absent).

### Already-present native CARTDECK UI: reuse, do not rebuild
| Component | Exact code path | Verified behaviour/source implications | Required decision |
|---|---|---|---|
| Libretro game menu host | `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/GameMenuActivity.kt` | Compose Activity with NavHost, game and core-options extras, controls/save routes and result contract | Retain/adapt vs embed within session; handle pause continuity and TV separately |
| Shared menu composition | `.../mobile/feature/gamemenu/SharedGameMenu.kt` | `GameMenuTheme`, `GameMenuPanel`, `SharedGameMenuHome` with DPAD/menu navigation | Make canonical CARTDECK UI surface; no second Compose menu library |
| Action capabilities + pause | `.../mobile/feature/gamemenu/GameMenuCapabilities.kt` | Action set currently Resume/Controls/SaveLoad/Settings/Advanced/Shelf; `GameMenuPauseGate` deduplicates pause requests | Expand safely into per-runtime *verified* capability/command outcomes; preserve existing gate |
| Native original Xbox menu | `.../mobile/feature/gamemenu/XboxGameMenu.kt` | Already Compose-hosted inside SDL Activity and reuses `SharedGameMenuHome`; states not exposed as supported | Reuse directly; do not describe existing native Xbox pause UI as universally stock `AlertDialog` |
| Native Xbox SDL host | `lemuroid-app/src/main/java/com/rfandango/haku_x/NativeCoreActivity.kt` | `SDLActivity`, shared `XboxGameMenu`, `GameMenuPauseGate`, recovery `AlertDialog` at repeated-restart guard | Preserve recovery function; if replacing its visual surface, retain noncancelable return-to-shelf failure path |
| Xbox360 game host | `.../shared/game/xbox360/Xbox360Activity.kt` | Compose menu and pause gate, separate `SurfaceView`, `GameplayControllerPresence`; persists offline selected profile | Reuse existing host menu; design must not recreate Xenia profile storage |
| Libretro input owner | `.../shared/game/viewmodel/GameViewModelInput.kt` | `initializeGamePadKeysFlow` at about line 329, `sendKeyEvent`, direct `GLRetroView` dispatch; axis motion path and shortcuts | Map/observe via current dispatcher; never install competing Android event loop |
| Handheld screen | `.../mobile/feature/game/MobileGameScreen.kt` and `GameScreenLayout.kt` | Existing shell + GripArc joint geometry, full-screen controller handling, viewport measurements, explicit fallback messages | Fix measured fit without replacing renderer or removing native controls |
| Controller detection | `.../shared/input/GameplayControllerPresence.kt` and `ControllerHardwareProfile.kt` | InputManager device listener; excludes remote-only devices from true gameplay pad | Reuse current presence semantics with reliable hotplug fallback |
| TV menu | `.../tv/gamemenu/TVGameMenuActivity.kt` and `TVGameMenuFragment.kt` | Independent TV Activity/Fragment exists, not necessarily same Compose presentation | Sienna determines canonical TV design and Daniel determines reuse/migration without losing remote navigation |
| Shared result protocol | `.../shared/GameMenuContract.kt` | Existing intent/result keys incl Wii controller/save; separate Activity menu contract | Preserve compatibility or provide tested versioned adapter |
| Wii U Vulkan host | `spikes/wiiu/vulkan-host/CartdeckVulkanPresenter.cpp`, `.../SurfaceMailbox.h`, `.../SwapchainContinuity.h`; `.../shared/game/wiiu/WiiuViewport.kt` | PR #263 merged controller-resize swapchain/loader lifecycle | Do not recreate/destroy Vulkan surfaces on harmless menu/input transitions |
| PS2/SDL engine route | `.../shared/game/ps2next/Armsx2Engine.kt`; `org/libsdl/app/SDLActivity.java`; `.../shared/game/engine/NativeEngineRegistry.kt` | Native ABI and isolated engine lifecycle, updated through merged #258 | Verify actual Activity and JNI input/menu state before choosing adapter |
| Save identity | `retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/saves/CartridgeSaveBinding.kt` and Room/alias tests | Stable cartridge binding protection | No menu-triggered save path re-derivation |
| Tests already available | `.../src/test/.../gamemenu/GameMenuCapabilitiesTest.kt`, `SharedGameMenuTest.kt`, `LibretroGameMenuTest.kt`; `.../src/test/.../xbox360/Xbox360PadTest.kt` | Test foundations, NOT proof of all engine on-device menu parity | Expand rather than replace |

**Important correction to earlier scope language:** Native Xbox's existing `AlertDialog` encountered in `NativeCoreActivity.kt` lines around 175–185 handles repeated crash/relaunch recovery. It is not evidence that the entire Xbox pause menu is a stock `AlertDialog`. Preserve this safety behaviour while unifying recovery presentation.

## Daniel — concrete architecture review decisions to record
- **A1 host reuse:** Authorise one existing shared Compose menu as the canonical CARTDECK UI. Compare separate Libretro Activity, Compose-in-SDL original Xbox, Compose Xbox360 Activity, Android TV fragment, PS2 and Wii U session hosts. Deliver a host matrix specifying precise launch path, menu overlay ownership, pause outcome, save operations, controller-back, native lifecycle and exact code adapter hook.
- **A2 command contract:** Choose explicit supported/unsupported capability structure for pause, native save, save state, load state, per-runtime settings, controller remap, viewport, exit; track native completion acknowledgement and error status; do not infer all engines support save states.
- **A3 lifecycle:** State diagram on Android Back, Menu, focus loss/return, rotation, foreground/background, crash-recovery, action request and engine teardown. Ensure `GameMenuPauseGate` remains sole dedupe owner where used, no engine frame mutex locks from UI.
- **A4 input ownership:** Establish each Android event dispatch path for Libretro, Xbox SDL, Xbox360, PS2, Wii U and TV. Reuse existing `GameplayControllerPresence`, `GameViewModelInput`, `ControllerHardwareProfile`; demonstrate down/up/held/chord/axis and menu interception without stuck inputs or duplicate event collectors. Wii controllers and GripArc must work through existing routes.
- **A5 viewport:** Define measured safe viewport, content effective/pixel ratio vs game source, shader/GL/Vulkan quad mapping, fit without crop/stretch, optional integer/native pixels, cutouts and DS 2-screen touchscreen transformation. Demonstrate that shell hiding changes usable bounds without invalidating Cemu Vulkan swapchain or creating a second renderer. Fullscreen default cannot silently stretch.
- **A6 persistence/security:** Existing ROM UUID, frozen save aliases, X360 profile, Wii NAND, PS2 cards, Libretro states and SAF permission retention unchanged. Identify controller overrides by stable device identity and cartridge/console identity; no filename-based collision.
- **A7 migration and rollback:** Existing menu routes and settings remain functional during incremental transition; no schema or engine change solely for the menu. State precise feature flag/adapter fallbacks and test cases.
- Each answer must cite **exact existing file/function and source SHA** or mark UNKNOWN; this handoff's file discovery is not substitute for a code-reviewed call graph. Mark decision `APPROVED`, `APPROVED WITH CONDITIONS`, or `BLOCKED` with reasoning.

## Sienna — concrete design deliverables to record
- **U1 canonical menu information architecture**: Resume; Save & Load with engine support/error; Controls (physical device/player, identify, map, test, save/reset, Console Authentic/Modern Function/Custom); Display (Fit, native pixels if available; shell, overlays); engine Advanced behind CARTDECK UI; exit shelf. Confirm handling of actual source's `SharedGameMenuHome`, `GameMenuPanel`, TV distinct menu.
- **U2 detailed interaction states**: playing/paused/saving/loading/unavailable/action-failed/recovery/crashed/controller disconnected/rotated, phone portrait+landscape, tablet medium/expanded and TV remote; define focus order, DPAD navigation, touch, accessibility, screen reader and Back/Exit confirmation.
- **U3 controller experience**: first connection; distinguish basic TV remote; unknown controller guided calibration; visual pressed/held/axis preview; simultaneous chords and physical position labels; GripArc/PivotArc no missing native buttons and overlay fallback; Wii attachment modes without fictitious pointer support.
- **U4 viewport/wireframe measurement pack**: 393dp phone portrait and landscape, tall/narrow phone, 600/840dp tablets, 1080p TV and 4K scaling; effective game pixels; DS 2-screen; safe Android gestures and min 48dp real hit targets; no stretched game image or top-line bleed. Provide screenshot/wireframe comparisons against actual current UI, not generic aesthetic guidance.
- **U5 brand and settings**: match approved Original/Pink/Green, StealthAcid, custom appearances; no separate visual skin per core, no animation that blocks resumed gameplay; preserve player settings.
- Record independent UX disposition and deliverable links; no implied approval based only on this handoff.

## Exit gate for Issue #265
Require both individually recorded decisions: Daniel checked source-backed per-runtime architecture/adapters/lifecycle/render/input/storage and approved with evidence; Sienna provided reviewed menu flow, responsive/TV/controller interaction wireframes and acceptance criteria. Record actual gaps or engineering blockers in linked issues. Only THEN close #265. Do not claim emulation device pass, run tests, launch Codex or merge source under this issue. Handoff to Elliot+Marcus #266 after design gate.

**STATE NOW: HANDOFF PREPARED, SOURCE PATHS VERIFIED, SPECIALIST APPROVALS PENDING.**
