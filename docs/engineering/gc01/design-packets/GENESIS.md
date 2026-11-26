# GENESIS — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Distinct 3-button/6-button controller profiles.

## MD-PA-01 — Digital 3 review candidate (26 Nov 2026)

Standard Mega Drive/Genesis three-button pad: digital D-pad, A/B/C, Start. Six-button variant remains separate open study. Design derived from owner-accepted PSP radial face arrangement observed in gameplay recording; retain GBA manufactured styling for digital profile. D-pad forms central movement nucleus. Remove analogue and shoulder seats (not native); propose A in upper-right arc (less-frequent special in Streets of Rage 2), B in lower-right arc (attack), C in lower-centre arc (jump); B and C adjacent for combinations, no intervening Hold in movement-to-action corridor. Use one provisional Hold B immediately outboard/right of B (configurable to another actual face key if separately approved, with visible identity), no permanent Hold A/C by default; confirm sustained need from broader game sample. No automatic turbo, macros, or fake combo. Start and CARTDECK Menu on separate utility rail; no Select in three-button mode. Sonic A/B/C are interchangeable jump, Streets of Rage 2 A special/B attack/C jump, Golden Axe A magic/B attack/C jump (game-specific defaults, not platform-wide mandates). Preserve native identity independent of spatial position; use exact current source inventory, 48dp+ discrete targets, clear directional/thumb sweep, B+C combination tests, Hold release, mirroring, measured device fit. If holding B is not useful in game sample, remove/disable the one Hold rather than add clutter. Layout and Hold proposals awaiting owner approval; six-button profile not approved; no APK.

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
