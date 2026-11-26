# WII — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: OWNER LAYOUT LOCKED; ENHANCED PHYSICAL TESTING REQUIRED**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Separate remote/nunchuk/classic; do not invent pointer/motion support.

## WII-PA-01A–D — Wii specialist family review candidates (26 Nov 2026)

Owner direction: push Wii controller capabilities, not curate only easy conventional games. Four currently registered modes in engineering NativeGameSystems.kt: WII_SIDEWAYS (default), WII_NUNCHUK, WII_CLASSIC, WII_GAMECUBE (only compatible software); pointer modes dolphin_ir_mode touch=2, stick=1, relative=0. Source Wii.kt blob f0d4f1826b8938289bf7af6062c597e65f498474 provides layout identities, not proof of actual guest motion dispatch. Preserve saved mode choices and separate CARTDECK Menu from guest Home.

A — Sideways Remote: compact radial digital 4, D-pad primary, 1/2 lower face pair (1 at SNES Y seat, 2 at B seat), A/B upper face pair (A at X seat, B at A seat), fixed H1/H2 only; adjacent mid-arc momentary Shake, rail Menu/Minus/Plus/Home. Illustrative 350×350 centres D (110,157), A (170,62), B (235,104), 2 (235,214), 1 (170,261), Shake (235,159), H2 (302,214), H1 (170,320); separate rail below, no analog stick. Native Android A=2, B=1, X=A, Y=B, START=+, SELECT=-, THUMBR=Home, R2=Shake per current source. Preserve native sideways orientation and real guest direction identity.

B — Remote + Nunchuk: PSP-derived radial four actions A/B/C/Z, side-mounted main Nunchuk stick and independent D-pad, pointer control via existing touch/stick/relative choice. Keep pointer-stick seat separately if active mode needs it; no fake second guest analogue. Fixed Holds A/B/Z proposed, C momentary, separate momentary Shake Remote/Shake Nunchuk adjacent mid-arc, rail Menu/Minus/Plus and Home only if verified. Illustrative 448×400 centres left stick (63,124), conditional pointer stick (63,248), D (188,185), C (242,90), B (294,120), A (294,266), Z (256,308), H B (364,105), H A (364,292), H Z (256,367), Shake Remote (352,197), Shake N (408,197); rail below. Existing Android A=A, B=B, X=C, Y=Z; L1=-, R1=+, L2=Shake N, R2=Shake; existing generic SELECT/START helpers and both analog helpers must be audited for actual guest mappings, not removed based on label assumptions. Preserve IR coordinate mapping and viewport touch pass-through; verify simultaneous point/press/stick and shake ownership. No Home invented.

C — Classic: inherit PSX-PA-03 side stacks, D-pad, action arc X/Y/A/B and four fixed face Holds; exact native classic labels ZL/ZR on inherited upper shoulder row, L/R lower row per current source L1/R1=ZL/ZR, L2/R2=L/R. Menu/Minus/Plus rail; verify guest Home and actual utility mapping before exposing. No native stick-click claims; remove only non-native seats after audit, retain verified pointer behavior only where actual mode supports it. Verify proportional vs digital shoulder capability.

D — GameCube: exact GC-PA-02 owner-approved geometry, conditional half triggers, fixed A/B/L/R Holds, enhanced gameplay acceptance; select only in Wii software with real GameCube support and verify actual port/mode.

Capability expansion (not assumed available): upright Remote variant, real IR, phone sensor tilt/swing/shake, MotionPlus, extension-specific and simultaneous motions. Establish actual Dolphin bridge and guest action evidence before adding any control or claiming compatibility; if absent record engineering gap, preserve working modes. Never map MotionPlus to generic shake. No gesture macros or simulated gyro claims. Test representative titles New Super Mario Bros. Wii (sideways and shake), Mario Kart Wii (steering), Super Mario Galaxy (pointing + movement), Metroid Prime Trilogy (IR aiming), Wii Sports Resort and Skyward Sword (MotionPlus), Super Smash Bros. Brawl (Classic/GameCube where supported). Exact requirements verify against titles; distinguish performance/compatibility from input defects. Require >=48dp independent discrete targets, full stick travel, nonoverlap, actual pointer coordinate transform, motion neutral/release, orientation, handedness, saved-mode persistence, measured fit and owner physical evidence. All coordinates illustrative; if fit fails return for review, do not shrink hit targets or hide essential inputs. Owner approved WII-PA-01A–D 26 Nov 2026; capability expansion and enhanced physical tests pending; no build. GripArc and Joint deferred.

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
