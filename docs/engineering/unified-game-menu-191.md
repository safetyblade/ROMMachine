# Unified in-game menu — #191

Baseline: master `57ba2869`. Owner Xbox visible-video acceptance is retained;
this change does not investigate or modify graphics, drivers or native engines.

## Shared presentation and capability boundaries

Both mobile Libretro GameMenuActivity and the Xbox Compose overlay use the same
GameMenuPanel, GameMenuTheme, settings-row wrapper and SharedGameMenuHome.
The existing AppTheme typography/shapes and CartridgeLibraryTheme edition/custom
materials supply the appearance. The right-side panel adapts to narrow portrait
and landscape widths, scrolls long pages and keeps Home's Return to Shelf at the
bottom. Resume is the first prominent action. Actions have at least 48dp bounds.

| Backend | Home / retained functions |
| --- | --- |
| Mobile Libretro: GB/GBC/GBA, DS, SNES, PS1, PSP, Dreamcast, GameCube, Wii, PS2 where operational | Resume, Controls, Save / Load iff statesSupported, Game Settings iff exposed options exist, Advanced, Return to Shelf |
| Libretro Controls | Existing touch editor/result contract, GripArc editor, mapped PivotArc settings, tilt selection and native controller options. Wii port 0 uses its existing per-cartridge picker with all four choices, including GameCube Controller; other ports retain generic controller settings. |
| Libretro Advanced | Existing restart, audio toggle, supported fast-forward and available multi-disc picker. Save/load routes and result codes are unchanged. |
| Xbox | Resume, Controls, Advanced, Return to Shelf. Controls edits the same touch-settings manager observed by the live PadKit overlay, with portrait/landscape settings. No save states, reset, disc selection or fast-forward is advertised. |
| Xbox Advanced | Current engine identity, existing clipboard report, valid rollback only, Troubleshooting. The existing pixel probe appears under Troubleshooting only for eligible debug builds/engine capability and an unused request. |

No second navigation framework was introduced. Both hosts use the existing
GameMenuRoute/NavHost conventions. Shared capability flags determine normal
Home presentation; backend adapters retain their real callbacks.

## Native lifecycle and input

Xbox's menu is composed inside its existing ComposeView over the current SDL
surface, in NativeCoreActivity's existing isolated process. Opening, closing,
Back and Resume do not launch an Activity, reload a ROM or change the runtime.
Explicit Previous Engine retains its existing separate-process rollback action.

One pause gate combines Activity foreground state, menu visibility, fatal
recovery visibility and leaving state, and deduplicates nativeSetPaused calls.
Closing a menu while backgrounded cannot unpause the guest. Opening the menu
clears/sends touch and hardware states. Gameplay key/axis events are blocked
while paused/menu-visible; D-pad/select events go only to the Compose overlay.
Android Back and controller B return through menu levels, then resume. Controller
Menu toggles the menu; Resume receives initial focus. Fatal/startup recovery
remains separate and keeps its existing safe exit/report paths.

Xbox HDD/UDATA/TDATA, engine binaries/versioning, Vulkan/SDL window setup,
Libretro host/result handling, PS2 JavaVM, disc staging, GripArc geometry and
player saves are untouched. Normal menu diagnostics reuse the existing report
and probe functions; no diagnostic mechanism or graphics investigation is added.

## Validation

- App Kotlin formatting and ktlintCheck passed.
- Direct Kotlin 2.0.21 compilation/JUnit passed 13 tests: four new capability/
  pause tests plus existing Xbox input/HDD and engine-store/rollback contracts.
  These run against the real listed sources and Android API constants; they do
  not compile the complete Compose/Activity integration.
- Three source-integration tests passed and are in Actions source preflight.
- Added five Robolectric/Compose tests for actual shared/Xbox presentation,
  supported action visibility, callbacks, back navigation, clipboard dispatch,
  rollback/probe eligibility, edition/custom theme propagation, portrait/
  landscape targets, Libretro save/load/controller routing and all four Wii
  controller options. These tests are UNEXECUTED locally.
- Full app source compilation and targeted Gradle menu/Wii/Xbox unit-test
  execution were attempted. Both are blocked resolving the existing
  com.github.Swordfish90:LibretroDroid:0.14.0 dependency: JitPack HTTP 403.
  No successful full app compile or Compose-test execution is claimed.

CI must compile the complete UI and run the tests. No APK workflow was launched,
waited on or polled. Owner/Marcus still need to compare menus across consoles,
confirm active editions/custom appearance and hardware navigation, background/
foreground with open menus, same-session Xbox Resume, touch-settings persistence,
return to shelf and accepted Xbox gameplay/data continuity. No device acceptance
is inferred from the source checks. #191 remains open for that gate.
