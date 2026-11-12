# CARTDECK — SIENNA V2 | GripArc Dock-First Combined Console Deck
**Date:** 12 November 2026. **State:** Sienna design direction — **OWNER REVIEW REQUIRED**. **Engineering:** NOT AUTHORISED by this file. **Relevant:** #135, #123, #125, #58, approved V1 asset PR #126.

## Why native GripArc fails
Device screenshots prove (1) Standard + GripArc reflows the original PS2 controls into awkward positions and then displays "Controls need more room below the game screen"; (2) PivotArc + GripArc displays the whole PivotArc plate and then reports the permanent full-L cannot fit. Neither configuration draws GripArc.

The failure is structural, not a missing emulator or button mapping:
- `GameScreenLayout` still positions Standard PadKit left/right radial clusters under legacy constraints. `GripArcStandardReflowPolicy` offsets only ONE existing group (horizontal ≤30dp, vertical ≤148dp); a dock region is never reserved.
- `MobileGameScreen` rejects occupied targets outside the safe rectangle or inside the video before even attempting L placement. This can reject the shifted Standard controls independently of whether the *actual GripArc arm* collides.
- GripArc's two-arm silhouette test is a partial repair; the solver still tries to insert a full L *after* controllers are already laid out, with bottom-first placement. The source L is 272×308 illustration units; its operational scale ranges 0.72–1.0, requiring careful measured phone geometry.
- In PivotArc mode, the large plate and controls solve separately, leaving no interlocking cutout. It needs a combined manufactured housing.
- The `individualInputs` bound observation is not keyed by cartridge GripArc mode/side, introducing stale-bound risk when a user changes layouts.

## Sienna proposed layout — design lock for review
1. **Standard-only and PivotArc-only remain 100% unchanged.** New composite layouts apply ONLY to `STANDARD_GRIPARC` and `PIVOTARC_GRIPARC`.
2. **Dock first.** Reserve the actual permanent RIGHT or mirrored LEFT L silhouette; G1/G2 upright, G3/G4 lower arm. Keep approved V1 272×308 source geometry, G1–G4 positions and fixed 1–4 progression; all four touch targets reserved even when only G1 is visible. Shoulder-shaped G buttons, no ghosts.
3. **Standard + GripArc:** dedicate separate measured upper **shoulder rail**, far-side **D-pad**, inner-L **face cluster** and below-L **analog terrace**. Start/Select/Menu fit reserved safe gaps. Stagger the D-pad vs faces in constrained widths; preserve viewport. Stop shifting existing two bulky PadKit groups.
4. **PivotArc + GripArc:** create a new compact **Pivot Island**, with a notched, manufactured shell that interfaces with the complete L (no second full-sized overlapping plate). Retain PivotArc D/L/B/A/R, illuminated Hold A/B and required utility controls, and reserve future X/Y. Solo Machined V2 plate unchanged.
5. **One solver:** calculate root safe inset, fixed viewport, source-native key inventory, dock footprint and all four G targets, each individual real PadKit touch target, and decorative material outlines before any module is rendered. Allocate, validate, then render from the SAME result. Do not use enclosing composite group boxes as the collision model.
6. **Input source locked:** reuse the existing PadKit IDs, independent hold/chord arbitration, settings, haptics and save/reopen. No engine, ROM, save system, or input-pipeline rewrite.
7. **Legible failure:** only when genuinely measured impossible, preserve saved cartridge choice and offer safe controls: try opposite side, reset accessory position, switch to Standard. No transient message that leaves the owner without an actual GripArc.
8. **Theme parity:** Original/Pink/Green/custom semantic tokens; hardware trim yields to clear 48dp+ touch and game/gesture safe areas.

## Engineering handoff after owner review
Introduce a `CompositeControlDeckSolver` that returns `viewport`, `safeDeck`, `dockFootprint`, `dockTargets[1..4]`, per-console input targets (keyed by existing PadKit IDs), `shellShapes`, `fitVerdict` and typed `failureReasons`. Compose individual controls from this placement, not radial groups moved by offsets. Key caches by mode, side, system, orientation, density, safe insets and measured view bounds. Preserve already merged games and controller-specific native layouts.

## Mandatory matrix
- Owner handset first, then 320/360/393dp; right and left; 1–4 active buttons.
- PS2 (4 face + two sticks + shoulders), GameCube (C-stick), Dreamcast (one stick + triggers), GBA and SNES (PivotArc including Hold A/B and X/Y reservation).
- Touch rectangles >=48dp, no collisions, no viewport shrink, no hidden/ghost buttons, mirrored input identity retained.
- Actual GPIO/touch events, simultaneous chords, held B+D-pad, Start, triggers, save/reopen and resume, negative-case specific fallback, theme/readability.
- Clear independent Marcus gameplay result **PASS/FAIL/NOT TESTED**, install-over ARM64 APK and source rollback.

**Design artefact:** Sienna's offline interactive HTML layout lab, provided in owner review conversation; it is a conceptual drag experiment and must NOT be treated as an Android placement proof or copied as production coordinates.

**Approval status:** proposal only; no native implementation and no claim that 320dp PS2/PivotArc combinations physically fit.
