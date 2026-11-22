# CARTDECK — PivotArc Handheld One-Hand Layouts
**Design identifier:** PIV-HH-01 | **Owner direction:** 22 November 2026 | **GC draft PR:** #362
**Status:** Detailed UX/engineering implementation DESIGN, not Kotlin implementation, APK acceptance, or Sienna/Marcus personal sign-off.

## 0. What the owner wants

Design **PivotArc for every source-registered handheld console**, based on the controls of the original handheld, maximising games playable with **one hand and one thumb** on an Android phone. Preserve the working tactile machined PivotArc identity; do not turn each console into a generic GBA controller. Keep complete original inputs, allow right/left handed setups and per-game choices, and be honest that some games need two independent simultaneous controls, stylus, sensor input or a physical gamepad.

**Not approved / not required:** three additional permanent H1/H2/H3 buttons in PivotArc; merging GripArc G1–G4 into PivotArc; replacing original Hold A/B; any compulsory automacro, autofire, auto-gameplay, button turbo or fake guest sensor inputs.

**Crucial distinction to prior HHOLD-01:** HHOLD-01 designs an optional up-to-three-slot **Standard touch** Hold Tray for handhelds; this NEW design explicitly expands **PivotArc console-specific layouts** (a new owner request). Their features are complementary but should never render duplicate hold controls over one another. Earlier HHOLD-01 wording saying "do not expand PivotArc to additional handhelds just to add holds" remains valid as historical limited scope, but DOES NOT prohibit this newly requested, deliberately designed handheld PivotArc family.

## 1. Source-verified foundation and exact original bindings

**Current supported PivotArc source profiles (22 Nov):** GB/GBC, GBA, SNES only, through PivotArcLayoutProfile.activeFor(system). Its baseline is a provisional owner-adjusted 393×852dp example; it is NOT a universal measured export or safe geometry for all handhelds.

**Source files to extend rather than replace:**
- PivotArcLayoutProfile.kt — activeFor, candidate C safe-area geometry, approved A/B/D face cluster and future X/Y envelope.
- PivotArcControlGroup.kt — actual PadKit output and existing A/B hold switches.
- PivotArcHeldInputGate.kt — source-ownership aggregator for one guest DOWN and one last-owner UP, PivotArc A/B holds, GripArc momentary chords.
- CompositeControlDeckSolver.kt, GripArcCombinedLayout.kt — safe placement, 48dp minimum, measured notched island and overlap constraints.
- MobileGameScreen.kt, GameScreenLayout.kt — actual source screen/insets, orientation and two-screen geometry.
- TouchControllerID.kt and per-system layout sources: GB.kt, GBA.kt, GG.kt, Lynx.kt, NGP.kt, WS.kt, PSP.kt, Desmume.kt, MelonDS.kt, Nintendo3DS.kt.
- NativeControlInventory.kt is currently a COMPLETE native inventory for a **limited set**, including GB/GBA/SNES/PSP, but returns null for several other handheld TouchControllerIDs. For these systems, derive verified input metadata from their existing PadKit layouts and guest adapter, or add safe accurate inventory entries; **never invent unsupported original keys or treat null inventory as playable proof**.
- Source identifiers for the eleven relevant systems: GB, GBC, GBA, GG, LYNX, NGP, WS, WSC, PSP, NDS, NINTENDO_3DS (SystemID.kt).

**Observed current source mappings that must be preserved:** Game Gear visibly labels native key A as "2" and native key B as "1"; Neo Geo Pocket visibly labels native key A as "B" and native key B as "A"; Atari Lynx maps Option 1/2 onto Android L1/R1 and its Pause to Start; existing WonderSwan has separate WS_LANDSCAPE and WS_PORTRAIT PadKit layouts; the current DS DeSmuME/MelonDS Mic/Screen auxiliary button key mappings differ, so use selected live backend. Never "correct" display labels by changing guest key codes without engine tests.

**Historical hardware reference for special-case layout (reference, not runtime test):**
- Atari Lynx reversible display and Option1/2 + Pause: https://atariage.com/Lynx/faq/index.html and https://github.com/asterick/atari-lynx-hardware-reference/blob/main/docs/01-system-overview.md
- WonderSwan X and Y directional clusters and orientation: https://mednafen.github.io/documentation/wswan.html
- Neo Geo Pocket click-stick, A/B and Option: https://manuals.plus/m/20e4f09a79a89a6534cd9ea4222dc8dc9a4c22101002c44aaaac667f57370dfe.pdf
The actual current CARTDECK PadKit IDs, active guest core and device measurements always take priority over generic historical iconography.

## 2. The PivotArc one-thumb physical design system

### 2.1 Common manufactured layout, adjusted by system
Every PivotArc handheld layout has a **single thumb island**, not the distant left/right original physical shell copied 1:1:
1. **Directional nucleus**, a true 4/8-way D-pad or verified virtual stick with diagonal/neutral feedback. At comfortable thumb centre inside measured safe controls deck.
2. **Action petals around nucleus**: A/B (or 1/2, four face symbols, Option) distributed in near-reach outer arc. Labels retain console meaning; no incorrect PlayStation/Xbox A/B assumptions.
3. **Secondary inner/outer reach ring** for L/R, extra original directions or context-sensitive guest utilities; these have independently operable >=48dp hit regions, with artwork permitted to look smaller than touch zone. No concealment of actual source buttons.
4. **Utility rail separate from fast controls**: Start, Select, Pause/Option and CARTDECK Menu, reachable intentionally but less likely to trigger during ordinary thumb slides. No replacing Menu escape.
5. **Existing PivotArc Hold A/B** (where those original buttons exist) remain adjacent to face keys in same geometry unless a console-specific approved layout proves a safe equivalent. These are **not extended with generic H1/H2/H3**.
6. **Mirror** for left vs right one-handed holding about the actual measured safe controls area; mirror positioning but **never reverse guest D-pad directions, X/Y labels, ROM video or the original key identity**. Save preferred hand and safe adjustment per game/player.
7. **Touchdown feedback**: real input event drives source-pressed/held highlight and optional haptic; visual treatment follows original approved machined PivotArc materials and named Original/Pink/Green/custom palettes. Do not create a second theme.

### 2.2 Three user-selectable interaction profiles, not three sets of hardware
- **Original PivotArc:** existing faithful A/B/D cluster, native buttons always present. Default for existing GB/GBC/GBA owner profiles, no change on upgrade.
- **One-Thumb Arc:** the same original keys arranged as a direction nucleus and closely grouped action petals, with a preferred hand; the new recommended default ONLY for newly enabled handheld PivotArc profiles when a measured safe fit exists.
- **Hold & Move:** same One-Thumb Arc plus explicitly configured latch of one or more supported original keys, so B-run + D-pad or L+action can be combined without needing another finger. No extra physical PivotArc H1/H2/H3; existing Hold A/B preserved. For other controls expose only a bounded console-aware hold selector using existing tactile control affordances and no invisible default key. This is OPT-IN and per-game.

**Optional advanced "Slide Action" (one-pointer combination)**: while the finger is held on the directional nucleus, it can travel into an outer action sector; the state machine continues the **last explicit direction** and sends A/B action down only while the finger is in that sector, then releases correctly on returning/UP. This is a proposed mode for genres needing move+act with one thumb and MUST be individually enabled/tested, not automatic. It must have clear thresholds and deadzone; moving between adjacent sectors must not spuriously fire. The last direction cannot remain stuck after finger lift, pause, screen rotation or loss. Do not remap this gesture to guest cheat macros. Prefer real observable combined key DOWN and UP over fake haptic/animation.

**Default input arbitration:** one active pointer owns the directional nucleus; a second pointer may press other real original keys; a physical gamepad can independently press buttons through the existing gate. The latching owner and current press owners aggregate so release of one finger never releases a still-held input from another. On exit/reset all session-latched keys UP; persistent saved presets are **assignments only**, never active key states.

### 2.3 Screen and ergonomics gates
- Portrait phone first, with dynamic measured safe controls area after game viewport, cutouts, system gestures, GripArc footprint and Android insets. Existing PivotArc current source requires portrait; treat landscape per-system adaptation as separately engineered rather than claiming an existing portrait solver is responsive.
- Minimum 48dp touch per discrete button; D-pad target accommodates diagonals; preferred 8dp clearance. The 393dp example is an illustration, not an absolute design for 320/360dp.
- **Preserve game-first display:** no clipping/cropping/stretching to make PivotArc fit, no hidden source console button, all original game-specific inputs still accessible via the displayed or clearly labelled secondary rail. On width failure use a truthful fallback: Original PivotArc if it fits, otherwise existing Standard touch; do NOT silently deform the game or drop Start/Select/face/stylus.
- Utility rail can collapse visual decoration, but never hide the actual Menu escape. Core video is always pointer-transparent to deck artwork, while the DS/3DS active stylus touch region receives real touch.
- Keep coexistence tests with GripArc, but the current owner-visible PivotArc+GripArc **FAIL #135** remains FAIL until joint measured solver and hardware proof. Do not assume room for both.

## 3. Individual handheld layout designs — all eleven SystemIDs

**Legend:** "Nucleus" is where thumb moves; "petals" are nearest game-action keys; "rail" is intentional infrequent actions. "One-thumb helpers" are optional modes, not evidence a specific game works. Every row must be backed by the active backend's original native inventory before UI can report support.

| System | Original hardware / currently implemented guest controls | PivotArc one-hand layout proposal | Helpful input modes / limits |
| --- | --- | --- | --- |
| **GB** | D-pad, A/B, Start/Select. Current PivotArc source supported. | Compact 4/8-way D nucleus with **B lower-left, A lower-right**, closer to one-thumb arc; Hold B/A keep approved adjacent switches; Start/Select on safe upper rail, CARTDECK Menu outer. | B-run/hold + D-pad, tap A/B, optional Slide Action; excellent candidate for verified one-thumb movement+action. |
| **GBC** | Same original button inventory as GB, color-era games may require quick repeated A/B. Current source supported. | Retain GB geometry and native identity; allow per-game right/left mirroring and local adjust without moving original output. | Hold B for run, B+direction; optional A/B adjacent action petals, quick release. No auto-turbo or game-specific action guesses. |
| **GBA** | D-pad, A/B, L/R, Start/Select. Current source supported. | **Preserve approved A/B/D triangle** and near-B L / near-A R shoulder satellites; one-thumb layout moves secondary L/R into reach without masking A/B. Hold A/B as already implemented; Start/Select/Menu rail. | Best candidate for Pokémon B-run + D-pad. L/R + A or B available with latching/sweep where tested; shoulder + directional shooters may remain two-thumb. Keep future X/Y reserved envelope only if approved for other systems. |
| **GG** | Game Gear D-pad, action **1/2**, Start, no original Select. Existing source shows native B as "1", native A as "2". | GB-size nucleus; action petals labelled **1** and **2** (not A/B in gameplay); Start on rail + CARTDECK Menu. No phantom Select/shoulder. | Optional Hold 1/2, slide-to-action for platformers; original Android key mapping maintained despite visible numbering. |
| **LYNX** | Directional pad, A/B, **Option 1/Option 2**, Pause. Lynx hardware has reversible display/control orientation; current PadKit source maps Option1→L1, Option2→R1, Pause via Start. | One-thumb D nucleus + A/B petals, Option1/2 as two distinct secondary arc buttons, Pause in intentional utility rail, Menu separate. Mirrored phone grip independent of **actual guest Lynx screen flip**; when game display flips, map guest original directions/Option combos correctly instead of cosmetic-only swapping. | A/B hold/slide for arcade movement; Option+Pause combos must still be expressible and safely guarded. Source-verified host orientation and game-specific Option handling required. |
| **NGP** | SNK click-stick (8-way feel), A/B and Option. Current PadKit **labels Android A→B, Android B→A**; keep guest identity. | Tactile directional disc with crisp 8-way and click visual; 2 close A/B petals, Option rail and Menu separately. No fabricated Start/Select both. | Fighting/combination games need proper diagonals and simultaneous A+B; optional source-owned holds. Quick motion responsiveness matters more than a decorative simulated mechanical click. |
| **WS** | Two distinct 4-direction button clusters **X1–X4/Y1–Y4**, A/B, Start (plus SOUND hardware control not universally guest exposed); horizontal vs vertical gameplay. Current PadKit separates WS_LANDSCAPE / WS_PORTRAIT. | **HORIZONTAL:** X cluster serves primary directional nucleus; A/B nearby, Y cluster remains independently available on collapsible-but-real secondary arc (do not alias Y to X); Start rail. **VERTICAL:** Y cluster is primary nucleus and X cluster remains a real secondary four-input petal/rail, A/B accessible. Layout rotates with active game orientation, not by literally mirroring X to Y. | Potentially one-hand friendly for games using one cluster; any game needing simultaneous independent X+Y inputs requires the two clusters or gamepad. Explicit on-device orientation chooser when metadata/runtime orientation unknown. No invented Y-to-X binding. |
| **WSC** | Same WonderSwan X/Y cluster topology and A/B/Start; orientation chosen per game. | Use the same **two distinct orientation-specific profiles as WS**, with colour/contrast independent of positional buttons. Keep X1–4 and Y1–4 positions/identities intact across orientations. | Aim to cover portrait puzzle and landscape RPG styles; preserve original both-cluster inputs for games using both. |
| **PSP** | D-pad, **one analog nub**, four symbols Cross/Circle/Square/Triangle, L/R, Start/Select. Existing controller layout has corresponding PadKit controls; game display 16:9. | **Input selector nucleus D-pad or analog** by verified game's input requirements. Four face petals in a compact diamond around the active nucleus; L/R as two real reachable secondary arc seats, Start/Select/Menu rail. Inactive D-pad/analog remains accessible in a clear secondary strip, never fake one for the other. | "Move with analog + press Cross" optional Slide Action/hold patterns; camera action and simultaneous stick+many buttons may remain difficult one-handed. Don't advertise right stick (PSP did not have a native second analog) or consume full display to fit controls. |
| **NDS** | D-pad, ABXY, L/R, Start/Select, **touchscreen + stylus**, microphone/lid auxiliary depending on selected DeSmuME/MelonDS core. Current Standard source differs on Mic/Screen button key codes by core. | **Dual-screen-first PivotArc**: preserve both top and bottom screens and active stylus touch area; compact D nucleus + ABXY petals in measured band outside both screens, L/R mini-seats, Start/Select/Menu rail. Optional **Stylus-first** preset moves secondary controls to one side and retains safe touch region for thumb tap/drag. Mic/Close control shown only if active core exposes working native action. | One-hand D-pad+face possible when safe space allows; games requiring *continuous stylus + D-pad* need explicit combo capability or alternate input; do not overlay a control on bottom screen. Landscape/TV DS fallback stays truthful under current screen policy. |
| **NINTENDO_3DS** | Circle Pad, D-pad, ABXY, L/R, Start/Select, dual screens/stylus, native gyro/accelerometer; some New 3DS titles use C-stick/ZL/ZR not universally present. | **Circle Pad** as primary analogue nucleus when title requires it; D-pad remains accessible independently; ABXY close four petals; L/R secondary; lower screen remains free for stylus. A separate optional **virtual Tilt Stick** docks beyond the main circle-pad island ONLY if measured space and actual guest gyro bridge allow it; otherwise phone sensor alternative with screen free. New-3DS-only buttons appear only if backend/game actually supports them. | Handheld one-thumb is conditional: Circle Pad+face is easier with hold/Slide Action; stylus+simultaneous controls and gyro+action often benefit from phone sensors or real gamepad. *Beyblade: Evolution* must respond to actual aim+pull-launch, not just animated tilt. |

**Important historical vs source discrepancy:** Neo Geo Pocket and Game Gear front labels differ from their underlying Android key IDs; WonderSwan orientation and DS auxiliary controls depend on the current core/layout. The design spec does not authorise changing emulation guest mappings to match simplified user-facing names. Source implementation must prove each platform's intended labels and actual native events in tests.

## 4. Make "as many games as possible" measurable, not a marketing statement

Offer three **game interaction patterns** and choose a conservative suggestion from actual control requirements:
1. **Menu / RPG / adventure:** directional nucleus, A confirm/B cancel, rails. Best reach and few simultaneous inputs.
2. **Platform / action / racing:** direction + frequently held action (run/accelerate) + another tap. Recommend Hold B/A (or verified console-specific held key), plus optional one-thumb Slide Action. No universal guess that B always means run.
3. **Fighting / shooters / rhythm / stylus / sensor-heavy:** preserve real diagonals and simultaneous face/shoulder/analog/stylus, honest "One-hand may require a configured hold, physical gamepad, phone sensor or two-touch interaction" notice. Never mark game fully one-handed from genre alone.

The player should see **Settings → Controls & Setup → One-Hand / PivotArc**:
- Console and selected game's original controls inventory.
- **Hand** Left / Right.
- **Layout** Original / One-Thumb Arc / Hold & Move (where supported).
- **Main direction** D-pad / analog / WS X / WS Y as verified.
- **Hold & Actions** choose genuine original guest key, optional Slide Action (off unless explicitly enabled), preview/test.
- **View / Adjust Position** with a safe live preview preserving game viewport; Reset to approved baseline; no unapproved per-title automatic mappings.
- **Game-specific** override saved per stable cartridge UUID + current profile, global fallback optional; a gamepad setup remains accessible if hardware needed.
- **One-Hand Confidence** as qualitative: *Good fit / Some simultaneous controls / Stylus or motion required / Unsupported bridge*, with exact reason from verified capability, not a fabricated DeckFit number.

**No automatic DeckFit inflation:** Editorial DeckFit v1 is whole 1–5 provisional unless evidence verifies control usability. A PivotArc layout may improve real touch ergonomics but cannot automatically upgrade all games on that console; Roxy may curate verified control-requirements metadata via her simple exact-ID GEM contribution interface. User GemRating 1–10 still alone trains personal recommendations; My DeckFit 1–5 is personal playability. This design does not require a new GEM database or recommendation engine.

## 5. Architecture and geometry: implementation without a second input/UI subsystem

### 5.1 Versioned profile rather than massive when(system) widget forks
Define **HandheldPivotArcProfile** (data-driven layout contract, not a new persistence database):
- key = original SystemID + validated TouchControllerID / emulator variant, original guest inventory signature and orientation (WS / Lynx / DS).
- nucleus = directional mode (DPAD, TRUE_ANALOG, WS_X_CLUSTER, WS_Y_CLUSTER) and actual PadKit key/axis identity.
- petals = ordered input keys + physical label + exact guest mapping.
- secondary = shoulder/Option/second directional cluster/DS mic etc.
- utility = Start/Select/Pause/Option/Mode and safe MENU route.
- special = DS stylus region, 3DS tilt sensor/extra stick, PSP analog, gamepad fallback, Lynx orientation.
- controls = original/one-thumb/hold-move, handedness, per-game override, live input tester capability flag.
- measured geometry = root safe bounds, current game viewport, every >=48dp touch rectangle, reserved active and future optional controls, GripArc collision, no false successful fit.
- evidence = source/engine backend verified, code-test verified, actual device NOT TESTED/PASS/FAIL separately.

Use existing PivotArcLayoutProfile and its owner-approved candidate-C geometry for **currently supported GB/GBC/GBA** without changing players' stored profiles. Additional profiles may be new small versioned profile assets/records, loaded and validated by ONE solver using existing PadKit input ownership, rather than hardcoding image skins and copy/pasting a screen for each console.

**Crucial:** PivotArcLayoutProfile.activeFor supports SNES too; keep the existing accepted SNES mode untouched even though SNES is not in the requested original handheld roster. Do not remove SNES from activeFor.

### 5.2 Geometry and gesture test contracts
- Solve layout only after exact viewport and touch-safe-area is available. Need lower island and all controls inside real measured deck with no overlap/inset; rendering and gameplay input share one position truth.
- At 320dp, 360dp, owner-class 393dp and tablet, test portrait left/right and supported landscape, themes, Original/Pink/Green/custom, text scale and large accessibility. For WS vertical/landscape and DS/3DS dual screens, prohibit false conversion of a portrait-only renderer.
- Preserve original directional 8-way diagonal movement and analog granularity, press/release edges, no stuck keys from gesture Slide Action or latches, back/menu/hold controls still responsive.
- Prefer authentic fallbacks to forced fitting: Standard, optional Hold Tray from HHOLD-01 on Standard, and gamepad when appropriate. **A gamepad is a legitimate fall-back, not a pivotarc regression.**
- Native controls must remain available; consider a secondary one-thumb **reveal/utility ring** only if a player can activate it without losing required guest down state, it cannot hide any essential button on an unsupported game, and state is clearly reversible.
- In modes with analog pointer/gyro, the underlying core must receive genuine motion signals; an animated graphical thumbstick is insufficient.

### 5.3 Explicit source integration sequence for Codex
1. Inspect active SystemID, selected core/backend, original Standard touch layouts and NativeControlInventory gaps; obtain a reliable guest button mapping for each of 11 systems before offering PivotArc in its UI.
2. Implement one reusable topology/geometry extension in PivotArcLayoutProfile / PivotArcControlGroup / CompositeControlDeckSolver; **do not rewrite** approved GB/GBA/SNES, source artwork or current input dispatch.
3. First deliver core minimal two-button geometry variants: **GG, NGP**, then **LYNX** with genuine Option1/2/Pause and source flip. Demonstrate real game input and UI accessibility.
4. Next deliver **WS/WSC with explicit X/Y cluster/orientation** and independent A/B. Do not fake 4-way directions by reusing a normal A/B petal code; verify both portrait and landscape hardware/game orientation.
5. Next **PSP** with independently retained D-pad/analog and Cross/Circle/Triangle/Square+L/R.
6. Finally **NDS + 3DS** genuine split screens, stylus exclusion zone and 3DS conditional native sensor/tilt link (PERF owner). Backend capability limited states are acceptable only when shown clearly; don't block progress for other handhelds.
7. Implement optional per-game One-Thumb Arc and Hold & Move / bounded Slide Action, plus settings, test and stable versioned preferences. Reuse existing hold source arbiter. Do not create separate PadKit controllers for each gameplay genre.
8. Supply real Android/engine tests, layout screenshot and fresh exact-head ARM64 APK for owner/Marcus physical testing. Keep PR #362 draft, no automatic merge/physical PASS.

**This is a recommended engineering order, not an excuse to stop at an intermediate profile. All systems remain in the design scope until evidence establishes a specific impossible backend capability.**

## 6. Formal UX acceptance by handheld

| System group | Minimum actual demonstration | Required negative / fallback |
| --- | --- | --- |
| GB/GBC | Original and new one-thumb layout source A/B/D, B-hold+movement, menu accessible | Existing PivotArc hand/position/hold setting unchanged on install-over |
| GBA | Hold B + 8 D-pad directions; A, L and R plus safe Start/Select | no lost shoulder or hidden future control on compact device |
| GG | visible keys 1/2 press actual existing engine guest A/B outputs | no phantom Select, no swapped source bindings |
| LYNX | A/B + Option1/2 + Pause, safe left/right/flip as supported | no Option-1/2 replacement with phantom L/R shoulders, unsafe combos |
| NGP | click-stick 8-way A/B, Option, native label mapping | never invert actual guest A/B without proof |
| WS/WSC | independent X1–X4 and Y1–Y4, A/B, Start, verified portrait and landscape selected routes | don't collapse secondary cluster into duplicate D-pad |
| PSP | real analog/dpad independent and full four faces + L/R, hold+action | native guest precision or UI space limitation truthfully shown |
| NDS | screen-safe D/A/B/X/Y/L/R, stylus drag, Mic/Screen when core supports | no lower touchscreen overlay, no loss of original input |
| 3DS | D-pad+circle pad separate, ABXY/L/R, stylus, additional virtual Tilt Stick and/or real phone sensor when available | disabled unsupported gyro bridge, no fake C-stick/new hardware input |
| All | left/right mirror, 48dp, safe portrait/rotation, pressed/held release and actual Menu, no player data regression | no build-only device PASS, no game screen cropped, correct existing PivotArc/GripArc status |

**Stop conditions:** actual source native-control identity missing; rendering an essential guest input is impossible within safe measured bounds; a backend lacks a real DS/3DS stylus/sensor bridge; physical layout causes loss of original controls; or data migration threatens existing profile/saves. Report exact subsystem and safe fallback, not "all eleven consoles passed" or new silent phantom mappings.

## 7. Coordination with current GC deliverables
This specific scope belongs under original GC **C/E** (physical/touch controls and PivotArc+GripArc), depends on source coverage of #122, #135 and the current HHOLD-01, and coordinates with #365 for 3DS sensor bridging. **It does not replace the universal gameplay menus, SafeSave all-console dashboard, controller remapping, GEM enrichment, Junior/TV, SIGNATURE or other GC A–H original requirements.**

Full configuration stays in **Settings → Controls & Setup → One-Hand/PivotArc**; in-game only the original quick controls, held-state indicators and short Settings link. Avoid turning the overlay into an admin UI.

**Do not treat this design-only document or git commit as a completed app feature, UI QA, release, merge or current APK.**
