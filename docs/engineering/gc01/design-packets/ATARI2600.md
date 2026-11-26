# ATARI2600 — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Single fire button and console switches.

## A26-PA-01 — Atari digital review candidate (26 Nov 2026)

One digital D-pad, one Fire and fixed Hold Fire. Inherit GB accepted D-pad/B/Hold B geometry without other face or shoulder seats; tighten chassis silhouette around retained seats without spreading controls. Use native Fire label, existing Android B guest mapping from Atari2600.kt blob cd5f063c735c1a5898bcf28085371f4f3a8d2af3. Dedicated Menu/Reset/Select utility rail, DIFF.A/B console switches accessible separately in gameplay menu (existing L1/L2 host identities), only real core dispatch. No fake analogue/paddle substitution; paddle/driving/keypad and colour/BW switch capabilities separately verified or explicitly limited. Preserve existing guest input identities, one-thumb proximity, no top shoulder buttons, mirrored layout, >=48dp independent touch targets, lifecycle release and measured screen clearance. Representative games Adventure, River Raid, Pitfall!, Space Invaders subject to actual compatible ROM and control requirements. Owner review pending; no runtime/build; GripArc and Joint deferred.

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
