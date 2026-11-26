# NGC — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Neo Geo Pocket Color, NOT GameCube.

## NGC-PA-01 — review candidate (26 Nov 2026)

Neo Geo Pocket Color (SystemID.NGC, not GameCube) has the same eight-way digital click-stick, A/B and Option topology as NGP. Propose exact inheritance of owner-approved [NGP-PA-01](NGP.md): Digital 2 Click shell and directional seat, B upper-right and A lower-left, individual H immediately right of B and below A, Menu/Option utility rail, one-handed reach and handedness. No analogue axis, push-click input, shoulders or fabricated Start/Select. Maintain distinct platform identity and saved preferences. Historical source-backed [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) identifies shared NGP/NGC control family; validate active NGC backend, exact guest mapping and Hold ownership, diagonals/neutral release, 48dp+ targets and physical fit independently. Owner approval pending; no build or physical PASS. GripArc and Joint deferred.

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
