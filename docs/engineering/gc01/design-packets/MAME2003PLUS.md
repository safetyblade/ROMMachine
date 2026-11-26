# MAME2003PLUS — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** 4/6 button arcade control modes and coin/start.

## ARC-PA-01 — shared arcade digital profiles owner layout lock (26 Nov 2026)

For MAME 2003-Plus, reuse approved GB Digital 2, MD-PA-01 Digital 3, SNES-PA-01 Digital 4 (remove shoulder seats without reflow), and D6-PA-01 Digital 6 radial geometry. Do not introduce conventional two-row fighting-game buttons into the one-thumb path. User-facing numbers 1–6 are physical legends, not assumed guest identity: proposed 2-button B/A seats = 2/1; 3-button A/B/C = 1/2/3; 4-button X/A/B/Y = 3/4/2/1; 6-button X/Y/Z/A/B/C = 4/5/6/1/2/3. Fixed Holds 1/2 for 2-button, 2 for 3-button, all four for 4-button, 2 for 6-button. These numbering and Hold choices are proposals, not verified core mappings; verify each actual game/core control port and explicit active mode before labelling or dispatch; game-specific controls must remain accessible. Dedicated Menu/Coin/Start utility rail, no invented guest credits, test coin/start identity and held release. Source GameSystem.kt exact engineering branch registers only FB_NEO_4/FB_NEO_6 and MAME_2003_4/MAME_2003_6; initial implementation 4/6 only. 2/3 are conditional compact profiles only if active game and backend mapping are verified and unused controls are genuinely unnecessary; no claim of current support. No automatic inference from ROM genre. Specialized trackball, spinner, light gun, twin-stick, >6-button and nonstandard games remain separate capability review/fallback, not silently converted. Native digital direction sectors and diagonals, 48dp independent effective targets, mirrored control geometry, measured viewport clearance, real guest input identity and release ownership required. Proposed tests: Metal Slug (four-button mapping/hold), Street Fighter II (six distinct actions and combinations), a simple shooter and beat-em-up for optional compact profiles only if supported. Owner approved ARC-PA-01 on 26 Nov 2026; no runtime/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — conditional 2/3 modes and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
