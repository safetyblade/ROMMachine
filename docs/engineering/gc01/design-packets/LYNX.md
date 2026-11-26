# LYNX — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## LYNX-PA-01 — Digital 2 + Options review candidate (26 Nov 2026)

Inherit approved GBA manufactured geometry and compact D-pad/A/B/Hold A/Hold B seats, substitute native Option 1/Option 2 for GBA L/R at existing shoulder seats (do not introduce top shoulder row or new remote bank); independent momentary options and separate Menu/Pause utility rail, no Select. Existing Lynx.kt engineering blob 8cd7e34107a98b23cf2f13ca7545db5e920899ab maps same-label Android A/B, Option 1=L1, Option 2=R1 and Start helper for Pause; verify actual Handy core guest identity, including option combinations and Pause, and no accidental reset. Preserve genuine vertical/rotated Lynx titles' display and directional mapping; mirror the full group without changing guest input identities, distinguish handedness from game orientation. No pretend rotation support; if unavailable disclose. Fixed A/B Holds proposed. Retain 48dp+ discrete targets, lifecycle release, existing video, measured fit and one-handed reach. Suggested tests California Games, Chip's Challenge, Blue Lightning, Gauntlet: The Third Encounter (orientation and input requirements to verify). Owner review pending; no implementation/build. GripArc/Joint deferred.

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
