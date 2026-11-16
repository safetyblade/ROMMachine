# CARTDECK GC-01C — Physical Controller Consistency: implementation design

**Status:** Design-to-source handoff for Codex, 16 November 2026. Approved GC-01 A–H design remains the controlling authority. **Not** implementation, source-test PASS, physical QA or release approval.

**Consolidation input:** `integration/gc01-ps3-controller-candidate-20261116` @ `95c23db2de649dd3111fd3bd9dc2bde932d5d205`. Do not rebase this source contract onto older master. Reference PRs #271–#277, #276 and integration #279. Parent A–B menu/viewport geometry and PS3 recovery are intentionally combined.

## A. User experience / interaction contract

1. **During play:** user can enter CARTDECK with mapped Menu/Guide/MODE, the existing discoverable touch action, or the platform-supported TV remote Menu/Back. No global interception of Start/Select, PS, Xbox, or Switch-specific game buttons when owned by gameplay. Where a Guide key is intercepted, consume the entire down/up pair.
2. **Menu opened:** show app-owned CARTDECK controls over the unchanged active native surface. Enter on the **release** edge when opening a new Activity/overlay can otherwise receive a trailing key release. Engine pause status remains truthful (`Submitted` is not `Completed`). Suppress duplicate press, repeat, long press and spurious key-up.
3. **Menu root focus:** Resume is first focus only when safe. If a native session has failed, Resume is disabled/unfocusable, focus goes to a visible Recovery/Return to Shelf action. Preserve visible focus ring in Original/Pink/Green/StealthAcid/high-contrast.
4. **Submenus:** DPAD up/down moves among supported actions; left/Back navigates one route upward; right/OK opens focused action; Guide/Menu closes the overlay only when it can safely resume, else returns to recovery or Shelf. Navigation may never forward MENU or DPAD to the emulator while menu is focused.
5. **Controller list:** show real connected devices, player port, platform family, recognized descriptor and enabled state; unknown pads use a generic label. TV remote is a navigation device, *not* a gameplay pad. PS3 supports seven gamepad ports/independent users as implemented in PR #276.
6. **Connect/hotplug:** Xbox/PlayStation/Switch/generic HID + wired/Bluetooth reconnect should update devices and mapped ports with no game restart. On phone/tablet with no eligible pad, restore touch controls. On TV show `Connect a game controller to play. Your remote can still open CARTDECK.` and leave Menu/Back working.
7. **Map/test:** expose `Console Authentic`, `Modern Function` (only if a mapped function exists), `Custom`. Display physical SOUTH/EAST/WEST/NORTH -> authentic target, Start/Select, shoulders/triggers, sticks, hats, and connected port. Test offers pressed/released/held buttons, simultaneous 2+ button chords, diagonals, analog neutral and range, digital+analog trigger duplicates. Capture maps by actual device; include Cancel, Reset and Undo. Mapping UI uses minimum 48dp touch targets and TV focus routes.
8. **Recovery:** engine crash, firmware setup-only PS3, BIOS recovery, failed resume and controller disconnect all retain a safe visible exit. Do not advertise gameplay Resume when `resume=false`. No automatic relaunch or save-state operations during mapping.
9. **Layouts:** 320–392dp one-column scroll, 393–599dp phone one-column, tablet max 560dp content, TV 10-foot panel max ~760dp logical with >=32dp overscan and visible focus. Preserve GC-01B source-aspect rendering, DS stylus, Cemu GamePad touch coordinates and on-screen controls.

## B. Design state machine and ownership

```
PLAYING -> MENU_REQUESTED -> MENU_ROOT -> CONTROLLERS -> DEVICE -> TEST / MAP / REVIEW
  ^                            |                 |        |
  |                            +-- Save & Load   |        +-- confirm/reset/cancel -> DEVICE
  |                            +-- Recovery -----+-- Back -> MENU_ROOT
  +-- resume ACK or host-safe close <- MENU_ROOT
  +-- disconnect -> TOUCH_RECOVERY (phone) / TV_CONTROLLER_NEEDED (TV)
ERROR / SETUP_ONLY -> RECOVERY -> RETURN_TO_SHELF  (never implicit RESUME)
```

**No new global engine input dispatcher.** Libretro: `GameViewModelInput.initializeGamePadKeysFlow` -> existing `GLRetroView`; `GameMenuActivity`, `GameMenuContract` and `PhysicalControllerMappingPanel` own route/capture. Original Xbox: SDL `NativeCoreActivity` + `XboxHardwareKeyRouter` / native pad state. Xbox 360: `Xbox360Activity` + `Xbox360Pad`/profiles. PS2: `Armsx2RuntimeProbeActivity`, original `NativeApp` port mapping. Wii U: `WiiuVulkanRuntimeProbeActivity`, `WiiuPad` and `WiiuViewport.pointer` unchanged. PS3: `Ps3RuntimeActivity`, its seven-port player mapping and firmware/setup-only recovery stay authoritative. Android TV: `TVGameActivity` + `TVGameMenuActivity` / Fragment retain platform DPAD/Back support.

**Event edge and focus contract:**
- Host-only key pairs are **fully consumed**: DOWN arms; UP opens/closes once; CANCEL/device removed clears arm, held state and chord buffer. Ignore repeatCount != 0. Do not open on unmatched/orphan UP.
- Per-device key state distinguishes Android device IDs (ephemeral) from stable hardware descriptor/profile identity. Reconnect rebuilds live port assignment without stealing another player's mapping or mixing held state from an earlier session.
- A shortcut fires only when the combination first becomes complete, never on a constituent release and never multiple times per held chord. In-flight menu transition is idempotent.
- Host/menu lifecycle owns pause/resume. No fake `Completed` for void JNI commands; recovery command failures do not hide the menu.
- Ignore gameplay key/axis/hat dispatch while menu or mapping capture owns them. If capture is canceled, all held game inputs are cleared. Capture must not consume global Android Home/Power.
- Device classification comes from `GameplayControllerPresence` and `ControllerHardwareProfile`, without interpreting a TV remote as an active gamepad. Preserve existing enabled/disabled preference.
- Always expose a non-controller navigation route: hardware Android Back, TV remote Back or touch Return to Shelf.

## C. Source modules and allowed modifications

- `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/shared/input/GameplayControllerPresence.kt`, `ControllerHardwareProfile.kt`: device lifecycle and real vs remote-only detection.
- `.../shared/game/viewmodel/GameViewModelInput.kt`: chord edges / ownership with original Libretro port routing.
- `.../mobile/feature/gamemenu/{SharedGameMenu,GameMenuActivity,GameMenuCapabilities,PhysicalControllerMappingPanel,XboxGameMenu}.kt`: status, screen focus, capture, action navigation, mapping and truthful capability UX.
- `.../tv/game/TVGameActivity.kt`, `.../tv/gamemenu/TVGameMenuActivity.kt`: remote Menu/Back pairs and focus.
- `.../shared/game/xbox360/Xbox360Activity.kt`, `.../main/java/com/rfandango/haku_x/NativeCoreActivity.kt`, `.../debug/java/com/swordfish/lemuroid/app/debug/{Armsx2RuntimeProbeActivity,WiiuVulkanRuntimeProbeActivity}.kt`: reuse original native event owners.
- `.../debug/java/com/swordfish/lemuroid/app/debug/ps3/Ps3RuntimeActivity.kt`: PS3 seven player slots and recovery-aware Menu; no firmware updater replacement.
- Existing device mapping stores and cartridge identity only: no new Room version, save aliases, filesystem rewrites, native ABI, Cemu swapchain or PS3 storage migration.

## D. Codex verification contract — source/CI now, owner hardware at end of A–H

| ID | Source requirement |
|---|---|
| C-01 | Opening with Menu/Guide/touch/TV remote is single-fire on a matched edge; down/up consumed and no stuck game input |
| C-02 | Focus starts at valid Resume or Recovery; DPAD/OK/Back in nested screens, TV 200% text, safe 48dp controls |
| C-03 | Xbox, PlayStation, generic, Switch and remote-only classification is accurate; remote never offered as gameplay pad |
| C-04 | First connect, disconnect/reconnect and device ID churn keep correct player port, profile, touch fallback, safe TV exit |
| C-05 | Mapping captures single and simultaneous inputs (press/release/hold/chord/diagonal/axes/triggers) without leaks |
| C-06 | Bound conflict, duplicate and unavailable native action give recoverable feedback; Undo/Reset never alters saves |
| C-07 | Seven PS3 ports/virtual users and PS3 setup-only `resume=false` preserved after consolidation |
| C-08 | Original Xbox, Xenia, ARMSX2, Cemu, Libretro and TV retain engine-thread ownership and no menu surface reset |
| C-09 | Unavailable save-state functions remain hidden or informative; no engine performance instrumentation in normal gameplay |
| C-10 | Regressions: DS touch, Cemu pointer bars, Wii modes, PivotArc+GripArc fixed positions and source-aspect display |

Use focused deterministic Kotlin/unit/Compose source tests plus exact-SHA Android APK CI. Report every test attempted, outcome and artifact hash. Do not claim owner physical hardware pass or close #262. Keep rollback to the staging SHA and preserve a pinned working APK candidate once CI passes.

## E. Handoff gate

Engineering may implement without asking for further design approval on this controller UX: it implements the previously approved Sienna/Daniel GC-01 scope. **No functional hardware acceptance** until Jonathan finishes the entire A–H lane. If backend behaviour is unsupported, label it accurately and retain existing native operation, not simulation.
