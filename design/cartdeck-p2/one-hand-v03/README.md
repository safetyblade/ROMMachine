# CARTDECK — One Hand Mode v0.3 Diagonal A/B
**Sienna Vale | Issue #58 | Design-source only, no Android implementation.**

This version completes the *revised design* requested after reviewing thumb biomechanics. The v0.2 A/B vertical group is retained in the branch for A/B comparison and rollback. In **right-hand mode**, D-pad center is [292,594], **A** [236,681], **B** [195,743]. In **left-hand mode**, D-pad [101,594], **A** [157,681], **B** [198,743]. All values are reference pixels in a conceptual 393×852 composition — **never Android dp coordinates**. D-pad was moved higher to create enough space between independent D-pad, A and B hit-region guides. Buttons are **below and inward**, not at the extreme bottom corner.

L/R remain on the same primary thumb-side island at y=484, actual reference 75×48. The opposite-side Select/Start/Menu protected utility panel remains outside the action island, away from Android navigation. Standard-mode Start/Select and all currently working controller positions are unchanged. One-Hand utility movement exists only as a future explicit, opt-in mode requiring Daniel approval.

## Behaviour locked
`Layout → One Hand Mode → Left/Right`. One selected layout applies globally to every game; per-game/genre switching is **not** in first implementation. Required controls vary by system, but the base shell and game video never change. Future Racing, Fighting, Sports are Layout siblings. Later, long-press cartridge → Game Layout override and opt-in genre rules are separate roadmap features.

## Design/test resources
The 6 colour/hand preview SVGs and 2 touch-region guide SVGs in `review/` are editable source views. The [full local v0.3 ZIP supplied to Jonathan in ChatGPT] contains larger PNG renders and comparative boards including old v0.2 vertical vs diagonal v0.3, hit-zone diagrams and the navigation experience.

### Verification required before engineering
Actual Android dp target >=48dp, safe insets, two phone sizes, both hands, sequential Dpad→A/B inputs, grip movement, five-minute comfort, accidental utility taps, combined inputs and D-pad diagonals. A single thumb **cannot independently hold D-pad and press A/B simultaneously**; this design is not a hidden key-latch. SNES, PS1 and DS expanded controls still need their own feasibility layouts. Owner/Cassia visual acceptance and Daniel PadKit review precede any native task.
