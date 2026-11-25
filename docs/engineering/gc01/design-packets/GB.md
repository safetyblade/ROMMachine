# GB — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## Owner layout lock — GB-PA-01 (26 Nov 2026)

Owner approved the supplied GBA runtime screenshot as the governing visual reference, including close-grip one-handed digital operation and individual face-button Hold. This supersedes the earlier conventional left/right controller sketch in #377. Preserve the accepted GBA shell, artwork, control sizes and actual native geometry: D-pad upper-left, B upper-right, A diagonally down-left of B, Hold B immediately right of B and Hold A directly below A. Remove GBA-only L/R controls and their visible/touch elements; do not spread or reflow the remaining controls. Preserve the existing separate three-button Menu/Start/Select utility rail and existing mapping/legends. No additional buttons or stick. Mirror the complete group for handedness without reversing native key/direction identities. Existing source-owned Hold behavior must allow latched B + direction, independent A/B, clear held inputs on exit/reset and avoid duplicate or stuck guest presses.

Screenshot-derived normalized centre targets (approximate visual reference, not measured dp, panel bounds x165–694 / y740–1345 in supplied 708×1536 image): D-pad (0.29,0.28), B (0.66,0.54), A (0.40,0.71), Hold B (0.88,0.54), Hold A (0.40,0.90). Existing utility rail outside panel below it. Engineering must reuse exact accepted GBA source positions/scale, not treat these approximations as a new geometry solver or pixel-perfect source export. Preserve game viewport, safe areas, independent >=48dp targets and close reach; check both hands, portrait and any supported orientation. If no safe fit, do not silently spread the cluster or hide controls.

Native inventory: D-pad, A, B, Start, Select; Menu and two H affordances are CARTDECK controls, not extra guest keys. Historical source-backed reference: [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) identifies GB/GBC as existing supported source profiles with common input topology; current PR #376 exact-head source binding/geometry validation and handset acceptance still required. GBA visual reference does not confer GB physical PASS. GripArc and Joint remain deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
