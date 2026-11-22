> **NEW OWNER DESIGN SCOPE — PIV-HH-01 (22 NOV, 20:12 AEDT):** [PivotArc handheld one-hand layouts for all 11 source-registered handheld systems](../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) is now the controlling **DESIGN** for the new handheld-specific PivotArc expansion. Preserve current GB/GBC/GBA/SNES PivotArc source/presets, keep existing PivotArc Hold A/B and **do not add permanent H1–H3**. Expand console-authentic one-hand PivotArc profiles for GG, LYNX, NGP, WS, WSC, PSP, NDS, NINTENDO_3DS with exact original guest controls and honest screen/touch/motion limits. This is **not** the Standard touch HHOLD-01 optional Hold Tray and does not change GripArc G1–G4. Implement within existing GC C/E, same PR, without new design/backend or claiming device QA. If an older statement says never extend PivotArc to more handhelds, that narrower instruction is superseded by this **new owner request**. Preserve all original A–H / SafeSave / GEM priorities.

# CARTDECK GC-01 — AUTHORITATIVE OWNER IMPLEMENTATION HANDOFF
22 November 2026 | Owner: Jonathan Prestwich | GC draft PR #362

**Status:** Owner-approved product and UX decisions; this is NOT delivered feature code, build acceptance, a human team review/signature, independent hardware QA, master merge or release approval.

## 0. Mission and controlling precedence

**Finish the ORIGINAL ENTIRE GC programme A–H** under owner [#262](https://github.com/safetyblade/ROMMachine/issues/262), including SafeSave, GEM, Junior/profiles, TV/DeX, SIGNATURE and Wii/3DS special inputs. Implement rather than rescope. The current #362 GC branch is the ONE implementation lane. Keep PERF engine/core work separate; coordinate with #60/#365 without blindly merging native sources. No merge to master without Jonathan.

This document is the current single owner contract and supersedes any conflicting **recommendation or pending-status wording** in older 22 November design proposals. The detailed earlier records remain supporting references:
- [Original GC design gaps](../../design/gc01/GC01_FULL_COMPLETION_DESIGN_SUPPLEMENT_2026-11-22.md)
- [Role-by-role proposals](../../design/gc01/GC01_TEAM_DECISION_RECOMMENDATIONS_2026-11-22.md)
- [OD01–06 approved](../../design/gc01/GC01_OWNER_APPROVED_DECISIONS_2026-11-22.md)
- [Reciprocal swap / simple Junior PIN / Wii and 3DS motion owner lock](../../design/gc01/GC01_INPUT_MOTION_GEM_JUNIOR_OWNER_LOCK_2026-11-22.md)
- [GEM missing metadata and user-rating recommendation contract](../../design/gc01/GC01_GEM_RECOMMENDATION_DATA_REQUIREMENTS_2026-11-22.md)
- [Connector-ready GEM design — apply the SIMPLIFIED scope of §5 here](../../design/gc01/GC01_GEM_CONNECTOR_READY_DATA_ARCHITECTURE_2026-11-22.md)

### DO NOT REBUILD PREVIOUS UI — REUSE THE REAL DELIVERED WORK

| Existing UI/research source | Reuse, do not recreate |
| --- | --- |
| [Sienna full original GC design M00–M24 / components C01–C14](https://github.com/safetyblade/ROMMachine/blob/engineering/gameplay-consistency-gc01/docs/engineering/gc01/04_SIENNA_COMPLETE_UX_DESIGN.md) and [Daniel/Sienna architecture](02_DANIEL_SIENNA_DESIGN.md) | Entire A–H interaction state map: M01 menu, M02 Save/Load, M04–M08 controller mapping/test, M09 Wii, M10–M12 touch/GripArc, M13–M14 display, M17 Xbox360, M18–M24 safe recovery. |
| **Merged [PR #294](https://github.com/safetyblade/ROMMachine/pull/294)** | Existing Compose shared menus, controller discovery, mapping, tester, persistence, Display; enhance live UI and real guest dispatch, don't replace. |
| [PhysicalControllerMappingPanel.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/PhysicalControllerMappingPanel.kt), [NativeControllerMappingSession.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/NativeControllerMappingSession.kt), [ControllerSettingsActivity.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/ControllerSettingsActivity.kt), [ControllerCalibrationPanel.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/gamemenu/ControllerCalibrationPanel.kt) | Panel ALREADY displays native bindings, captures physical presses and offers a conflict-swap command. Use those existing stores, key capture and input probe; change discovery/preview/default action to the approved reciprocal two-press swap. |
| **Merged [PR #312](https://github.com/safetyblade/ROMMachine/pull/312) and [PR #317](https://github.com/safetyblade/ROMMachine/pull/317)**; [HOT03](../../design/HOT03_UNIFIED_PRODUCT_NAVIGATION.md) and [HOT04](../../design/HOT04_ROADMAP_UI_DELIVERY.md); [#303](https://github.com/safetyblade/ROMMachine/issues/303), [#308](https://github.com/safetyblade/ROMMachine/issues/308) | Preserve real Shelf/navigation compact landscape, horizontal sections/independent vertical scroll, ten-foot focus and controller-only journey. Extend MainNavigationRoutes/MainNavigationBar/HomeScreen/PagedShelfSections; no second navigation framework. |
| [JR01 final UX](../../design/jr01/JR01_FINAL_SCREEN_INPUT_PARENT_CONTRACT.md), [JR01 product spec](../../design/jr01/JR01_PRODUCT_UX_SPEC.md), [SafeSave API](../../architecture/jr01/SAFESAVE_FORMAT_AND_API_V1.md), [architecture](../../architecture/jr01/JR01_PROFILES_SAFESAVE_ARCHITECTURE.md), [ProductSurfaces.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/product/ProductSurfaces.kt) | Existing SafeSaveSurface, PlayerChooserSurface and JuniorPlayroomSurface are present but require production wiring; do not create a parallel save centre or child UI. |
| **Merged [PR #282](https://github.com/safetyblade/ROMMachine/pull/282)** and [GEM B2](../../gemrating/GEM_B2_RATING_CHECKPOINT.md), [offline engine](../../gemrating/OFFLINE_ENGINE_CHECKPOINT.md), [B7-A](../../gemrating/GEM_B7_A_EXPANSION_CHECKPOINT.md); [GemLibraryScreen.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/product/GemLibraryScreen.kt), [GemFinderScreen.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/product/GemFinderScreen.kt), [GemMyRatingsScreen.kt](../../../lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/product/GemMyRatingsScreen.kt) | The real user rating UI, offline catalogue and pure recommendation scorer already exist. Wire feature enrichment and stored ratings to these; do not create a new recommender. |
| **Open child [PR #368](https://github.com/safetyblade/ROMMachine/pull/368)** | B7-B provisional touch-only DeckFit work targets GC #362 but is NOT merged/accepted. Inspect and reconcile without auto-merging, rewriting scores or assuming game runtime verification. |
| [Modern Function research PR #175](https://github.com/safetyblade/ROMMachine/pull/175) / [#172](https://github.com/safetyblade/ROMMachine/issues/172), [#135](https://github.com/safetyblade/ROMMachine/issues/135) GripArc, [#85](https://github.com/safetyblade/ROMMachine/issues/85) Wii, [#365](https://github.com/safetyblade/ROMMachine/issues/365) 3DS, [#360](https://github.com/safetyblade/ROMMachine/issues/360) SIGNATURE | Respect approved action matrix, native controls, PivotArc/GripArc inventory, motion peripherals and intro video/audio. Documentation alone never proves game action. |

**Single product navigation:** Shelf / GEM / SafeSave / CARTMOD / Settings, with Profiles in header. Website search/My Sources remains in CARTMOD; GEM is offline catalogue/recommendation. Controller settings are ordinary player-facing Settings, with diagnostics under Advanced.

## 1. Complete owner decision register

- **OD01 Display:** Default original aspect / Fit. Optional explicit, renderer-backed Stretch to Fill (may distort, never crop); preserve stored choices and DS/3DS/Wii U special viewports.
- **OD02 Junior:** Simple parent PIN to exit Junior, access Grown-Ups or edit protected settings/allowlists. Local setup/confirm/change, rate-limited salted protected verifier; no mandatory online account, biometric or Android system credential. No universal bypass, no OS-wide kiosk claim.
- **OD03 Rating precision:** Always **whole-number personal GemRating 1–10** and **whole-number My DeckFit 1–5**; editorial/provisional DeckFit 1–5 or Not yet assessed. No half-points now or planned. Only user-entered GemRatings are preference-training data.
- **OD04 Mapping:** Two physical presses, live highlighted controller view, **reciprocal SWAP as default** rather than Replace/Cancel-default. RT's old guest action moves to X, X's old action moves to RT; Confirm then Test/Save/Undo/Reset. **Analogue-to-digital swaps carry an explanatory WARNING ONLY; the player may explicitly proceed, never block because pressure fidelity differs.** Three-layer physical normalisation → correct guest engine/port → per-cartridge Custom override > verified opted-in Modern Function > Authentic. Preserve old mappings; no global A/B swap.
- **OD05 Unassigned saves:** Safe validated imported orphan archives visible as **Unassigned Backups** in SafeSave; user-controlled explicit link/export/delete only, immutable and never auto-linked by title.
- **OD06 Data safety:** Preserve existing SafeSave snapshot/index v1 database. Only additive necessary main Room profile/ownership migration and durable journal; no destructive schema reset or save alias overwrite.
- **GEM connector/scaling:** Provider-neutral metadata contribution interface, indexed fast offline reads and a versioned optional overlay; game release universe finite historically, metadata grows. User/team annotations easy to add/edit, no heavy user-edit governance. Packaging redistributed third-party material must respect actual source rights; private ratings never exposed.
- **SafeSave ALL:** Console summary shows distinct games with saves, manual states, autosave/resume states, complete shared cards/VMUs, unassigned backups, verified archive counts, per-console exceptions. Back Up Everything and Export All cover all **actually supportable** items, with transparent exclusions, not blocked by unsupported state support on another engine.
- **Wii+3DS:** GC owns an adaptable, per-game **Choose Your Controls** experience. **Wii defaults to a connected gamepad** with matching proven Sideways/Nunchuk/Classic configuration, with optional **virtual on-screen IR pointer/tilt** and **phone gyro/accelerometer motion**. **3DS offers an independent additional virtual Tilt Stick** plus optional phone sensors, preserving Circle Pad and stylus. Allow combined gamepad buttons + virtual pointer/motion. Choose/recommend by *actual device, core capability and game needs*; never claim a fake sensor bridge. PERF owns guest sensor/IR/core/host integration.
- **Original GC:** A–H remains entire finish line. No merge/QA PASS on UI alone or green CI.

## 1A. FINAL OWNER HOLD CORRECTION — HANDHELD-WIDE, NO PIVOTARC BUTTON ADDITIONS

**OWNER CORRECTION:** The earlier proposed extra PivotArc H1–H3 buttons are **NOT APPROVED and must NOT be built**. The intended change is **universal handheld hold-control availability**, using console-aware optional touch actions, **not an expansion of PivotArc's existing physical/control layout**.

**Controlling full design:** [HHOLD-01 Universal Handheld Hold Controls](../../design/gc01/GC01_HANDHELD_UNIVERSAL_HOLD_CONTROLS_DESIGN_2026-11-22.md). This directly supersedes the wrong 22 Nov evening draft and associated obsolete GitHub comments describing H1–H3 added to PivotArc.

- Normal configuration remains **Settings → Controls & Setup → Touch Controls → Hold Controls**. In-game pause offers only a short summary/shortcut to settings; no duplicate full mapping/setup editor.
- **PivotArc:** preserve the existing **Hold A/B** buttons, placement, key-down/up semantics and existing settings, **no additional three permanent H1–H3 controls**. Existing PivotArc A/B working acceptance retained. GripArc G1–G4 remain different programmable momentary/chord controls.
- **Handheld Standard touch layouts:** enable an **optional compact Hold Tray with up to three configurable quick-hold slots** for actual supported guest buttons, only when enabled and safely fitted. Actual guest labels on gameplay e.g. Hold B. Slot targets assigned in Settings by active system/cartridge; no forced/default generic key mapping. Do not add these to the PivotArc module.
- Scope based on source `SystemID`: **GB, GBC, GBA, GG, LYNX, NGP, WS, WSC, PSP, NDS, NINTENDO_3DS**, using each active backend's actual NativeControlInventory and capable input host. This is source roster, **not a claim 11 engines physically passed**. Preserve WonderSwan orientations, PSP analog, NDS/3DS dual-screen stylus and 3DS Tilt Stick separately.
- Shared source-ownership and correct guest **DOWN on first owner / UP on last owner**; held state does not persist across game close, player switch or unsafe lifecycle transitions, while configuration does. No stuck keys, input leak, ghost supports, overlapping 48dp targets or missing native buttons.
- Reuse `PivotArcHeldInputGate.kt`, `PivotArcControlGroup.kt`, `GameScreenLayout.kt`, `MobileGameScreen.kt`, `NativeControlInventory`, `CompositeControlDeckSolver.kt` and current settings UI, not a second latch engine or redrawn handheld core controllers. Do not activate PivotArc on new systems just to support generic holds.
- Initial console gameplay proof: **GBA Pokémon Emerald Rogue Hold B + D-pad**; tests for listed handhelds with actual guest input and no screen overlap; previously owner-accepted PivotArc behaviour and GripArc fit failure #135 remain recorded accurately.
- Keep all original GC A–H, SafeSave, GEM, TV and SIGNATURE scopes intact. No owner/Marcus physical QA PASS from this design.

**NO NEW PIVOTARC H1/H2/H3. Shared handheld tray is a separate, opt-in touch-control enhancement.**

## 2. SafeSave — finish the existing surface and real services

On the existing SafeSaveSurface home show **current player**, games with native saves per console, occupied manual save-state slots, auto/resume slots, complete cards and VMUs, backup totals, unassigned verified archives, latest backup, Export All, Restore and Exceptions.

**Count meaning must be exact:** "Xbox — 5 games with saves" means five distinct verified cartridge/game save groups, **not five files**; "PS2 — 5 games with saves, 3 manual states, 2 complete cards" is illustrative ONLY; one shared card with two games cannot be exported twice or counted as two cards. Never hardcode values or infer from registered core list. Show unsupported state count as unavailable, not zero if not measurable.

**Back Up Everything / Export All:** discover real supported engines and all player-authorised save roots, native game saves, state files/metadata/previews, whole shared memory cards/VMUs, safe orphan archives. Show preview of exact included/excluded/locked/unknown groups, sizes and affected players; export all supported data even if another engine has no savestate API. Quiesce writers, stage, SHA verify, bounded portable .cartsave through SAF, verify provider where possible and return receipt. No ROMs/BIOS/mixed NAND/HDD content. Restore validates exact ROM/core/engine identity and state compatibility, backs up destination, journals and verifies activation/reopen, rolls back on failure. Show persistent Exceptions for Research: engine, kind, count, reason and next action. Existing SNAPSHOT v1/old saves must remain safe. No fake "all saved" when something failed.

**Direct UI acceptance:** visible working Save Centre in Android APK, functional Back Up Everything and Restore from actual in-game save + save state, complete per-registered-console status register, no destructive old install migration, independent physical games proof for claiming actual console PASS. One unavailable state engine is a documented limit not an excuse to skip backups from supported engines.

## 3. Controller mapping — visually live two-press reciprocal exchange

Reuse PR #294, M04–M08 and the current PhysicalControllerMappingPanel/NativeControllerMappingSession.

**User flow:** Connected gamepad → select controller, global or This Game and port → **Press button whose guest action to move** (RT) → pad visual lights RT, shows previous output A → **Press replacement physical button** (X) → pad lights X/output B → preview clearly shows X→A and RT→B (both preserved) → **Confirm Swap** → Test actual engine-bound native guest press/release/axes → Save. Undo/Reset/Cancel/visible draft, TV/gamepad-only setup. Don't make user first pick a line in a dense table. Diagram/list alternative for non-emitting physical keys/accessibility. Menu/Back escape never becomes unreachable; capture never leaks game/shelf events.

**Special safety — OWNER FINAL:** Swapping a continuous analogue trigger with a digital face button loses proportional pressure; **show a clear WARNING but DO NOT BLOCK the swap**. The player can Confirm and Save; explain the digital button will generally mean full press/release rather than variable throttle/brake. Preserve the reciprocal other action, native guest control and Test/Undo/Reset. Do not misrepresent full analogue fidelity, even when the owner chooses reduced fidelity. If a target button has no previous assignment, explain the consequence and keep an accessible way to restore native buttons. P1/P2 profiles and menu ownership are distinct. Reconnect and neutralise held states; test actual Play!/Wii/Libretro/Xenia guest dispatch and verified Modern Function.

**Additional pressure-touch gameplay support (part of GC Controls, not a new engine lane):** When a game expects variable analogue input (e.g., racing acceleration/brake), support an optional **touch analogue trigger** using an on-screen graduated slider/arc or swipe gesture with visible 0–100% position, smooth range and return-to-neutral on lift. A configurable press-and-hold ramp is an accessible secondary mode if physically useful. On hardware where Android touch-event pressure demonstrably has a reliable calibrated range, optionally use real pressure as one more input source, but **never assume a normal touchscreen measures physical force**. A basic tap can be configured as full press; clearly label it as digital. Preserve source-native gameplay keys, multi-touch ownership, GripArc/PivotArc hit-region spacing, TV focus and input release when game pauses or app backgrounds. Map analogue values only into an engine guest analogue axis where real support exists; otherwise expose a plain-language limit and maintain the user's chosen digital swap. Test partial throttle/pressure, sustained hold, neutral on release, rotations, per-game save and native game response.

**The previous 'warn or block analogue↔digital swaps' guidance in earlier records is superseded; WARNING ONLY is controlling.**

## 4. Shared Wii / 3DS Motion & Pointer controls

**GC UX — setup-based choices, per-game saved preference:**

- **Wii default:** a **connected gamepad** in the best *verified game-compatible* attachment profile (Sideways Remote, Remote+Nunchuk, Classic). For an unknown/IR-required title, show capability and let the user select rather than silently claiming a gamepad can cover motion/IR.
- **Wii Virtual Pointer:** an **on-screen drag/aim + tap/click interface** emitting actual Wii IR pointer events through native support, optionally anchored to the gamepad right stick; this is NOT a requirement for a physical Wii Remote or sensor bar. Distinguish visible crosshair from an emulated pointer that the Wii title truly receives.
- **Wii Motion:** optional Android phone gyro/accelerometer and, where valid, onscreen gesture/tilt controls for shake/swing/aim; per-game calibration, angle/acceleration mapping and actual host/guest capability, not D-pad emulation.
- **3DS Virtual Tilt Stick:** an **additional independent on-screen stick** mapped to *emulated 3DS gyro/acceleration*, separate from Circle Pad/C-Stick/D-pad and touch/stylus. Provide recenter, sensitivity and a separate **pull/launch gesture** for *Beyblade: Evolution*. A generic joystick-to-digital mapping is NOT an emulated sensor.
- **3DS Phone Motion:** use the Android phone's actual sensor signals as another selectable source; support switching between hardware motion and virtual Tilt Stick, without activating both accidentally.
- **Mixed setup:** allow a gamepad to provide ordinary game buttons **while the touchscreen virtual pointer/tilt or phone sensor handles the missing special control**; don't force an all-or-nothing controller mode. Choose a sensible device-specific recommendation based on installed controller, touchscreen, actual sensors, game requirements and the *active core's* capabilities; keep manual override and persist by player/cartridge.
- **Off/Authentic:** do not hide normal/native controls; don't show unsupported modes as functional.

All modes need visible **Test Motion/Pointer**, calibrate/recentre, measured responsive >=48dp non-overlapping targets, per-game settings, no loss of original DS/3DS screens, GripArc/PivotArc, Android TV/DeX fallback and a truthful disabled notice if guest interfaces are absent. When modes change/pause/background, stop sensing and set guest inputs neutral. Default to gamepad for Wii whenever a real compatible gamepad route exists; **do not default Wii to phone motion**.

The DeckFit model may consider verified usable controls (virtual pointer/tilt, real phone motion, gamepad) as separate evidence categories. Do NOT silently raise an editorial DeckFit merely because a UI toggle or theoretical bridge exists. A game-specific, controller-free DeckFit estimate remains distinct from the user's actual optional gamepad setup, user My DeckFit and physical compatibility proof.

PERF #60/#365: verify real Android accelerometer/gyro/rotation signal into actual guest emulator (Citra/Azahar/Dolphin) using pinned/native host supported API. Show "not supported in this engine" if not wired; do not claim core support from an Android sensor callback alone. Keep Culdcept #311 black-screen as separate PERF defect; do not transfer Wii original GC UX into PERF.

## 5. GEM — MINIMAL Roxy/team add-to interface; do not redesign the database

**OWNER INSTRUCTION: Do not spend this pass comparing databases, designing a schema platform, building a plugin framework or selecting a new recommender.** The current catalogue/package and GemRatingRecommender/GemDiscovery/GemGameplayFeatures already provide a straightforward place to connect new metadata. Keep that baseline, avoid app database/Room migrations for basic game facets, and implement one small stable source contribution input.

### 5.1 Simple contributor contract

Roxy/team can add or amend **a batch of exact-ID feature rows** in a plain CSV or JSON file, or call one equivalent provider-neutral programmatic importer. Each row supports:
- **releaseId**: required exact existing stable source-qualified release key.
- **primaryGenre, crossoverGenre, playstyle, mechanics[]**: optional P0 fields already accepted by the recommender; normalize tags and allow blanks/unknown.
- **optional controls/peripherals and session style** only where data is available and will actually be consumed. These are P1, do not delay P0.
- **sourceId / source record / revision / evidence** where available, for provenance and included third-party rights/attribution. Ordinary team/user annotations require no onerous manual review workflow; private player rating data is never in contributor rows.

**One implementation seam:** Read contribution → validate exact ID/schema/tags → preview **added / updated / rejected with per-row reason** → apply an **additive versioned GameplayFeatures overlay** keyed by release ID → existing GemDiscovery projects merged fields into existing GemRatingRecommender.Game → existing GEM Browse/For You displays the effect. Preserve old base release IDs, existing ratings and previous version on error; no scanner, save, engine, controller or Room changes.

- No need to preload or rewrite 88,246 rows; start with zero overlay and additive 20–300 record batches, easy for Roxy to extend. Team should not have to edit Kotlin per game, create a GitHub issue per tag or wait for the whole arcade DAT ingestion to see a working result.
- Data from a future connector enters via **the same importer**. Connector-friendly means a documented stable format and validation, NOT executable network plugins or continuous update jobs inside the Android app in this pass.
- Existing memory/indexing approach is acceptable **unless objective startup, search, RAM or ranking tests demonstrate a real bottleneck**. Fix measured problems locally, not speculative 500k-scale replatform. Preserve offline operation and a rollback-able pack, avoid resetting user data.
- Historical game releases are finite, but metadata and user corrections can be enriched. Don't impose strict human approval on personal notes/genres. A bundled/re-distributed externally sourced dataset still needs lawful source rights and traceability.

### 5.2 Make the existing designed recommendation logic truly function

Existing source facts on current GC branch:
- GemCataloguePackage.Release stores release ID, title, platform, optional primary genre but not widespread playstyle/mechanics.
- GemDiscovery.prepare currently calls a small GemGameplayFeatures.find lookup for crossover, playstyle and mechanics; missing features become empty/weak context.
- GemRatingRecommender already accepts these fields and **only explicit user GemRating preferences**, with offline deterministic scoring and existing 16 fixtures from PR #282.

**The task is connecting incremental sourced facets to that existing projection/scorer**—not building another ranking model or database.

Required evidence/tests:
1. Add sample exact-ID source contribution to a genre/mechanics playstyle title; confirm loading and GEM Browse display.
2. A player enters whole-number 9/10 GemRating; with sourced shared features the related candidate increases rank, with grounded reason.
3. The same player's explicit low GemRating reduces related preference; clearing returns baseline without residual training signal.
4. A second player has independent ratings and results; no Roxy/DeckFit/critic/popularity score is secretly passed as player rating.
5. An unknown release ID/bad tag yields a clear rejected row; existing catalogue and prior ratings continue unchanged.
6. Adding a second batch / installing new catalogue maintains identical immutable release/cartridge IDs and ratings; uninstalled titles can be rated and honestly marked Not in Library.
7. With no user ratings/real features, show neutral Browse/Rate a Game rather than invented For You; wildcard share and exclusions deterministic.
8. UI to live engine/ratings store → reload → source-driven rank/reason change; no fake recommendation card.

Rating UI stays **whole GemRating 1–10**, **whole My DeckFit 1–5**. Editorial provisional DeckFit and actual physical game compatibility remain separate. Current separate child PR #368 is only a provisional scoring candidate; coordinate rather than silently merge.

**Roxy's scheduled/source review may supply more rows, but completing the interface and these tests does not wait for that review.** Roxy/team can then add metadata forever with the same simple contract; no new architecture exercise.

## 6. Remaining original GC A–H (mandatory)

| Area | Delivered experience and reuse anchor |
| --- | --- |
| **A Native Menu** | Single app-owned game menu, real Pause/Resume/Save/Load capability and errors, Back/TV input. Reuse M00–M03/M15/M18/M20/M24 and PR #294. |
| **B Display** | Correct original image/aspect, safe TV/handset landscape, top-line bleed, DS/3DS dual screens, Wii U transform, supported user Fill. Reuse M13–M14, PR #294/#312, UNI. |
| **C Physical Controllers** | Two-press reciprocal swap, native actual input translation, controller-only TV, ports/reconnect and tester. Reuse M04–M08, PR #294, #308. |
| **D Wii/3DS GC UX** | Wii attachment/IR and 3DS tilt/pointer choice, on-screen/phone sensors and truthfully blocked backend. Reuse M09, #85 and #365 (PERF bridge). |
| **E PivotArc + GripArc** | Measured physical Standard+GripArc/PivotArc+GripArc, G1–G4, no missing original buttons, 48dp targets. Reuse M10–M12 and #135. |
| **F Modern Function** | Verified exact-game/region/edition actions to real guest keys, opt-in with Custom/Authentic fallback, no global A/B. Reuse M08, #172 and PR #175 research. |
| **G Xbox360 continuity** | Preserve guest profile/save identity, menu, native save/reopen/install-over, true port mappings; no false savestate claim. Reuse M17/#255. |
| **H Engine quality** | Real capability/recovery states across actual registered console roster; PERF runtime defects separate, no fake supported status. Reuse M16/M18/M19 and #262. |

Additionally shelf #303, TV/DeX #308, SIGNATURE #360 original approved video/audio/framing/volume, normal Settings/Advanced hierarchy and full profiles/Junior must remain covered. No user-facing diagnostics outside Advanced.

## 7. LAST SMALL DETAILS TO TIGHTEN — no new broad decisions

1. **SafeSave numbers:** distinct games-with-saves vs files vs state slots vs shared card banks; don't double-count or silently omit.
2. **Analogue-to-digital mapping:** **warning only, never block swap on lost pressure fidelity**; let player choose on/off guest input; preserve both old actions. Offer an optional analog touch trigger (slider/swipe/press ramp and sensor pressure only if hardware proves it) for games where proportional control is useful. Test actual guest partial/full/release and explain any backend limitations.
3. **Junior forgotten PIN — APPROVED:** offer a **parent-held offline recovery code at PIN setup** with clear one-time display and confirmation; protect its verifier independently from PIN, rate-limit and lock out automated guesses. The same parent can recover/change PIN using that proof. No universal bypass, default/reset PIN or data deletion. If both PIN and recovery code are lost and no verified device-backed proof exists, leave protected access unavailable and preserve saves; any further reset flow requires a separately reviewed secure design.
4. **Motion physical proof:** no visual sensor feature called working unless the actual guest moves; report PERF dependency explicitly.
5. **GEM unknown data:** unknown vs verified must remain distinct. Only source-backed feature explanations and actual user ratings may drive the displayed recommendations. No speculative DB project.
6. **Existing child PR #368:** inspect and reconcile before integration, do not auto-merge.

## 7A. CONNECTOR-PRELOADED IMPLEMENTATION SHORTCUTS — DO NOT SPEND CODEX TOKENS REDISCOVERING

**Live source entry points already verified on the #362 branch:**

| Deliverable | Existing file/function | Actual current gap / focused work |
| --- | --- | --- |
| Save Centre | `ProductSurfaces.kt` includes `SafeSaveSurface(playerName, inventory, availableActions, onDetails)` | A generic Compose list is NOT a wired whole-console backup inventory. Connect actual adapters, player scope, counts, Export All / Restore / Exceptions to real service. Do not build a new UI framework. |
| Controller editor | `PhysicalControllerMappingPanel.kt` has live probe, guided button capture, a **Swap conflicting bindings** command, Undo/Reset/Save; `NativeControllerMappingSession.kt` owns key capture and host selection | Current workflow starts by choosing an original target; owner instead wants **press physical source then press physical replacement** and automatic reciprocal swap, visual arrows and real guest output. Reuse draft/store/tester, not replace all of PR #294. |
| Controller axes | Existing PhysicalControllerMappingPanel currently displays **"Stick and trigger axis remapping is unavailable"** in one configuration; `NativeControllerMappingSession.observeMotion` samples axes and hats | Do NOT claim that showing analogue trigger values equals routeable analogue remapping. Implement source-specific axis/threshold/neutral semantics and the optional on-screen pressure trigger where supported, while always letting player accept warning-only digital swaps. |
| GEM source | `GemCataloguePackage.Release` / `GemGameplayFeatures` / `GemDiscovery.prepare` / `GemRatingRecommender.Game` | Existing recommendation fields already exist; gameplay facets are sparse. Add a simple exact-release-ID CSV/JSON overlay importer and **verify it actually populates the scorer and UI**. Never replatform database without a measured fault. |
| GEM personal scores | `GemRatingRepository`, `GemMyRatingsScreen.kt`, `GemFinderScreen.kt`, offline recommender PR #282 | Only explicit user whole 1–10 GemRatings learn; 1–5 My DeckFit separate. Tests high / low / clear / no ratings / multiple profiles; don't invent user score from catalogue or DeckFit. |
| GC menu/TV | `SharedGameMenu.kt`, `GameMenuActivity`, `MainNavigationRoutes.kt`, `PagedShelfSections.kt`, UI PR #294/#312/#317, GC #303/#308 | Preserve focus/release, same game surface, portrait/TV, already accepted navigation and universal game menu; **no second menu or shelf**. |
| Tilt foundation | Android `TiltSensor`, `TiltConfiguration`, `GameViewModelTilt` cited in PERF #365 | Existing tilt-to-D-pad/analog conversion **does not prove gyro/accel guest input**. PERF native core bridge/host negotiation must be verified; GC chooser displays honest availability. |
| Wii core capability | Historic Wii #85 + Sienna M09 | Normal gamepad buttons default, optional real virtual IR/right-stick pointer, separate accelerometer/gyro; respect guest host capability. No fake sensor support from a UI crosshair. |
| Junior PIN | `PlayerChooserSurface`, `JuniorPlayroomSurface`, JR01 final UX | Existing Compose shells are not the secure profile service. Add local PIN + enrolled offline recovery code, service/repository/deep-link permission guard and one cover child UI. |
| SafeSave storage | JR01 SafeSave architecture/format docs, SAVE #290, current SafeSave v1 store | Add adapters, crash journal and source preserving migration; **no new database**. `SafeSaveSurface` rows must use actual game/save/state/card inventory. |

**Minimum high-value tests before claiming ready:**
1. Controller physical RT→X reciprocal press selection; visual preview, confirm, actual guest events on original & swapped buttons; **show pressure loss warning and allow Save**, optional analogue touch slider sends partial values where a guest axis is real, returns neutral.
2. Wii gamepad default on compatible Wii title; virtual IR drag/click + normal gamepad buttons on an IR title, disabled if engine incapable; phone gyro availability and pause reset; TV no-touch gamepad/right-stick fallback.
3. 3DS extra Tilt Stick separate from Circle Pad, phone sensor option, Beyblade directional aim and pull-back launch **at actual guest sensor interface**, portrait/landscape, no overlap with touchscreen/stylus.
4. Junior enter, 4–6 digit PIN, parent-held recovery code enrolment/use, no Back/deep-link escape and no save loss.
5. SafeSave real console-specific game count vs slot vs card count, Export All supported + exceptions, hash and rollback on interrupted restore.
6. GEM contributor batch import (20–300 records), exact IDs and rejected rows, user whole-number high/low/clear changes existing GemFinder ranks and grounded reasons, no data loss. **No new SQL database or general source crawl.**
7. All original GC A–H, SIGNATURE and legacy install-over retained; same-head APK, owner/Marcus physical results NOT TESTED unless actually performed.

**Parallel work boundaries / avoiding major pitfalls:**
- **#368 remains an OPEN, UNMERGED child GEM B7-B draft**; check its current head and reconcile only after explicit authorisation, never mistake a provisional DeckFit overlay for real playability.
- **#365 is a cross-lane input dependency**, not permission to replace GC input flow with PERF native core changes; PERF core/host work needs its own verified native artifact and saves-safe integration gate.
- **GC docs alone do not build an APK.** Preserve previous last verified APK; new source must produce a new exact-commit testable APK.
- Real Libretro/native backend support determines whether Save State, analogue axis, Wii IR and 3DS gyro can work. **Distinct unsupported capability is visible**, not an app-wide blocked test or a synthetic PASS.
- A user's preferred mapping is allowed with **warning-only** analogue loss; a sensor-dependent game cannot be claimed supported if its guest bridge is absent.
- Never mark all 88,246 releases physically tested or treat a source heuristic/connector record as player GemRating.
- Preserve signing, package ID, immutable release/cartridge UUIDs, profiles, native save roots, memory cards/VMUs, appearance and controller settings.

## 8. Exit requirements for Codex

- **Actual production code**, not another documentation/design-only return; one draft #362 and no master merge without owner.
- Same-head ARM64 APK artifact link, package/version/signing and prior rollback comparison; full unit, Room, source/native and UI tests.
- **SafeSave:** first-class usable destination; per-console real save/state/card/unassigned counts; one-action Export All for safe supported data, truthful exceptions, working restore/reopen and real data durability.
- **Controllers:** visual direct RT↔X swap with reciprocal bindings and **warning-only** analogue fidelity reduction, optional real analogue touch trigger/pressure alternative, Test/Save/Undo and native guest action; controller-only TV/DeX journey and P1/P2 where supported.
- **GEM:** simple Roxy/team contributor file/import interface, actual feature-to-existing-GemFinder pipeline and player GemRating high/low/clear/profile test; not a new database.
- **Motion:** Wii **gamepad-first**, selectable virtual IR pointer/phone sensors/mixed buttons+pointer; 3DS independent virtual Tilt Stick plus phone sensor and Beyblade launch; capability-accurate GC UI/calibration with **PERF** engine bridge evidence or blocked status.
- **Full A–H console matrix** and independent Marcus/owner hardware test instructions; source PASS is not hardware PASS. No silent system exclusions.
- List only genuine technical/engine/legal blockers and narrow outstanding owner decisions.

**Controller configuration is under Settings; PivotArc retains existing Hold A/B only, while eligible handheld Standard touch layouts may show a separate optional three-slot Hold Tray per HHOLD-01.**

**DO THE IMPLEMENTATION. Keep exact current source identity and all existing player ROMs, saves, states, appearances and ratings intact.**
