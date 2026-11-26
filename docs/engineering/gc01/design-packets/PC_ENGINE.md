# PC_ENGINE — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## PCE-PA-01A/B — review candidates (26 Nov 2026)

Original PC Engine/TurboGrafx standard pad: D-pad, I/II, Run, Select. Optional Avenue Pad 6 has additional III–VI. Repository registers MEDNAFEN_PCE_FAST; upstream [Beetle PCE FAST controller documentation](https://docs.libretro.com/library/beetle_pce_fast/) distinguishes 2/6-button mode; III/IV can be turbo functions in 2-button mode. This is upstream capability, not current CARTDECK input proof.

A — standard (default): exact approved Digital 2 geometry: D-pad unchanged, I in lower-left action seat, II upper-right, independent H below I and right of II; Run/Select in existing Start/Select seats and Menu unchanged. No phantom buttons or automatic turbo. Preserve existing visual treatment and one-handed reach.

B — optional six-button mode: separate compact Digital 6 prototype, D-pad nucleus left/upper-left and two staggered rows of three face controls (upper IV/V/VI, lower III/II/I; preserve original Avenue Pad 6 relative order). Independent Hold for each face button required; exact coordinates and fit to be separately reviewed when the shared six-button family is designed. Retain Run/Select/Menu utility rail. No forced default, phantom input or claiming full 6-button operation until exact installed backend and PadKit mode/key routing verified; III/IV must not activate turbo instead of face keys. Mode selection intentional and per game if supported; no automatic gameplay. One-handed aim applies to both modes; require independent >=48dp targets, neutral release, simultaneous input, mirroring and physical reach; if six buttons and Holds cannot safely fit, flag limitation rather than silently remove controls. Standard and optional six-button concept await owner approval; B geometry remains unapproved until separate review. GripArc and Joint deferred.

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
