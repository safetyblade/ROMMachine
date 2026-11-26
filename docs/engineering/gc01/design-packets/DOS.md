# DOS — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Keyboard/mouse and DOS gamepad profile options.

## DOS-PA-01A/B/C — PC input owner layout lock (26 Nov 2026)

A — Gamepad: inherit approved Sony side-mounted dual analogue chassis, D-pad, radial X/Y/A/B, 2×2 mid-arc L1/R1/L2/R2 and fixed X/Y/A/B Holds; Menu/Start/Select and explicit keyboard icon on utility rail. DOS has no universal guest gamepad mapping: preserve DOSBox Pure DOS_AUTO per-game actual mapping; do not claim all DOS games use these controls. Existing engineering DOS.kt blob 01176c39c3e994af6ffdea087be2a638a882033f uses ABXY, both analogue helpers, D-pad, L1/L2/R1/R2, Start/Select, THUMBL keyboard icon and CARTDECK Menu. Keyboard seat is not guest L3; no invented R3. B — Keyboard action profile: compact D-pad + four radial action seats with explicit saved game-specific key legends, fixed Holds for mapped actions only; accessible full keyboard, modifiers, Esc/Enter and other required keys, no silent removal of controls; actual key dispatch and key-up/lifecycle verification required before enabling. C — Mouse profile: accessible left/right click with optional fixed left/right Holds if real button-down/up supported, separate keyboard/menu access, touchscreen relative mouse or actual absolute mapping where supported, no fake joystick as mouse. Preserve screen viewport, pointer drag/release and independent mouse/keyboard concurrency. No game-specific macros or automatic mappings guessed from genre. B/C are proposed capability designs, not source-confirmed existing modes; verify DOSBox Pure guest keyboard/mouse bridge and native gamepad mapping before implementing. Keep conventional Standard available where unsupported. Representative tests Commander Keen, Jazz Jackrabbit, DOOM, SimCity 2000, The Secret of Monkey Island, with actual input requirements verified. Windows/Wine independent PC runtime remains separate, no claims of EXE compatibility from DOS profile. All geometry illustrative, 48dp independent targets, full analogue travel, mirror, saved per-game mapping, input release and viewport clearance measured. Owner approved 26 Nov 2026; native capability and physical gates remain open; no implementation/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — capability and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
