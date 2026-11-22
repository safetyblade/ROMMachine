# CARTDECK GC-01 — Full Completion Design Supplement (22 November 2026)

**Status:** Proposed implementation handoff and cross-contract reconciliation; **not new owner acceptance, engineering code, Daniel approval, Marcus QA, or permission to merge**. Draft PR #362; preserve existing release/rollback safeguards.

**Purpose:** Close underspecified seams in the existing approved GC A–H, SafeSave, GEM, Junior and controller design **without replacing their extensive original contracts**. This document gives concrete screen/interaction, data ownership and acceptance guidance to Codex. Any new product policy explicitly flagged **OWNER DECISION REQUIRED** must be resolved before changing that behavior. All other guidance is a proposed implementation interpretation of the cited source contracts, subject to existing approval hierarchy.

## 0. Sources, precedence and known conflicts

Read these source documents alongside this supplement:
- GC owner programme #262, control #304, independent QA #248; historic implementation design docs/engineering/gc01/04_SIENNA_COMPLETE_UX_DESIGN.md on engineering/gameplay-consistency-gc01 (25 screens M00–M24, A–H), source-execution contract #262.
- GC current source/delivery: docs/engineering/GC-INTEGRATION-ACCEPTANCE.md, draft PR #362.
- SafeSave #290, docs/architecture/jr01/JR01_PROFILES_SAFESAVE_ARCHITECTURE.md, SAFESAVE_FORMAT_AND_API_V1.md, ENGINE_SAVE_SOURCE_AUDIT_2026-11-16.md, docs/design/jr01/JR01_FINAL_SCREEN_INPUT_PARENT_CONTRACT.md.
- GEM #193, #194, #210, docs/gemrating/GEM_B2_RATING_CHECKPOINT.md, GEM_B7_A_EXPANSION_CHECKPOINT.md, OFFLINE_ENGINE_CHECKPOINT.md.
- TV/controller #303, #308, #179, #172, #177, Wii #85, GripArc #135; SIGNATURE #360.
- docs/design/HOT03_UNIFIED_PRODUCT_NAVIGATION.md and HOT04_ROADMAP_UI_DELIVERY.md, with their historic status/limitations preserved.

**Precedence guidance, not a silent policy change:**
1. Current owner instructions, then the latest explicitly approved architecture/UX decisions, then older design drafts. Retain all original A–H requirements unless a newer owner decision actually supersedes them.
2. **Website searches/My Sources:** On current PR #362 at ce9633ff, website search was moved from GEM into CARTMOD with saved user sources intact. Older GEM/HOT03/B2 text naming GEM as website-search owner is superseded for this integration. GEM catalogue browsing/recommendations stay offline and must not invoke web search.
3. **Navigation:** Historic HOT03 text describes four adult product destinations, while its newer amendment points to five with Settings fifth. Implement Shelf, GEM, SafeSave, CARTMOD and Settings as distinct reachable product destinations using stable IDs; profile chooser is a header/session entry. Make no dead navigational tile. The cited B2 final handoff path is not present at the inspected current branch/master location; do not silently treat an inaccessible document as reviewed.
4. **Display conflict — OWNER DECISION REQUIRED before renderer policy change:** GC A–H design states source-fit with no cropping or stretching; #210 DeckFit also requires full source image at original proportions. Historical #162/#167 says explicit Fill may stretch 4:3 on TV/landscape. Preserve the current working native aspect/default/user preferences, do not force stretch or silently remove a user-selected mode. Present a choice and exact rendering semantics only after Daniel/Sienna/owner reconcile latest controlling intent. This is not a pretext to stall SafeSave/GEM/controller work.
5. **No false universal save-state claim:** #197/#255 establish Xenia Xbox 360 game-native saves, not native emulator save states. Some native engines may not support savestates. Back up whatever real native saves/states an adapter proves, classify absent capability openly.
6. Historic screen M00–M24 is **already a complete design for original A–H**. Reuse the controls, menus, focus and recovery designs; do not recreate an alternate UI. The supplement primarily defines cross-product integration and edge cases.

## 1. SafeSave — player-visible information architecture and data semantics

### 1.1 Navigation and initial states
**Proposed implementation interpretation of JR01/HOT03:**

SafeSave home -> current authorised player -> one of:
- **Protect My Progress**: one guided inventory + Back Up action across all detected supported save groups.
- **Games**: list by console; each title has separate **In-game Saves**, **Save States**, and **Shared Cards/VMUs** sections only when applicable.
- **Backups & Recovery**: immutable versions grouped by date, game/console/player, with verification badge and Restore.
- **Export / Import**: Android SAF file picker, receipt and import conflict preview.
- **Other Players / Everyone**: visible only after manager authorisation, with preview of owner scope.

No Save Centre item may be selectable if only a display-only UI callback exists; mount real handlers before publishing the destination. If not yet supported by an engine, expose that game in inventory with a truthful capability/coverage reason, not a green empty category.

### 1.2 Save-kind distinction — essential
**In-game save:** game-created SRAM, files, card blocks, NAND save-only files, savedata directories; restoring normally takes effect on engine/game reopening.

**Emulator Save State:** engine-created execution snapshot, including slot, preview and compatibility with exact emulator/core/build and relevant game/ROM identity. A state is **not interchangeable** with native saves or with another core's state.

**Complete Shared Memory Card/VMU:** one whole bank that can contain progress for multiple cartridges. The user sees its linked games, target console/engine and warning before export/replacement; never split arbitrarily or claim one game's archive contains only that game.

**Backup** means creating a verified immutable copy of detected source artifacts. **Save State Now** means instructing an actual compatible running emulator to create a state. **Restore** means applying a prior verified backup/state after correct quiesce and conflict handling. Distinguish these verbs and operations in every menu and receipt.

### 1.3 Game details and example layout
For an installed cartridge:
- Header: title, system, current player, engine, last successful verification, size and compatibility.
- Tabs/sections: **In-game Saves** | **Save States** | **Cards & Shared Saves**. Hide sections with genuinely inapplicable semantics, but keep unsupported save types discoverable under a clear explanation where useful.
- In-game save item: saved/modified date, backup version count, size, native save slot/region if verified, **Back Up** and **Restore previous** actions.
- State item: slot, manual/auto/resume category, source core/version, preview if available, **Back Up**, **Restore** (possibly **Load in Game** only when engine and active session support it). Show "State from another core — cannot load here" on mismatch.
- Shared card: Card 1/2 etc, complete bank size, linked titles and owner(s), **Back Up Entire Card**, **Restore Entire Card** after explicit multi-title warning.

Neither a cached path nor a core registry entry is proof of support. Each item displays one of the previously approved coverage statuses: SUPPORTED_VERIFIED, DETECTED_UNVERIFIED, SKIPPED_LOCKED, UNSUPPORTED, EXCLUDED_MIXED_CONTENT or LEGACY_UNASSIGNED. If no physical device proof exists, do not mislabel an adapter as physically verified merely because source/unit tests pass.

### 1.4 Protect My Progress / Back Up Everything flow
**Select scope** -> **Scan actual providers** -> **Review coverage** -> **Quiesce/close active game when needed** -> **Create snapshot** -> **Hash verify** -> **Optional SAF Export** -> **Verification receipt**.

Review must show, grouped by console, the number of save artifacts, states, card banks, estimated total size, active/locked groups, unverified detections, genuine exclusions and reason. Never show "Everything is safe" if even one detected eligible group was omitted. Preferred truthful completion copy is **"Backed up all verified supported items"** plus clear exclusions. For SAF providers that do not permit reliable readback: **"Written to destination; provider verification unavailable."**

Export scope Game, Console, Player, Everyone (authorised), full Memory Card/VMU and selected backup version must map to the existing v1 .cartsave protocol and its immutable manifest rather than six incompatible ad hoc formats. A game with shared bank prompts before including other titles' savedata. Unsupported state transport must not prevent valid native game-save backup; show separate counts.

### 1.5 Restore / import / copy decisions
**Choose backup** -> **Validate archive and hashes** -> **Select target player and verified cartridge/engine identity** -> **Preview affected game(s), state versions and ownership** -> **Choose Skip / New slot when truly supported / Replace with verified current backup** -> **Stage + re-verify** -> **Activate after engine close** -> **Reopen/check restored progress** -> **Receipt**.

Never select Replace by default. Reject unknown owner, ambiguous game/edition, unsupported state-core compatibility and mixed-content native storage. An archive for an uninstalled title may remain an inert, explicitly unassigned backup; **do not create a ROM, launch target, fake game or Junior allowlist permission just to complete import**. Cross-player transfer is a **copy** from immutable snapshot to independently owned target, not a shared writable save path. A manager gate is required where specified in JR01.

Receipt states: Working; Written but destination not independently verified; Verified locally; Failed (original untouched); Recovery required (backup kept). If restore cannot roll back, never delete recovery material or report full success. The real game still needs reopen/physical QA before a platform is marked verified.

### 1.6 Adapter-by-adapter coverage register
One current runtime-derived row for each registered SystemID + chosen actual backend, not a hardcoded thirty-system promise. Columns:
- Platform, engine/core and pinned source version.
- Native game-save format and actual roots.
- Manual state, autosave/resume/quick-save support (separate capability and verified status).
- Whole card/VMU/shared bank support; linked game/owner scope.
- Verified import/export/restore path.
- Guest/user identity mapping and cross-profile isolation.
- Excluded mixed content or unavailable encryption.
- Source test, real game save/reopen, exact APK/device evidence, and NOT TESTED/BLOCKED reasoning.

Specific restrictions: Xenia X360 native saves **without savestate claim**; distinct Play! and ARMSX2 PS2 card roots; PSP modern/legacy distinct SAVEDATA; GameCube/Wii save vs NAND distinction; Xbox save-only container vs HDD; PS3 savedata vs firmware/trophies/account; Wii U save-only vs NAND. No ROMs, firmware, BIOS, user accounts or installed game payload in .cartsave.

## 2. Controller mapping — finish the end-to-end interaction design

Existing M04–M09 and #308 T3 already specify guided discovery, capture, test, Save/Undo/Reset. **The remaining risk is mapping precedence and native host wiring**, not inventing another settings screen.

### 2.1 Plain-language route and steps
**Settings / In-game Controls -> Controllers -> Connected Devices -> Select physical controller -> Assign game port -> Choose mapping scope -> Mode -> Map -> Test -> Save.**

For every device card show real device name, connection type, connected/disconnected, unique session identity (where differentiable), active gameplay port, and separately **"Controls CARTDECK menus"**. Do not silently treat the app player profile as a gamepad port. An Xbox-style "A" key is a *physical button*; a PS1 "Cross" is a *guest action/input*. Show both.

Unknown pads may use a short first-use calibrate-by-press/observed-axis flow; known pads get a safe position-based auto-map and can immediately **Test** or edit. Do not force the guided wizard every launch.

### 2.2 Three mapping layers — must remain separate
1. **Physical device normalisation**: actual Android key/axis/hat/trigger capability, neutral/calibration, player-selected source device. Stable per device where verified.
2. **Guest-console translation**: physical position -> correct native guest key or analogue control for this installed engine/port, including key down/up, held triggers, DPAD diagonals, stick directions/axes and guest-specific utility buttons.
3. **Optional game-action choice**: **Console Authentic**, **Modern Function (only verified title/region/edition)** or **Custom** per cartridge, mapped to real original in-game inputs.

Suggested precedence for design review: explicit per-cartridge Custom > selected verified Modern Function profile > Console Authentic; shared device calibration/normalisation is always applied first. **Daniel/Sienna to confirm precedence** against the existing ControllerProfileStore, mapper and action resolver before changing persistent semantics. No global letter swap, no ambiguous title-only Modern Function, no duplicate delivery to Libretro and native host.

### 2.3 Mapping editor row and example
Each row: **Game action** (when known) / **Original console control** / **Your controller button** / **Change** / **Test result**. Examples clearly distinguish label and meaning: "Jump / SNES B / Face South (Xbox A)" or "Accelerate / native GBA A / Right Trigger" only for genuinely verified action profiles.

Tabs/filters: All controls, Buttons, Sticks & Triggers, Special/Attachments. Show complete *actual active* NativeControlInventory; never remove buttons because a GripArc/PivotArc attachment does not fit.

Actions: Map (press and release), Review Conflict, Swap, Cancel capture, Test, Save, Undo, Reset this game, Restore device default. Warn before leaving a dirty draft. Never silently save a test input or irreversible remap. Reserve a reliable MENU/Back escape during capture, including controller-only TV; disable capture if there is no guaranteed escape path.

### 2.4 Native host / lifecycle contract
For each active engine/backend explicitly state whether it accepts analogue sticks, analogue triggers, DPAD/hat, simultaneous diagonal, multiple guest ports, a menu shortcut, rumble and special peripheral APIs. **Source-only capability claims are not device acceptance.**

Input receiver is exactly one of shelf, cartridge detail, menu, mapping capture, tester, loading, game or app-pointer. On transition release held guest inputs; consume matched press/release; no guest gameplay event while mapper/menu has focus. Disconnect during mapping discards the draft and preserves old saved binding; disconnect in game releases axes and offers touch fallback on handheld or controller-needed status on TV. Do not assign P2 to navigate by default; P1 or explicitly chosen menu device does.

**TV acceptance journey:** connect unknown gamepad -> select it by controller/remote -> choose console or cartridge -> map and test without touch -> Save -> shelf -> focus a cartridge -> A/OK -> Open/Resume -> game actions visibly obey map -> MENU -> shelf -> reconnect -> unchanged map.

### 2.5 Special consoles: Wii, touch, motion
Wii chooses **Sideways Remote / Remote + Nunchuk / Classic** only where actual Dolphin/native bridge capability exists. Profile choice is per game and persistent; auto-choice only when metadata/host proof is verified. Distinguish ordinary key mapping from IR pointer, sensor gyro and accelerometer/tilt. A right-stick cursor must never masquerade as fully supported Wii IR without proven dispatch and coordinate mapping.

For DS/3DS stylus, 3DS tilt/motion (see issue #365) or Wii U GamePad touch, a physical pad mapping is not automatically equivalent to a sensor/multitouch input. Show exactly what inputs are available, choose original device sensor or optional stick emulation only when host implements it, and preserve touch coordinates/dual screens. Do not silently remove access to touch-only games.

### 2.6 Three controller mode screens
- **Console Authentic:** safe default, exact original guest input inventory, simple button diagram.
- **Modern Function:** explanatory verified game actions, recommended controls, one-tap Apply with Undo and display of original button output; unknown titles explicitly unavailable, no guessing.
- **Custom:** actual capture/test/editor, editable per-game override and original-device restore.

For hardware with missing buttons or triggers, show **"This controller lacks [input]; choose another input or supported layout"**, not a fake full compatibility badge. Analog full/neutral/diagonal, L3/R3, release after held and chord conflict must be observable in tester.

## 3. GEM — complete ratings, DeckFit and discovery handoffs

### 3.1 Four core user journeys
1. **Unrated player:** GEM opens offline; clear Browse Games and Rate a Game actions; no invented personalised "For You".
2. **Installed title:** Shelf cartridge -> GEM Rating and My DeckFit -> edit/clear/save -> re-open installed cartridge using same immutable binding.
3. **Uninstalled catalogue title:** GEM search -> release details -> voluntary rating/wishlist -> preserved "Not in Library" state; no ROM download or implied install.
4. **Player with ratings:** GEM -> For You -> evidence-based short reasons and eligible title details; adjust wildcard/console/played/hidden preferences; return to exact previous selection and focus.

GEM remains offline. External website search and My Sources are CARTMOD only, using user-added sources without automatic registrations. A catalog record may contain an external attribution URL but it is not an in-app ROM source.

### 3.2 Four separate scores/states
- **My GemRating:** whole-number 1–10 subjective enjoyment, null means unrated, editable/clearable per player.
- **Editorial DeckFit:** handheld playability estimate on 1–5 scale with explicit provenance and evidence tier; **provisional** is not physical gameplay verification.
- **My DeckFit:** player's own 1–5 judgement, editable/clearable; independent from editorial rating and enjoyment.
- **Runtime compatibility:** registered/engine present/per-title verified/untested/unsupported, never inferred from any rating or catalogue presence.

**Underdefined design choice:** half-point precision for DeckFit remains historical proposal, while existing repository rating models use integer scores. Do not silently change persisted range/precision; Daniel/Sienna to select an explicit compatible versioned contract if half-points are desired.

### 3.3 Universal catalogue coverage honesty
For every eligible release in the 88,246-row candidate, support showing DeckFit as **Provisional [score]** only when documented rubric inputs warrant that score, otherwise **Not yet assessed**. Never assign a fabricated confidence or 5/5 to fill coverage. Missing DOS/FBNeo/MAME2003Plus datasets remain honest gaps until suitable rights-verified coverage is available. Release rows are not unique game counts or tested playable titles. Unknown editions, mods/regions and title-only matches remain distinct unless evidence authorises linking.

Local catalogue updates cannot overwrite personal ratings, SaveBindings, old source IDs, or custom game edits. Preserve offline search/filter/pagination and usable performance without internet.

### 3.4 Preferences and recommendations
Specify default filters and reversible choices: console(s), installed/wishlist status, played/hidden, preferred genres, wildcard percentage, Not Interested. Keep recommendations deterministic and grounded in sourced attributes, with 2–3 clear reasons where data exists. A game lacking enough matching metadata is omitted or shows "Not enough detail", not random fictional similarity. A new/unrated user sees neutral discovery rather than manufactured preference.

Each preference change saves under current profile, survives reboot/upgrade, and cannot be inherited by another profile without explicit authorised transfer.

## 4. Profiles and Junior — safe entry, blocked routes and recovery

Reuse JR01 screen contract exactly: "Who's playing?", Manager/Standard/Junior, one visible Junior cover, "Grown-Ups", parent-gated exit and profile switching. Controller/TV focus must work from all these screens.

**Important unresolved security choice — OWNER/DANIEL APPROVAL REQUIRED:** secure parent-verification and recovery mechanism on actual Android devices (device credential vs own manager PIN/recovery secret), safe migration when no credential exists and lost-credential recovery. Do not invent a universal bypass, silently default to an insecure PIN, or promise OS kiosk protections. Manager access to other players' saves or "Everyone" export is a guarded privileged action.

Session switching requires engine quiesce, no mixed guest save roots, no leftover gameplay keys, and all permissions checked at service/repository/deep-link/native-launch entry. Existing Manager progress and GEM rating identity must be adopted non-destructively on upgrade. A newly created Junior profile gets no imported game access by default.

Blocked/edge screens to implement: manager authentication unavailable, no approved Junior games, imported save with no ROM, ambiguous legacy save, unsafe native guest isolation, engine cannot close, and return from Android Home. Each must provide a safe next action without wiping data.

## 5. Other original GC A–H handoff clarifications

| GC | Reuse approved design | Missing integration/acceptance detail to require |
| --- | --- | --- |
| A menu | M00–M03, M15, M18, M20, M24; one CARTDECK pause/menu | Console-by-console real native menu/pause/save ACK; clear "game may continue" state and Back/remote escape, no duplicate guest input |
| B display | M13–M14, source viewport geometry/DS dual screens | Reconcile Fit/Fill policy before altering renderer; real pixel bounds/top-line bleed/safe areas, no visual-only fullscreen claim |
| C controllers | M04–M08, #308 T1–T5 | Complete physical->guest->game-action contract, port assignments, no-touch mapping on TV, reconnect and two-player evidence |
| D Wii | M09; #85 approved attachment choice | Per-game verified attachment auto-detection versus explicit user choice; separate IR/gyro/tilt capability and safe fallback |
| E PivotArc/GripArc | M10–M12; full native inventory | Measured joint fit across side/width/orientation, 48dp hits, all originals and G1–G4, session-only fallback; prior device FAIL remains |
| F Modern Function | M08, #172 verified action catalogue | Exact title/edition verification, actual input down/up translation, one-step opt-in/Undo, no global A/B |
| G Xbox 360 | M17, #255 Xenia profile/save | Guest account/XUID binding and restore, source/physical save->exit->reopen; do not claim emulator save states |
| H engine consistency | M16/M18/M19; #262 roster | Separate GC player-facing workflow truth from PERF actual engine work; all actual SystemID entries have launch/video/audio/input/menu/save/restore evidence or NOT TESTED/BLOCKED |

## 6. Cross-cutting responsiveness and product finish

- **Handset:** one-hand/touch-friendly accessible targets, normal original buttons and sensible backup/controller actions; never overlay UI on game pixels.
- **Landscape/tablet:** shelf consumes measured content viewport, horizontal section paging and independent vertical scroll, preserved focus and artwork proportions.
- **TV/DeX:** ten-foot large focus targets, no mandatory pointer or touch, correct selected cartridge dialog ownership, remote Back from every screen including import, controller mapping and Junior.
- **SIGNATURE:** fixed app brand palette midnight #13131A / neon pink #FF4FA3 / cyan #19F5DA independent of selected deck appearance; approved original startup clip/audio and safe complete wordmark, saved sound/mute with app/system audio focus. Source hash audit is not device playback proof.
- **Advanced:** source reports, technical emulator options, developer diagnostics only under Advanced. Main product areas SafeSave, GEM and Controllers use everyday language.
- No inaccessible routes, no claimed success before native/storage acknowledgement, no destructive owner-state changes to "pass" a test.

## 7. Design-to-implementation verification before calling complete

For every design screen/action, record **Design reference / Component / Backend service or adapter / State transitions / Known unsupported reasons / Unit/UI test / Device test / Evidence**.

Additional required negative tests:
1. Backup Everything with a locked engine and a verified PSP save: report partial coverage, preserve both.
2. A cartridge with native save and four manual+autosave states: export/restore types distinctly, correct compatible core.
3. Full shared PS1/PS2 card used by two titles: explicit multi-title warning, full-card backup/restore and both-title reopen.
4. Xbox 360 on Xenia lacking savestate feature: native game-save backup shown, "Save State Now" honestly unavailable.
5. Unknown gamepad with swapped physical ABXY: normalise by physical position, map guest output once, verify press/hold/release in game and restart.
6. TV remote maps no gameplay port by default but can complete shelf/menu/Save Centre/Controller navigation without touch.
7. Wii peripheral-required game on pad without IR/motion: human explanation and honest blocked capability, no fake simulated success.
8. Offline GEM title with no DeckFit evidence: "Not yet assessed" rather than fabricated number; separate user edit remains.
9. Manager switches to Junior with active save write: quiesce and bind new owner before launch; no stale data leakage.
10. Portrait/landscape and TV genuine game surface geometry, appearance settings, intro audio and focus; owner/Marcus hardware results independent from source checks.
11. Full A–H exact registered console roster: each engine shows backend runtime evidence and save/reopen or meaningful NOT TESTED/BLOCKED, never inferred from 32/32 marketing count.

## 8. Decisions versus implementation blockers

**Design/policy decisions to explicitly resolve and record with Daniel/Sienna/owner (without blocking unrelated work):**
1. Display Fit versus opt-in/stretching Fill semantics and default for each device class.
2. Android parent authentication and genuinely recoverable lost-PIN flow.
3. Exact rating precision for DeckFit when half-points are requested versus integer storage.
4. Mapping override precedence where current stores/action mapping semantics differ from the suggested 3-layer model.
5. Whether an imported save from an uninstalled/ambiguous catalogue title is retained in an unassigned local backup inventory, with bounded retention/security design.
6. SafeSave database migration choice (separate SafeSave v1 store vs additive main Room), including verified old/new migration and rollback; must preserve users' already written snapshots.

**Not design blockers:** writing the existing mounted SafeSave UI; implementing supported adapters; wiring Controller Map/Test/Save; GEM browse/rating persistence; genuine source tests; same-head APK; on-device proof. These are engineering delivery/QA tasks against documented requirements.

**Release guard:** This supplement does not approve a source merge, issue closure or physical PASS. Build and physical evidence is tracked under #262/#248 and PR #362; PERF emulator defects and CARTMOD patching remain separate programmes.
