# PLAYSTATION_3 — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## PS3-PA-01 — shared Sony side-stack review candidate (26 Nov 2026)

Propose exact inheritance of owner-approved [PSX-PA-03](PSX.md) DualShock topology: central D-pad, left-side independent L/R analogue stack (L above R), explicit corresponding L3/R3 only through real native inputs; PSP-derived Square/Triangle/Cross/Circle radial face arc and four fixed face Holds, mid-arc 2×2 L1/R1 and L2/R2 shoulder bank, rear Menu/Start/Select rail. Same chassis design language, not a bottom stick row or upper shoulder bank. Source and actual engine differences are separate from visual inheritance. DualShock 3 includes analogue L2/R2 and pressure-sensitive buttons plus PS and Sixaxis motion. Retain same bank, verify real proportional trigger axes, digital/pressure semantics and actual native PS/motion support separately; PS action in utility/menu only if real dispatch exists, never substitute CARTDECK Menu as guest PS. Engineering NativeGameSystems.kt identifies nativeBackend ps3 and empty systemCoreConfigs; do not assume Libretro input mappings or claim working native bridge. Preserve real independent axes and click semantics, verify actual backend, measured >=48dp discrete targets, full analogue travel, shoulder/trigger reach, mirror, holds/release, no overlap, viewport and exact physical fit. Dual-analogue simultaneous actions may require two hands. Enhanced gameplay tests required; unsupported engine features explicitly limited rather than simulated. Candidate awaiting owner approval; no code/build. GripArc and Joint deferred.

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
