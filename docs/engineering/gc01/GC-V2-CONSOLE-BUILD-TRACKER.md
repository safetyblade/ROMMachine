# CARTDECK GC-V2 — console controller build and acceptance register

Owner revised order (26 Nov 2026): **1. Prove full-piece GripArc on GBA; 2. Design, implement and verify PivotArc for all 33 consoles; 3. Design, implement and verify full-piece GripArc for all 33 consoles; 4. Joint LAST, positioning only, without changing either completed pad.**

This is the canonical progress register for PR #376 and its successors. Update **each console row after each build** with commit, source/CI gate, visual owner decision and actual input acceptance. Never change status to accepted based on code or screenshot alone. Console-specific variant inventory is in `GC-ARC-CONSOLE-HARDWARE-V2-20261126.md`.

**Current rollout gate: GBA full-piece physical proof PASS (owner, 26 Nov 2026); spatial utilisation FAIL. All 33 PivotARC design directions approved; full phase 2 implementation authorized under #377; final GBA GripArc design and wider GripArc rollout remain open/deferred to phase 3. No Joint work until both solo programmes finish.**

**Frozen reference:** solo GBA and PSP PivotArc screenshot appearance and relative control placements. Keep both source and owner acceptance when working on GripArc. Legacy L-style GripArc remains the fallback until full-body style is validated on device; don't assert Joint complete.

## GBA full-piece hardware — owner screenshot review (26 Nov 2026)

**Evidence:** GBA BattleBots menu screenshot, after successful `e7815a71` APK. The new full-height GripArc housing appears as a single cohesive physical panel immediately below the game viewport. It is visibly large, but owner is considering that a desirable **shared GripArc hardware chassis standard**, not necessarily a sizing failure. This is a *provisional design direction*, **not yet a PASS** for spacing or gameplay.

**Observed placement defects:** Standard-layout L/R buttons remain at extreme top corners; D-pad far left and B/A stagger far right; Start/Select separated; Menu isolated; G1–G4 small and scattered along the right/lower edge. Most empty space belongs to *control layout*, not to wrong outer shell size.

**Design decision to test next:** Keep the full-size GripArc body's silhouette, bevels, corner mounts, rails, and wordmark consistent across console families. Create **per-console compact native-control constellations** within a reserved main control area, and a deliberate, legible four-button G1–G4 bank inside the same hardware face. Preserve native PadKit control input, hold gestures, touch target safety, per-game mappings, video and saves. Do not simply stretch existing Standard positions into a bigger panel.

**Future Joint**, only after standalone PivotArc and GripArc for all consoles have been accepted: mount the already finished PivotArc module onto this shared GripArc chassis as a complete immutable hardware component. Joint is a rigid placement/fit problem; no changing either module's physical style, relative controls, logos, button sizes or mapping; no overlapping or duplicate live inputs.

**Required next GBA gate:** Implement a GBA-specific compact native-control layout and cohesive G1–G4 bank in the shared full-height chassis, exact-head format/unit/APK checks, then owner physical appearance and game-input retest. Current GBA status: full-body chassis **candidate accepted for further design exploration**, button placement **needs redesign**, functional acceptance **unverified**. Other consoles remain unbuilt and this screenshot does not promote them.

## GBA physical proof — owner decision, 26 Nov 2026

Owner reports "GripARC physically passed but also failed as not utilising the space enough" with GBA BattleBots gameplay screenshot (1000097297.jpg). Physical proof PASS as reported; separate spatial/design FAIL. Full-size housing is retained as shared chassis foundation; exact final layout is NOT approved. Observed small visible L/R and G1–G4 caps, large gaps and unused lower face; propose larger native controls and a prominent G1–G4 bank, deliberate utility placement and better reach using the existing body. Exact geometry awaits phase 3 design approval. Preserve viewport, native mapping, saves, >=48dp effective targets, and no overlap. Owner physical report does not establish individual G-slot programming or exhaustive input/rotation/handedness tests; those remain final acceptance gates. Latest known GBA prototype a627a713 had Room/Kotlin/APK workflow PASS; screenshot does not display version or commit, so exact installed build is not independently verified.

Phase 1 chassis proof satisfied; all 33 PivotARC system designs approved (batch 01: 15; batch 02: 18, commit 697afe49). Full phase 2 may enter source implementation. Conditional variants and capability gates remain open. No final GripArc layout acceptance or Joint authorization.

## Console-by-console build matrix

| Console | Input family | PivotArc SOLO (specific style) | GripArc SOLO (full hardware) | Joint (pad placement only, LAST) | Latest GC run/commit |
|---|---|---|---|---|---|
| GB | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| GBC | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| NES | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| SMS | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| GG | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| NGP | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| NGC | Two-button | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| PC_ENGINE | Two-button | Approved standard + six-button design; Avenue Pad 3 alias open; runtime pending | Input mapped; full-body not built | Not built | — |
| GBA | Four-button/shoulders | Owner-approved solo visual; frozen, regression test pending | Full-body owner physical PASS; spatial utilisation FAIL; final layout open | Not built | a627a713 CI PASS; owner screenshot build unverified |
| SNES | Four-button/shoulders | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| GENESIS | Six-button | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| SEGACD | Six-button | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| WS | Handheld dual direction | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| WSC | Handheld dual direction | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| NDS | Touch dual-screen | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| NINTENDO_3DS | Touch dual-screen | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| PSP | Single analog | Owner-approved revised layout; source/clearance and physical retest pending | Input mapped; full-body not built | Not built | Previously owner-approved solo |
| N64 | Single analog | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| PSX | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| PLAYSTATION_2 | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| PLAYSTATION_3 | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| DREAMCAST | Single analogue | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| GAMECUBE | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| XBOX | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| XBOX360 | Dual analog | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| FBNEO | Arcade | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| MAME2003PLUS | Arcade | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| ATARI2600 | Atari | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| ATARI7800 | Atari | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| WII | Wii Remote | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| DOS | Desktop | Owner layout approved; source/CI and physical pending | Not built | Not built | — |
| LYNX | Handheld options | Owner layout approved; source/CI and physical pending | Input mapped; full-body not built | Not built | — |
| WII_U | Wii U | Owner layout approved; source/CI and physical pending | Not built | Not built | — |

## GBA execution ledger

| Build gate | PivotArc solo | GripArc solo | Joint | Source & test evidence | Owner visual |
|---|---|---|---|---|---|
| Previous accepted baseline | PASS; frozen | Legacy detached L-piece, FAIL visually | Overlap / scattered, FAIL | Master 68a6c161 | PivotArc PASS, Grip/Joint FAIL |
| GBA-V2B-01 | Unmodified | Full-piece faceplate replaces L-piece on Standard+GripArc portrait only | Not touched (LAST) | Commit 7e10b4c4; new geometry tests; CI pending | Not tested |
| GBA-V2B-02 | Unmodified | Same full faceplate with integrated GripArc engraving (no new floating element) | Not touched (LAST) | Commit a1eac63f; exact-head CI pending | Not tested |
| GBA-V2B-03 | Unmodified | Full body geometry uses correct dp→pixel conversion for all four programmable keys | Not touched (LAST) | Commit def9ed0a; exact-head CI pending | Not tested |
| GBA-V2B-04 | Unmodified | Full body hidden whenever touch controls/gameplay hardware are not visible | Not touched (LAST) | Commit 8869755c; exact-head CI pending | Not tested |
| GBA-V2B-05 | Unmodified | Same full-piece GBA style; Kotlin formatting repaired | Not touched (LAST) | Commit 750bedb9; superseded by 06 | Not tested |
| GBA-V2B-06 | Unmodified | Full-body candidate; fastener-loop formatting fixed | Not touched (LAST) | e7815a71; Kotlin PASS; Room PASS; APK PASS; run 45711515604; artifact 15067444903 | **Screenshot reviewed: outer chassis promising; control positioning NOT ACCEPTED; input unverified** |

## Phase gate and progress roll-up

| Phase | Scope | Code/CI state | Owner acceptance | Unlock condition |
|---|---|---|---|---|
| 1 — GBA GripArc proof | Full-piece GBA Standard+GripArc | a627a713 — Room/Kotlin/APK PASS | Owner physical proof PASS; spatial utilisation FAIL | Phase 2 unlocked; final GripArc layout remains open |
| 2 — All PivotArc | 33 consoles, console-specific visual and input architecture | 33/33 designs approved, batch 01 + batch 02; implementation not established | 33/33 design directions; device gates pending | All supported standalone PivotARC profiles implemented and exact-head verified; owner physical acceptance |
| 3 — All GripArc | 33 consoles using proven GBA full-piece style contract | Only GBA proof candidate implemented | Pending | Every applicable console built and accepted after PivotArc |
| 4 — Joint | Whole-pad placement using immutable PivotArc + GripArc | Existing legacy combination NOT accepted; new Joint not started | Previous combined overlap FAIL | All standalone pad designs locked and accepted |

**Per-build reporting rule:** Update the affected console row, the GBA/per-console run ledger, CI status, APK artifact if available, and physical decision after each build. Never extrapolate acceptance from one console to another.

## Advance criteria

- **PivotArc design gate:** per-console physical panel, button shape/spacing, correct native PadKit inputs, utility/hold. Only mark accepted from owner visual+functional confirmation.
- **GripArc design gate:** full-body same premium finish, G1-G4 all visible and programmable, native inputs unchanged, no viewport overlap, no floating panels, left/right and sizes tested; demonstrate it independently before Joint.
- **Joint gate (last):** take both locked console profiles and place them with rigid whole-pad translation within safe bounds. Preserve original controls, exact artwork, labels and relative positions; no reskin, no button reflow, no hidden native controls. If fitting is impossible, explicit unavailable fallback, not overlapping artwork.
- **CI gate:** exact-head Kotlin formatting, source tests, APK and install-over check. Test source completeness and touch hitboxes, not just screenshot art.
- **Run tracking:** each CI build adds one row in GBA ledger and updates the relevant 33-console matrix rows; never apply PASS globally to a console family.

## Full PivotARC design gate — 26 Nov 2026

All 33 design directions approved; [batch 01](GC-ARC-APPROVED-PIVOT-BATCH-01.md) (15) and [batch 02](GC-ARC-APPROVED-PIVOT-BATCH-02.md) (18, 697afe49) comprise the frozen engineering handoff. PC Engine Avenue Pad 3 remains open; conditional native host and specialist variants require evidence. No new runtime implementation or APK established by the documentation commits. Enhanced physical acceptance N64, GameCube, Wii, Wii U, Xbox and Xbox 360. First run ktlintCheck, compile, unit tests, then one consolidated APK after the complete source batch. No merge, GripARC or Joint until authorized gates. 
