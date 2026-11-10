# CARTDECK | P2-C1 — PivotArc Candidate C native implementation plan
**Design:** Sienna Vale · **Engineering:** Elliot Shaw · **Architecture:** Daniel Mercer · **QA:** Marcus Reid · **Owner:** Jonathan Prestwich
**Parent issue:** #58 · **Design PR:** #106 · **Status:** design prototype ready; native implementation planning, **NOT an approved runtime merge**.

## Decision baseline
The owner's hand-adjusted screenshot is the controlling *relationship* for Candidate C: central/right D-pad, descending B→A below-left, L immediately left of D beside B, R alongside A lower-right, X/Y above-left. Optional L2/R2, analog, P5/P6 and DS controls appear only when required. **Do not replace this with a symmetrical ABXY cluster.**

At 393×852 reference size, the v0.7 HTML normalises painted sizes to **D-pad 92×92dp proxy / hit 96×96; A/B/X/Y 44dp painted / hit 48dp; L/R 43dp painted / hit 48dp; analog 58dp painted / hit 64dp.** Control centres are *approximate readings from the screenshot*, with a few sub-10dp spacing corrections. The **owner's next exported JSON** becomes authoritative for final centres, visual sizes and separations. The user can drag/resize independently now; native per-button editing remains later scope.

The default editor tests clear the 8dp modeled spacing preference for GBA, SNES, PS1/PSP and DS *at the 393dp reference size*. This says nothing about actual thumb comfort, pressure/contact patch, or real PadKit touch behaviour.

## Stage A — Design lock and measured feasibility
1. Owner uses the supplied offline PivotArc Candidate C Editor v0.7 and exports JSON. Store resulting exact coordinates as an approval-controlled JSON source, preserving the reference canvas dimensions and handedness.
2. Daniel reviews existing `MobileGameScreen.kt`, `GameScreenLayout.kt`, `CartdeckConsoleShell.kt` and PadKit input layouts. Establish one source of truth for real GL viewport bounds, safe Android status/navigation/cutout insets, input IDs, actual touch rects and pointer event semantics.
3. Implement a **pure solver** transforming the canonical relative button positions into safe target rectangles for each device/hand/console profile. Distinguish button artwork geometry from minimum **48×48dp touch targets**; model preferred ≥8dp between active regions. The solver returns Fits / Constrained(reasons) with no silent input loss.
4. Compare a narrow phone and a large phone with real dimensions. If DS/expanded controls cannot fit, retain global mode preference and present an explicit compatibility warning/alternative choice. Do not shrink essential touch targets, crop the game image or silently change modes.

## Stage B — Opt-in Android PivotArc foundation
1. Introduce **Layout → Standard | One Hand Mode → PivotArc → Left/Right → Adjust Position**; first implementation persists one global mode and simple **whole-cluster** offset (plus a Reset action). All games use the selected global mode; only required controls vary by system.
2. Keep the existing Standard-mode Start/Select controls, touch bounds and layout exactly as they are. The screenshot's utility controls are **not** an instruction to relocate Standard controls. For PivotArc, utility placement gets a separate safe-position approval.
3. Reuse existing CARTDECK Original/Pink/Green semantic colours and physical button sources. The new artwork is non-interactive and placed behind existing PadKit touch controls.
4. Preserve the existing LibretroDroid GL renderer, cartridge/library identity, ROM paths, save persistence, in-place app upgrades and controller settings.
5. Add reversible feature gating: normal Standard controls remain immediately selectable; no destructive mapping migrations.

## Stage C — Core system support
1. Activate D/A/B for GB/GBC/NES-style inputs and D/A/B/L/R for GBA. Apply left/right mirroring to **positions only**, never key IDs, letterforms, brand or video.
2. Extend to four action buttons X/Y for SNES after reach testing; verify chords, diagonal movement, holds, thumb sweeps, D-pad transitions and shoulder presses.
3. Preserve PadKit's multi-pointer model; do not implement new slide-to-activate, hold-latching or synthetic simultaneous-input automation without Daniel's review. A browser diagram is not input acceptance.
4. Confirm artwork visually scaled separately from functional hit rectangles and that selected visual scale never changes mapping.

## Stage D — Conditional modules, not blockers to core
- Six-button systems: P5/P6 extension with its own fit test.
- PS1/PSP: shoulders, L2/R2 and analog integration; multi-press limits explicitly validated.
- Nintendo DS: both screens and stylus coordinates protected; Mic/Screen seats need dedicated actual viewport feasibility.
- Dreamcast/GameCube/PS2/N64: design interfaces for future modules, not a claim of playable one-hand compatibility.

## Engineering slices / deliverables
| Slice | Output | Gate |
| --- | --- | --- |
| P2-C1A | Data model, saved global selection, measured safe-region solver, preview-only layouts, collision tests | Daniel geometry review |
| P2-C1B | Feature-flagged native GB/GBC/GBA PivotArc and mirrored positions, Standard rollback | Real device + Marcus controls QA |
| P2-C1C | SNES four-face activation, analog/extra-console feasibility branches | Owner game acceptance per system |
| P2-C1D (later) | Full advanced per-button editor, per-cartridge overrides, automatic genre suggestions | Separate approval, not included now |

## Independent acceptance tests
- 320dp and 393dp+ phone widths; status/gesture insets and orientations.
- A/B, L/R and D-pad contact boundaries; 30 D-pad→B→A cycles; D-pad diagonals, shoulder taps, A+B, sustained/short holds and pointer ownership; no accidental Start/Select/Menu.
- Five-minute one-handed grip/use; missed or false presses and need to regrip.
- GB/GBC/GBA then SNES physical gameplay; PS1 and DS **separate** feasibility.
- Standard mode unchanged, progress/save/reopen unchanged, preference remains after in-place APK update, rollback to Standard immediate.
- No geometry claim based solely on the mockup or the build succeeding.

**Current authority:** prepare implementation architecture and staged Codex prompt. **Do not implement native gameplay input or merge a runtime PR until owner-exported layout is reviewed.**