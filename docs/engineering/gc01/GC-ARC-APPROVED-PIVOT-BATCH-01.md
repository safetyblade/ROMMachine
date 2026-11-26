# GC-ARC — approved PivotARC batch 01

Owner authorized pushing completed designs before resuming Nintendo DS on 26 Nov 2026. Scope: 15 system IDs; issue #377, draft engineering PR #376. This is an immutable design handoff snapshot, NOT a runtime implementation or physical/CI acceptance. Prior accepted GBA and PSP artwork remains protected; only owner-approved ergonomic PSP-PA-03 adjustments authorized. DS, 3DS, PC Engine Avenue Pad 3 alias, all other unapproved variants, new GripARC designs and Joint excluded. GBA GripARC physical acceptance remains a prerequisite for new solo PivotARC runtime implementation per #377. No merge authorized. Do not use historical candidate geometry where a later owner decision supersedes it. In particular use revised PCE six-button radial profile, not rejected rows; PSP-PA-03 overrides PSP-PA-02 proposals. Retain all approved fixed Hold assignments, native input identity and 48dp+ targets.

## Engineering acceptance sequence

1. Verify exact current source mappings and active backend/mode for each approved profile; keep unsupported variants unavailable. Preserve GBA visual geometry and existing working controls.
2. Implement approved profiles in a coherent source batch, not per-console commits/builds. Keep existing saves and settings compatible. No DS changes.
3. Deterministic geometry and input ownership tests: all native controls, discrete target sizes, non-overlap, measured safe areas, mirrored layouts, real Down/Up, simultaneous keys and release on pause/background; for PSP enforce downward translation without moving internal geometry; six-button modes verify real guest mapping.
4. Run formatting (ktlintCheck), Kotlin compilation and unit tests before native staging. Only then intentional integration APK, record exact SHA/versionCode/artifact and per-console physical tests. GBA GripARC exact-head a627a713 had three successful workflows (Room, Kotlin, APK), not handset PASS.

## Scope and source record

Source branch: `design/gc-arc-33-console-packets-20261126`; manifest blob `e61ec3cb2643814b5bbe7c39cad04f34dd4342ec`. Packet snapshots below are copied verbatim except heading nesting and relative links retained as historical references; canonical source links provided. Later approvals take priority over earlier proposals. Only approved scope is authorized.

| System | Design status | Source blob | Scope |
|---|---|---|---|
| GB | owner_layout_locked | `d290a3c3c81163f794430ae22df380e6ad5438ce` | GB-PA-01 owner layout locked: exact GBA D/A/B and Hold A/B geometry, remove L/R, compact one-handed use; source and physical pending. |
| GBC | owner_layout_locked | `5eb60c03ee128a5ae50007ee5b9a111d54776d93` | GBC-PA-01 owner approved exact GB-PA-01 geometry, one-handed, individual Hold A/B; engineering and physical pending. |
| NES | owner_layout_locked | `34b7d6e3405e94d916dd0302278d0d01fdd2f696` | NES-PA-01 owner approved identical GB/GBC one-thumb layout, individual Hold A/B; source and physical pending. |
| SMS | owner_layout_locked | `6c7489d0d6be9011fc03f20ae8e4298611faa4be` | SMS-PA-01 owner approved Digital 2 geometry, 1/START lower-left, 2 upper-right, Holds, Menu and guest Pause; engineering and physical pending. |
| GG | owner_layout_locked | `f1dccf4bff5014cf142158703c0401359dc745f1` | GG-PA-01 owner advanced with Next; Digital 2 geometry with native Start, 1/2 Holds and mapping preservation; source and physical pending. |
| NGP | owner_layout_locked | `661f68f4a271190b59fb4319e50c25ef48ae2bda` | NGP-PA-01 owner approved Digital 2 Click, A/B Holds and Menu/Option; engineering and physical pending. |
| NGC | owner_layout_locked | `c1cbea8409a5b6fcfec52bc0238b13fad910701f` | Neo Geo Pocket Color, NOT GameCube. NGC-PA-01 owner approved exact NGP Digital 2 Click inheritance; separate backend and physical validation pending. |
| PC_ENGINE | standard_layout_locked_variants_open | `a5d8871daa011cca58dd7d041caaed701f3c83c0` | Standard Digital 2 and revised D6-PA-01 six-button radial profiles owner approved; fixed Holds I/II; Avenue Pad 3 alias open; source, mode and physical pending. |
| GBA | solo_visual_accepted | `e242910c1521051ecdfaec11bbdc633bf172decf` | Solo PivotArc owner-accepted; large GripArc full-body prototype in draft PR #376; pending new GBA button-position acceptance. |
| SNES | owner_layout_locked | `82d02cf3e6072c13e99860e76f2012c5cac28f63` | SNES-PA-01 owner approved PSP-derived Digital 4 X/A/B/Y circle, four Holds and adjacent L/R; engineering and physical pending. |
| GENESIS | owner_layout_locked | `050b1fd5cbea44bf4a3bea67fa7613bd674fd7d6` | Standard Digital 3 and D6-PA-01 six-button radial profiles owner approved; fixed Hold B; source, mode and physical acceptance pending. |
| SEGACD | owner_layout_locked | `0efc479de49e35306c11f623445e2162c11641e6` | Standard Digital 3 and D6-PA-01 six-button radial profiles owner approved; fixed Hold B; source, mode and physical acceptance pending. |
| WS | owner_layout_locked | `c491efc74c2a3b7ee93dcdd8cae5c3f000e876ee` | Owner approved WS-PA-01B dual directional layout (secondary above B), shared WS/WSC orientation mapping and fixed Holds A/B; source/physical pending. |
| WSC | owner_layout_locked | `4e0990f20f02fd5868a8caa38c18bb9860a7ef52` | Owner approved WS-PA-01B dual directional layout (secondary above B), shared WS/WSC orientation mapping and fixed Holds A/B; source/physical pending. |
| PSP | owner_layout_locked | `7808891fdac2a857546f7a5044b0d2fef832828d` | PSP-PA-03 owner approved four fixed face Holds, R right of L, existing circle/rail; mandatory rigid downward translation for measured chassis clearance; revised physical/source pending. |

## Frozen packet snapshots

### GB

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/GB.md) — blob `d290a3c3c81163f794430ae22df380e6ad5438ce`

#### GB — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### Owner layout lock — GB-PA-01 (26 Nov 2026)

Owner approved the supplied GBA runtime screenshot as the governing visual reference, including close-grip one-handed digital operation and individual face-button Hold. This supersedes the earlier conventional left/right controller sketch in #377. Preserve the accepted GBA shell, artwork, control sizes and actual native geometry: D-pad upper-left, B upper-right, A diagonally down-left of B, Hold B immediately right of B and Hold A directly below A. Remove GBA-only L/R controls and their visible/touch elements; do not spread or reflow the remaining controls. Preserve the existing separate three-button Menu/Start/Select utility rail and existing mapping/legends. No additional buttons or stick. Mirror the complete group for handedness without reversing native key/direction identities. Existing source-owned Hold behavior must allow latched B + direction, independent A/B, clear held inputs on exit/reset and avoid duplicate or stuck guest presses.

Screenshot-derived normalized centre targets (approximate visual reference, not measured dp, panel bounds x165–694 / y740–1345 in supplied 708×1536 image): D-pad (0.29,0.28), B (0.66,0.54), A (0.40,0.71), Hold B (0.88,0.54), Hold A (0.40,0.90). Existing utility rail outside panel below it. Engineering must reuse exact accepted GBA source positions/scale, not treat these approximations as a new geometry solver or pixel-perfect source export. Preserve game viewport, safe areas, independent >=48dp targets and close reach; check both hands, portrait and any supported orientation. If no safe fit, do not silently spread the cluster or hide controls.

Native inventory: D-pad, A, B, Start, Select; Menu and two H affordances are CARTDECK controls, not extra guest keys. Historical source-backed reference: [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) identifies GB/GBC as existing supported source profiles with common input topology; current PR #376 exact-head source binding/geometry validation and handset acceptance still required. GBA visual reference does not confer GB physical PASS. GripArc and Joint remain deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### GBC

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/GBC.md) — blob `5eb60c03ee128a5ae50007ee5b9a111d54776d93`

#### GBC — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### GBC-PA-01 — owner layout lock (26 Nov 2026)

Owner approved inheriting [GB-PA-01](GB.md) unchanged: same original D-pad/A/B/Start/Select inventory, same close-grip D/A/B triangle, both individual Hold affordances, same GBA-derived chassis and utility rail, same handedness and native input semantics. No L/R or sticks. Game Boy Color game screen/palette and platform identity remain independent; no forced console-specific skin or geometry. Original control reference: https://www.manualslib.com/manual/4039910/Nintendo-Game-Boy-Color.html?page=4 and https://en.wikipedia.org/wiki/Game_Boy_Color . Owner design approval granted 26 Nov 2026; current exact-head native/source validation pending. No physical or build PASS.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### NES

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/NES.md) — blob `34b7d6e3405e94d916dd0302278d0d01fdd2f696`

#### NES — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### NES-PA-01 — owner layout lock (26 Nov 2026)

Original NES standard pad uses D-pad, B and A, Select and Start (no sticks/shoulders); Nintendo original control deck instructions: https://www.world-of-nintendo.com/manuals/nes/control_deck.shtml . Primary D-pad/A/B, secondary Select/Start (some games use Select during play). Owner approved identical GB-PA-01/GBC-PA-01 compact GBA-derived one-thumb geometry, including B upper-right, A diagonally below-left, Hold B immediately right and Hold A below; preserve separate Menu/Start/Select rail and actual guest mappings. Deliberately do not copy the original widely separated horizontal arrangement: one-handed operation is the universal PivotArc aim. Special peripherals and additional player controllers outside this standard-pad layout decision. Owner design approval granted; exact source binding and physical acceptance pending. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### SMS

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/SMS.md) — blob `6c7489d0d6be9011fc03f20ae8e4298611faa4be`

#### SMS — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### SMS-PA-01 — owner layout lock (26 Nov 2026)

Original standard pad D-pad, 1 (also Start), 2; Pause on console, no separate Select. Reference: https://www.manualslib.com/manual/1027819/sega-master-system.html and https://wiki.batocera.org/systems%3Amastersystem . Owner approved reuse of locked GB/GBC/NES compact GBA-derived geometry: D-pad unchanged, 1/START in lower-left A seat, 2 in upper-right B seat, independent H1 below 1 and H2 right of 2 (same visual H glyph as existing face Hold). Preserve one-handed reach, shell materials, button dimensions and handedness. Separate utility rail retains CARTDECK Menu and actual guest Pause, no invented Select or independent Start; remove unused utility seat from active controls and preserve remaining geometry unless separately approved. Pause is guest input, not emulator menu. Validate real current PadKit/core input mapping (numbered visual labels must not be assumed from Android A/B), native Pause routing, Hold semantics, 48dp+ hitboxes and physical one-handed reach. Special peripherals outside standard pad scope. Owner design approval granted; no APK or physical acceptance. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### GG

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/GG.md) — blob `f1dccf4bff5014cf142158703c0401359dc745f1`

#### GG — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### GG-PA-01 — owner layout lock (26 Nov 2026)

Original Game Gear: D-pad, 1, 2 and dedicated Start; no Select or independent Pause. Owner approved reuse of owner-locked SMS/GB Digital 2 one-hand geometry, GBA shell and independent face Holds: 1 in lower-left seat, 2 in upper-right seat, Hold 1 below and Hold 2 immediately right, D-pad unchanged. Separate utility rail uses CARTDECK Menu and native Start instead of SMS Pause; no phantom Select or shoulder. Preserve palette, sizes, mirroring and close one-thumb reach. Historical repository source-backed [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) explicitly records native B = displayed 1, native A = displayed 2; seat identity is visual, not guest key identity. Preserve each native mapping and bind Holds to the same underlying guest input as the corresponding face button. Exact current PR head validation, 48dp+ targets and physical verification still pending. Owner design approval inferred from 'Next' in sequential approval workflow; engineering and physical pending. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### NGP

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/NGP.md) — blob `661f68f4a271190b59fb4319e50c25ef48ae2bda`

#### NGP — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### NGP-PA-01 — owner layout lock (26 Nov 2026)

Original Neo Geo Pocket: eight-way digital click-stick, A/B, Option. Use approved GB Digital 2 GBA-derived one-hand shell and exact action/Hold geometry: B upper-right, A diagonally lower-left, H immediately right of B and below A. Replace D-pad artwork only in its existing seat with circular tactile digital click-stick (eight directional sectors, clear diagonal feedback, no analogue axis, no invented push-click input). Preserve the original directional event identities, compact reach and existing mirrored group. Separate rail: CARTDECK Menu and native Option; no invented Start/Select. The historical repository source audit [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) records Android A displayed B and Android B displayed A. Match Hold to actual guest key, not label-derived Android key. Preserve simultaneous A+B and directional combinations through existing input ownership; validate exact-head mapping, Option routing, diagonal transitions/neutral release, >=48dp discrete targets and physical fit. No claim of physical mechanical click or analogue support. Proposed shared variant for NGC after separate owner review. Owner design approval granted 26 Nov 2026; no physical/CI PASS. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### NGC

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/NGC.md) — blob `c1cbea8409a5b6fcfec52bc0238b13fad910701f`

#### NGC — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Neo Geo Pocket Color, NOT GameCube.

##### NGC-PA-01 — owner layout lock (26 Nov 2026)

Neo Geo Pocket Color (SystemID.NGC, not GameCube) has the same eight-way digital click-stick, A/B and Option topology as NGP. Owner approved exact inheritance of owner-approved [NGP-PA-01](NGP.md): Digital 2 Click shell and directional seat, B upper-right and A lower-left, individual H immediately right of B and below A, Menu/Option utility rail, one-handed reach and handedness. No analogue axis, push-click input, shoulders or fabricated Start/Select. Maintain distinct platform identity and saved preferences. Historical source-backed [PIV-HH-01](../../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) identifies shared NGP/NGC control family; validate active NGC backend, exact guest mapping and Hold ownership, diagonals/neutral release, 48dp+ targets and physical fit independently. Owner design approval granted 26 Nov 2026; no build or physical PASS. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — exact source geometry audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### PC_ENGINE

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/PC_ENGINE.md) — blob `a5d8871daa011cca58dd7d041caaed701f3c83c0`

#### PC_ENGINE — controller design packet

**Batch:** A · **Hardware family:** Two-button · **Status: STANDARD AND SIX-BUTTON OWNER LAYOUTS LOCKED; AVENUE PAD 3 ALIAS OPEN**.

**PivotArc:** standard_layout_locked_variants_open; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### PCE-PA-01A/B — owner decision (26 Nov 2026)

Original PC Engine/TurboGrafx standard pad: D-pad, I/II, Run, Select. Optional Avenue Pad 6 has additional III–VI. Repository registers MEDNAFEN_PCE_FAST; upstream [Beetle PCE FAST controller documentation](https://docs.libretro.com/library/beetle_pce_fast/) distinguishes 2/6-button mode; III/IV can be turbo functions in 2-button mode. This is upstream capability, not current CARTDECK input proof.

A — standard (default), OWNER APPROVED: exact approved Digital 2 geometry: D-pad unchanged, I in lower-left action seat, II upper-right, independent H below I and right of II; Run/Select in existing Start/Select seats and Menu unchanged. No phantom buttons or automatic turbo. Preserve existing visual treatment and one-handed reach.

B — optional six-button mode: previous staggered two-row / six permanent Hold proposal WITHDRAWN by owner due to thumb path clashes. No implementation authorized for that geometry. Pull forward common three/four/six-button design study; use accepted PSP radial distribution as the starting reference, not the rejected rows. Revised six-button design approved below; one-hand reach and actual guest mappings require engineering validation.

C — historical Avenue Pad 3: third physical button mirrors Run or Select, not an independent extra guest key. Consider as a configurable near-thumb duplicate only for games benefiting from this affordance; separate approval and current core validation required. See https://docs.joypad.ai/protocols/PCENGINE/ and https://www.classicplastic.net/trt/issue039/ . No extra default button in locked standard layout.

For multi-button variants retain all guest face inputs, select dedicated Hold affordances by sustained-action usage rather than installing one per face key; two-button approvals unchanged. See [multi-button study](../GC-ARC-MULTIBUTTON-STUDY.md). Standard geometry source/physical verification pending; variants open; GripArc and Joint deferred.

##### PCE-PA-01B — revised Digital 6 owner layout lock (26 Nov 2026)

Shared Digital 6 owner-approved D6-PA-01: single extended radial face arc around D-pad; concept canvas 350×405, centres D-pad (140,172), X/IV (61,84), Y/V (137,48), Z/VI (215,72), A/III (257,140), B/II (242,222), C/I (177,285), H B/II (310,222), H I for PCE only (177,350). Coordinates illustrative, not dp or source geometry. Keep primary A/B/C in lower/right sectors; upper three additions outside D-pad travel corridor. No automatic per-game Hold mapping, no Hold inside arc, no turbo/macros. Six independent actual face keys and safe >=48dp hit regions, measured collar/chassis clearance, one-handed reach, both hands, directional-to-face transitions and fighting-game combinations require proof; if unsafe, return for review rather than shrinking/hiding buttons. Standard approved controller geometry stays unchanged; six-button profile opt-in/actual mode-aware, no false runtime claims.

PCE legends IV/V/VI upper, III/II/I right/lower. Fixed Holds II and I (retain standard two-button Hold identities, not identical seat positions in extended mode). Run/Select/Menu rail and verified native Mode Switch (upstream [Beetle PCE FAST](https://docs.libretro.com/library/beetle_pce_fast/)); verify mode really exposes III–VI, not turbo aliases. Earlier two-row sketch remains withdrawn. Avenue Pad 3 Run/Select alias separately open; no additional guest key assumed. Owner design approval granted 26 Nov 2026; source/physical acceptance pending; no APK.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### GBA

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/GBA.md) — blob `e242910c1521051ecdfaec11bbdc633bf172decf`

#### GBA — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** solo_visual_accepted; owner physical visual_only.  
**GripArc:** full_body_candidate_pr376; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Solo PivotArc owner-accepted; large GripArc full-body prototype in draft PR #376; pending new GBA button-position acceptance.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### SNES

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/SNES.md) — blob `82d02cf3e6072c13e99860e76f2012c5cac28f63`

#### SNES — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### SNES-PA-01 — Digital 4 owner layout lock (26 Nov 2026)

Original SNES: D-pad, A/B/X/Y, L/R, Start/Select. Adapt owner-approved [PSP-PA-03](PSP.md) circular geometry to digital-only hardware: omit analogue stick and its seat; retain D-pad nucleus, PSP relative radial face seats (X in Square/top seat, A in Triangle/upper-right seat, B in Cross/lower-right seat, Y in Circle/lower-centre seat). Four fixed independent H affordances adjacent/outboard of respective faces (X right of X, A right of A, B right of B, Y below Y), no per-game Hold reassignment required. L/R adjacent with R to right of L between A/B, separate Menu/Start/Select utility rail. Preserve GBA digital manufactured styling, existing PSP radial geometry and mandatory measured downward chassis clearance principle; no fake analogue or reflow into vacant analogue seat. Proposed schematic centre coordinates on 300x350 concept canvas: D-pad (136,153), X (164,64), A (217,103), B (212,213), Y (159,253), H X (212,64), H A (264,103), H B (264,213), H Y (159,293), L (212,158), R (262,158); these are pre-clearance relative reference only, not validated dp. Prefer compact chassis left extent around D-pad without moving approved relative control seats; measure all visible collars and hitboxes, >=48dp independent targets and safe rim/utility rail clearance, rigid downward adjustment as needed; do not shrink hitboxes or silently move individual buttons. Owner design approval granted; exact source and measured geometry pending.

Rationale: Super Mario World Y/X run, B jump, A spin (https://www.world-of-nintendo.com/manuals/super_nes/super_mario_world.shtml); Super Metroid default B dash, A jump, X fire (https://www.world-of-nintendo.com/manuals/super_nes/super_metroid.shtml). Different sustained actions justify four fixed Holds. Confirm exact current native identity, held ownership/lifecycle release, L/R simultaneous input, Y hold plus D-pad/B, diagonals, mirrored layout and device fit. If four Holds or paired shoulders cannot fit, return for review rather than removing native inputs or forcing per-game remaps. No source/physical PASS, no APK. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — source/clearance audit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


---

### GENESIS

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/GENESIS.md) — blob `050b1fd5cbea44bf4a3bea67fa7613bd674fd7d6`

#### GENESIS — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: THREE- AND SIX-BUTTON PIVOTARC OWNER LAYOUTS LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Distinct 3-button/6-button controller profiles.

##### MD-PA-01 — Digital 3 owner layout lock (26 Nov 2026)

Standard Mega Drive/Genesis three-button pad: digital D-pad, A/B/C, Start. Six-button variant separately approved below. Design derived from owner-accepted PSP radial face arrangement observed in gameplay recording; retain GBA manufactured styling for digital profile. D-pad forms central movement nucleus. Remove analogue and shoulder seats (not native); owner approved A in upper-right arc (less-frequent special in Streets of Rage 2), B in lower-right arc (attack), C in lower-centre arc (jump); B and C adjacent for combinations, no intervening Hold in movement-to-action corridor. Owner approved one Hold B immediately outboard/right of B (fixed B in the approved default; any alternate assignment requires separate approval), no permanent Hold A/C by default; confirm sustained need from broader game sample. No automatic turbo, macros, or fake combo. Start and CARTDECK Menu on separate utility rail; no Select in three-button mode. Sonic A/B/C are interchangeable jump, Streets of Rage 2 A special/B attack/C jump, Golden Axe A magic/B attack/C jump (game-specific defaults, not platform-wide mandates). Preserve native identity independent of spatial position; use exact current source inventory, 48dp+ discrete targets, clear directional/thumb sweep, B+C combination tests, Hold release, mirroring, measured device fit. Do not silently alter the approved one-Hold layout; any later universal Hold extension must be separately reviewed. Owner approved MD-PA-01 on 26 Nov 2026; six-button profile separately approved below; exact source and physical validation pending; no APK.

##### MD-PA-02 — Digital 6 owner layout lock (26 Nov 2026)

Shared Digital 6 owner-approved D6-PA-01: single extended radial face arc around D-pad; concept canvas 350×405, centres D-pad (140,172), X/IV (61,84), Y/V (137,48), Z/VI (215,72), A/III (257,140), B/II (242,222), C/I (177,285), H B/II (310,222), H I for PCE only (177,350). Coordinates illustrative, not dp or source geometry. Keep primary A/B/C in lower/right sectors; upper three additions outside D-pad travel corridor. No automatic per-game Hold mapping, no Hold inside arc, no turbo/macros. Six independent actual face keys and safe >=48dp hit regions, measured collar/chassis clearance, one-handed reach, both hands, directional-to-face transitions and fighting-game combinations require proof; if unsafe, return for review rather than shrinking/hiding buttons. Standard approved controller geometry stays unchanged; six-button profile opt-in/actual mode-aware, no false runtime claims.

Mega Drive legends X/Y/Z upper and A/B/C right/lower; fixed Hold B only as in approved Digital 3. Start/Menu and native Mode where supported (not assumed distinct guest key; verify mode/compatibility semantics against installed backend). Upstream [Genesis Plus GX](https://docs.libretro.com/library/genesis_plus_gx/) documents three/six-button devices; current CARTDECK bindings unverified. Street Fighter II X/Y/Z punches, A/B/C kicks (https://www.manualshelf.com/manual/radica-games/street-fighter-ii-75030/mega-drive-instruction-manual.html). Owner design approval granted 26 Nov 2026; exact source/physical acceptance pending; no APK.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### SEGACD

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/SEGACD.md) — blob `0efc479de49e35306c11f623445e2162c11641e6`

#### SEGACD — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: THREE- AND SIX-BUTTON PIVOTARC OWNER LAYOUTS LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Verify 3-button/6-button game input mode.

##### SCD-PA-01 — Digital 3 owner layout lock (26 Nov 2026)

Sega CD / Mega-CD uses the Mega Drive controller family. Owner approved exact inheritance of owner-locked [MD-PA-01](GENESIS.md) standard three-button radial geometry: D-pad nucleus, A upper-right, B right/lower-right, C lower-centre, fixed Hold B outboard, Start and CARTDECK Menu on separate rear utility rail. Preserve one-hand reach, GBA digital styling, mirroring and all native identities. No invented Select or shoulder. No per-game Hold configuration required; do not change approved Mega Drive geometry or add controls solely for CD media. Separate Sega CD platform identity and saved preferences; verify actual selected backend, guest key mapping, Start, combinations, Hold release, >=48dp independent targets, chassis clearance and physical fit. Six-button design separately approved below; compatibility verification pending; do not infer current backend support. Owner design approval granted 26 Nov 2026. GripArc and Joint deferred; no build.

##### SCD-PA-02 — Digital 6 owner layout lock (26 Nov 2026)

Owner approved exact inheritance of [MD-PA-02](GENESIS.md) extended radial six-button profile and fixed Hold B, subject to separate Sega CD native backend/controller-mode proof. Standard three-button owner approval unchanged; verify native Mode if exposed, Start/Menu, all six faces, independent touch targets and one-hand reach. Owner design approval granted 26 Nov 2026; exact source/physical acceptance pending; no APK.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### WS

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/WS.md) — blob `c491efc74c2a3b7ee93dcdd8cae5c3f000e876ee`

#### WS — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two directional clusters and handheld orientation.

##### WS-PA-01B — Dual Directional owner layout lock (26 Nov 2026)

Original WonderSwan has independent X1–X4 and Y1–Y4 cursor groups, A/B, Start, and hardware Sound (not presumed a guest key). [Mednafen documentation](https://mednafen.github.io/documentation/wswan.html) and existing repository PIV-HH-01 establish X usually primary in horizontal games, Y usually primary in vertical games; opposite cluster may act as actions. Existing source audit identifies WS_LANDSCAPE and WS_PORTRAIT PadKit layouts. Propose one compact GBA-derived Digital 2 layout with both cursor groups always visible: primary nucleus at (100,148), secondary four separate cardinal caps directly above B at (203,72), B (203,177), A (148,235), H B (260,177), H A (148,290) on illustrative 300×355 canvas. Owner placement correction: secondary cluster sits above B, not above the primary directional nucleus. Keep the main D-pad, A/B and Hold positions unchanged. Secondary cluster is a separate four-input control, not a decorative or aliased D-pad. Menu/Start separate utility rail. Horizontal assigns actual X group to primary seat, Y to secondary; vertical swaps groups to primary Y/secondary X, with orientation-correct native directions verified against live layout and game viewport. Preserve native A/B identities and two fixed Holds in both modes; no per-game assignment needed, no automatic alias, no invented Select/Sound. Use detected game orientation when reliable, explicit orientation override otherwise. Release inputs before any orientation switch. Exact current source mapping, independent >=48dp cardinal touch targets, two-group simultaneous combinations, chassis clearance, mirroring and one-thumb reach require measured verification; return for design review if safe fit fails. Some games requiring simultaneous independent X/Y may require a second finger or gamepad. Owner approved revised layout and shared WSC inheritance on 26 Nov 2026; no runtime or physical PASS.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### WSC

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/WSC.md) — blob `4e0990f20f02fd5868a8caa38c18bb9860a7ef52`

#### WSC — controller design packet

**Batch:** B · **Hardware family:** Four/six-button and handheld · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two directional clusters and handheld orientation.

##### WSC-PA-01 — owner layout lock (26 Nov 2026)

Owner approved exact inheritance of [WS-PA-01B](WS.md): primary D-pad, separate four-direction secondary cluster directly above B, A/B and fixed Holds, Start/Menu rail; X primary in horizontal and Y primary in vertical, other cluster independently available. Preserve distinct WSC system identity, saved preferences and native direction/key semantics. Verify selected source/PadKit orientation routing, >=48dp touch targets, reach, safe clearance and physical fit independently. No runtime or physical PASS. GripArc and Joint deferred.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
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


---

### PSP

[Canonical design packet](https://github.com/safetyblade/ROMMachine/blob/design/gc-arc-33-console-packets-20261126/docs/engineering/gc01/design-packets/PSP.md) — blob `7808891fdac2a857546f7a5044b0d2fef832828d`

#### PSP — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: PSP-PA-03 OWNER LAYOUT LOCKED; CLEARANCE AND ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; revised layout physical pending (prior visual acceptance retained as historical evidence).  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Solo PivotArc appearance owner-accepted.

##### PSP-PA-02 — ergonomic review, owner gameplay feedback (26 Nov 2026)

Reviewed 58-second owner-supplied recording 1000097281.mp4. Visible working reference: analogue left, central D-pad, Square above, Triangle upper-right, Cross lower-right, Circle lower-centre, L between Triangle/Cross, R lower-left, separate Hold Cross to right and Hold Circle below, rear three-control utility rail. Sampled gameplay uses stick, Square and Triangle; touch indicators do not prove one-hand operation or comfortable reach. Owner reports full circle and rear utility placement acceptable in practice, concerns L/R reach, whether X/O are the best lower pair and whether Square/Triangle need Holds. Retain approved visual language, radial layout, analogue and D-pad, and rear utility rail.

Owner correction (26 Nov): retain L at its existing right-middle seat between Triangle and Cross; move R from lower-left to immediately right of L. Earlier proposed move of L toward R is superseded and must not be implemented. Retain existing full circle, analogue, D-pad, rear utility rail, and X/O lower positions for the first comparison (lower pair still subject to gameplay review).

PSP-PA-03 owner-approved design: universal no-per-game-setup Hold access for all four face buttons. Existing H Cross right and H Circle below retained; H Square to the right/outboard of Square and H Triangle to the right/outboard of Triangle. Each H fixed to its own real guest face key; no per-game remapping required, no turbo or macro, explicit latched state/release and independent input ownership. Four dedicated H approved, subject to measured safe fit. Reopened supplied recording frame (540x1172 illustration): L center approximately (381,727); candidate R (465,727); candidate H Square (380,555), H Triangle (465,628); retain H Cross (465,823), H Circle (284,979). These are screenshot-relative concept centres only, not validated dp or exact source geometry. Keep all >=48dp effective touch regions independent and inside measured safe area; check primary thumb sweep, outer edge/shoulder reach, mirroring, held-plus-momentary input, neutral and lifecycle release. If four dedicated H cannot safely fit, return for owner design review; do not shrink hit targets, silently remap keys, or enforce per-game configuration. Two fixed Holds Cross/Square is a possible fallback for review only. No change to accepted visual style. Owner approved PSP-PA-03 on 26 Nov 2026 with mandatory chassis clearance correction. Translate the entire gameplay control constellation downward as a rigid group (analogue, D-pad, face buttons, L/R and all four Holds); preserve relative geometry, visual materials and separate rear utility rail. Measure actual safe panel interior including rim, wordmark, collars, painted extents and independent >=48dp touch bounds, ensuring clearance from every edge and the lower utility rail/viewport. Do not shrink hit targets, shift individual buttons or move the utility rail to force fit; if rigid translation cannot satisfy clearance, return for owner review. Exact downward offset is intentionally not fixed until measured against the actual chassis. Screenshot-relative centres above are pre-clearance concept targets, not approved final absolute positions. The previously accepted visual styling remains frozen. Revised design is approved; actual source and physical fit unverified. No runtime changes.

##### PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

##### GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

##### Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

##### Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | OWNER LAYOUT LOCKED — mandatory measured downward clearance adjustment pending |
| Solo PivotArc visual | PRIOR VISUAL PASS; REVISED DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*

