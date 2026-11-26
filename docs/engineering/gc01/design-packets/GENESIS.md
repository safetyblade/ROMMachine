# GENESIS — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: THREE- AND SIX-BUTTON PIVOTARC OWNER LAYOUTS LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Distinct 3-button/6-button controller profiles.

## MD-PA-01 — Digital 3 owner layout lock (26 Nov 2026)

Standard Mega Drive/Genesis three-button pad: digital D-pad, A/B/C, Start. Six-button variant separately approved below. Design derived from owner-accepted PSP radial face arrangement observed in gameplay recording; retain GBA manufactured styling for digital profile. D-pad forms central movement nucleus. Remove analogue and shoulder seats (not native); owner approved A in upper-right arc (less-frequent special in Streets of Rage 2), B in lower-right arc (attack), C in lower-centre arc (jump); B and C adjacent for combinations, no intervening Hold in movement-to-action corridor. Owner approved one Hold B immediately outboard/right of B (fixed B in the approved default; any alternate assignment requires separate approval), no permanent Hold A/C by default; confirm sustained need from broader game sample. No automatic turbo, macros, or fake combo. Start and CARTDECK Menu on separate utility rail; no Select in three-button mode. Sonic A/B/C are interchangeable jump, Streets of Rage 2 A special/B attack/C jump, Golden Axe A magic/B attack/C jump (game-specific defaults, not platform-wide mandates). Preserve native identity independent of spatial position; use exact current source inventory, 48dp+ discrete targets, clear directional/thumb sweep, B+C combination tests, Hold release, mirroring, measured device fit. Do not silently alter the approved one-Hold layout; any later universal Hold extension must be separately reviewed. Owner approved MD-PA-01 on 26 Nov 2026; six-button profile separately approved below; exact source and physical validation pending; no APK.

## MD-PA-02 — Digital 6 owner layout lock (26 Nov 2026)

Shared Digital 6 owner-approved D6-PA-01: single extended radial face arc around D-pad; concept canvas 350×405, centres D-pad (140,172), X/IV (61,84), Y/V (137,48), Z/VI (215,72), A/III (257,140), B/II (242,222), C/I (177,285), H B/II (310,222), H I for PCE only (177,350). Coordinates illustrative, not dp or source geometry. Keep primary A/B/C in lower/right sectors; upper three additions outside D-pad travel corridor. No automatic per-game Hold mapping, no Hold inside arc, no turbo/macros. Six independent actual face keys and safe >=48dp hit regions, measured collar/chassis clearance, one-handed reach, both hands, directional-to-face transitions and fighting-game combinations require proof; if unsafe, return for review rather than shrinking/hiding buttons. Standard approved controller geometry stays unchanged; six-button profile opt-in/actual mode-aware, no false runtime claims.

Mega Drive legends X/Y/Z upper and A/B/C right/lower; fixed Hold B only as in approved Digital 3. Start/Menu and native Mode where supported (not assumed distinct guest key; verify mode/compatibility semantics against installed backend). Upstream [Genesis Plus GX](https://docs.libretro.com/library/genesis_plus_gx/) documents three/six-button devices; current CARTDECK bindings unverified. Street Fighter II X/Y/Z punches, A/B/C kicks (https://www.manualshelf.com/manual/radica-games/street-fighter-ii-75030/mega-drive-instruction-manual.html). Owner design approval granted 26 Nov 2026; exact source/physical acceptance pending; no APK.

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
| Design prepared | THREE- AND SIX-BUTTON OWNER LAYOUTS LOCKED |
| Solo PivotArc visual | THREE- AND SIX-BUTTON DESIGNS LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
