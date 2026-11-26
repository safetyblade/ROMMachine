# PC_ENGINE — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: STANDARD AND SIX-BUTTON OWNER LAYOUTS LOCKED; AVENUE PAD 3 ALIAS OPEN**.

**PivotArc:** standard_layout_locked_variants_open; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

## PCE-PA-01A/B — owner decision (26 Nov 2026)

Original PC Engine/TurboGrafx standard pad: D-pad, I/II, Run, Select. Optional Avenue Pad 6 has additional III–VI. Repository registers MEDNAFEN_PCE_FAST; upstream [Beetle PCE FAST controller documentation](https://docs.libretro.com/library/beetle_pce_fast/) distinguishes 2/6-button mode; III/IV can be turbo functions in 2-button mode. This is upstream capability, not current CARTDECK input proof.

A — standard (default), OWNER APPROVED: exact approved Digital 2 geometry: D-pad unchanged, I in lower-left action seat, II upper-right, independent H below I and right of II; Run/Select in existing Start/Select seats and Menu unchanged. No phantom buttons or automatic turbo. Preserve existing visual treatment and one-handed reach.

B — optional six-button mode: previous staggered two-row / six permanent Hold proposal WITHDRAWN by owner due to thumb path clashes. No implementation authorized for that geometry. Pull forward common three/four/six-button design study; use accepted PSP radial distribution as the starting reference, not the rejected rows. Revised six-button design approved below; one-hand reach and actual guest mappings require engineering validation.

C — historical Avenue Pad 3: third physical button mirrors Run or Select, not an independent extra guest key. Consider as a configurable near-thumb duplicate only for games benefiting from this affordance; separate approval and current core validation required. See https://docs.joypad.ai/protocols/PCENGINE/ and https://www.classicplastic.net/trt/issue039/ . No extra default button in locked standard layout.

For multi-button variants retain all guest face inputs, select dedicated Hold affordances by sustained-action usage rather than installing one per face key; two-button approvals unchanged. See [multi-button study](../GC-ARC-MULTIBUTTON-STUDY.md). Standard geometry source/physical verification pending; variants open; GripArc and Joint deferred.

## PCE-PA-01B — revised Digital 6 owner layout lock (26 Nov 2026)

Shared Digital 6 owner-approved D6-PA-01: single extended radial face arc around D-pad; concept canvas 350×405, centres D-pad (140,172), X/IV (61,84), Y/V (137,48), Z/VI (215,72), A/III (257,140), B/II (242,222), C/I (177,285), H B/II (310,222), H I for PCE only (177,350). Coordinates illustrative, not dp or source geometry. Keep primary A/B/C in lower/right sectors; upper three additions outside D-pad travel corridor. No automatic per-game Hold mapping, no Hold inside arc, no turbo/macros. Six independent actual face keys and safe >=48dp hit regions, measured collar/chassis clearance, one-handed reach, both hands, directional-to-face transitions and fighting-game combinations require proof; if unsafe, return for review rather than shrinking/hiding buttons. Standard approved controller geometry stays unchanged; six-button profile opt-in/actual mode-aware, no false runtime claims.

PCE legends IV/V/VI upper, III/II/I right/lower. Fixed Holds II and I (retain standard two-button Hold identities, not identical seat positions in extended mode). Run/Select/Menu rail and verified native Mode Switch (upstream [Beetle PCE FAST](https://docs.libretro.com/library/beetle_pce_fast/)); verify mode really exposes III–VI, not turbo aliases. Earlier two-row sketch remains withdrawn. Avenue Pad 3 Run/Select alias separately open; no additional guest key assumed. Owner design approval granted 26 Nov 2026; source/physical acceptance pending; no APK.

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
| Design prepared | STANDARD AND SIX-BUTTON OWNER LAYOUTS LOCKED; Avenue Pad 3 alias open |
| Solo PivotArc visual | STANDARD AND SIX-BUTTON DESIGNS LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
