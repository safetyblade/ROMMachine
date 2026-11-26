# WS — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two directional clusters and handheld orientation.

## WS-PA-01B — Dual Directional owner layout lock (26 Nov 2026)

Original WonderSwan has independent X1–X4 and Y1–Y4 cursor groups, A/B, Start, and hardware Sound (not presumed a guest key). [Mednafen documentation](https://mednafen.github.io/documentation/wswan.html) and existing repository PIV-HH-01 establish X usually primary in horizontal games, Y usually primary in vertical games; opposite cluster may act as actions. Existing source audit identifies WS_LANDSCAPE and WS_PORTRAIT PadKit layouts. Propose one compact GBA-derived Digital 2 layout with both cursor groups always visible: primary nucleus at (100,148), secondary four separate cardinal caps directly above B at (203,72), B (203,177), A (148,235), H B (260,177), H A (148,290) on illustrative 300×355 canvas. Owner placement correction: secondary cluster sits above B, not above the primary directional nucleus. Keep the main D-pad, A/B and Hold positions unchanged. Secondary cluster is a separate four-input control, not a decorative or aliased D-pad. Menu/Start separate utility rail. Horizontal assigns actual X group to primary seat, Y to secondary; vertical swaps groups to primary Y/secondary X, with orientation-correct native directions verified against live layout and game viewport. Preserve native A/B identities and two fixed Holds in both modes; no per-game assignment needed, no automatic alias, no invented Select/Sound. Use detected game orientation when reliable, explicit orientation override otherwise. Release inputs before any orientation switch. Exact current source mapping, independent >=48dp cardinal touch targets, two-group simultaneous combinations, chassis clearance, mirroring and one-thumb reach require measured verification; return for design review if safe fit fails. Some games requiring simultaneous independent X/Y may require a second finger or gamepad. Owner approved revised layout and shared WSC inheritance on 26 Nov 2026; no runtime or physical PASS.

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
| Design prepared | OWNER LAYOUT LOCKED — exact source/geometry pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
