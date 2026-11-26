# SNES — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## SNES-PA-01 — Digital 4 owner layout lock (26 Nov 2026)

Original SNES: D-pad, A/B/X/Y, L/R, Start/Select. Adapt owner-approved [PSP-PA-03](PSP.md) circular geometry to digital-only hardware: omit analogue stick and its seat; retain D-pad nucleus, PSP relative radial face seats (X in Square/top seat, A in Triangle/upper-right seat, B in Cross/lower-right seat, Y in Circle/lower-centre seat). Four fixed independent H affordances adjacent/outboard of respective faces (X right of X, A right of A, B right of B, Y below Y), no per-game Hold reassignment required. L/R adjacent with R to right of L between A/B, separate Menu/Start/Select utility rail. Preserve GBA digital manufactured styling, existing PSP radial geometry and mandatory measured downward chassis clearance principle; no fake analogue or reflow into vacant analogue seat. Proposed schematic centre coordinates on 300x350 concept canvas: D-pad (136,153), X (164,64), A (217,103), B (212,213), Y (159,253), H X (212,64), H A (264,103), H B (264,213), H Y (159,293), L (212,158), R (262,158); these are pre-clearance relative reference only, not validated dp. Prefer compact chassis left extent around D-pad without moving approved relative control seats; measure all visible collars and hitboxes, >=48dp independent targets and safe rim/utility rail clearance, rigid downward adjustment as needed; do not shrink hitboxes or silently move individual buttons. Owner design approval granted; exact source and measured geometry pending.

Rationale: Super Mario World Y/X run, B jump, A spin (https://www.world-of-nintendo.com/manuals/super_nes/super_mario_world.shtml); Super Metroid default B dash, A jump, X fire (https://www.world-of-nintendo.com/manuals/super_nes/super_metroid.shtml). Different sustained actions justify four fixed Holds. Confirm exact current native identity, held ownership/lifecycle release, L/R simultaneous input, Y hold plus D-pad/B, diagonals, mirrored layout and device fit. If four Holds or paired shoulders cannot fit, return for review rather than removing native inputs or forcing per-game remaps. No source/physical PASS, no APK. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — source/clearance audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
