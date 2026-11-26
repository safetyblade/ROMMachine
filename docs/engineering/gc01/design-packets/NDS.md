# NDS — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two screens, stylus and real mic/lid capabilities.

## NDS-PA-01 — Digital 4 Compact review candidate (26 Nov 2026)

Owner constraint: two vertical screens leave less space than other systems. Existing approved stacked screen bounds, aspect, lower touchscreen/stylus coordinates and viewport separation take priority. Reserve the measured free lower band after system insets and existing utility rail; no overlay, transparent hit intercept or automatic shrinking of screens. Adapt SNES radial topology, not its full-sized housing. Proposed compact wide/short housing: D-pad (87,110), X (168,40), A (238,70), B (238,138), Y (168,174), adjacent vertical L/R right bank (297,40)/(297,92), H B (303,145), H Y (108,190) on an illustrative 360×280 reference canvas, rear Menu/Start/Select rail at y250. These are candidate positions only, not dp or validated handset fit; compact measured chassis and control group mirroring required. Two fixed Holds Y and B proposed as run/action and lower face defaults, no per-game remap; A/X remain directly accessible. Four Holds not ruled out if safe clearance exists; if two insufficient or targets cannot fit, return for owner review, don't silently hide native inputs or shrink touch targets below 48dp. Keep auxiliary microphone/lid/screen actions accessible separately in the existing supported menu where real backend functionality exists, not invented guest controls; core-specific DeSmuME/melonDS auxiliary codes require exact current source verification (historical PIV-HH-01). Screen swap if already available must preserve stylus coordinate mapping. Verify real two-screen rendered bounds, touch pass-through/tap/drag, guest keys, held input/release, screen rotation, L/R, one-hand reach, no overlap and clearance at 320/360/393dp and actual owner handset. If safe fit fails, retain existing Standard or supported alternative layout; no universal one-thumb guarantee for simultaneous stylus/digital input. Owner review pending; no build/physical acceptance. GripArc and Joint deferred.

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
