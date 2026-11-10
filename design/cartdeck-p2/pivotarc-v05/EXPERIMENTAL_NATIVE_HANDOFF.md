# PivotArc v0.5 — bounded experimental Android study handoff
**Prepared for Elliot Shaw; architecture prerequisite Daniel Mercer. Not yet authority to merge runtime behavior or publish a release.**

## What this prototype must prove
The second-sketch PivotArc action arc and horizontal/vertical shoulder pair can be placed for Left and Right hands without breaking the approved CARTDECK Standard controller, video viewport or save continuity. A single GLOBAL One Hand preference persists for all games; active input set is system-aware. This proof is not a claims-of-novelty study.

## Inputs to inspect
- Native current `MobileGameScreen.kt`, `GameScreenLayout.kt`, `CartdeckConsoleShell.kt`, `PadKit`, touch layout profiles under `lemuroid-touchinput/.../layouts/`.
- Versioned design `layout-contract.json`, all `diagrams/`, and offline `prototype.html` under `design/cartdeck-p2/pivotarc-v05/`. Do not use SVG positions as hard-coded display dp values.
- P1-C2 owner position lock: existing **Standard-mode** Start and Select touch bounds are unchanged. One Hand utility placements require a separate constrained safe-region measurement.

## Technical spikes (separate commits / no public release)
**A. Geometry and fitting — no runtime input changes.** 
Create a pure `PivotArcLayoutSolver` driven by measured safe window, game viewport bottom, Android gesture/cutout insets, hand and system-control set, plus optional small whole-group translation. It must clamp the entire array together, verify >=48dp targets and >=8dp modeled separation and return `Fits` / `Constrained(reason)`. No shrinking keys, no placing art over game video; compact/DS constrained cases must return a clear reason. Add unit tests for small/medium/large phone constraints and mirroring.
 
**B. Read-only on-device measurement/debug overlay.**
Feature flag for developer testers only: show actual bound outlines and anchor, log proposed Pi target rectangles plus Android insets. **Do not route game touch input through new scheme yet**. Verify grip positions and existing control objects are distinct from cosmetic seats.

**C. Input experiment behind explicit opt-in and Daniel review.**
Retain PadKit's input IDs, active pointer tracking, haptics and D-pad diagonals. Candidate A primary A=P4/B=P3 versus Candidate B A=P3/B=P2 must map to existing IDs without changing emulator core handling. Two-action game deactivates unused P1/P2; four-face system activates all four. Only after review can a touch-down ownership experiment be added, not silently applied to existing PadKit.
 
**D. One global selection.**
`Layout → Standard | One Hand Mode → PivotArc → Left/Right → Adjust Position`. Save global mode/hand/anchor; no per-game/genre assignment now. Standard switching restores exact current control positions/values. Do not touch ROM/save progress storage. Allow reset of the whole-cluster offset; individual button manipulation is a future separate Advanced setting.

**E. System compatibility**
GB/GBC/GBA first acceptance; SNES four-face second. Six-face, N64 C/Z, PS1/PSP analog and secondary shoulders, DS/stylus, Dreamcast/GameCube/PS2 only get explicit module support AFTER measured available control region and required PadKit semantics pass. Global user preference may persist across an unsupported family, but never silently crop the viewport, remove mandatory controls, or switch to Standard. Expose a visible reason and option to choose an available layout.

## Owner / architecture / QA gates
- Sienna and Jonathan: approve actual P1–P4 primary A/B mapping via in-device comparison and acceptable Separate Utility location.
- Daniel: PadKit pointer state and touch-down ownership approach, screen safe-area solver, and switch persistence.
- Elliot: bounded native test feature with no emulator/core/signer changes.
- Marcus: 320dp/393dp+ phones, both grips, 30 input transitions per candidate, 5-minute comfort, A+B/shoulders and diagonals, accidental utility activation, screen continuity, saved progress and in-place APK update.

**No change to P1-C2 default console gameplay, Standard Start/Select, PS1/DS viewport policy, save logic, libraries or cartridge shelf. No full-control editor, game-specific profiles or glow/faceplate controls in the spike.**
