# ATARI7800 — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two fire buttons and console switches.

## A78-PA-01 — Atari digital owner layout lock (26 Nov 2026)

Use exact approved GB Digital 2 geometry with native 1 lower-left and 2 upper-right, fixed H1/H2 adjacent/outboard. Atari7800.kt blob a83e870cac8ff14c744e9c53c8079034cf3c402f maps Android B=1 and Android A=2; current Start/Select controls retained on separate Menu/Start/Select utility rail with guest switch semantics verified (including Reset/Pause where actually supported); no invented difficulty controls. 2600 compatibility/paddle modes not assumed. Preserve existing guest input identities, one-thumb proximity, no top shoulder buttons, mirrored layout, >=48dp independent touch targets, lifecycle release and measured screen clearance. Representative games Food Fight, Robotron: 2084, Ninja Golf, Asteroids subject to actual compatible ROM and control requirements. Owner approved 26 Nov 2026; no runtime/build; GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
