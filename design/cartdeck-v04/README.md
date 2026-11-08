# CARTDECK v0.4 — Universal console and editable asset library

**Stage:** design source assets / engineering handoff. **NOT integrated into Android.**

Built from the approved Sienna/Cassia CARTDECK Original/Pink device direction, Jonathan's Nov 8 phone screenshots and new universal-shell + sideways-bookcase instructions.

## What is already canonical (DO NOT REPLACE)
- C-slot logo, wordmark and Android app icon: `../cartdeck-v03/brand/` and existing Android launcher assets.
- Corrected angular D-pad (no circular disc): `../cartdeck-v03/controls/dpad-master.svg`.
- Approved-direction Original / Pink cosmetics: `../cartdeck-v03/editions/{original,pink}.json`.
- Mono and Green are visual proof concepts only, **not shipping-approved**.

## Added by v0.4
- Common scalable portrait/landscape shell, independently layered screen bezel, generic tactile buttons, four/six face arrangements, shoulders, triggers, analogs, Start/Select and stylus.
- Sideways bookshelf spine, editable front cover case, non-infringing fallback artwork and cartridge insertion lip.
- Boot slot pulse and shelf/cover/display/pause/navigation SVGs.
- `catalog.json`: material-aware assets and immutable inherited identity.
- `system-viewports.json`: one CARTDECK shell for all systems, aspect-correct video, styling-only profile selection. PS1 must work in the *same portrait shell* as GB/GBA; optional extra controls do not redesign the console.
- `shelf-journey.json`: **Book spine → Cover/detail → Open/Resume**, independent customizable cover and local-only artwork.
- `build_assets.py`: generates recoloured SVG sources for each edition. Master geometry never changes with colour. `test_assets.py`: checks catalog, shell, PS1, shelf behaviour and inherited D-pad.

## Asset implementation policy
Every SVG is an editable **design master**, not an Android-ready runtime Drawable. Use a reviewed Compose Canvas/VectorDrawable conversion with versioned assets; do not load raw SVG from an APK and presume Android native support. All colour-swappable surfaces use `data-cd-fill`/`data-cd-stroke`; colour token IDs must match the authoritative edition JSON (no direct hex search-replace of geometry). Decorations are independent of PadKit input hit zones. A/B/X/Y labels are rendered as text or separate glyph layers, not permanently baked into the button face.

### One frame, many systems
For GB/GBC use core geometry (typically 160×144); GBA typically 240×160; PS1 often 4:3 but actual core video geometry can vary. Always use **contain** in the shared viewport, with unobtrusive letter/pillarboxing, never distort/crop. Don't change the emulator's internal resolution/renderer when swapping the shell. Some multi-screen systems need multiple separate bounded game surfaces inside the same CARTDECK identity, not forced into a single image.

### Controls
- GB/GBC: D-pad + A/B + Start/Select/Menu.
- GBA: add L/R.
- NES/SMS/Genesis3: two-face mapping using inherited core config.
- SNES/PSX: four-face A/B/X/Y and L/R. PS1 may require L1/L2/R1/R2 and analogs depending on active core/profile; present all required controls.
- Genesis6: six-face layout. Other systems: stylus, analog or keyboard extensions.
- PS1 symbol visuals: default to **original neutral CARTDECK labels**, not copied console manufacturer button emblems. Input mapping remains the upstream PadKit/core profile.

## Screen states
See `SCREEN_CHANGE_MATRIX.md` and `P1_IMPLEMENTATION_SEQUENCE.md`. A mere SVG commit is not a UI launch. Marcus's P0-B independent QA Issue #9 remains open; user-observed playable game + retained progress is PASS (OWNER OBSERVED), distinct from independent multi-system/upgrade checks.

**Owners:** Sienna visual master; Cassia experience; Daniel architecture/input boundary; Elliot implementation after recorded gate; Marcus independent test; Jonathan approves final product and edition additions.
