# SMS — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## SMS-PA-01 — review candidate (26 Nov 2026)

Original standard pad D-pad, 1 (also Start), 2; Pause on console, no separate Select. Reference: https://www.manualslib.com/manual/1027819/sega-master-system.html and https://wiki.batocera.org/systems%3Amastersystem . Propose reuse of locked GB/GBC/NES compact GBA-derived geometry: D-pad unchanged, 1/START in lower-left A seat, 2 in upper-right B seat, independent H1 below 1 and H2 right of 2 (same visual H glyph as existing face Hold). Preserve one-handed reach, shell materials, button dimensions and handedness. Separate utility rail retains CARTDECK Menu and actual guest Pause, no invented Select or independent Start; remove unused utility seat from active controls and preserve remaining geometry unless separately approved. Pause is guest input, not emulator menu. Validate real current PadKit/core input mapping (numbered visual labels must not be assumed from Android A/B), native Pause routing, Hold semantics, 48dp+ hitboxes and physical one-handed reach. Special peripherals outside standard pad scope. Owner review pending; no APK or physical acceptance. GripArc and Joint deferred.

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
