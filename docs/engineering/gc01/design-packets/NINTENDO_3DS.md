# NINTENDO_3DS — controller design packet

**Batch:** C · **Hardware family:** Dual-screen / single-analogue · **Status: PIVOTARC OWNER LAYOUT LOCKED; ENGINEERING EVIDENCE PENDING**.

**PivotArc:** owner_layout_locked; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Two screens and circle pad; verify runtime.

## 3DS-PA-01 — Analogue 4 Compact owner layout lock (26 Nov 2026)

Inherit NDS-PA-01B compact radial philosophy and preserve actual 3DS upper/lower screen aspect and lower stylus input region. Circle Pad is primary movement nucleus with independent D-pad immediately left. Illustrative 360×280 reference canvas centres: D (47,113), Analog (126,112), X (196,38), A (254,76), B (254,134), Y (196,184), L (319,38), R (319,98), H B (319,158), H Y (130,201), separate Menu/Start/Select rail y250. Concept D-pad envelope 64×64, analogue 80×80, smaller visible face caps acceptable; not measured dp or validated geometry. Maintain close Circle Pad/face reach and no Hold in main movement corridor. Two fixed Holds B/Y proposed; assess four only if safe fit and owner approval. Preserve independent real analogue and digital axes, native ABXY/LR and utility semantics. Historical PIV-HH-01 source audit and engineering PivotArcLayoutProfile.activeFor at 6a08c56c list D/Analog/ABXY/LR/Start/Select/Menu; this is source inventory not proven guest dispatch. New 3DS C-stick/ZL/ZR only as separately approved and backend/game-verified variant, never fake mappings. Real gyro/sensor and virtual tilt affordances only if verified backend bridge and measured room; no assumed support. Require independent >=48dp discrete hit regions, actual analogue travel, measured nonoverlap, lower touchscreen tap/drag, no screen shrink, rotation, mirror, lifecycle release and full native key/mode verification. If fit fails return for redesign or retain existing supported Standard; one-handed simultaneous analogue/stylus/face input not guaranteed. Owner approved 26 Nov 2026; no build. GripArc and Joint deferred.

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
| Design prepared | OWNER LAYOUT LOCKED — measured fit pending |
| Solo PivotArc visual | DESIGN LOCKED — handset visual pending |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
