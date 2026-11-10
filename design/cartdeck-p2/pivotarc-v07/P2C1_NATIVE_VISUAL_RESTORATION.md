# CARTDECK | PivotArc Candidate C — native hardware restoration
**Owner review:** 10 November 2026 · Screenshot: installed PivotArc GB/GBA gameplay.
**Design:** Sienna Vale · **Engineering:** Elliot Shaw · **Issue:** #58 · **PR:** #107

## Owner-reported regression
The original native PivotArc patch preserved the exterior chassis, brand and game aperture but left the lower control deck visually sparse. Individual A/B buttons had no CARTDECK hardware wells, speaker and indicator placement collided with the PivotArc utility rail, and 44dp action / 43dp shoulder artwork appeared far too small.

**Directive:** PivotArc is another *native hardware configuration*, never a visual replacement for CARTDECK. Keep the current case, full branded header, screen bezel, chassis, deck, speakers, light rail family, physical seats, colour variants, safe utility positions and renderer. Standard Mode must not change.

## Native correction

1. **Keep approved Candidate C topology:** D-pad upper central; L at D-pad left near B; B below-left; A below-right of B; R adjacent to A; X/Y added above-left for SNES. Left-handed use mirrors positions, not input IDs or display.
2. **Restore hardware seats:** retain approved C2 D-pad, shoulder, Start/Select and Menu seat assets. For spaced individual face buttons, use colour-token-matched individual circular machined wells rather than stretching/cropping the grouped AB/ABXY SVG assets.
3. **Restore speaker/indicator family:** reuse original/pink/green source resources. Check for safe free space between game aperture and utility rail; use measured free space after the controls only when that region is unavailable. No art may cover pixels or pointer targets.
4. **Correct button proportions:** Candidate C JSON remains the archived geometry reference. At runtime, scale the entire working gameplay constellation and its *touch bounds* together; prefer 1.28× on roomy portrait surfaces (D-pad visual ~118dp, action ~56dp, L/R ~55dp; action hit zones ~61dp). Try smaller safe presets only when fit requires it. Do not shrink below the baseline 48dp hit targets. Utility controls retain their original dimensions.
5. **Measured no-overlay validation:** only paint background artwork under PadKit; do not alter LibretroDroid viewport, game aspect ratio, ROMs, saves, core mapping or standard input controls.

## Verification

- Screenshot compare to the original CARTDECK Standard chassis: complete shell, seats, rails, grille, indicator and full logo remain visible
- Original, Pink and Green palette and original shell symmetry
- Thumb reach and no neighbouring key false positives with enlarged targets
- Compact phone chooses safe alternative or explains Standard fallback, never discards system keys
- GB/GBC/GBA/SNES in-game movement, holds, diagonals, shoulder combinations and save/resume
- Check on the actual owner's device; pure solver tests and APK signing success are not visual/gameplay acceptance

The owner-provided screenshot is evidence of the v0.7 first-pass regression. The follow-up native patch is not automatically visually accepted by a green CI job.
