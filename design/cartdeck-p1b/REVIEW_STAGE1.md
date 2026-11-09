# CARTDECK P1-B / Sienna Stage 1 — Original Handheld Console

**Status:** candidate artwork awaiting Jonathan / Cassia approval. **No Android runtime change**.

## Source fidelity and recovered implementation

Inspected latest `master`, Issue #13, `MobileGameScreen.kt`, `GameScreenLayout.kt`, PadKit `LemuroidControlCross` and `GB.kt`/`GBA.kt`/`PSX.kt`, plus the approved C-slot, chamfered D-pad and v04 universal screen/control assets. These are the authority. The old user-supplied game screenshots show a tall black upper viewport and detached grey circular virtual control islands. The correction should not alter the actual Libretro renderer.

## First definitive design

- **One chassis:** gently chamfered graphite shoulders, fine machined edging, meaningful shallow screen bezel, tiny C-slot / wordmark, attached bottom control deck.
- **Game first:** use the actual viewport width with a calculated *contain* aspect (GBA 3:2 / GB-GBC 10:9). Same outer screen geometry; safe dark glass matte around whichever video does not fill. No image cropping, tint or stretched pixels.
- **Controls:** no circular disc behind the D-pad; same approved cross silhouette. Layered tactile A/B states. GB: A/B, D-pad, Start/Select and reachable Menu. GBA: same plus L/R. PS1: four face/shoulder/analog modules with shared shell family; exact fit awaits landscape/expanded layout review.
- **Safe phone:** ergonomic lower control deck, Android cutout and gesture insets, >=48dp touch targets, no dead animated interaction or status overlay over gameplay.

## Transparent editable masters

`console-bezel-depth-overlay.svg` provides an open literal screen aperture (no video-covering tinted rectangle). Reuse canonical `design/cartdeck-v03/brand/cslot-master.svg`, `controls/dpad-master.svg`, and v04 button/shoulder/analog/Start-Select assets rather than fork another brand.

`button-{normal,pressed,focused,disabled}.svg` demonstrates independent visual state assets; the A/B/X/Y face labels and corresponding PadKit hit zones remain runtime overlays, never baked into button art.

## Real proportions, shown at a 393 × 852 reference phone

- Shared screen aperture: left 37, top 141, width 319, height 238 reference pixels.
- GBA game image: 315×210 at x=39, y=155 (exact 3:2).
- GB/GBC game image: 264×237.6 at x=64.5, y=141.2 (exact 10:9).
- Original pixel scenes Moon Quay/Archive 03 used **only in mockups**; they are not ROM screenshots or a replacement for the real renderer. Link to downloadable vector previews in Sienna's review, not automatically bundled in app.
- Screen and app coordinate values are illustrative **reference pixels**, not fixed dp constants. Engineering derives rectangles from measured available display, gesture/cutout insets and reported core geometry.

## Native integration map

1. `MobileGameScreen.kt`: add Compose physical shell visual layer around existing AndroidView; preserve PadKit scope and `viewModel.createRetroView` instance. Existing `fullScreenPosition` / `viewportPosition` measurement remains the GL viewport authority, not unreviewed fixed coordinates.
2. `GameScreenLayout.kt`: adjust only screen/control constraints, paddings, safe insets and adaptive portrait/landscape.
3. `LemuroidCrossForeground.kt` / `LemuroidControlBackground.kt` and `LemuroidButtonForeground.kt`: replace appearance only, preserving `ControlCross` diagonal and face-button input semantics, press states and all actual PadKit hit targets.
4. `GB.kt`, `GBA.kt`: preserve A/B, L/R, Select/Start/Menu IDs. Don't move mandatory controls off-screen for a visual.
5. `PSX.kt` and `PSXDualShock.kt`: later expand **same** chassis. PS1 video follows core reported geometry; all required four-face, L1/L2/R1/R2 and optional analog stay accessible.

## Stage progression

Stage 1: Owner/Cassia approve screen-frame treatment, silhouette, D-pad placement and balance from visual review. Stage 2: Sienna finishes landscape/PS1/edition/responsive art in **same** family. Stage 3: Daniel reviews renderer viewport and PadKit constraints. Stage 4: Elliot implements separately under #13. Stage 5: Marcus validates real game video, diagonals, A+B, shoulder buttons, Start/Select/Menu, suspend/resume and save/update continuity.

This PR is **design-only**. An SVG or visual preview is not Android runtime acceptance.
