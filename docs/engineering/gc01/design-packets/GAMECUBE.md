# GAMECUBE — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** C-stick / Z / analogue triggers need real native input audit.

## GC-PA-01 — Analogue 4 Asymmetric review candidate (26 Nov 2026)

Inherit approved Sony side-mounted dual-stick architecture, main analogue above smaller C-stick, independently accessible central D-pad. GameCube-specific action constellation: larger A at nucleus, B lower-left, Y upper-left, X upper-right; retain native guest identity. L/R adjacent mid-arc, Z immediately below trigger bank, never top shoulder row. Propose four fixed Holds A/B/L/R, favor sustained A and trigger functions; X/Y/Z remain momentary. L/R full-press Holds only, not invented variable pressure. Provide distinct L-half/R-half at reachable mid-arc only if real backend mappings verified; no fake L3/R3 labels because original GameCube sticks do not click. Illustrative 448×410 body centres: main analogue (62,116), C-stick (62,253), D (157,170), A (254,200), B (207,268), Y (241,105), X (303,118), L (347,190), R (402,190), L-half (347,247), R-half (402,247), Z (375,304), H A (270,267), H B (207,333), H L (347,365), H R (402,365), separate Menu/Start utility rail below. Half-trigger positions conditional; if real half presses absent, disclose and keep unused positions inactive rather than misrepresenting input. All centres illustrative, not measured dp or verified fit. Exact engineering GameCube.kt blob b2b90aeeb36082b6d6f35148139c937b1d618ab6 documents Android A/B/X/Y guest same-label keys, L1/R1 full shoulders, R2 Z, left/right axes for main/C-stick, and stick-click host mappings for analog half-triggers (verify actual Dolphin core/native host mapping independently). No automatic half trigger on stick movement; explicit intended action only. Require independent real axes, full/half trigger semantics, no phantom L3/R3, independent 48dp+ discrete targets, directional/analogue travel, held ownership/release, mirror, viewport and measured fit. Proposed enhanced gameplay tests Mario Kart: Double Dash!!, Super Smash Bros. Melee, Super Mario Sunshine, Luigi's Mansion; check trigger depth in titles requiring it and actual C-stick control. If safe fit or input capability fails, return for redesign or retain supported Standard, never omit essential inputs silently. Owner approval pending; no implementation/build. GripArc and Joint deferred.

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
| Design prepared | PENDING |
| Solo PivotArc visual | PENDING |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
