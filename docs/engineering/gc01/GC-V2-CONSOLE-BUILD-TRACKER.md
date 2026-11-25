# CARTDECK GC-V2 — console controller build and acceptance register

Owner revised order (26 Nov 2026): **1. Prove full-piece GripArc on GBA; 2. Design, implement and verify PivotArc for all 33 consoles; 3. Design, implement and verify full-piece GripArc for all 33 consoles; 4. Joint LAST, positioning only, without changing either completed pad.**

This is the canonical progress register for PR #376 and its successors. Update **each console row after each build** with commit, source/CI gate, visual owner decision and actual input acceptance. Never change status to accepted based on code or screenshot alone. Console-specific variant inventory is in `GC-ARC-CONSOLE-HARDWARE-V2-20261126.md`.

**Current rollout gate: GBA GripArc physical proof only. Do not advance a broad PivotArc or GripArc rollout until the GBA-specific complete housing is confirmed by the owner. Console-specific PivotArc may have existing code but is not considered complete until console styling and physical acceptance. New Joint work is deferred until both solo programmes have finished.**

**Frozen reference:** solo GBA and PSP PivotArc screenshot appearance and relative control placements. Keep both source and owner acceptance when working on GripArc. Legacy L-style GripArc remains the fallback until full-body style is validated on device; don't assert Joint complete.

## Console-by-console build matrix

| Console | Input family | PivotArc SOLO (specific style) | GripArc SOLO (full hardware) | Joint (pad placement only, LAST) | Latest GC run/commit |
|---|---|---|---|---|---|
| GB | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| GBC | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| NES | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| SMS | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| GG | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| NGP | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| NGC | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| PC_ENGINE | Two-button | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| GBA | Four-button/shoulders | Accepted solo visual | Full-body build PASS; owner proof pending | Not built | GBA-V2B-06 (e7815a71) |
| SNES | Four-button/shoulders | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| GENESIS | Six-button | Not built | Not built | Not built | — |
| SEGACD | Six-button | Not built | Not built | Not built | — |
| WS | Handheld dual direction | Mapped; console styling unapproved | Not built | Not built | — |
| WSC | Handheld dual direction | Mapped; console styling unapproved | Not built | Not built | — |
| NDS | Touch dual-screen | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| NINTENDO_3DS | Touch dual-screen | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| PSP | Single analog | Accepted solo visual | Input mapped; full-body not built | Not built | Previously owner-approved solo |
| N64 | Single analog | Not built | Not built | Not built | — |
| PSX | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| PLAYSTATION_2 | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| PLAYSTATION_3 | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| DREAMCAST | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| GAMECUBE | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| XBOX | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| XBOX360 | Dual analog | Not built | Input mapped; full-body not built | Not built | — |
| FBNEO | Arcade | Not built | Not built | Not built | — |
| MAME2003PLUS | Arcade | Not built | Not built | Not built | — |
| ATARI2600 | Atari | Not built | Not built | Not built | — |
| ATARI7800 | Atari | Not built | Not built | Not built | — |
| WII | Wii Remote | Not built | Not built | Not built | — |
| DOS | Desktop | Not built | Not built | Not built | — |
| LYNX | Handheld options | Mapped; console styling unapproved | Input mapped; full-body not built | Not built | — |
| WII_U | Wii U | Not built | Not built | Not built | — |

## GBA execution ledger

| Build gate | PivotArc solo | GripArc solo | Joint | Source & test evidence | Owner visual |
|---|---|---|---|---|---|
| Previous accepted baseline | PASS; frozen | Legacy detached L-piece, FAIL visually | Overlap / scattered, FAIL | Master 68a6c161 | PivotArc PASS, Grip/Joint FAIL |
| GBA-V2B-01 | Unmodified | Full-piece faceplate replaces L-piece on Standard+GripArc portrait only | Not touched (LAST) | Commit 7e10b4c4; new geometry tests; CI pending | Not tested |
| GBA-V2B-02 | Unmodified | Same full faceplate with integrated GripArc engraving (no new floating element) | Not touched (LAST) | Commit a1eac63f; exact-head CI pending | Not tested |
| GBA-V2B-03 | Unmodified | Full body geometry uses correct dp→pixel conversion for all four programmable keys | Not touched (LAST) | Commit def9ed0a; exact-head CI pending | Not tested |
| GBA-V2B-04 | Unmodified | Full body hidden whenever touch controls/gameplay hardware are not visible | Not touched (LAST) | Commit 8869755c; exact-head CI pending | Not tested |
| GBA-V2B-05 | Unmodified | Same full-piece GBA style; Kotlin formatting repaired | Not touched (LAST) | Commit 750bedb9; superseded by 06 | Not tested |
| GBA-V2B-06 | Unmodified | Full-body candidate; fastener-loop formatting fixed | Not touched (LAST) | e7815a71; Kotlin PASS; Room PASS; APK PASS; run 45711515604; artifact 15067444903 | **OWNER PROOF PENDING** |

## Phase gate and progress roll-up

| Phase | Scope | Code/CI state | Owner acceptance | Unlock condition |
|---|---|---|---|---|
| 1 — GBA GripArc proof | Full-piece GBA Standard+GripArc, touch + G1–G4 | e7815a71 — all three CI gates PASS | Pending physical screenshot/gameplay | Owner confirms the GBA full-piece style and functional controls |
| 2 — All PivotArc | 33 consoles, console-specific visual and input architecture | Existing coverage mixed, not complete | GBA and PSP solo visual accepted; rest unapproved | Each console individually verified; no family-wide automatic PASS |
| 3 — All GripArc | 33 consoles using proven GBA full-piece style contract | Only GBA proof candidate implemented | Pending | Every applicable console built and accepted after PivotArc |
| 4 — Joint | Whole-pad placement using immutable PivotArc + GripArc | Existing legacy combination NOT accepted; new Joint not started | Previous combined overlap FAIL | All standalone pad designs locked and accepted |

**Per-build reporting rule:** Update the affected console row, the GBA/per-console run ledger, CI status, APK artifact if available, and physical decision after each build. Never extrapolate acceptance from one console to another.

## Advance criteria

- **PivotArc design gate:** per-console physical panel, button shape/spacing, correct native PadKit inputs, utility/hold. Only mark accepted from owner visual+functional confirmation.
- **GripArc design gate:** full-body same premium finish, G1-G4 all visible and programmable, native inputs unchanged, no viewport overlap, no floating panels, left/right and sizes tested; demonstrate it independently before Joint.
- **Joint gate (last):** take both locked console profiles and place them with rigid whole-pad translation within safe bounds. Preserve original controls, exact artwork, labels and relative positions; no reskin, no button reflow, no hidden native controls. If fitting is impossible, explicit unavailable fallback, not overlapping artwork.
- **CI gate:** exact-head Kotlin formatting, source tests, APK and install-over check. Test source completeness and touch hitboxes, not just screenshot art.
- **Run tracking:** each CI build adds one row in GBA ledger and updates the relevant 33-console matrix rows; never apply PASS globally to a console family.
