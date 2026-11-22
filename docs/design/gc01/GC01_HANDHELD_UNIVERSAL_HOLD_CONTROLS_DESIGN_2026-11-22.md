> **LATEST OWNER PIVOTARC EXPANSION — 22 NOV 20:12 AEDT:** [PIV-HH-01 dedicated 11-handheld PivotArc layout specification](GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) is a **new, separate approved design scope**. HHOLD-01 continues to govern only the **optional up-to-three-slot Hold Tray for Standard handheld touch**. Earlier HHOLD-01 wording "do not expand PivotArc to every handheld" applies only to the hold-tray task and is **not** a blanket prohibition: the new request explicitly designs PivotArc profiles for every registered handheld while preserving its original Hold A/B and no new H1–H3. No duplicate Standard tray on PivotArc. Do not alter GripArc G1–G4 or claim new source/QA success from design documentation.

# CARTDECK — GC-01 Handheld Hold Controls (HHOLD-01)
**Design created:** 22 November 2026 | **Owner:** Jonathan Prestwich | **Working PR:** #362 | **Design status:** Owner-scoped design proposal / Codex implementation handoff, **not** code/QA acceptance.

## A. Corrected owner instruction and design lock

**Expand HOLD functionality across handheld console gameplay; do NOT add three more permanent buttons to PivotArc.** The previous 22 Nov suggestion to add PivotArc H1/H2/H3 was incorrect and is **withdrawn**. Existing PivotArc **Hold A + Hold B** remain exactly as already designed and working in their supported layouts. Existing GripArc **G1–G4** remain untouched; those are separate programmable momentary/chord buttons, not three new held-state buttons.

Normal **Control & Setup lives under Settings → Controls & Setup**, not a dense in-game controller administration menu. Gameplay contains only direct **quick-hold actions** for supported layouts, the normal universal pause/menu, and optionally an **Open Controls Settings** shortcut with safe return to gameplay.

The goal is to let handheld games remain playable with touch when one button must stay held while the player uses a directional pad, stick, stylus or another face/shoulder button. First proof scenario remains **Pokémon Emerald Rogue: hold B while moving**. This is **not a PivotArc-only feature**.

## B. Actual handheld SystemID roster — do not guess by console label

Handheld source identities from current `SystemID.kt`:

| Handheld platform | IDs | Core touch/control design consideration |
| --- | --- | --- |
| Game Boy | `GB` | A/B, D-pad, Start/Select; the first Hold B + movement proof |
| Game Boy Color | `GBC` | Same physical family; per-game holds and colour/skin support |
| Game Boy Advance | `GBA` | A/B/L/R, movement; B-run and shoulder held game actions |
| Game Gear | `GG` | Original button inventory from active core; don't assume Android A/B labels match |
| Atari Lynx | `LYNX` | Original controls and orientation/right-vs-left layout; avoid system-specific button guess |
| Neo Geo Pocket | `NGP` | Native A/B and stick/D-pad according to active core input mapping |
| WonderSwan | `WS` | Native controls/orientation variant; do not obscure dual directional groups |
| WonderSwan Color | `WSC` | Same family with its own actual active input inventory |
| PlayStation Portable | `PSP` | D-pad, analogue, four faces, shoulders; backend may be native or Libretro; do not drop sticks/triggers |
| Nintendo DS | `NDS` | Two screens, D-pad/faces/shoulders plus stylus; hold tray outside active touch-screen interaction |
| Nintendo 3DS | `NINTENDO_3DS` | Two screens, circle pad/faces/shoulders/stylus, independent Tilt Stick / phone motion design (PERF #365 native bridge) |

**This is a source-registered handheld roster, not a claim each engine currently launches, supports guest key latches, or has physical QA.** Build the actual capability table from runtime `SystemID`, `NativeControlInventory`, selected core and guest input dispatch. Do not include SNES, PS1, desktop consoles or Wii in *handheld-specific default UI* merely because their games can be played on a handheld phone. Existing general touch input and GripArc behaviour on those systems stays unchanged. This doesn't preclude later extending the same safe component.

## C. Universal player design: optional three-slot Hold Tray

### C1. Settings design (primary)
**Settings → Controls & Setup → Touch Controls → Hold Controls**

- **Enable Hold Tray:** Off by default for existing installs so no controls appear unexpectedly; optionally surface a one-time, dismissible tip after a player first uses or manually enables a hold action. Preserve any existing saved PivotArc Hold A/B configuration.
- **Scope:** Global handheld default and **This Game** override, clearly shown with the console/current cartridge/active profile. Do not convert current PivotArc Hold A/B into three new assignments.
- **Hold Slots:** up to **three optional slots** for the universal handheld tray; label **Hold 1 / Hold 2 / Hold 3** in Settings, but the play surface shows **the actual guest action** (e.g. Hold B) not anonymous H1/H2/H3. Slot choices come only from the selected active handheld's real button inventory (face, shoulder or genuinely discrete inputs), not a global generic list.
- **Assignment:** Choose actual console input → Live Test/Preview → Save; optional Rename as a personal label without changing the guest control. **No macros**, sequenced commands, timed button loops, core options or game cheats.
- **One slot can mean one held guest input** in v1; simultaneous slots are supported only if host routing and source ownership work. Duplicate guest action assignments across slots are flagged and require an explicit owner choice or prevention where they cannot be tested; never silently clone outputs.
- **Layout:** left/right dock choice, visible slot count 1–3, compact/standard density where 48dp hit targets and complete native button inventory allow; no option that hides/overlaps source buttons or emulator screen. Gameplay-only Hold Tray, independent of permanent GripArc housing.
- **Per-game persistence:** store slot targets/layout/visibility by immutable cartridge UUID + original SystemID + player/profile; preserve safe global fallback and legacy SaveBinding aliases; no ROM scans/database migration solely to store hold preferences. Migrate/update safely and preserve saved appearance/controls.

### C2. In-game presentation
**A small, optional hold tray** close to controls but **not on top of the video aperture, touch/stylus screen, gamepad inputs, Android gesture zone or other accessories**. One to three 48dp+ touch targets; each shows e.g. **B / HOLD** and an unambiguous lit/latched indicator **only while the guest input is genuinely held**.

Touch once → latch guest button **DOWN**. Touch again → release **UP**. Normal tap of the underlying game's A/B/shoulder input still works and does not toggle the latch. Use visual pressed colour, small latch mark and accessibility announcement "Hold B: On/Off"; haptic cues obey accessibility/user preference. If a target doesn't fit, shrink decoration or move tray to safe edge; never shrink required touch targets, remove real console controls or crop screen. Show an explicit unsupported layout/fallback rather than pretending successful fit.

**Important PivotArc exception:** Preserve **its existing two nearby Hold A/B switches** and their current geometry/caps; **do not append Hold 1–3 to the PivotArc module**. When PivotArc is active, the existing switches remain its quick-hold gameplay controls. Do not automatically render a second hold tray duplicating them. A player can still edit handheld global/per-game hold preferences from Settings, but incompatible tray slots are shown as unavailable on this mode until Sienna's separate physically verified layout supports them. No new PivotArc H1–H3 or redesigned PivotArc hardware is approved by this scope.

**GripArc coexistence:** G1–G4 are separate action buttons/chords with their current full-L module. Do not hijack those IDs or expand the GripArc hardware; if Standard+GripArc has measured room, optional handheld Hold Tray must cooperate. PivotArc+GripArc remains **physical FAIL #135** until genuine device QA.

### C3. Handheld-specific placements
- **GB/GBC/GBA/GG/NGP/Lynx:** dock tray near unused outer touch-controller margin, preferably opposite to the D-pad without blocking face or Start/Select. In portrait, game viewport must stay original ratio and fully visible.
- **WS/WSC:** respect orientation and both directional groups; recalculate tray placement on portrait/landscape/orientation changes, and never co-opt an existing X/Y direction as a hold slot.
- **PSP:** avoid D-pad/analogue stick overlap and preserve face buttons, L/R, Start/Select and native 16:9 video. Optional hold tray must be repositionable rather than obscuring the screen.
- **DS/3DS:** keep both video screens legible and all stylus/touch targets clear. If usable touch bounds don't allow a 48dp tray, show supported Settings-only hold choices or hide tray with explanation, not overdraw. **3DS virtual Tilt Stick and phone motion are separate controls**, do not merge them into three Hold slots.
- **TV/DeX/physical gamepad only:** no extra decorative 3-slot tray if touch unavailable. Existing physical mapping and optional mapped hold shortcuts only when actually implemented, with Settings accessible by controller; do not suggest the touchscreen tray is a TV solution.

## D. Input contract — extend existing source ownership, not fabricate a second latching engine

**Source anchors verified on #362:**
- `lemuroid-app/.../shared/game/viewmodel/PivotArcHeldInputGate.kt`: source-ownership aggregation, one DOWN at first owner, one UP after final owner; currently PivotArc latchable inputs limited to **A and B**, with GripArc handled separately.
- `lemuroid-app/.../mobile/feature/game/PivotArcControlGroup.kt`: currently renders **HoldA/HoldB** in PivotArc.
- `lemuroid-app/.../mobile/feature/game/PivotArcLayoutProfile.kt`: PivotArc active only for **GB/GBC, GBA, SNES**. **Do not expand PivotArc mode to every handheld** to add universal holds.
- `lemuroid-app/.../mobile/feature/game/MobileGameScreen.kt`, `GameScreenLayout.kt`, `CompositeControlDeckSolver.kt`, `GripArcCombinedLayout.kt`: actual layout/safe-area boundaries.
- `lemuroid-app/.../mobile/feature/main/MainGameContextActions.kt` currently has a per-cartridge **Hold Controls** layout chooser. **Move full setup and hold assignment to Settings**; the in-game view may link to it or enable/disable currently configured quick holds only.
- `lemuroid-touchinput/.../settings/NativeControlInventory.kt`: actual native guest button set (including systems not in PivotArc).
- `retrograde-app-shared/.../library/SystemID.kt`: registered systems; platform eligibility is not a generic `HANDHELD` display-mode classification.

Use the **same input owner arbiter** for Standard touch/GripArc/PivotArc/physical input. Generalise source tokens and safe latchable button inventory in one place, not by toggling `pivotEnabled=true` for every handheld. A hold is a per-user action source, distinct from touch press and gamepad, and must not prematurely release a key pressed by either. If an engine lacks safe down/up mapping for a guest input, show it unavailable rather than a fake held indicator.

**No automatically persistent held state.** When opening menu/suspending, follow the tested current host pause semantics; on game stop, switching cartridge/core/player, activity loss, controller reconfiguration or unrecoverable error, issue exactly one final UP to the native guest. There must never be a stuck held button on resume, rotation or save-state load. A saved configuration does not mean an active latch survives app restart.

### D1. User workflow examples
**GBA / Pokémon Emerald Rogue:** In Settings → Hold Controls, assign Hold 1 = native B for this cartridge. Start game Standard touch; tap Hold B; move D-pad including diagonal; B stays DOWN; tap again and B releases once. A/B normal momentary remains usable, and save→close→reopen keeps assignment but not held state.

**Game Boy / turn-based RPG:** Hold A if the actual game benefits, but do not infer game action from filename or automatically force latch on. No unsupported turbo/autofire.

**PSP:** hold supported R while tapping another original face key; gamepad press and touch holds coexist; backend-specific down/up proven.

**DS / 3DS:** hold actual supported face/shoulder while drawing on stylus screen or using virtual Tilt Stick, with no screen overlays, no ghost gestures, and no false motion equivalence.

## E. Design decisions / safe defaults
1. **No additions to PivotArc.** Existing Hold A/B unchanged and should not be regressed. The optional three-slot tray is a **shared handheld Standard touch enhancement**, not three extra permanent hardware controls.
2. **Off by default** on upgrades and new games, with per-game opt-in; never surprise-lock a game input.
3. **Three configurable optional slots maximum** for first implementation, 1–3 visible only when assigned and enabled. If safe layout cannot fit 3, offer 1 or 2, preserving settings and full native controls; don't hide controls to force a number.
4. **Single real guest key per slot**; do not confuse Held Input with rapid-fire, macros, button sequences or analogue pressure. Touch pressure feature remains a different GC input component.
5. **Roxy/DeckFit:** A game may become easier to play using a verified hold control, but that doesn't automatically increase controller-free DeckFit. Editorial rating needs actual game input requirements and physical controls evidence.
6. **Player-facing Settings owns all setup**, including PivotArc/GripArc existing settings; in-game only quick controls and an optional link. Junior PIN policy still applies to protected settings.

## F. Implement with tests in SAME GC PR #362 — not another planning lane

**Functional delivery sequence:**
1. Inspect current hold input gate and actual console-native button inventory; add reusable optional handheld hold sources and stable per-cartridge binding without disturbing existing PivotArc and GripArc.
2. Build Settings → Touch Controls → Hold Controls using current app settings design; connect targets, 1–3 count, side, test, reset and per-game save.
3. Build only the optional Standard handheld tray with safe layout reservation. Preserve PivotArc Hold A/B, GripArc G1–G4, touch game video and original native buttons.
4. Wire real guest DOWN/UP, source aggregation, pause/close/reset and visual/accessibility state.
5. Verify with actual supported handheld source/engine capability matrix (GB/GBC/GBA/GG/LYNX/NGP/WS/WSC/PSP/NDS/3DS); if native host unsupported report exact engine reason.
6. Build a new exact-head ARM64 APK, include source tests and a Marcus/owner physical test matrix. **No master merge or physical PASS from code alone.**

**Tests required:** GBA B + D-pad 8 directions; Standard vs PivotArc existing Hold B unchanged; GBC/A, GBA shoulder, GG, NGP, Lynx, WS/WSC rotated control lists; PSP R + another key; NDS and 3DS stylus simultaneous; repeated tap/rapid toggle; physical gamepad + touch + GripArc one DOWN/UP per owner; game exit/background/rotation/suspend; missing native key; 320/360/393dp devices, Original/Pink/Green, landscape/portrait, hold tray 1/2/3 slots and left/right, zero overlap; install-over preservation of pre-existing profiles and old saves.

**Non-goals:** new PivotArc buttons, a second latch system, automatic emulation fast-forward/turbo, ROM modifications, changing engine selection, global forced hold key, redesign of GEM database, overriding existing WII/3DS sensor plan, or full rework of GC source.

**Governing links:** original GC #262 A–H; input #122; PivotArc/GripArc blocker #135; prior UI PR #294; [Sienna original GC 25-screen design](https://github.com/safetyblade/ROMMachine/blob/engineering/gameplay-consistency-gc01/docs/engineering/gc01/04_SIENNA_COMPLETE_UX_DESIGN.md); [controlling main GC source contract](../../engineering/gc01/GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md).
