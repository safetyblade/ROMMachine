# SEGACD — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: THREE-BUTTON PIVOTARC OWNER LAYOUT LOCKED; SIX-BUTTON OPEN**.

**PivotArc:** standard_layout_locked_variants_open; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Verify 3-button/6-button game input mode.

## SCD-PA-01 — Digital 3 owner layout lock (26 Nov 2026)

Sega CD / Mega-CD uses the Mega Drive controller family. Owner approved exact inheritance of owner-locked [MD-PA-01](GENESIS.md) standard three-button radial geometry: D-pad nucleus, A upper-right, B right/lower-right, C lower-centre, fixed Hold B outboard, Start and CARTDECK Menu on separate rear utility rail. Preserve one-hand reach, GBA digital styling, mirroring and all native identities. No invented Select or shoulder. No per-game Hold configuration required; do not change approved Mega Drive geometry or add controls solely for CD media. Separate Sega CD platform identity and saved preferences; verify actual selected backend, guest key mapping, Start, combinations, Hold release, >=48dp independent targets, chassis clearance and physical fit. Six-button compatibility/design remains open and will be reviewed with Genesis/PCE Digital 6; do not infer current backend support. Owner design approval granted 26 Nov 2026. GripArc and Joint deferred; no build.

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
| Design prepared | THREE-BUTTON OWNER LAYOUT LOCKED; six-button open |
| Solo PivotArc visual | THREE-BUTTON DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
