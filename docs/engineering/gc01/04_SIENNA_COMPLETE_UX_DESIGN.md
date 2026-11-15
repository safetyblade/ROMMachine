# CARTDECK GC-01 — Sienna Vale complete UX design v1.0
Design date: 15 November 2026. SOURCE-BASED DESIGN, NOT CODE OR PHYSICAL QA.
Master inspected: 7362f442f88dc2de2260030ab11fc08e24fb8271. Controlling #264 requirements, #265 architecture and #262 entire priority-one programme.
CARTDECK app owns all user-facing gameplay menus. Emulators execute game and native save functions through their original hosts. Do not change live emulation lane, app identity, Room, cartridge UUID, SaveBinding or existing rendering/input.

## 1. Interaction system
Visual identity: re-use GameMenuTheme, GameMenuPanel, SharedGameMenuHome, original C-slot CARTDECK brand and existing appearance semantic tokens. Support Original, Pink, Green, StealthAcid, high contrast and personal appearance. Use existing resources and type scale; do not invent hardcoded new colours or licences. No engine-specific menu themes. When playing: no extra thick bezel, game-first true pixel aspect; original face/button inventory visible wherever touch gameplay requires it. 48dp minimum interactive mobile touch target, no hit-region overlaps. Focus/selection highly visible for TV and gamepad, separate from optional lighting; lighting Off remains safe.

Components (design identities tied to implementation):
C01 SessionHeader: game title, console, truthful status, Back/Resume, actual menu page.
C02 ActionRow: icon/title, support status or reason, focus, click and disabled state.
C03 StatusFeedback: paused/overlay not actually paused, busy/saved/failed/retry.
C04 ControllerDevice: player port, platform family, connection, safe unknown label.
C05 LiveInputProbe: last down/up, held count, DPAD diagonals, axes, triggers, buttons and input port.
C06 BindingRow: physical position/source -> authentic target, capture/reset/undo.
C07 NativeControlPreview: source NativeControlInventory original buttons, 48dp measured hit regions, collisions shown; no generic remapped console.
C08 ViewportPreview: measured source ratio and safe container; video and letterbox bars separate.
C09 CapabilitiesNotice: real supported/unsupported engine operations and save limitations.
C10 OutcomeMessage: command acknowledgement, pending and error, never fake success.
C11 SafeRecovery: native engine report, copy logs, verified fallback and return.
C12 ExitConfirmation: no implied save, continue/return choice.
C13 ProfileStatus: Xenia guest account masked identifier and actual native save method.
C14 FocusRail: touch, keyboard, gamepad DPAD, Android Back and remote semantics.
Existing file foundations:
- lemuroid-app/.../mobile/feature/gamemenu/SharedGameMenu.kt (GameMenuPanel, GameMenuTheme, SharedGameMenuHome)
- .../mobile/feature/gamemenu/GameMenuActivity.kt and GameMenuHomeScreen.kt
- .../mobile/feature/gamemenu/PhysicalControllerMappingPanel.kt and GripArcEditorScreen.kt
- .../shared/game/viewmodel/GameViewModelInput.kt (single Libretro dispatcher)
- .../shared/input/GameplayControllerPresence.kt and ControllerHardwareProfile.kt
- .../mobile/feature/game/MobileGameScreen.kt, GameScreenLayout.kt, CartdeckViewportPolicy.kt
- lemuroid-touchinput/.../settings/NativeControlInventory.kt
- .../shared/game/wiiu/WiiuViewport.kt and Cemu Vulkan presenter.

## 2. Complete screen inventory (design IDs become acceptance test names)
M00 PLAYING: source-aspect video in safe screen, original touch controller if required; MENU affordance, physical MENU and remote Back accessible.
M01 PAUSE: header game/console/status, Resume, Save & Load if engine state support, Controllers, Display, Game Options, Advanced, Return to Shelf.
M02 SAVE/LOAD: native slot chooser and preview only for real savestate engines; overwrite confirmation; asynchronous write/read feedback.
M03 SAVE HELP: engine-native in-game save guidance for Xbox HDD, PS2 cards, Wii NAND, Xenia. Never fabricate Save State.
M04 CONTROLLERS: connected pads, port, device type, mapping mode, touch controls, Wii/GripArc specific entry.
M05 FIRST PAD: device detected, safe Console Authentic default, Map Now/Test/Skip; TV remote never identified as gameplay pad.
M06 TEST & CALIBRATE: capture press/release, two-button chords, held input, analog neutral and min/max, hats, trigger range; live feedback.
M07 REMAP: authentic original key labels, target position, capture, collision warning, per-device or per-cartridge scope, Save/Undo/Reset.
M08 CONTROL MODE: Console Authentic / Modern Function exact verified title only / Custom; action explanation and one-step undo.
M09 WII ATTACHMENT: choose only core-proven Sideways Remote / Remote+Nunchuk / Classic; name IR/gyro/motion unavailable truthfully.
M10 TOUCH LAYOUT: Standard/PivotArc, attachment and positioning without changing engine controls or save.
M11 GRIPARC STUDIO: left/right, G1-G4 fixed positions, active count 1–4, simultaneous chord mapping, preview, reset and save.
M12 UNFIT FALLBACK: original buttons cannot all fit; session-only Standard option, saved profile unchanged, explain conflicting controls.
M13 DISPLAY: Aspect Fit default, optional true native/integer scale if supported, portrait shell/fullscreen/floating touch, reset.
M14 VIEWPORT PREVIEW: effective source ratio, fitted image rect and bars; actual touch transform preview, no crop/stretch.
M15 GAME OPTIONS: core-advertised settings only, disc change/audio/fast forward where proven and Wii choice, CARTDECK presentation.
M16 ADVANCED: truthful runtime info, copy logs, previous verified engine when available, debug tools hidden in user build.
M17 X360 PROFILE: offline profile ID masked, game-native save instructions, missing-account safe recovery.
M18 ACTION RESULT: pause/save/load pending, explicit host ACK or unsupported/failure, retry/cancel where safe.
M19 RECOVERY: failed launch/graphics/engine freeze, diagnostics, safe exit and only verified rollback option.
M20 EXIT: stay/return; warn only when actual game-save unconfirmed.
M21 DISCONNECT: phone/tablet restores usable touch overlay; TV displays connect-controller + remote safe exit.
M22 REMOTE GUIDE: DPAD/OK/Back navigates CARTDECK, gameplay controller may be required.
M23 UNSUPPORTED: concise reason, real alternate path if one exists, back.
M24 SESSION ENDED: return to current cart/shelf with intact favourite, look, controller and save data.

## 3. Navigation and action behaviour
From gameplay MENU/Back/touch opens M01; Back inside subpage returns M01, Back on root resumes, physical MENU always escape even in capture; exit goes M20 then Shelf. Resume always first action and initial focus; all other rows capability gated. Do not imply Paused until engine acknowledges it. If native host cannot fully pause, title status is 'Menu open — gameplay may continue'. Controller UI input never propagates to guest game while menu/capture has ownership. On menu open release held guest keys; on resume reset stale held source; no stuck triggers or multiport migration.
M02 supports only core savestates: Save slot pending until ACK, Load confirmation because live state will be replaced, failed save retains prior progress. Native save games get M03 instructions, never mocked state buttons. On Android background while pending, preserve transactional data and show truthful result on return.
M04->M05->M06->M07: detected enabled pad (not basic TV remote), map with press/release capture, expose source and destination, allow collision rework, test actual game controls, Save scoped only after confirmation. Disconnect while mapping cancels without persisting partial mappings. Reserve MENU/Back as universal escape.
M08 only proposes verified exact game+console+region+edition action maps; unavailable shows 'No verified Modern Function profile for this edition — Console Authentic stays selected'. Example title evidence: Super Mario World Jump -> South; Super Mario Kart Accelerate -> RT; game semantics matter more than matching letters. No universal A/B inversion.
M09 Wii only exposes profiles advertised by core, saves actual choice by cartridge, clearly distinguishes button mapping from unavailable pointer/gyro signals.
M11 preview/validate actual native inventory before GripArc. Four stable G slots, chord/hold semantics source owned; if 48dp targets overlap or original buttons disappear, M12 offers safe Standard just for current session. Both Standard+GripArc and PivotArc+GripArc require physical proof.
M13 always fits video without crop/stretch and never creates a new renderer. Switching settings should not stop and restart guest game or recreate Cemu Vulkan swapchain unnecessarily.
M17 Xenia: says 'Save inside the game using your offline profile'; no Xbox Live and no savestate promise. Missing profile never automatically creates replacement over an existing player identity.
M19: 'Game stopped unexpectedly. Existing library and saves have been retained' only if storage untouched/verified; copyable error and Return; never loop fatal start.

## 4. Responsive frame design
Compact <600dp, medium 600–839dp, expanded >=840dp measured content width. TV is focus capability, not simply a wide tablet. All layout uses measured system bars, gesture cutout, actual control inventory and TV overscan.
320–392dp portrait: full-height scrollable menu; pinned header min 56dp; 16dp horizontal padding; rows min 48dp; no tiny/two-column touch forms. If 200% text enlarged, scroll—not truncate control actions.
393–599dp portrait: same panel with title/console and 1-column links; game remains in current correct aspect shell when menu dismissed.
Phone landscape: side panel up to ~360dp only when engine surface can remain stable with overlay; otherwise opaque full-screen CARTDECK menu. Never resize game simply to draw menu.
600–839dp tablet: 480dp max main panel, contextual game details if stable, 24dp safe padding, no duplicate video surface.
>=840dp tablet: max 560dp main content, optional secondary status region, retain original surface ownership and separate focus order.
1080p/4K TV: 10-foot DPAD navigable panel max ~760dp logical size, large focus ring, 32dp+ platform safe margin, scroll and Back from every route; no touch buttons even when controller absent.
Nintendo DS/3DS: two complete screens and stylus coordinates, portrait display exception; do not crop a lower screen to enforce widescreen.
WiiU/GamePad: native Vulkan surface remains in engine host; letterbox touch outside actual image rejected.
Video image fit: available safe rectangle (W,H), effective source aspect A. imageW=min(W,H*A); imageH=imageW/A; center. Correct pixel aspect where reported, no crop/stretch. Distinguish standard physical 4:3 glass aperture from actual GB 10:9, GBA 3:2, PSP widescreen, 4:3 TV source, Dreamcast 4:3 and DS combined image. Bars are not game pointers. Cover top-line bleed, notch, rotation, Android split-window and decorative trim.
Touch: Min 48dp effective targets, no overlapping original buttons, game viewport prioritised. Fullscreen pad present on touchscreen devices hides virtual shell and controls; disconnect restores touch immediately. Do not force portrait phone into fullscreen on connection.

## 5. Source authentic input inventories and design constraints
Source of truth: lemuroid-touchinput/src/main/java/com/swordfish/touchinput/radial/settings/NativeControlInventory.kt. Its enum and exact active profile must drive control rendering and GripArc validity; never use a generic reconstructed pad.
GB/GBC: DPAD, A/B, Start/Select, Menu. GBA adds L/R. SNES has A/B/X/Y, L/R. PS1 uses Cross/Circle/Triangle/Square, L1/L2/R1/R2, DualShock analog where real. PSP has its native single analog and L/R. GC retains A/B/X/Y, Z, C-Stick, shoulder and DPAD. DC retains analog stick and analog triggers. Original Xbox retains native Black/White and two sticks; X360 LB/RB/LT/RT and profile. Wii needs selected attachment-specific semantics, not imaginary motion. DS has two screens, touch/stylus and correct buttons. PS2 has both sticks plus shoulders/triggers. WiiU actual controller/GamePad only where source and host prove implementation. All Start/Select/Menu preserved.

## 6. Exact error, disabled and recovery copy
Pause not real: "Menu open. This engine may continue running."
Save states not supported: "Save through the game itself. Emulator save states aren't available here."
Save failed: "The save wasn't completed. Your previous progress has not been replaced." with Retry only if safe.
Unknown pad: "Controller detected. Map its buttons to use it with this game."
TV pad missing: "Connect a game controller to play. Your remote can still open CARTDECK."
GripArc collision: "This layout cannot fit every original control. Standard can be used for this session."
Modern Function unknown: "No verified Modern Function controls for this edition. Console Authentic remains selected."
Wii motion missing: "This controller setup doesn't provide the pointer or motion input this game needs."
Xenia profile gone: "Your saved Xbox 360 profile wasn't found. Restore its account before continuing."
WiiU render lost: "Wii U display couldn't be restored. Copy the report or return to the shelf."
Repeated Xbox restart: "This game is repeatedly restarting. Return to your shelf before trying another engine."
Fault: "The game stopped unexpectedly." plus verified data state, Copy report, Return.
Exit without known save: "Return to your shelf? Save inside the game first to keep your progress."

## 7. Workstream test and handoff contract
A: M00–M03, M15/M18/M20/M24, full menu path and save-state unsupported/failed; gamepads and TV DPAD.
B: M13/M14, source pixel geometry and real letterboxing, shell/control bounds, DS, WiiU, TV, top bleed.
C: M04–M07/M21/M22, pad detection, axes, held, chords, multiport and reconnect. Real device outputs required.
D: M09, Wii actual advertised modes and no fabricated pointer.
E: M10–M12, both combo modes left/right, G1-G4 and original inventory preserved; current PivotArc+GripArc fail stays FAIL until hardware proof.
F: M08, exact verified title actions, Authentic and Undo.
G: M17/M03, guest offline profile and real game-native save/reopen/install-over.
H: M16/M18/M19, spinner vs freeze, PSP visual errors, engine fallback, no false source success.
Cross-cutting: preserving cartridge UUID, SaveBinding, Room, artwork, styles, shelves, native HDD/NAND/cards, and exact APK signing across install-over. Marcus #248 independent QA.
Technical targets include: existing SharedGameMenuTest, GameMenuCapabilitiesTest, LibretroGameMenuTest, WiiControllerCompatibilityTest, NativeControllerInventoryConfigTest, Xbox360ProfilesTest; extend rather than replace.
No new shelf, GEM, ROM management, CartMods, catalogue screens in this lane: adjacent programme waves, protected through regression only.

## 8. Implementation-ready review state
Design specification: COMPLETE for A–H, source-anchored IA, state/interaction, layout, error strings, controller and acceptance. Final visual pixel matching requires rendered/installed screen review; no Android screenshot, CI or device test performed. Daniel approved with runtime-host verification conditions, Elliot/Marcus #266 implement and test, owner final acceptance. Model-authored design spec is not independent authentication of named human reviews.
