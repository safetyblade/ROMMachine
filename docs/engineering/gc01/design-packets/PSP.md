# PSP — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** solo_visual_accepted; owner physical visual_only.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Solo PivotArc appearance owner-accepted.

## PSP-PA-02 — ergonomic review, owner gameplay feedback (26 Nov 2026)

Reviewed 58-second owner-supplied recording 1000097281.mp4. Visible working reference: analogue left, central D-pad, Square above, Triangle upper-right, Cross lower-right, Circle lower-centre, L between Triangle/Cross, R lower-left, separate Hold Cross to right and Hold Circle below, rear three-control utility rail. Sampled gameplay uses stick, Square and Triangle; touch indicators do not prove one-hand operation or comfortable reach. Owner reports full circle and rear utility placement acceptable in practice, concerns L/R reach, whether X/O are the best lower pair and whether Square/Triangle need Holds. Retain approved visual language, radial layout, analogue and D-pad, and rear utility rail.

Owner correction (26 Nov): retain L at its existing right-middle seat between Triangle and Cross; move R from lower-left to immediately right of L. Earlier proposed move of L toward R is superseded and must not be implemented. Retain existing full circle, analogue, D-pad, rear utility rail, and X/O lower positions for the first comparison (lower pair still subject to gameplay review).

PSP-PA-03 review candidate: universal no-per-game-setup Hold access for all four face buttons. Existing H Cross right and H Circle below retained; propose H Square to the right/outboard of Square and H Triangle to the right/outboard of Triangle. Each H fixed to its own real guest face key; no per-game remapping required, no turbo or macro, explicit latched state/release and independent input ownership. Design for four dedicated H first, not an assumed four-H fit. Reopened supplied recording frame (540x1172 illustration): L center approximately (381,727); candidate R (465,727); candidate H Square (380,555), H Triangle (465,628); retain H Cross (465,823), H Circle (284,979). These are screenshot-relative concept centres only, not validated dp or exact source geometry. Keep all >=48dp effective touch regions independent and inside measured safe area; check primary thumb sweep, outer edge/shoulder reach, mirroring, held-plus-momentary input, neutral and lifecycle release. If four dedicated H cannot safely fit, return for owner design review; do not shrink hit targets, silently remap keys, or enforce per-game configuration. Two fixed Holds Cross/Square is a possible fallback for review only. No change to accepted visual style. Shoulder direction owner instructed; exact seat dimensions, four-H design and physical fit remain unapproved/unverified. No runtime changes.

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
| Solo PivotArc visual | OWNER VISUAL PASS — LOCKED |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
