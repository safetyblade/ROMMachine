# DREAMCAST — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## DC-PA-01 — Analogue 4 review candidate (26 Nov 2026)

PSP/Sony-derived compact action arc, one side-mounted left analogue (no right stick), independent central D-pad, Dreamcast X top/Y upper-right/A lower-right/B lower-centre (rotated native compass), adjacent L/R triggers at mid-arc between Y/A outboard; separate Menu/Start rail, no Select. Proposed fixed Holds A/B/L/R: prioritize racing acceleration/braking and common arcade action, leave X/Y momentary; no per-game remapping. Illustrative 420×360 canvas: stick (60,160), D (150,160), X (205,70), Y (260,110), A (260,230), B (205,275), L (320,150), R (375,150), H L (320,205), H R (375,205), H A (330,260), H B (205,335); separate utility rail below body. All centres illustrative, not measured dp/fit. Source Dreamcast.kt blob 1ddf5ea2961b69b2b184f421b7794c9c43fd9c7c confirms Android B=guest A, Android A=guest B, Android Y=guest X, Android X=guest Y, L2/R2 keys labelled L/R, one analogue and discrete D-pad. Native Dreamcast triggers are proportional; existing layout dispatches key IDs, so verify real Flycast trigger axes before claiming variable pressure. Fixed Hold L/R may only latch digital/full press supported by actual backend, never simulate pressure. Preserve independent axes and inputs, explicit held/release state, mirror, viewport, full travel and measured >=48dp discrete targets; if no safe fit return for owner review. Suggested gameplay tests Crazy Taxi, Daytona USA 2001, Soulcalibur, Sonic Adventure; actual tests pending. Owner approval pending; no implementation/build. GripArc and Joint deferred.

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
