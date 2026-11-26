# GC-ARC — approved PivotARC batch 02 and full programme execution gate

Owner approved the last two systems Wii U and DOS on 26 Nov 2026. All 33 system design directions are approved; source implementation, device acceptance and conditional variant capability remain distinct. Issue #377; engineering PR #376. Batch 02 adds 18 system IDs to the 15 in [Batch 01](GC-ARC-APPROVED-PIVOT-BATCH-01.md). This supersedes historical batch 01 exclusions of DS and later systems and its instruction to stop at 15. No merge authorized. GBA full-body GripArc physical proof PASS/spatial design FAIL; all further GripArc design and Joint deferred. Do not change accepted GBA/PSP art; implement only approved relative geometry including PSP measured rigid downward adjustment. 

Immutable source commit: `8b0cb46a9050f0f0f08c967eb7df408aae371772`; manifest blob `10209db01fb2feb0fb11ff1245ebc1facb068e93`. Latest owner-approved revision supersedes earlier rejected candidates within each packet. Conditional variants are not implementation or compatibility claims; PC Engine Avenue Pad 3 alias remains open. Source snapshots follow, with original relative links retained as historical references. 

## Full 33-system design register

| System | Design state | Scope |
|---|---|---|
| GB | owner_layout_locked | GB-PA-01 owner layout locked: exact GBA D/A/B and Hold A/B geometry, remove L/R, compact one-handed use; source and physical pending. |
| GBC | owner_layout_locked | GBC-PA-01 owner approved exact GB-PA-01 geometry, one-handed, individual Hold A/B; engineering and physical pending. |
| NES | owner_layout_locked | NES-PA-01 owner approved identical GB/GBC one-thumb layout, individual Hold A/B; source and physical pending. |
| SMS | owner_layout_locked | SMS-PA-01 owner approved Digital 2 geometry, 1/START lower-left, 2 upper-right, Holds, Menu and guest Pause; engineering and physical pending. |
| GG | owner_layout_locked | GG-PA-01 owner advanced with Next; Digital 2 geometry with native Start, 1/2 Holds and mapping preservation; source and physical pending. |
| NGP | owner_layout_locked | NGP-PA-01 owner approved Digital 2 Click, A/B Holds and Menu/Option; engineering and physical pending. |
| NGC | owner_layout_locked | Neo Geo Pocket Color, NOT GameCube. NGC-PA-01 owner approved exact NGP Digital 2 Click inheritance; separate backend and physical validation pending. |
| PC_ENGINE | standard_layout_locked_variants_open | Standard Digital 2 and revised D6-PA-01 six-button radial profiles owner approved; fixed Holds I/II; Avenue Pad 3 alias open; source, mode and physical pending. |
| GBA | solo_visual_accepted | Solo PivotArc owner-accepted; large GripArc full-body prototype in draft PR #376; pending new GBA button-position acceptance. |
| SNES | owner_layout_locked | SNES-PA-01 owner approved PSP-derived Digital 4 X/A/B/Y circle, four Holds and adjacent L/R; engineering and physical pending. |
| GENESIS | owner_layout_locked | Standard Digital 3 and D6-PA-01 six-button radial profiles owner approved; fixed Hold B; source, mode and physical acceptance pending. |
| SEGACD | owner_layout_locked | Standard Digital 3 and D6-PA-01 six-button radial profiles owner approved; fixed Hold B; source, mode and physical acceptance pending. |
| WS | owner_layout_locked | Owner approved WS-PA-01B dual directional layout (secondary above B), shared WS/WSC orientation mapping and fixed Holds A/B; source/physical pending. |
| WSC | owner_layout_locked | Owner approved WS-PA-01B dual directional layout (secondary above B), shared WS/WSC orientation mapping and fixed Holds A/B; source/physical pending. |
| NDS | owner_layout_locked | NDS-PA-01B owner approved tighter radial D/ABXY island, fixed B/Y Holds, adjacent L/R, protected stacked screens and stylus; engineering/physical pending. |
| NINTENDO_3DS | owner_layout_locked | 3DS-PA-01 owner approved Analogue 4 Compact: Circle Pad, independent D-pad, ABXY, adjacent L/R, fixed B/Y Holds; conditional New 3DS/motion, engineering/physical pending. |
| PSP | owner_layout_locked | PSP-PA-03 owner approved four fixed face Holds, R right of L, existing circle/rail; mandatory rigid downward translation for measured chassis clearance; revised physical/source pending. |
| N64 | owner_layout_locked | N64-PA-01 owner approved; enhanced five-title physical test gate required (analogue, C directions, Z and A/B Holds); source/physical pending. |
| PSX | owner_layout_locked | PSX-PA-02A Digital approved unchanged; PSX-PA-03 DualShock owner approved side-mounted L/R stack, central D-pad, mid-arc shoulders/four Holds; measured fit and native input pending. |
| PLAYSTATION_2 | owner_layout_locked | PS2-PA-01 owner accepted PSX-PA-03 Sony side-stack inheritance; native capabilities and physical fit pending. |
| PLAYSTATION_3 | owner_layout_locked | PS3-PA-01 owner accepted PSX-PA-03 Sony side-stack inheritance; native capabilities and physical fit pending. |
| DREAMCAST | owner_layout_locked | DC-PA-01 owner approved side analogue, central D-pad, radial X/Y/A/B, mid-arc triggers, fixed A/B/L/R Holds; measured fit and trigger semantics pending. |
| GAMECUBE | owner_layout_locked | GC-PA-02 owner approved compact A/trigger reach; enhanced four-title physical acceptance required, half/full trigger and C-stick verification pending. |
| XBOX | owner_layout_locked | XB-PA-02 owner approved Sony PSX-PA-03 geometry, native labels and four fixed face Holds; host/fit pending. |
| XBOX360 | owner_layout_locked | X360-PA-02 owner approved Sony PSX-PA-03 geometry, native labels and four fixed face Holds; host/fit pending. |
| FBNEO | owner_layout_locked | ARC-PA-01 approved numbered 4/6 radial profiles; conditional 2/3 only where mapping verified; fixed Holds by profile, Coin/Start; engineering/physical pending. |
| MAME2003PLUS | owner_layout_locked | ARC-PA-01 approved numbered 4/6 radial profiles; conditional 2/3 only where mapping verified; fixed Holds by profile, Coin/Start; engineering/physical pending. |
| ATARI2600 | owner_layout_locked | A26-PA-01 approved compact Digital 1, Fire/Hold Fire and native console switches; mapping and physical pending. |
| ATARI7800 | owner_layout_locked | A78-PA-01 approved GB-derived Digital 2, fixed H1/H2 and native switch semantics; mapping and physical pending. |
| WII | owner_layout_locked | WII-PA-01A–D approved; enhanced IR/motion/MotionPlus and physical testing mandatory, capability gaps explicit. |
| DOS | owner_layout_locked | DOS-PA-01A/B/C approved gamepad and conditional keyboard/mouse profiles; native bridge/mapping and physical gate pending. |
| LYNX | owner_layout_locked | LYNX-PA-01 owner approved GBA geometry with Option 1/2 replacing L/R, fixed A/B Holds and Menu/Pause; rotation and physical pending. |
| WII_U | owner_layout_locked | WU-PA-01A/B approved GamePad/Pro Sony-derived geometry; native touch/dual-screen/gyro capability and enhanced physical gate pending. |

## Implementation and verification authority

- Elliot: implement the complete approved solo PivotARC family on existing PR #376, group shared geometry and profile data by exact active controller mode, preserve working Standard/GripArc, native runtime paths, video, saves and preferences. No merge, no GripArc expansion, no Joint work. Do not enable unsupported inputs or variants to achieve a count.
- Daniel: audit current native control inventories and actual guest mappings by engine, including non-face Holds, shoulder/trigger pressure, independent axes, touch/motion and DOS keyboard/mouse. Source audit before coding: PivotArcLayoutProfile.activeFor partial coverage, PivotArcControlGroup fixed HoldA/B handling, PivotArcHeldInputGate fixed A/B routing; extend coherently per profile rather than adding generic labels or globally enabling. Confirm all actual render/dispatch/visibility eligibility callers. Distinguish source-mode availability from native host capabilities.
- Sienna: latest owner-approved geometry controls; no reinterpreting the Sony family, GameCube reach correction or N64/DS geometry. Reference coordinates are not verified dp. Require measured >=48dp independent targets, full stick envelopes, no overlap, safe viewport, utility rail and chassis clearance, whole-group mirror; return for focused review if exact fit cannot be achieved rather than hiding native controls.
- Marcus: first run formatting (ktlintCheck) and fix failures, then compile and unit tests for the exact head; validate geometry, actual guest identities and modes, held key ownership and lifecycle release including non-A/B, simultaneous input, mirroring, native host routing. No per-console APK cycle; one consolidated intentional acceptance APK only once source gates pass. Record exact commit, versionCode and artifact; do not infer acceptance from a successful build.
- Enhanced physical gates N64, GameCube, Wii, Wii U and Xbox family; test specific title/action pairs in packets and add Roxy's challenge collection once available. Wii upright/MotionPlus/IR and Wii U gyro/dual-screen plus DOS specialist bridges are capability investigations, not claims of working support. Do not make placeholder motion/pressure buttons appear functional.
- Complete all source-supported approved profiles and report explicit capability gaps in the same PR. Only owner accepts physical gameplay. No merge until expressly authorized. GripArc resumes only after PivotARC implementation/verification; Joint last rigid placement only.

## Batch 02 packet snapshots

### NDS — blob `814b6e1750ec397c1c3f98b8e3cc4825b9f12bf0`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/NDS.md)

#### NDS — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two screens, stylus and real mic/lid capabilities.

##### NDS-PA-01 — Digital 4 Compact review candidate (26 Nov 2026)

Owner constraint: two vertical screens leave less space than other systems. Existing approved stacked screen bounds, aspect, lower touchscreen/stylus coordinates and viewport separation take priority. Reserve the measured free lower band after system insets and existing utility rail; no overlay, transparent hit intercept or automatic shrinking of screens. Adapt SNES radial topology, not its full-sized housing. Proposed compact wide/short housing: D-pad (87,110), X (168,40), A (238,70), B (238,138), Y (168,174), adjacent vertical L/R right bank (297,40)/(297,92), H B (303,145), H Y (108,190) on an illustrative 360×280 reference canvas, rear Menu/Start/Select rail at y250. These are candidate positions only, not dp or validated handset fit; compact measured chassis and control group mirroring required. Two fixed Holds Y and B proposed as run/action and lower face defaults, no per-game remap; A/X remain directly accessible. Four Holds not ruled out if safe clearance exists; if two insufficient or targets cannot fit, return for owner review, don't silently hide native inputs or shrink touch targets below 48dp. Keep auxiliary microphone/lid/screen actions accessible separately in the existing supported menu where real backend functionality exists, not invented guest controls; core-specific DeSmuME/melonDS auxiliary codes require exact current source verification (historical PIV-HH-01). Screen swap if already available must preserve stylus coordinate mapping. Verify real two-screen rendered bounds, touch pass-through/tap/drag, guest keys, held input/release, screen rotation, L/R, one-hand reach, no overlap and clearance at 320/360/393dp and actual owner handset. If safe fit fails, retain existing Standard or supported alternative layout; no universal one-thumb guarantee for simultaneous stylus/digital input. Owner review pending; no build/physical acceptance. GripArc and Joint deferred.

##### NDS-PA-01B — tighter control island (26 Nov 2026)

Owner approved the compactness adjustment: bring the D-pad substantially nearer the face cluster; smaller visible caps acceptable, not smaller effective targets. This supersedes the PA-01 candidate coordinates only; all screen, stylus, native input, handedness and fallback constraints remain. Revised illustrative 360×280 canvas centres: D (112,109), X (166,32), A (226,68), B (226,128), Y (166,185), adjacent L (300,38)/R (300,98), H B (300,158), H Y (102,198); Menu/Start/Select rail y250. Illustrative D-pad envelope 92×92; individual face and Hold visual caps can shrink independently of hit regions. The D-to-face centre distances are reduced relative to PA-01. Keep the space between D and face buttons free of Holds. Two fixed Holds B/Y remain the proposed default; assess four only with measured safe clearance and owner review. These are reference coordinates, not dp, exact runtime geometry or proof of adequate directional hit areas. Geometry solver must ensure >=48dp discrete targets, full D-pad directional accessibility, non-overlap, and actual free lower-band fit; if impossible, return for redesign or retain existing Standard, never shrink screens or intercept stylus. Full NDS-PA-01B layout owner approved 26 Nov 2026 by progression to next console; measured source/physical fit pending, no APK. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — measured fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### NINTENDO_3DS — blob `b0ef41757e918a47d686392c710a7270446aa26b`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/NINTENDO_3DS.md)

#### NINTENDO_3DS — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two screens and circle pad; verify runtime.

##### 3DS-PA-01 — Analogue 4 Compact owner layout lock (26 Nov 2026)

Inherit NDS-PA-01B compact radial philosophy and preserve actual 3DS upper/lower screen aspect and lower stylus input region. Circle Pad is primary movement nucleus with independent D-pad immediately left. Illustrative 360×280 reference canvas centres: D (47,113), Analog (126,112), X (196,38), A (254,76), B (254,134), Y (196,184), L (319,38), R (319,98), H B (319,158), H Y (130,201), separate Menu/Start/Select rail y250. Concept D-pad envelope 64×64, analogue 80×80, smaller visible face caps acceptable; not measured dp or validated geometry. Maintain close Circle Pad/face reach and no Hold in main movement corridor. Two fixed Holds B/Y proposed; assess four only if safe fit and owner approval. Preserve independent real analogue and digital axes, native ABXY/LR and utility semantics. Historical PIV-HH-01 source audit and engineering PivotArcLayoutProfile.activeFor at 6a08c56c list D/Analog/ABXY/LR/Start/Select/Menu; this is source inventory not proven guest dispatch. New 3DS C-stick/ZL/ZR only as separately approved and backend/game-verified variant, never fake mappings. Real gyro/sensor and virtual tilt affordances only if verified backend bridge and measured room; no assumed support. Require independent >=48dp discrete hit regions, actual analogue travel, measured nonoverlap, lower touchscreen tap/drag, no screen shrink, rotation, mirror, lifecycle release and full native key/mode verification. If fit fails return for redesign or retain existing supported Standard; one-handed simultaneous analogue/stylus/face input not guaranteed. Owner approved 26 Nov 2026; no build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — measured fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### N64 — blob `11b4f48dd034e04eb96467132e4cb7d02f90d3c4`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/N64.md)

#### N64 — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENHANCED PHYSICAL TESTING REQUIRED**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Real analogue, Z and C-button identity.

##### N64-PA-01 — Analogue 2 + C-Pad owner layout lock (26 Nov 2026)

Primary analogue movement nucleus, independent digital D-pad upper-left, A/B lower radial pair with Z immediately above/right of movement as a frequent gameplay action (not a remote shoulder). Four independent native C directions in a compact directional cluster upper-right; preserve distinct guest C-Up/Down/Left/Right, no invented analogue C-stick, no diagonals forced. Adjacent L/R upper bank, separate Start/Menu utility rail, no Select. Proposed fixed Holds A/B/Z (acceleration, sustained action, targeting/crouch/aim/fire depending on title), outboard of respective buttons and clear of movement corridor. No Hold on C directions initially. Illustrative 360×350 design centres: D (62,87), stick (139,171), C cluster (266,92), L (119,33), R (184,33), Z (227,176), B (237,246), A (159,281), H Z (302,176), H B (312,246), H A (159,339); separate rail below body. Visual concepts only; actual chassis dimensions and hit targets pending. Gameplay examples Super Mario 64 A jump/B attack/Z crouch/C camera; Mario Kart 64 A accelerate/B brake/Z item/R drift; Ocarina of Time A action/B sword/Z targeting/C items; GoldenEye default control style Z fire/C movement (control style dependent). Existing engineering N64.kt at 6a08c56c-era branch uses Android B displayed A, Android Y displayed B, Android L2 displayed Z (duplicated across Standard layouts); C directions are DiscreteDirection(MOTION_SOURCE_RIGHT_STICK), allowDiagonals=false. Preserve those actual guest identities and one Z target in PivotArc; no label-derived remapping. Verify actual core binding and analogue axis, independent D-pad/C inputs, simultaneous action ownership, fixed Hold guest keys/release, 48dp+ discrete targets, direction sector reach, mirror, viewport, measured fit; one-hand simultaneous analogue and discrete input cannot be guaranteed. If fit fails return for design review rather than shrinking hit targets or removing native controls. Owner approved 26 Nov 2026 with enhanced physical testing required; no build. GripArc/Joint deferred.

##### Enhanced N64 acceptance — owner-directed

Use the same integrated APK for five proposed representative titles: Super Mario 64 (analogue range, A/B/Z combinations, camera); Mario Kart 64 (Hold A plus steering, R drift, Z item); Ocarina of Time (Z targeting, sword and individual C items); GoldenEye 007 (control-style-dependent movement/aim/fire combinations); Mischief Makers (D-pad precision and actions). Record actual one-handed reach, accidental presses, independent C directions, simultaneous/held input ownership and lifecycle release, analogue dead zone/range, both handedness options and viewport clearance. Explicitly flag game-specific two-hand requirements; no physical PASS until owner review of enhanced evidence. Title list is a proposed test sample, not completed tests.

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
| Design prepared | OWNER LAYOUT LOCKED — enhanced test gate pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### PSX — blob `fc2f153702fd025cd8534ffaab872bdca8ecc5c0`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/PSX.md)

#### PSX — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT DIRECTION LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Separate digital and DualShock profiles.

##### PSX-PA-01A/B — Digital 4 and DualShock review candidates (26 Nov 2026)

A — original digital controller: D-pad primary nucleus, PSP-derived Square top, Triangle upper-right, Cross lower-right, Circle lower-centre, four fixed per-face H outboard, two adjacent shoulder pairs L1/L2 and R1/R2 in upper bank, Menu/Start/Select separate utility rail. No analogue controls in digital mode. Illustrative 360×400 body centres: D (93,185), Square (180,108), Triangle (248,148), Cross (248,222), Circle (180,280), H Square (180,50), H Triangle (314,148), H Cross (314,222), H Circle (180,344); shoulder centres L1 (48,35), L2 (108,35), R1 (252,35), R2 (312,35). B — DualShock: inherit action and H arc and shoulder banks; left analogue at digital D nucleus (93,185), independent D-pad (58,104), right analogue (58,300), L3/R3 separate explicit controls if actual backend supports stick click; illustrative placement (48,368)/(112,368) subject to clearance and actual native identity. No automatic stick-click on moving or touching the analogue. Preserve independent left/right axes and D-pad, no silent substitution. Separate explicit digital and DualShock controller modes with core/device mode verification and preserved preferences; no claim of analogue support for digital-only games.

Exact source inventory observed in engineering PSX.kt and PSXDualShock.kt: Android A=Circle, B=Cross, Y=Square, X=Triangle; native L1/L2/R1/R2, Start/Select, D-pad; DualShock adds SecondaryAnalogLeft/Right. Verify actual guest mode, axis and L3/R3 dispatch independently; unsupported click must be disclosed, not simulated. All proposed coordinates illustrative, not dp or verified hitboxes. Require >=48dp independent targets, full analogue travel, native identity, Hold ownership/release, correct mode, handedness, viewport and physical fit. Keep original material and utility conventions. Digital-first for games that support it; dual-analogue simultaneous gameplay may not be achievable one-thumb. No removal of required controls or false support if fit or mode fails; return for redesign. Owner approval pending, no build. GripArc and Joint deferred.

##### PSX-PA-02A/B — revised mid-arc shoulder bank (26 Nov 2026)

Owner rejected top shoulder placement for PivotArc one-handed reach and directed PSP-like placement. Supersede PA-01 upper shoulder bank and concept coordinates, retain digital/DualShock variants, PSP face order and four dedicated fixed Holds. Place a compact 2×2 shoulder bank outboard of the radial action arc between Triangle and Cross: L1/R1 upper row, L2/R2 lower row, R immediately right of corresponding L. No shoulder in main movement-to-face corridor. Proposed shared 360×400 illustrative chassis: primary D or left analogue (100,185), Square (154,90), Triangle (206,120), Cross (206,266), Circle (168,308); H Square (154,30), H Triangle (276,105), H Cross (276,292), H Circle (168,367). Shoulder centres L1 (264,169), R1 (320,169), L2 (264,225), R2 (320,225). Digital variant has no extra sticks. DualShock adds independent D-pad (54,90), right analogue (54,292), separate L3/R3 (38,376)/(94,376) only if real guest click input supported. All centres illustrative; measure effective directional/analogue envelopes, 48dp+ discrete independent targets, housing clearance, shoulder-to-face reach, no overlap, held ownership, mirroring and actual mode/guest mapping. Reduce visible caps if useful, never reduce effective targets below minimum or omit native controls. If fitting requires changing topology, return for review. Four-Hold arrangement retained as candidate, exact fit pending. Owner shoulder correction recorded; full revised designs still awaiting owner approval; no implementation or build.

##### PSX-PA-03 — owner layout lock (26 Nov 2026)

Digital PSX-PA-02A approved unchanged. DualShock owner approved PSP-like side-mounted analogue stack instead of sticks below/main nucleus: keep D-pad in digital nucleus and face/Hold/shoulder/utility geometry unchanged relative to it; add a dedicated left-side wing with left analogue above right analogue and explicit L3/R3 next to their respective ends where supported. This supersedes all PA-01/02B analogue positions. Illustrative enlarged 448×400 body, translate PA-02A gameplay and utility geometry +88 horizontally as a group, D (188,185), left stick (63,124), right stick (63,248), L3 (63,42), R3 (63,330); visible stick radius 40, actual travel/targets measured later. Side wing is part of cohesive standalone housing; not a detached dock, no bottom row, no shoulder reposition. Primary analogue remains left stick in analogue games; D-pad independently accessible. Four fixed H unchanged. Owner approved direction by affirmative response to lock proposal; exact illustrative centres are design interpretation, not measured acceptance. Preserve mode and native input semantics, test enhanced dual-axis/stick clicks and reach; if independent 48dp targets, full stick travel or chassis fit fail, return for owner review; never crop or silently delete controls. No build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — exact fit pending |
| Solo PivotArc visual | DESIGN LOCKED — device visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### PLAYSTATION_2 — blob `b1fd2b8f3e255b2da1eab0bfa2744b7b3ba4ba78`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/PLAYSTATION_2.md)

#### PLAYSTATION_2 — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### PS2-PA-01 — shared Sony side-stack owner layout lock (26 Nov 2026)

Propose exact inheritance of owner-approved [PSX-PA-03](PSX.md) DualShock topology: central D-pad, left-side independent L/R analogue stack (L above R), explicit corresponding L3/R3 only through real native inputs; PSP-derived Square/Triangle/Cross/Circle radial face arc and four fixed face Holds, mid-arc 2×2 L1/R1 and L2/R2 shoulder bank, rear Menu/Start/Select rail. Same chassis design language, not a bottom stick row or upper shoulder bank. Source and actual engine differences are separate from visual inheritance. DualShock 2 pressure-sensitive native buttons (including shoulder/face where applicable); verify backend capabilities and expose only real pressure behavior, ordinary digital press/Hold is not variable pressure. Engineering NativeGameSystems.kt registers PLAY_MODERN with NATIVE_ANALOG; verify Play! and ARMSX2 host independently before enabling PivotArc. No pretend pressure support. Preserve real independent axes and click semantics, verify actual backend, measured >=48dp discrete targets, full analogue travel, shoulder/trigger reach, mirror, holds/release, no overlap, viewport and exact physical fit. Dual-analogue simultaneous actions may require two hands. Enhanced gameplay tests required; unsupported engine features explicitly limited rather than simulated. Owner accepted shared Sony inheritance 26 Nov 2026; no code/build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### PLAYSTATION_3 — blob `9476c8cba524d01480a54bb55e60bdcaf277689d`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/PLAYSTATION_3.md)

#### PLAYSTATION_3 — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### PS3-PA-01 — shared Sony side-stack owner layout lock (26 Nov 2026)

Propose exact inheritance of owner-approved [PSX-PA-03](PSX.md) DualShock topology: central D-pad, left-side independent L/R analogue stack (L above R), explicit corresponding L3/R3 only through real native inputs; PSP-derived Square/Triangle/Cross/Circle radial face arc and four fixed face Holds, mid-arc 2×2 L1/R1 and L2/R2 shoulder bank, rear Menu/Start/Select rail. Same chassis design language, not a bottom stick row or upper shoulder bank. Source and actual engine differences are separate from visual inheritance. DualShock 3 includes analogue L2/R2 and pressure-sensitive buttons plus PS and Sixaxis motion. Retain same bank, verify real proportional trigger axes, digital/pressure semantics and actual native PS/motion support separately; PS action in utility/menu only if real dispatch exists, never substitute CARTDECK Menu as guest PS. Engineering NativeGameSystems.kt identifies nativeBackend ps3 and empty systemCoreConfigs; do not assume Libretro input mappings or claim working native bridge. Preserve real independent axes and click semantics, verify actual backend, measured >=48dp discrete targets, full analogue travel, shoulder/trigger reach, mirror, holds/release, no overlap, viewport and exact physical fit. Dual-analogue simultaneous actions may require two hands. Enhanced gameplay tests required; unsupported engine features explicitly limited rather than simulated. Owner accepted shared Sony inheritance 26 Nov 2026; no code/build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### DREAMCAST — blob `8096816faff249cf5de37022c1f9363fe36140fd`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/DREAMCAST.md)

#### DREAMCAST — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### DC-PA-01 — Analogue 4 owner layout lock (26 Nov 2026)

PSP/Sony-derived compact action arc, one side-mounted left analogue (no right stick), independent central D-pad, Dreamcast X top/Y upper-right/A lower-right/B lower-centre (rotated native compass), adjacent L/R triggers at mid-arc between Y/A outboard; separate Menu/Start rail, no Select. Proposed fixed Holds A/B/L/R: prioritize racing acceleration/braking and common arcade action, leave X/Y momentary; no per-game remapping. Illustrative 420×360 canvas: stick (60,160), D (150,160), X (205,70), Y (260,110), A (260,230), B (205,275), L (320,150), R (375,150), H L (320,205), H R (375,205), H A (330,260), H B (205,335); separate utility rail below body. All centres illustrative, not measured dp/fit. Source Dreamcast.kt blob 1ddf5ea2961b69b2b184f421b7794c9c43fd9c7c confirms Android B=guest A, Android A=guest B, Android Y=guest X, Android X=guest Y, L2/R2 keys labelled L/R, one analogue and discrete D-pad. Native Dreamcast triggers are proportional; existing layout dispatches key IDs, so verify real Flycast trigger axes before claiming variable pressure. Fixed Hold L/R may only latch digital/full press supported by actual backend, never simulate pressure. Preserve independent axes and inputs, explicit held/release state, mirror, viewport, full travel and measured >=48dp discrete targets; if no safe fit return for owner review. Suggested gameplay tests Crazy Taxi, Daytona USA 2001, Soulcalibur, Sonic Adventure; actual tests pending. Owner approved 26 Nov 2026; no implementation/build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### GAMECUBE — blob `8545923b4fd2dbaa7371b72228586e71c2ba52f1`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/GAMECUBE.md)

#### GAMECUBE — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENHANCED PHYSICAL TESTING REQUIRED**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** C-stick / Z / analogue triggers need real native input audit.

##### GC-PA-01 — Analogue 4 Asymmetric review candidate (26 Nov 2026)

Inherit approved Sony side-mounted dual-stick architecture, main analogue above smaller C-stick, independently accessible central D-pad. GameCube-specific action constellation: larger A at nucleus, B lower-left, Y upper-left, X upper-right; retain native guest identity. L/R adjacent mid-arc, Z immediately below trigger bank, never top shoulder row. Propose four fixed Holds A/B/L/R, favor sustained A and trigger functions; X/Y/Z remain momentary. L/R full-press Holds only, not invented variable pressure. Provide distinct L-half/R-half at reachable mid-arc only if real backend mappings verified; no fake L3/R3 labels because original GameCube sticks do not click. Illustrative 448×410 body centres: main analogue (62,116), C-stick (62,253), D (157,170), A (254,200), B (207,268), Y (241,105), X (303,118), L (347,190), R (402,190), L-half (347,247), R-half (402,247), Z (375,304), H A (270,267), H B (207,333), H L (347,365), H R (402,365), separate Menu/Start utility rail below. Half-trigger positions conditional; if real half presses absent, disclose and keep unused positions inactive rather than misrepresenting input. All centres illustrative, not measured dp or verified fit. Exact engineering GameCube.kt blob b2b90aeeb36082b6d6f35148139c937b1d618ab6 documents Android A/B/X/Y guest same-label keys, L1/R1 full shoulders, R2 Z, left/right axes for main/C-stick, and stick-click host mappings for analog half-triggers (verify actual Dolphin core/native host mapping independently). No automatic half trigger on stick movement; explicit intended action only. Require independent real axes, full/half trigger semantics, no phantom L3/R3, independent 48dp+ discrete targets, directional/analogue travel, held ownership/release, mirror, viewport and measured fit. Proposed enhanced gameplay tests Mario Kart: Double Dash!!, Super Smash Bros. Melee, Super Mario Sunshine, Luigi's Mansion; check trigger depth in titles requiring it and actual C-stick control. If safe fit or input capability fails, return for redesign or retain supported Standard, never omit essential inputs silently. Owner approval pending; no implementation/build. GripArc and Joint deferred.

##### GC-PA-02 — compact action/trigger island (26 Nov 2026)

Owner correction: A button size is not the issue; thumb reach between movement, A and back buttons is. Supersede PA-01 oversized A emphasis and remote right-side trigger bank. Use a normal/slightly prominent A, move A toward central D-pad and main stick; bring L/R/Z into the immediate action cluster, in a mid-arc row directly above A/X rather than at the top edge or beyond X. Keep side-mounted main/C sticks. Proposed 400×360 reference canvas centres: main stick (55,140), C-stick (55,263), D (137,175), A (215,177), B (215,245), Y (157,105), X (280,177), L (215,105), R (280,105), Z (344,105), H L (215,48), H R (280,48), H A (280,245), H B (215,309), conditional L-half (344,177), R-half (344,245); separate Menu/Start rail below. A visible cap radius 21, no oversized button requirement. L/R/Z occupy the same reachable cluster as A and X; half-press controls outboard of primary action path only when real backend capability exists. Proposed fixed A/B/L/R Holds retained, full press only. Prior illustrative A-to-R centre distance ~148 reference units; new ~97; A-to-L ~72 and A-to-D ~78 (concept, not physical measurements; no inference of actual thumb comfort). All native input/trigger capability, 48dp independent targets, measured nonoverlap, full stick travel, release, mirroring and enhanced game testing requirements from PA-01 retained. Specifically test repeated stick-to-A-to-L/R/Z transitions, A plus R/L/Z using separate fingers where required, and sustained full trigger Hold with movement; compare against PA-01 if available. If width cannot support safe targets, reflow with owner approval, do not compress hitboxes or relegate native inputs to inaccessible areas. Owner accepted GC-PA-02 on 26 Nov 2026 and directed considerable additional physical testing; no implementation/build. GripArc/Joint deferred.

##### Enhanced GameCube acceptance — owner-directed

Required additional representative gameplay: Super Mario Sunshine (main stick/A and real partial/full R trigger transitions), Mario Kart: Double Dash!! (sustained A plus steering/drifting), Super Smash Bros. Melee (A/B/X/Y and C-stick response), Luigi's Mansion (dual analogue and trigger control). Record repeated D/stick-to-A-to-L/R/Z reach, accidental presses, both handedness modes, simultaneous action where feasible and actual independent half/full trigger dispatch and release. Compare one-hand operation against Standard; flag two-hand requirements. No physical PASS until owner review. Proposed test titles, not executed results.

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
| Design prepared | OWNER LAYOUT LOCKED — enhanced test gate pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### XBOX — blob `b40818f40add3a47004981b1a51afc9d3e2d93ce`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/XBOX.md)

#### XBOX — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Real independent native host input bridge required.

##### XB-PA-01 — shared Xbox dual-analogue compact review candidate (26 Nov 2026)

Retain Sony side-mounted independent sticks (left above right, LS/RS click seats only where actual native dispatch verified), central D-pad and compact action arc with X upper-left, Y upper-right, B right, A lower-left. Adopt GameCube reach correction: LT/RT adjacent immediately above the principal action cluster, not top edge; White/Black pair directly right of Y/B in the same action island. Four fixed Holds A/B/LT/RT proposed; X/Y and secondary buttons momentary. Full trigger Hold only where real guest dispatch exists, do not simulate pressure. Separate Menu/Start/Back rail; no invented Guide key; preserve native White/Black and original pressure-sensitive controls where actually supported. Shared illustrative 440×370 chassis: left stick (55,112), right stick (55,250), LS (55,40), RS (55,322), D (142,181), X (207,114), Y (267,114), B (267,237), A (207,237), LT (207,55), RT (267,55), White (329,114), Black (329,174), H LT (329,55), H RT (389,55), H A (207,300), H B (267,300), separate utility rail below. Exact fit, full analogue travel, thumb reach, independent >=48dp effective targets and no overlap to be measured. Keep trigger and action reach close; do not stretch chassis to fit blindly. Source engineering Xbox.kt blob 94cff86bcbbbf7e877b0a6c966fb4b1d036a0ba6 uses same-label ABXY, L1/R1 labelled White/Black per bumpers flag, L2/R2 labelled LT/RT, SELECT labelled Back, left/right analog helpers; current key dispatch is not evidence of proportional axes or stick-click guest support. TouchControllerID selects bumpers false for Xbox and true for Xbox360. Verify independent native host mapping, axes, trigger ranges, held input ownership/release, no regression of native host touch routing, mirrored operation and viewport. Enhanced game acceptance (racing, dual-analogue action and D-pad menus) separately on each host; existing Xbox/360 performance issues must not be confused with input defects. Owner review pending; no implementation/build. GripArc/Joint deferred.

##### XB-PA-02 — Sony geometry inheritance owner layout lock (26 Nov 2026)

Owner rejected PA-01 distinct action/trigger arrangement and directed closer inheritance of PlayStation. Supersede PA-01 geometry and proposed Hold assignments with exact PSX-PA-03 illustrative DualShock topology (PSX.md): same integrated side wing, independent L/R sticks and central D-pad, PSP-derived radial action arc, 2×2 mid-arc shoulder bank, utility rail. Replace Sony legends and guest identities only: Square seat = X, Triangle = Y, Cross = A, Circle = B; L1/R1 seats = White/Black; L2/R2 seats = LT/RT; L3/R3 seats = LS/RS; Select = Back. Proposed four fixed face Holds X/Y/A/B at respective inherited outboard seats (instead of PA-01 trigger Holds; this is a design proposal for owner approval, not owner-directed Hold change). No additional trigger Holds without review. Illustrative 448×400 reference body centres: L stick (63,124), R stick (63,248), LS (63,42), RS (63,330), D (188,185), X (242,90), Y (294,120), A (294,266), B (256,308), H X (242,30), H Y (364,105), H A (364,292), H B (256,367), White (352,169), Black (408,169), LT (352,225), RT (408,225). Utility Menu/Start/Back rail below. Original Xbox and Xbox 360 share geometry, not guest mapping or backend assumptions. Retain PA-01 source identity and native host verification, including real proportional trigger/pressure capabilities where supported, click dispatch, release ownership, measured >=48dp independent touch targets, full axis travel, safe viewport and mirrored reach; no simulated pressure. Enhanced physical acceptance on each host separately. Owner approved revised layout and four fixed face Holds 26 Nov 2026; no runtime changes/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### XBOX360 — blob `7ca2d77bcbf4989adff7a78275ab28473324d87c`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/XBOX360.md)

#### XBOX360 — controller design packet

**Batch:** D · **Hardware family:** Console native / analogue · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Xenia native host controls; not original Xbox.

##### X360-PA-01 — shared Xbox dual-analogue compact review candidate (26 Nov 2026)

Retain Sony side-mounted independent sticks (left above right, LS/RS click seats only where actual native dispatch verified), central D-pad and compact action arc with X upper-left, Y upper-right, B right, A lower-left. Adopt GameCube reach correction: LT/RT adjacent immediately above the principal action cluster, not top edge; LB/RB pair directly right of Y/B in the same action island. Four fixed Holds A/B/LT/RT proposed; X/Y and secondary buttons momentary. Full trigger Hold only where real guest dispatch exists, do not simulate pressure. Separate Menu/Start/Back rail; guest Guide only through real separately supported dispatch (menu access), never conflate with CARTDECK Menu. Shared illustrative 440×370 chassis: left stick (55,112), right stick (55,250), LS (55,40), RS (55,322), D (142,181), X (207,114), Y (267,114), B (267,237), A (207,237), LT (207,55), RT (267,55), LB (329,114), RB (329,174), H LT (329,55), H RT (389,55), H A (207,300), H B (267,300), separate utility rail below. Exact fit, full analogue travel, thumb reach, independent >=48dp effective targets and no overlap to be measured. Keep trigger and action reach close; do not stretch chassis to fit blindly. Source engineering Xbox.kt blob 94cff86bcbbbf7e877b0a6c966fb4b1d036a0ba6 uses same-label ABXY, L1/R1 labelled LB/RB per bumpers flag, L2/R2 labelled LT/RT, SELECT labelled Back, left/right analog helpers; current key dispatch is not evidence of proportional axes or stick-click guest support. TouchControllerID selects bumpers false for Xbox and true for Xbox360. Verify independent native host mapping, axes, trigger ranges, held input ownership/release, no regression of native host touch routing, mirrored operation and viewport. Enhanced game acceptance (racing, dual-analogue action and D-pad menus) separately on each host; existing Xbox/360 performance issues must not be confused with input defects. Owner review pending; no implementation/build. GripArc/Joint deferred.

##### X360-PA-02 — Sony geometry inheritance owner layout lock (26 Nov 2026)

Owner rejected PA-01 distinct action/trigger arrangement and directed closer inheritance of PlayStation. Supersede PA-01 geometry and proposed Hold assignments with exact PSX-PA-03 illustrative DualShock topology (PSX.md): same integrated side wing, independent L/R sticks and central D-pad, PSP-derived radial action arc, 2×2 mid-arc shoulder bank, utility rail. Replace Sony legends and guest identities only: Square seat = X, Triangle = Y, Cross = A, Circle = B; L1/R1 seats = LB/RB; L2/R2 seats = LT/RT; L3/R3 seats = LS/RS; Select = Back. Proposed four fixed face Holds X/Y/A/B at respective inherited outboard seats (instead of PA-01 trigger Holds; this is a design proposal for owner approval, not owner-directed Hold change). No additional trigger Holds without review. Illustrative 448×400 reference body centres: L stick (63,124), R stick (63,248), LS (63,42), RS (63,330), D (188,185), X (242,90), Y (294,120), A (294,266), B (256,308), H X (242,30), H Y (364,105), H A (364,292), H B (256,367), LB (352,169), RB (408,169), LT (352,225), RT (408,225). Utility Menu/Start/Back rail below. Original Xbox and Xbox 360 share geometry, not guest mapping or backend assumptions. Retain PA-01 source identity and native host verification, including real proportional trigger/pressure capabilities where supported, click dispatch, release ownership, measured >=48dp independent touch targets, full axis travel, safe viewport and mirrored reach; no simulated pressure. Enhanced physical acceptance on each host separately. Owner approved revised layout and four fixed face Holds 26 Nov 2026; no runtime changes/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### FBNEO — blob `347d0158e81abf7cd20dc74650806d0dc8e1360d`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/FBNEO.md)

#### FBNEO — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** 4/6 button arcade control modes and coin/start.

##### ARC-PA-01 — shared arcade digital profiles owner layout lock (26 Nov 2026)

For FBNeo, reuse approved GB Digital 2, MD-PA-01 Digital 3, SNES-PA-01 Digital 4 (remove shoulder seats without reflow), and D6-PA-01 Digital 6 radial geometry. Do not introduce conventional two-row fighting-game buttons into the one-thumb path. User-facing numbers 1–6 are physical legends, not assumed guest identity: proposed 2-button B/A seats = 2/1; 3-button A/B/C = 1/2/3; 4-button X/A/B/Y = 3/4/2/1; 6-button X/Y/Z/A/B/C = 4/5/6/1/2/3. Fixed Holds 1/2 for 2-button, 2 for 3-button, all four for 4-button, 2 for 6-button. These numbering and Hold choices are proposals, not verified core mappings; verify each actual game/core control port and explicit active mode before labelling or dispatch; game-specific controls must remain accessible. Dedicated Menu/Coin/Start utility rail, no invented guest credits, test coin/start identity and held release. Source GameSystem.kt exact engineering branch registers only FB_NEO_4/FB_NEO_6 and MAME_2003_4/MAME_2003_6; initial implementation 4/6 only. 2/3 are conditional compact profiles only if active game and backend mapping are verified and unused controls are genuinely unnecessary; no claim of current support. No automatic inference from ROM genre. Specialized trackball, spinner, light gun, twin-stick, >6-button and nonstandard games remain separate capability review/fallback, not silently converted. Native digital direction sectors and diagonals, 48dp independent effective targets, mirrored control geometry, measured viewport clearance, real guest input identity and release ownership required. Proposed tests: Metal Slug (four-button mapping/hold), Street Fighter II (six distinct actions and combinations), a simple shooter and beat-em-up for optional compact profiles only if supported. Owner approved ARC-PA-01 on 26 Nov 2026; no runtime/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — conditional 2/3 modes and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### MAME2003PLUS — blob `a9537256cd7413b31e856a75647197ed0b5aaa1d`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/MAME2003PLUS.md)

#### MAME2003PLUS — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** 4/6 button arcade control modes and coin/start.

##### ARC-PA-01 — shared arcade digital profiles owner layout lock (26 Nov 2026)

For MAME 2003-Plus, reuse approved GB Digital 2, MD-PA-01 Digital 3, SNES-PA-01 Digital 4 (remove shoulder seats without reflow), and D6-PA-01 Digital 6 radial geometry. Do not introduce conventional two-row fighting-game buttons into the one-thumb path. User-facing numbers 1–6 are physical legends, not assumed guest identity: proposed 2-button B/A seats = 2/1; 3-button A/B/C = 1/2/3; 4-button X/A/B/Y = 3/4/2/1; 6-button X/Y/Z/A/B/C = 4/5/6/1/2/3. Fixed Holds 1/2 for 2-button, 2 for 3-button, all four for 4-button, 2 for 6-button. These numbering and Hold choices are proposals, not verified core mappings; verify each actual game/core control port and explicit active mode before labelling or dispatch; game-specific controls must remain accessible. Dedicated Menu/Coin/Start utility rail, no invented guest credits, test coin/start identity and held release. Source GameSystem.kt exact engineering branch registers only FB_NEO_4/FB_NEO_6 and MAME_2003_4/MAME_2003_6; initial implementation 4/6 only. 2/3 are conditional compact profiles only if active game and backend mapping are verified and unused controls are genuinely unnecessary; no claim of current support. No automatic inference from ROM genre. Specialized trackball, spinner, light gun, twin-stick, >6-button and nonstandard games remain separate capability review/fallback, not silently converted. Native digital direction sectors and diagonals, 48dp independent effective targets, mirrored control geometry, measured viewport clearance, real guest input identity and release ownership required. Proposed tests: Metal Slug (four-button mapping/hold), Street Fighter II (six distinct actions and combinations), a simple shooter and beat-em-up for optional compact profiles only if supported. Owner approved ARC-PA-01 on 26 Nov 2026; no runtime/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — conditional 2/3 modes and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### ATARI2600 — blob `83de955abc36a1563f4cec7941a91471ff9922b1`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/ATARI2600.md)

#### ATARI2600 — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Single fire button and console switches.

##### A26-PA-01 — Atari digital owner layout lock (26 Nov 2026)

One digital D-pad, one Fire and fixed Hold Fire. Inherit GB accepted D-pad/B/Hold B geometry without other face or shoulder seats; tighten chassis silhouette around retained seats without spreading controls. Use native Fire label, existing Android B guest mapping from Atari2600.kt blob cd5f063c735c1a5898bcf28085371f4f3a8d2af3. Dedicated Menu/Reset/Select utility rail, DIFF.A/B console switches accessible separately in gameplay menu (existing L1/L2 host identities), only real core dispatch. No fake analogue/paddle substitution; paddle/driving/keypad and colour/BW switch capabilities separately verified or explicitly limited. Preserve existing guest input identities, one-thumb proximity, no top shoulder buttons, mirrored layout, >=48dp independent touch targets, lifecycle release and measured screen clearance. Representative games Adventure, River Raid, Pitfall!, Space Invaders subject to actual compatible ROM and control requirements. Owner approved 26 Nov 2026; no runtime/build; GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### ATARI7800 — blob `7df9c7300eae57b4f65d83c8089a6d65677bcc8a`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/ATARI7800.md)

#### ATARI7800 — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two fire buttons and console switches.

##### A78-PA-01 — Atari digital owner layout lock (26 Nov 2026)

Use exact approved GB Digital 2 geometry with native 1 lower-left and 2 upper-right, fixed H1/H2 adjacent/outboard. Atari7800.kt blob a83e870cac8ff14c744e9c53c8079034cf3c402f maps Android B=1 and Android A=2; current Start/Select controls retained on separate Menu/Start/Select utility rail with guest switch semantics verified (including Reset/Pause where actually supported); no invented difficulty controls. 2600 compatibility/paddle modes not assumed. Preserve existing guest input identities, one-thumb proximity, no top shoulder buttons, mirrored layout, >=48dp independent touch targets, lifecycle release and measured screen clearance. Representative games Food Fight, Robotron: 2084, Ninja Golf, Asteroids subject to actual compatible ROM and control requirements. Owner approved 26 Nov 2026; no runtime/build; GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### WII — blob `170eef000134b709eab12999cf2f72b3058a2ab9`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/WII.md)

#### WII — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENHANCED PHYSICAL TESTING REQUIRED**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Separate remote/nunchuk/classic; do not invent pointer/motion support.

##### WII-PA-01A–D — Wii specialist family review candidates (26 Nov 2026)

Owner direction: push Wii controller capabilities, not curate only easy conventional games. Four currently registered modes in engineering NativeGameSystems.kt: WII_SIDEWAYS (default), WII_NUNCHUK, WII_CLASSIC, WII_GAMECUBE (only compatible software); pointer modes dolphin_ir_mode touch=2, stick=1, relative=0. Source Wii.kt blob f0d4f1826b8938289bf7af6062c597e65f498474 provides layout identities, not proof of actual guest motion dispatch. Preserve saved mode choices and separate CARTDECK Menu from guest Home.

A — Sideways Remote: compact radial digital 4, D-pad primary, 1/2 lower face pair (1 at SNES Y seat, 2 at B seat), A/B upper face pair (A at X seat, B at A seat), fixed H1/H2 only; adjacent mid-arc momentary Shake, rail Menu/Minus/Plus/Home. Illustrative 350×350 centres D (110,157), A (170,62), B (235,104), 2 (235,214), 1 (170,261), Shake (235,159), H2 (302,214), H1 (170,320); separate rail below, no analog stick. Native Android A=2, B=1, X=A, Y=B, START=+, SELECT=-, THUMBR=Home, R2=Shake per current source. Preserve native sideways orientation and real guest direction identity.

B — Remote + Nunchuk: PSP-derived radial four actions A/B/C/Z, side-mounted main Nunchuk stick and independent D-pad, pointer control via existing touch/stick/relative choice. Keep pointer-stick seat separately if active mode needs it; no fake second guest analogue. Fixed Holds A/B/Z proposed, C momentary, separate momentary Shake Remote/Shake Nunchuk adjacent mid-arc, rail Menu/Minus/Plus and Home only if verified. Illustrative 448×400 centres left stick (63,124), conditional pointer stick (63,248), D (188,185), C (242,90), B (294,120), A (294,266), Z (256,308), H B (364,105), H A (364,292), H Z (256,367), Shake Remote (352,197), Shake N (408,197); rail below. Existing Android A=A, B=B, X=C, Y=Z; L1=-, R1=+, L2=Shake N, R2=Shake; existing generic SELECT/START helpers and both analog helpers must be audited for actual guest mappings, not removed based on label assumptions. Preserve IR coordinate mapping and viewport touch pass-through; verify simultaneous point/press/stick and shake ownership. No Home invented.

C — Classic: inherit PSX-PA-03 side stacks, D-pad, action arc X/Y/A/B and four fixed face Holds; exact native classic labels ZL/ZR on inherited upper shoulder row, L/R lower row per current source L1/R1=ZL/ZR, L2/R2=L/R. Menu/Minus/Plus rail; verify guest Home and actual utility mapping before exposing. No native stick-click claims; remove only non-native seats after audit, retain verified pointer behavior only where actual mode supports it. Verify proportional vs digital shoulder capability.

D — GameCube: exact GC-PA-02 owner-approved geometry, conditional half triggers, fixed A/B/L/R Holds, enhanced gameplay acceptance; select only in Wii software with real GameCube support and verify actual port/mode.

Capability expansion (not assumed available): upright Remote variant, real IR, phone sensor tilt/swing/shake, MotionPlus, extension-specific and simultaneous motions. Establish actual Dolphin bridge and guest action evidence before adding any control or claiming compatibility; if absent record engineering gap, preserve working modes. Never map MotionPlus to generic shake. No gesture macros or simulated gyro claims. Test representative titles New Super Mario Bros. Wii (sideways and shake), Mario Kart Wii (steering), Super Mario Galaxy (pointing + movement), Metroid Prime Trilogy (IR aiming), Wii Sports Resort and Skyward Sword (MotionPlus), Super Smash Bros. Brawl (Classic/GameCube where supported). Exact requirements verify against titles; distinguish performance/compatibility from input defects. Require >=48dp independent discrete targets, full stick travel, nonoverlap, actual pointer coordinate transform, motion neutral/release, orientation, handedness, saved-mode persistence, measured fit and owner physical evidence. All coordinates illustrative; if fit fails return for review, do not shrink hit targets or hide essential inputs. Owner approved WII-PA-01A–D 26 Nov 2026; capability expansion and enhanced physical tests pending; no build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — capability and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### DOS — blob `76bf8e74b4d41ed1eb665ba4fa9aedb851a02923`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/DOS.md)

#### DOS — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Keyboard/mouse and DOS gamepad profile options.

##### DOS-PA-01A/B/C — PC input owner layout lock (26 Nov 2026)

A — Gamepad: inherit approved Sony side-mounted dual analogue chassis, D-pad, radial X/Y/A/B, 2×2 mid-arc L1/R1/L2/R2 and fixed X/Y/A/B Holds; Menu/Start/Select and explicit keyboard icon on utility rail. DOS has no universal guest gamepad mapping: preserve DOSBox Pure DOS_AUTO per-game actual mapping; do not claim all DOS games use these controls. Existing engineering DOS.kt blob 01176c39c3e994af6ffdea087be2a638a882033f uses ABXY, both analogue helpers, D-pad, L1/L2/R1/R2, Start/Select, THUMBL keyboard icon and CARTDECK Menu. Keyboard seat is not guest L3; no invented R3. B — Keyboard action profile: compact D-pad + four radial action seats with explicit saved game-specific key legends, fixed Holds for mapped actions only; accessible full keyboard, modifiers, Esc/Enter and other required keys, no silent removal of controls; actual key dispatch and key-up/lifecycle verification required before enabling. C — Mouse profile: accessible left/right click with optional fixed left/right Holds if real button-down/up supported, separate keyboard/menu access, touchscreen relative mouse or actual absolute mapping where supported, no fake joystick as mouse. Preserve screen viewport, pointer drag/release and independent mouse/keyboard concurrency. No game-specific macros or automatic mappings guessed from genre. B/C are proposed capability designs, not source-confirmed existing modes; verify DOSBox Pure guest keyboard/mouse bridge and native gamepad mapping before implementing. Keep conventional Standard available where unsupported. Representative tests Commander Keen, Jazz Jackrabbit, DOOM, SimCity 2000, The Secret of Monkey Island, with actual input requirements verified. Windows/Wine independent PC runtime remains separate, no claims of EXE compatibility from DOS profile. All geometry illustrative, 48dp independent targets, full analogue travel, mirror, saved per-game mapping, input release and viewport clearance measured. Owner approved 26 Nov 2026; native capability and physical gates remain open; no implementation/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — capability and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### LYNX — blob `35f3f3ad88bb31d3114d40341861af879c6f9b9a`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/LYNX.md)

#### LYNX — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Audit the active native controller input inventory and physical console-specific legends.

##### LYNX-PA-01 — Digital 2 + Options owner layout lock (26 Nov 2026)

Inherit approved GBA manufactured geometry and compact D-pad/A/B/Hold A/Hold B seats, substitute native Option 1/Option 2 for GBA L/R at existing shoulder seats (do not introduce top shoulder row or new remote bank); independent momentary options and separate Menu/Pause utility rail, no Select. Existing Lynx.kt engineering blob 8cd7e34107a98b23cf2f13ca7545db5e920899ab maps same-label Android A/B, Option 1=L1, Option 2=R1 and Start helper for Pause; verify actual Handy core guest identity, including option combinations and Pause, and no accidental reset. Preserve genuine vertical/rotated Lynx titles' display and directional mapping; mirror the full group without changing guest input identities, distinguish handedness from game orientation. No pretend rotation support; if unavailable disclose. Fixed A/B Holds proposed. Retain 48dp+ discrete targets, lifecycle release, existing video, measured fit and one-handed reach. Suggested tests California Games, Chip's Challenge, Blue Lightning, Gauntlet: The Third Encounter (orientation and input requirements to verify). Owner approved 26 Nov 2026; no implementation/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*


### WII_U — blob `c1a006cf2c91e0b60f9d8d6c282944b0d242f366`

[Canonical packet at design commit](https://github.com/safetyblade/ROMMachine/blob/8b0cb46a9050f0f0f08c967eb7df408aae371772/docs/engineering/gc01/design-packets/WII_U.md)

#### WII_U — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Native host and GamePad control proof needed.

##### WU-PA-01A/B — GamePad and Pro Controller owner layout lock (26 Nov 2026)

Inherit exact approved PSX-PA-03 448×400 reference chassis and side-mounted sticks, central D-pad, radial face seats Square=X, Triangle=A, Cross=B, Circle=Y (Nintendo compass rotated as approved SNES), fixed independent Holds X/A/B/Y, mid-arc 2×2 L/R upper and ZL/ZR lower, L/R stick-click seats only with verified guest dispatch. Rail Menu/Minus/Plus, native Home only if separately verified. A GamePad mode must protect independent GamePad interactive screen and TV viewport, support true guest touchscreen coordinate mapping and release, dual-screen switching/layout where available, and genuine gyro only via proven backend. Never treat TV touch as GamePad input without explicit mapped mode. B Pro Controller uses same geometry without GamePad touch or motion claims; actual guest device selection required. No fabricated Wii Remote support in Wii U mode. Engineering NativeGameSystems.kt registers Wii U as nativeBackend=cemu, hasTouchScreen=true and empty systemCoreConfigs; WiiuEngine.kt blob 98736e8db022f34d34471e83779fe41c4d0273f9 requires engineering build. These declarations do not prove working native input bridge, dual-display or gyro. Verify Cemu host actual capabilities before enabling each profile; absent support is explicit engineering gap, not a visual pass. Challenge games proposed Nintendo Land, Splatoon, ZombiU, Super Mario Maker, The Wind Waker HD; verify game-specific device requirements, screen dependencies, touch, gyro and simultaneous inputs. Require >=48dp independent discrete targets, full analogue travel, mirror, safe viewport, held ownership/release, native mode, and enhanced handset acceptance. Geometry inherited, not measured. Owner approved 26 Nov 2026; native capability and physical gates remain open; no implementation/build. GripArc/Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — capability and fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
