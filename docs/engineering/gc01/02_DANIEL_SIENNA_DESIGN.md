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
