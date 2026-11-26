# PSX — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Separate digital and DualShock profiles.

## PSX-PA-01A/B — Digital 4 and DualShock review candidates (26 Nov 2026)

A — original digital controller: D-pad primary nucleus, PSP-derived Square top, Triangle upper-right, Cross lower-right, Circle lower-centre, four fixed per-face H outboard, two adjacent shoulder pairs L1/L2 and R1/R2 in upper bank, Menu/Start/Select separate utility rail. No analogue controls in digital mode. Illustrative 360×400 body centres: D (93,185), Square (180,108), Triangle (248,148), Cross (248,222), Circle (180,280), H Square (180,50), H Triangle (314,148), H Cross (314,222), H Circle (180,344); shoulder centres L1 (48,35), L2 (108,35), R1 (252,35), R2 (312,35). B — DualShock: inherit action and H arc and shoulder banks; left analogue at digital D nucleus (93,185), independent D-pad (58,104), right analogue (58,300), L3/R3 separate explicit controls if actual backend supports stick click; illustrative placement (48,368)/(112,368) subject to clearance and actual native identity. No automatic stick-click on moving or touching the analogue. Preserve independent left/right axes and D-pad, no silent substitution. Separate explicit digital and DualShock controller modes with core/device mode verification and preserved preferences; no claim of analogue support for digital-only games.

Exact source inventory observed in engineering PSX.kt and PSXDualShock.kt: Android A=Circle, B=Cross, Y=Square, X=Triangle; native L1/L2/R1/R2, Start/Select, D-pad; DualShock adds SecondaryAnalogLeft/Right. Verify actual guest mode, axis and L3/R3 dispatch independently; unsupported click must be disclosed, not simulated. All proposed coordinates illustrative, not dp or verified hitboxes. Require >=48dp independent targets, full analogue travel, native identity, Hold ownership/release, correct mode, handedness, viewport and physical fit. Keep original material and utility conventions. Digital-first for games that support it; dual-analogue simultaneous gameplay may not be achievable one-thumb. No removal of required controls or false support if fit or mode fails; return for redesign. Owner approval pending, no build. GripArc and Joint deferred.

## PSX-PA-02A/B — revised mid-arc shoulder bank (26 Nov 2026)

Owner rejected top shoulder placement for PivotArc one-handed reach and directed PSP-like placement. Supersede PA-01 upper shoulder bank and concept coordinates, retain digital/DualShock variants, PSP face order and four dedicated fixed Holds. Place a compact 2×2 shoulder bank outboard of the radial action arc between Triangle and Cross: L1/R1 upper row, L2/R2 lower row, R immediately right of corresponding L. No shoulder in main movement-to-face corridor. Proposed shared 360×400 illustrative chassis: primary D or left analogue (100,185), Square (154,90), Triangle (206,120), Cross (206,266), Circle (168,308); H Square (154,30), H Triangle (276,105), H Cross (276,292), H Circle (168,367). Shoulder centres L1 (264,169), R1 (320,169), L2 (264,225), R2 (320,225). Digital variant has no extra sticks. DualShock adds independent D-pad (54,90), right analogue (54,292), separate L3/R3 (38,376)/(94,376) only if real guest click input supported. All centres illustrative; measure effective directional/analogue envelopes, 48dp+ discrete independent targets, housing clearance, shoulder-to-face reach, no overlap, held ownership, mirroring and actual mode/guest mapping. Reduce visible caps if useful, never reduce effective targets below minimum or omit native controls. If fitting requires changing topology, return for review. Four-Hold arrangement retained as candidate, exact fit pending. Owner shoulder correction recorded; full revised designs still awaiting owner approval; no implementation or build.

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
