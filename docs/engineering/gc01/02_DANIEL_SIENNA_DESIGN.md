# Daniel and Sienna — source-backed gameplay architecture and native UX design
**Design decisions recorded 15 Nov 2026; implementation and physical QA pending.** Priority 1 after active emulation lane.

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

---

# DANIEL + SIENNA — COMPLETED TECHNICAL AND INTERACTION DESIGN DECISIONS
**Design-review date:** 15 Nov 2026. **Source snapshot examined:** master \`7362f442f88dc2de2260030ab11fc08e24fb8271\` (Wii U #263 merge). **Scope:** architecture and native user experience design for all eight GC-01 areas only; no emulator/runtime modification. This is a model-authored review in the named project roles, not independent human review or physical QA. The final issue design gate can be closed for documented design scope; implementation feasibility and real device verification remain #266/#248 gates.

## Daniel Mercer — architecture decision record

### ADR-01 APPROVED — One app-owned menu by consolidating the EXISTING Compose system
**Evidence:** 
- \`lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/GameMenuActivity.kt\` \`GameMenuScreen\`: existing Compose \`NavHost\` with Resume, States, Controls, Advanced, GripArc, PivotArc, Options and \`GameMenuContract\` result handling.
- \`.../mobile/feature/gamemenu/SharedGameMenu.kt\`: already defines \`GameMenuTheme\`, \`GameMenuPanel\`, \`SharedGameMenuHome\`, gamepad navigation/focus.
- \`.../mobile/feature/gamemenu/GameMenuCapabilities.kt\`: \`GameMenuCapabilities\`, \`GameMenuAction\`, \`GameMenuPauseGate\`.
- \`.../mobile/feature/gamemenu/XboxGameMenu.kt\`: existing Compose in SDL host reuses \`SharedGameMenuHome\`.
- \`.../shared/game/xbox360/Xbox360Activity.kt\`: Compose \`XboxGameMenu\` over native \`SurfaceView\`, pause gate and controller presence.
- \`.../tv/gamemenu/TVGameMenuActivity.kt\` / \`TVGameMenuFragment.kt\`: separate LeanbackPreference UI requiring deliberate migration to consistent presentation and safe remote navigation.

**Decision:** Retain \`SharedGameMenu\` components as canonical native CARTDECK presentation; reuse the *existing* host-specific embedded Compose overlay where native host exposes it and existing Activity+result dispatch where Libretro requires it. Extract only minimal shared presentation/action contracts needed for menu parity. Every engine implements capabilities and operations through host adapter (may be facade over existing contract), NOT by switching or wrapping the entire emulator runtime. TV must use visually consistent CARTDECK native menu; reuse backend TV settings/save options without forcing desktop/touch interaction. Non-menu fatal recovery can use existing safety mechanisms until safe equivalent native CARTDECK replacement is implemented. No wholesale UI-framework rewrite.

**Safety correction:** \`com/rfandango/haku_x/NativeCoreActivity.kt\` has \`AlertDialog.Builder\` ~lines 175-185 for FIVE rapid title relaunches; it is a noncancelable crash/restart escape and MUST NOT simply be deleted. Main native Xbox pause is ALREADY Compose.

### ADR-02 APPROVED WITH CAPABILITY CONDITIONS — Host action and pause contract
**Existing:** \`GameMenuCapabilities\` flags controls, states, settings, advanced; \`GameMenuPauseGate.transition(foreground,menuOpen,recoveryOpen,leaving)\` returns changed desired pause state. \`GameMenuContract\` result keys are in use.

**Approved target:** \`MenuCapabilities\` descriptive, engine specific: pauseSemantics [native true pause / background-safe suspension / nonblocking overlay], nativePersistentSave [engine automatic or in-game, NOT a manual savestate command], saveStateSlots [supported engine only], loadStateSlots, controllerEdit, videoOption, advancedEntries, restart/recovery, quit. Each capability has availability reason and provenance. \`MenuCommand\` invokes host-owned \`Pause/Resume/SaveState/LoadState/ChangeController/ChangeViewport/Exit\` with typed outcome [accepted, completed, unavailable(reason), failed(error)]; NEVER acknowledge a manual state as written just because UI dispatched result. Preserve existing \`GameMenuPauseGate\`; if a backend lacks real core pause, display an honest menu state and do not claim frozen gameplay. Do not block Android UI on Vulkan SDL/guest locks.

**Routing table (existing verified anchors; integration target is design decision, not code implemented):**

| Host | Existing menu/control owner | Host adapter placement and constraints | Verified limitation |
|---|---|---|---|
| Libretro GB/GBC/GBA, PS1/PSP where configured, DS, Dreamcast and other cores | \`GameMenuActivity\` + \`GameMenuContract\`; \`GameViewModelInput\` -> \`GLRetroView\` | Retain Activity results and game screen viewmodel; unify Compose routes and result/capability facade; respect core options/state namespace | Per-core save-state, controller and aspect behavior must be checked individually |
| Original Xbox hakuX SDL | \`NativeCoreActivity\` + \`XboxGameMenu\` Compose and pause gate | Keep ComposeView in SDL host and existing native commands; theme/recovery contract; preserve watchdog escape | Native Xbox save states not presumed, system HDD saves separate |
| Xbox 360 Xenia | \`Xbox360Activity\` + \`XboxGameMenu\`, SurfaceView, GameMenuPauseGate | Retain same surface, pause gate, menu overlay; use \`Xbox360Profiles.prepare\` existing account | Native in-game save only; save states unsupported, real game write/read not certified |
| PS2 ARMSX2 | \`Armsx2Engine\`, existing SDL/JNI host and \`PS2RecoveryActivity\`; exact entry host must be resolved with live current launcher | Attach menu at existing isolated host UI boundary; no JNI ABI shape change to deliver visuals | Real pausing/BIOS/memory-card path and physical input still require proof |
| Wii U Cemu | \`WiiuEngine\`, native Vulkan presenter and \`WiiuViewport\` | Add only host-owned overlay/command interface, avoid replacing \`Surface\` or recreating swapchain on menu/controller changes | PR #263 swapchain/loader fixes merged, physical stall/reopen acceptance pending |
| Wii Dolphin | Libretro/Core configuration and \`GameViewModelInput.selectWiiController\` where supported | Existing controller selection state and backend controls, capability gate advanced attachment | IR, gyro, pointer not invented; per-core support must be tested |
| Android TV | \`TVGameMenuActivity/Fragment\` Leanback settings, core state slots | Reuse settings and state owners; native CARTDECK visual navigation as focusable remote/DPAD route | TV UI divergence remains implementation work |

### ADR-03 APPROVED — Input ownership/press-release consistency
**Verified:** \`.../shared/game/viewmodel/GameViewModelInput.kt\` \`initializeGamePadKeysFlow\` filters repeat, combines shortcuts, ports and bindings, handles MENU, PivotArc held capture and \`GLRetroView.sendKeyEvent(action,mappedKey,port)\`; analog trigger key suppressed when analog motion exists; motion/axis path separate. \`.../mobile/feature/gamemenu/PhysicalControllerMappingPanel.kt\` already offers device selection, last key test, per-button binding capture/reset. \`.../shared/input/GameplayControllerPresence.kt\` uses \`InputManager.InputDeviceListener\` with enabled-pad classification; \`ControllerHardwareProfile.kt\` excludes TV remote-only controllers.

**Decision:** Input events remain owned by one engine dispatcher per session. A shared per-device profile translates Android physical position to existing expected engine input at the current mapping boundary; zero duplicate global listeners introduced. Menu open must stop gameplay forwarding only within existing dispatch route, release already held inputs, remove capture state and re-establish clean ownership on resume. Maintain port IDs, axes, triggers, diagonal/chord and native-specific mappings. New controller-discovery UI is a reader/controller of \`InputDeviceManager\` and existing native host managers, not second gameplay pipeline. Unknown devices fall back to Console Authentic with guided map/test/save/reset. TV remote navigates UI; never classified as playable full pad. Modern Function requires exact verified title/release binding and explicit opt-in; original engine key sent according to actual game action, NOT global letter remapping.

**Risks to test:** shortcut consumed on one constituent key and subsequent release, input device ID replacement after Bluetooth reconnect, analog+digital trigger dual-emission, multiport duplicate controllers, ghost keys when opening menu, PivotArc capture while physical button held, GamePad pointer screen transforms.

### ADR-04 APPROVED — Renderer and geometry remain engine-owned
**Verified:** \`MobileGameScreen.kt\` calculates shell/GripArc controls, touch visibility, current viewport and native control inventory; \`GameScreenLayout.kt\` portrait \`STANDARD\` uses a *4:3 manufactured glass opening* with 84dp top and 22dp sides and leaves actual source aspect to the renderer; dual-screen portrait uses flexible available geometry. \`CartdeckViewportPolicy.fitStandard\` currently fits physical 4:3 glass, not a universal framebuffer fit proof. \`WiiuViewport.pointer\` already maps input through letterboxed aspect-fit content; native Cemu Vulkan presenter/SwapchainContinuity changed with PR #263.

**Decision:** Define \`ViewportBounds\` = window safe bounds minus measured obstructing controls (actual active console inventory, GripArc, cutouts, system gestures), then compute visible content rect from CURRENT frame dimensions and pixel aspect (if reported). For container width W, height H and effective game aspect A>0, size is (min(W,H*A), min(H,W/A)); center in safe container. This is FIT: no crop, no image stretch. A user-selectable 1:1 pixel or integer-scale option is conditional on actual output metadata/resolution; never a default that makes game tiny without choice. Full-screen prioritises larger safe game video; slim decorative casing must not take video pixels. Keep native DS two-screen composite and accurate stylus mapping; Wii U touchscreen input must reject letterbox touches per \`WiiuViewport\`. TV/landscape UI keeps menu accessible and no touch overlay with a real controller. Device touch overlay returns when the last enabled gameplay pad disconnects. Do not conflate native renderer frame coordinate transform with Android Compose view enlargement.

**Conflict resolved:** Historical #162/#167 requested 4:3-stretched widescreen Fill; owner 15 Nov requires **no stretch and no crop**. This decision overrides old Fill default. If a user-controlled distortion option is ever proposed it requires a separate owner decision; do not implement under GC-01.

**Cemu protected:** do not alter/replace Vulkan \`Surface\`/swapchain for menu overlay, gamepad attachment or settings redraw. Preserve oldSwapchain, null-window generation and callback lifetime from merged #263 and other runtime fixes. Controller layout resizing may still trigger actual native \`Surface\` size change where necessary; test rather than asserting never.

### ADR-05 APPROVED — Save and identity contracts
**Verified:** \`retrograde-app-shared/.../saves/CartridgeSaveBinding.kt\` uses cartridge UUID, locks, atomic marker and verified copy; legacy shared SRAM recovery user-confirmed only. Its documentation explicitly excludes engine-owned HDD/NAND, cards and PSP savedata from filename-based new save-binding semantics. \`Xbox360Profiles.kt\` provisions one offline Xenia profile via atomic profile marker, selects configured/existing ID and rejects missing account. \`Xbox360Activity.kt\` passes logged_profile_slot_0_xuid and explicitly states savestates unsupported.

**Decision:** Menu never makes saves by rewriting ROM names or migrating engine storages. Save-state requests continue through core- and cartridge-bound current \`StatesManager\` namespaces; show a manual state action only where core supports it. PSP saves remain in existing engine namespace, Wii NAND, PS2 memory card and Xenia offline content roots remain engine-owned. Xbox360 menu shows 'Save through the game' and account/profile status, not a fake savestate button. All future user controller configurations persist by stable device descriptor + existing binding semantics; inspect identity alias collisions before schema change. No Room migration required by native-menu design. Regression includes reinstall? NO: install OVER same app/package/signature, never uninstall/clear data.

### ADR-06 APPROVED — Error and rollback
- Recovery UI is CARTDECK-native where safe, with copyable diagnostics and Return to Shelf. Original Xbox repeat-relaunch guard cannot be removed until equivalent safe exit proves functional; it is not a cosmetic menu defect.
- Use isolated app-layer adapter and capability routing patches, no emulator core swap or VM compiler/NDK change to implement GC-01.
- Feature-flag/migration compatibility for each host if runtime ownership shifts; do not assume rollback to lower DB schema is safe. Keep source commit, same-signature compatible APK and verified current data snapshot.
- Independent physical verification remains later consolidated #248; source/CI and Android screenshot test results are separate evidence classes.

## Sienna Vale — approved product-facing UX design specification
**Design decision:** One CARTDECK menu hierarchy with same visual language and action order for phone/tablet/TV, using current \`GameMenuTheme\` / \`SharedGameMenuHome\`, NOT a generic system preferences page or duplicated Xbox UI.

### UXR-01 Menu navigation / hierarchy — design locked
\`\`\`
GAME PLAYING  [Android Back / MENU / on-screen CARTDECK]
  └── CARTDECK PAUSED / SESSION MENU
        RESUME GAME                        always first
        SAVE & LOAD                        only if supported savestates
        CONTROLLERS                        attached device / test / mapping
        DISPLAY                            Aspect Fit / shell+overlay / conditional pixel scale
        GAME OPTIONS                       core-verified settings and Wii controller modes
        ADVANCED / DIAGNOSTICS             host-capability gated, no exposed engine dialogs
        RETURN TO CARTRIDGE SHELF          confirmation when unsaved session risk
\`\`\`
\`Save & Load\` is hidden/disabled with clear explanation for native Xenia/HDD, PS2 card and other engines without savestates; native in-game save guidance lives in session details. Native engine settings inside branded CARTDECK screens. No blank or unrelated advanced actions for production users. For unsupported pause show 'Menu open — gameplay may continue' rather than falsely freezing game.

**Focus rules:** MENU opens pause (not a blind Start that disrupts a game); resumed focus defaults to Resume; DPAD Up/Down steps controls without gameplay events, Left/Right adjusts only intended control, OK activates, Back goes one level then Resume; long-press Back or destructive Exit must not bypass confirmation. Touch taps and screen readers read identical descriptive labels. Escape still works with ONLY TV remote; it need not simulate joystick inputs.

### UXR-02 Layout specs / interaction targets
**393dp phone portrait** (visual hierarchy, proportions to be measured from real safe rect):
\`\`\`
┌───────────────────────────────────────┐
│ CARTDECK     [GAME]        [Resume ✕] │  native branded title
├───────────────────────────────────────┤
│ Resume Game                           │  first-focus DPAD / large touch
│ Save & Load      [only if supported] │
│ Controllers                           │
│ Display                               │
│ Game Options                          │
│ Advanced                              │
│ Return to Shelf                       │
└───────────────────────────────────────┘
\`\`\`
Portrait closed: real source-aspect screen centred in shell's safe glass; native-looking controls never overlap. Portrait with intentional fullscreen: video can use additional safe height and true controller absence requires accessible touch buttons.

**Landscape phone / 600dp tablet**: game stays in untouched engine-native viewport when menu opens, darkened only where overlay is safe; compact left-side or centred menu panel with stable Back and focus, never force surface recreate. On tablet use two-pane game + settings only when available, keep pointer and screen bounds unchanged.

**≥840dp tablet / 1080p and 4K Android TV**: large-readable focus cards/rows, remote-focus emphasis and scroll; no tiny phone-sized text. TV game has no touch buttons; remote always opens menu. Overscan and safe insets enforced at real pixels, accessibility target sizes measured in dp. Exact text/icon pixel artwork to be signed off against screenshots during implementation; do not claim these textual sketches are rendered final mockups.

**Control targets:** minimum 48dp interactive touch regions for applicable mobile touch controls; no real input hit-area overlaps, no native face/shoulder/utility disappearance. Screen-reader semantics, captions, high-contrast, reduced motion and transparent safe areas apply.

### UXR-03 Controller journey — design locked
\`\`\`
CONTROLLERS
  Connected controller(s) / Player 1...n
    Device detected: family + safe "generic" fallback
    Test inputs (press/release, axis live, trigger range, diagonal/chord)
    Configure:
      Console Authentic [default]
      Modern Function [only exact verified title binding, opt-in]
      Custom [per-cartridge override]
    Remap [press then release] / Undo / Reset / Save
  Touch controls: Console / PivotArc / GripArc, available based on fit
  Wii game: verified Sideways / Nunchuk / Classic and clear pointer/motion limitations
\`\`\`
If hardware disconnects mid-game, show non-blocking notification and restore phone/tablet touch controls without relaunch. TV shows 'Connect a gameplay controller' rather than touchscreen overlay when pad missing. If PivotArc+GripArc cannot preserve original native controls on measured screen, show a clearly explained usable Standard fallback **for this session only**, preserving saved preference until user explicitly changes it. No automatic permanent migration.

### UXR-04 Display and error journeys — design locked
\`\`\`
DISPLAY
  Screen: Aspect Fit — correct native proportions [DEFAULT]
  Scale: Native/Integer (if renderer and device support) [OPTIONAL]
  Shell: default portrait / intentional fullscreen / floating touch
  Touch overlay: enabled unless actual compatible gameplay pad active
  Restore defaults
\`\`\`
Do not offer an illusory Fill option that stretches classic 4:3 content or crops DS. Top-line bleed = FAIL if game pixels cross video aperture/safe bounds.
State handling:
- \`PAUSING\`: short in-menu loading acknowledgement, prevent repeated command.
- \`SAVE_PENDING\` / \`LOAD_PENDING\`: progress until engine result; show outcome and error/retry/cancel. Background interruption must preserve persistent data.
- \`UNSUPPORTED\`: explain engine-specific capability and alternate native in-game save where applicable.
- \`CONTROLLER_DISCONNECTED\`: restore phone/tablet touch or TV controller-needed message, retain MENU/Back.
- \`CRASH/RECOVERY\`: no ordinary resume unless host safe, copy diagnostic and Return to Shelf. Retain native hard-stop safety guard.
- \`ROTATE/BACKGROUND\`: no phantom resume, keep pressed inputs released and navigation state as permitted by host.
- \`EXIT\`: confirm unsaved-session risk when no demonstrated engine auto-save; never imply games are saved because a return path exists.

### UXR-05 Source geometry verification table for engineer
| View | Required layout proof | Source target | Acceptance |
|---|---|---|---|
| 393dp narrow phone portrait, tall phone | aspect FIT, 48dp native pads, shell trim, no top bleed | \`GameScreenLayout\` and \`MobileGameScreen\` | video within aperture; all actual native keys remain |
| landscape phone | fit maximal content in remaining safe space; controls can hide only with real pad | \`GameScreenLayout\`, \`CartdeckViewportPolicy\` | no cropping or forced aspect distortion |
| 600dp / 840dp tablet | larger game-first aperture, accessible side controls and menus | same + native host adapters | focus/rotation, touch state, stable video |
| 1080p / 4K TV | true ten-foot CARTDECK UI and no touch overlay | \`TVGameMenuActivity/Fragment\`, host viewport | DPAD/Back only to menu, pixel fit, correct bars |
| DS two-screen | both screens/stylus transform | \`CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN\` | no lost bottom screen or misaligned input |
| Wii U Cemu | immutable native surface lifetime through menu; accurate touch bars | \`WiiuViewport\`, Vulkan presenter | no swapchain regression; outside-bars touches rejected |
| PivotArc+GripArc | full native control inventory, 48dp real hitboxes | \`MobileGameScreen\`, native inventory | both combined layouts physically test, session fallback explained |

**Visual treatment:** use approved original CARTDECK C-slot, Original/Pink/Green, StealthAcid and high-contrast tokens. Quiet translucent chrome if supported, not larger bezel or obstructing artwork. Current menu shared theme retained; visual variation belongs to appearance tokens, not engine-specific widgets.

## Explicit eight-area engineering dependency and design-to-test mapping
| Workstream | Architecture owner/entry | UX owner/entry | What implementation MUST prove |
|---|---|---|---|
| A native menu | \`GameMenuActivity\` + \`SharedGameMenu\` + existing host menus | UXR-01 / UXR-04 | real menu parity and accurate unsupported/save outcomes across integrated engines |
| B screen | \`GameScreenLayout\`, \`CartdeckViewportPolicy\`, \`WiiuViewport\` + host renderer | UXR-02 / UXR-05 | maximum safe aspect FIT without distortion/top bleed; DS and TV |
| C physical gamepads | \`GameViewModelInput\`, \`GameplayControllerPresence\`, \`PhysicalControllerMappingPanel\` | UXR-03 | physical map/test/save/reset, held release and hotplug, correct port |
| D Wii modes | existing \`selectWiiController\` plus Dolphin actual capabilities | UXR-03 Wii option | supported Sideways/Nunchuk/Classic actions without fake IR |
| E GripArc | \`MobileGameScreen\`, joint native control inventory | UXR-03 fallback and geometry | correct real native keys, PivotArc+GripArc current fail must be closed on device |
| F Modern Function | current device mappings and exact title/edition source matrix | UXR-03 opt-in selection | verified actual game action maps, authentic baseline and Undo |
| G Xbox360 profiles | \`Xbox360Profiles\`, \`Xbox360Activity\`, Xenia engine roots | native save guidance UXR-01 | real offline account and progress survive title reopen/install-over |
| H emulation quality | per-engine pinned hosts, PS1/PSP fixtures and fallback | meaningful error/recovery UXR-04 | same-hash standalone comparison and no working-console regressions |

## Preflight gaps and explicit conditions to carry to #266
- This review examined actual code in named files; it did NOT execute Gradle, instrument Android, render screenshot mockups or test user Samsung hardware. Host-specific PS2 live Activity and WiiU menu callback integration need full call-graph read by Elliot before implementation. Mark them \`SOURCE TRACE INCOMPLETE\`, not proven.
- Live emulation #263 merged into master, but no proof that Cemu now tolerates physical controller resize after owner test; do not begin GC-01 source PR until engine gate handoff recorded.
- Some native host existing menu parity is present; tests must first isolate remaining inconsistencies rather than assume universal grey dialogs.
- Hardware keyboard/TV remote DPAD focus, Wii attachment motion and native engine actual save states require capability proofs.
- No owner-supplied visual screenshot comparison completed here; UXR-02 outlines are actionable wireframe specifications but are NOT approved rendered designs. Sienna's artistic final visual lock remains an implementation-level review gate.
- Console logo usage, post-emu shelf, GEM and CartMods are not GC-01 implementation authority.
- Exact source SHA, JNI threading, original saved APK hash, CI and hardware testing are #266 execution gates.

## Named review dispositions — design scope, not person-authentication
**Daniel architecture-design disposition: APPROVED WITH IMPLEMENTATION CONDITIONS.** ADR-01..06 provide the approved candidate contracts derived from inspected files. Conditions: PS2/WiiU host command/thread call graph and menu-lifecycle integration verified in next live master; tests and native build once active emulation lane yields control; no native core changes for UI.

**Sienna UX-design disposition: APPROVED WITH IMPLEMENTATION CONDITIONS.** UXR-01..05 define canonical interaction/behaviour, responsive layouts, fallback and appearance rules; visual pixel-perfect signed screenshots, accessibility instrumentation and physical device acceptance remain future gates.

**Issue #265 deliverable criterion:** source-backed architecture and UX decisions documented, self-contained to hand to Elliot+Marcus #266. This sign-off is a **role-based design determination authored in this ChatGPT session**, not an independently authenticated review by external human specialists. Engineering/Marcus test QA and release gates remain OPEN. Close #265 only as design-scope handoff, no claim of actual game pass.


## Sienna full-scope design superseding preliminary textual wireframes (15 Nov 2026)
The full design deliverables are now committed and mandatory implementation references:
- [04_SIENNA_COMPLETE_UX_DESIGN.md](04_SIENNA_COMPLETE_UX_DESIGN.md): 25-screen/state inventory M00–M24, component catalogue C01–C14, eight complete functional flows, source-linked visual design, input control inventories, adaptive geometry, exact user messages and acceptance per GC-01 A–H.
- [05_SIENNA_SCREEN_PROTOTYPE.html](05_SIENNA_SCREEN_PROTOTYPE.html): navigable design-only HTML screen studio with 25 states and responsive phone/landscape/tablet/TV selector, controller mode/GripArc selection and Save capability toggle. This is an interactive design reference and is NOT wired to native emulator controls, Android renderer, storage or actual game sessions.
The specification supersedes shorter UXR-01–05 sketches where more precise. It preserves approved CARTDECK appearance tokens and existing Compose menu classes. The full UX deliverable is complete for engineering handoff; rendered Android implementation, screenshot matching, specialist device validation and Marcus hardware QA remain unfinished. Never treat HTML mock interactions as proof of feature implementation.
