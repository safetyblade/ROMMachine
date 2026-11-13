# CARTDECK — Dreamcast controller geometry v1

**Design:** Sienna Vale · **Engineering:** Elliot Shaw · **Architecture:** Daniel Mercer · **QA:** Marcus Reid  
**Evidence:** owner screenshot supplied 13 November 2026; actual handset clearance remains to be retested.  
**Scope:** only standalone Dreamcast portrait controller presentation. Existing input mappings and combined accessories are not rewritten.

## Observed owner-device defects

- Left analogue visual and surrounding well are cut at the shell's lower boundary.
- L and R trigger capsules sit at inconsistent heights.
- D-pad dominates the control panel, and vertical space between screen and deck appears excessive.
- The shell must retain its aperture, legal gameplay area, compact brand, three approved editions and console-specific controls.

## Implemented targeted geometry

1. Dreamcast pad uses a 0.90 primary left-dial size factor; all other PadKit primary dial defaults remain 1.0.
2. Dreamcast left analogue radial position changes from -80° to -50° and radial scale from 2.0 to 1.8, keeping its real PadKit analog input and stick-press identity.
3. Both native Flycast L2/R2 trigger widgets use the same 90° radial anchor. Labels remain L/R and their codes are unchanged.
4. Standard Dreamcast portrait shifts the whole pair of touch pad groups above the shell's bottom edge by 24dp for portrait height ≥680dp, otherwise 12dp. This avoids lower-edge clipping and reduces the empty screen-to-pad band without changing video aspect, the renderer or the safe game area.
5. The additional inset is zero for landscape, all other consoles and every non-Standard combined accessory layout; existing GripArc/PivotArc geometry keeps its own solver. Existing shared left analogue defaults are unchanged for other consoles.

## Protected behaviour

- Dreamcast A/B/X/Y original compass; one left analogue; D-pad; L/R native triggers; Start and menu.
- No fabricated Select or right analogue.
- No emulator core/ROM/saves/state controller pipeline, library, shell graphics, edition semantics or package/signing changes.
- No global Start/Select movement, no viewport change, no fabricated GripArc device pass.
- **Issue #135 remains an independent blocking defect**: owner has reported oversize GripArc, missing native buttons on console change and failure of PivotArc+GripArc.

## Acceptance / verification matrix (not presumed PASS)

| Gate | Required real evidence |
| --- | --- |
| DC tall handset | Entire analogue dial and press target inside shell/navigation safe area; no controller overlaps |
| DC compact portrait | Same, including 48dp minimum measured interactive target on owner-class density |
| Trigger symmetry | L and R same visual shoulder height and both work in Flycast |
| D-pad/face/analog/Start | All native inputs, tap/hold, D-pad + analog + face simultaneous play |
| Video | No lost pixels, crop or forced stretch; blank band visibly reduced |
| Other platforms | PS1/PS2/PSP/GameCube/GB/GBA/SNES and DS controls untouched |
| Combined accessory | #135 own matrix; changes must not bypass blocker or claim a fix |
| Editions | Original/Pink/Green/custom material and shell/brand stay coherent |
| Continuity | Install-over existing signed APK, resume/saves unchanged |
| Source | Unit assertions for console/mode/landscape/height inset and full ARM64 workflow |

**Rollback:** revert this PR; existing saved data/ROMs and all input mappings remain compatible. No migration required.

**Do not merge as device accepted on a source-only result.** Capture owner Android SM-S721B screenshot and Marcus's independent gameplay matrix after build.
