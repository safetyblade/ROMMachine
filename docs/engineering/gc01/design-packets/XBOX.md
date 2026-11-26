# XBOX — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Real independent native host input bridge required.

## XB-PA-01 — shared Xbox dual-analogue compact review candidate (26 Nov 2026)

Retain Sony side-mounted independent sticks (left above right, LS/RS click seats only where actual native dispatch verified), central D-pad and compact action arc with X upper-left, Y upper-right, B right, A lower-left. Adopt GameCube reach correction: LT/RT adjacent immediately above the principal action cluster, not top edge; White/Black pair directly right of Y/B in the same action island. Four fixed Holds A/B/LT/RT proposed; X/Y and secondary buttons momentary. Full trigger Hold only where real guest dispatch exists, do not simulate pressure. Separate Menu/Start/Back rail; no invented Guide key; preserve native White/Black and original pressure-sensitive controls where actually supported. Shared illustrative 440×370 chassis: left stick (55,112), right stick (55,250), LS (55,40), RS (55,322), D (142,181), X (207,114), Y (267,114), B (267,237), A (207,237), LT (207,55), RT (267,55), White (329,114), Black (329,174), H LT (329,55), H RT (389,55), H A (207,300), H B (267,300), separate utility rail below. Exact fit, full analogue travel, thumb reach, independent >=48dp effective targets and no overlap to be measured. Keep trigger and action reach close; do not stretch chassis to fit blindly. Source engineering Xbox.kt blob 94cff86bcbbbf7e877b0a6c966fb4b1d036a0ba6 uses same-label ABXY, L1/R1 labelled White/Black per bumpers flag, L2/R2 labelled LT/RT, SELECT labelled Back, left/right analog helpers; current key dispatch is not evidence of proportional axes or stick-click guest support. TouchControllerID selects bumpers false for Xbox and true for Xbox360. Verify independent native host mapping, axes, trigger ranges, held input ownership/release, no regression of native host touch routing, mirrored operation and viewport. Enhanced game acceptance (racing, dual-analogue action and D-pad menus) separately on each host; existing Xbox/360 performance issues must not be confused with input defects. Owner review pending; no implementation/build. GripArc/Joint deferred.

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
