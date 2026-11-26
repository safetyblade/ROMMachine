# NGP — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## NGP-PA-01 — review candidate (26 Nov 2026)

Original Neo Geo Pocket: eight-way digital click-stick, A/B, Option. Use approved GB Digital 2 GBA-derived one-hand shell and exact action/Hold geometry: B upper-right, A diagonally lower-left, H immediately right of B and below A. Replace D-pad artwork only in its existing seat with circular tactile digital click-stick (eight directional sectors, clear diagonal feedback, no analogue axis, no invented push-click input). Preserve the original directional event identities, compact reach and existing mirrored group. Separate rail: CARTDECK Menu and native Option; no invented Start/Select. The historical repository source audit [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) records Android A displayed B and Android B displayed A. Match Hold to actual guest key, not label-derived Android key. Preserve simultaneous A+B and directional combinations through existing input ownership; validate exact-head mapping, Option routing, diagonal transitions/neutral release, >=48dp discrete targets and physical fit. No claim of physical mechanical click or analogue support. Proposed shared variant for NGC after separate owner review. Owner approval pending, no physical/CI PASS. GripArc and Joint deferred.

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
