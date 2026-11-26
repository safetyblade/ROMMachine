# GAMECUBE — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENHANCED PHYSICAL TESTING REQUIRED**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** C-stick / Z / analogue triggers need real native input audit.

## GC-PA-01 — Analogue 4 Asymmetric review candidate (26 Nov 2026)

Inherit approved Sony side-mounted dual-stick architecture, main analogue above smaller C-stick, independently accessible central D-pad. GameCube-specific action constellation: larger A at nucleus, B lower-left, Y upper-left, X upper-right; retain native guest identity. L/R adjacent mid-arc, Z immediately below trigger bank, never top shoulder row. Propose four fixed Holds A/B/L/R, favor sustained A and trigger functions; X/Y/Z remain momentary. L/R full-press Holds only, not invented variable pressure. Provide distinct L-half/R-half at reachable mid-arc only if real backend mappings verified; no fake L3/R3 labels because original GameCube sticks do not click. Illustrative 448×410 body centres: main analogue (62,116), C-stick (62,253), D (157,170), A (254,200), B (207,268), Y (241,105), X (303,118), L (347,190), R (402,190), L-half (347,247), R-half (402,247), Z (375,304), H A (270,267), H B (207,333), H L (347,365), H R (402,365), separate Menu/Start utility rail below. Half-trigger positions conditional; if real half presses absent, disclose and keep unused positions inactive rather than misrepresenting input. All centres illustrative, not measured dp or verified fit. Exact engineering GameCube.kt blob b2b90aeeb36082b6d6f35148139c937b1d618ab6 documents Android A/B/X/Y guest same-label keys, L1/R1 full shoulders, R2 Z, left/right axes for main/C-stick, and stick-click host mappings for analog half-triggers (verify actual Dolphin core/native host mapping independently). No automatic half trigger on stick movement; explicit intended action only. Require independent real axes, full/half trigger semantics, no phantom L3/R3, independent 48dp+ discrete targets, directional/analogue travel, held ownership/release, mirror, viewport and measured fit. Proposed enhanced gameplay tests Mario Kart: Double Dash!!, Super Smash Bros. Melee, Super Mario Sunshine, Luigi's Mansion; check trigger depth in titles requiring it and actual C-stick control. If safe fit or input capability fails, return for redesign or retain supported Standard, never omit essential inputs silently. Owner approval pending; no implementation/build. GripArc and Joint deferred.

## GC-PA-02 — compact action/trigger island (26 Nov 2026)

Owner correction: A button size is not the issue; thumb reach between movement, A and back buttons is. Supersede PA-01 oversized A emphasis and remote right-side trigger bank. Use a normal/slightly prominent A, move A toward central D-pad and main stick; bring L/R/Z into the immediate action cluster, in a mid-arc row directly above A/X rather than at the top edge or beyond X. Keep side-mounted main/C sticks. Proposed 400×360 reference canvas centres: main stick (55,140), C-stick (55,263), D (137,175), A (215,177), B (215,245), Y (157,105), X (280,177), L (215,105), R (280,105), Z (344,105), H L (215,48), H R (280,48), H A (280,245), H B (215,309), conditional L-half (344,177), R-half (344,245); separate Menu/Start rail below. A visible cap radius 21, no oversized button requirement. L/R/Z occupy the same reachable cluster as A and X; half-press controls outboard of primary action path only when real backend capability exists. Proposed fixed A/B/L/R Holds retained, full press only. Prior illustrative A-to-R centre distance ~148 reference units; new ~97; A-to-L ~72 and A-to-D ~78 (concept, not physical measurements; no inference of actual thumb comfort). All native input/trigger capability, 48dp independent targets, measured nonoverlap, full stick travel, release, mirroring and enhanced game testing requirements from PA-01 retained. Specifically test repeated stick-to-A-to-L/R/Z transitions, A plus R/L/Z using separate fingers where required, and sustained full trigger Hold with movement; compare against PA-01 if available. If width cannot support safe targets, reflow with owner approval, do not compress hitboxes or relegate native inputs to inaccessible areas. Owner accepted GC-PA-02 on 26 Nov 2026 and directed considerable additional physical testing; no implementation/build. GripArc/Joint deferred.

## Enhanced GameCube acceptance — owner-directed

Required additional representative gameplay: Super Mario Sunshine (main stick/A and real partial/full R trigger transitions), Mario Kart: Double Dash!! (sustained A plus steering/drifting), Super Smash Bros. Melee (A/B/X/Y and C-stick response), Luigi's Mansion (dual analogue and trigger control). Record repeated D/stick-to-A-to-L/R/Z reach, accidental presses, both handedness modes, simultaneous action where feasible and actual independent half/full trigger dispatch and release. Compare one-hand operation against Standard; flag two-hand requirements. No physical PASS until owner review. Proposed test titles, not executed results.

## PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

## GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

## Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

## Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — enhanced test gate pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
