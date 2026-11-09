# ELLIOT SHAW — P1-C2A HARDWARE SOURCE HANDOFF (DESIGN ONLY)

**Controller:** Issue #74; asset pack `design/cartdeck-p1c2/hardware/asset-manifest.json`. **No app code is authorised by this design PR.** P1-C3 #75 and custom game layouts #34 are separate.

## Current implementation to preserve

Merged PR #78 already has the working 4:3 standard screen and expanded NDS/3DS exception. `GameScreenLayout.buildConstraintSetPortrait` uses:
- STANDARD 4:3 viewport with 84dp portrait top and 22dp horizontal margins, constrained to PadKit leftPad top.
- DUAL_SCREEN: flexible viewport with 18dp top/side margins and bottom constraint above controls. Nintendo DS composite video/stylus is owner-tested; 3DS not independently tested.
- Landscape overlay/non-overlay remain unchanged.
- `MobileGameScreen.kt`: one `AndroidView(viewModel.createRetroView)`; `viewportPosition` and `fullScreenPosition` set `gameView.viewport` when bounds change; PadKit controls composed later under `ConstraintLayout`.
- `CartdeckConsoleShell.kt`: current 7dp bezel + full-width bottom deck seam, except split around compact DS C-slot.
- **Critical:** `PadContainer` currently draws `GlassSurface` using `LemuroidPadTheme.level0Fill`, `level0Shadow`, etc. The lower translucent rectangle is produced by this composable, not only the Shell Canvas. The intended replacement is an **opaque connected CARTDECK hardware deck** behind the existing functional PadKit components; do not move the controls as a first step.

## Minimal implementation sequence after visual lock

1. **Hardware material renderer only**: create reusable noninteractive, pointer-transparent Compose/Vectors from new v1.0 asset masters. Use one instance of the measured screen ring (7dp outside GL viewport) rather than drawing the full normalized source SVG over game pixels. Add top/body material, exposed side rails, restrained grilles and optional panel sections. Keep iconic protected branding in `CartdeckConsoleShell`. All optional glow masks are installed but **runtime disabled**.
2. **Opaque deck with safe seams**: replace the *visual content only* of `PadContainer`'s `GlassSurface` with the integrated opaque control-deck painter. Do not remove or remap PadKit input scopes or responsive ConstraintLayout. Position the main deck from union of real PadKit bounds, clamp to Android safe area. Paint structural upper and lower seams only in free fragments **after subtracting rectangles for controls, labels, protected brand seat and game aperture**, with at least 4dp functional and 8dp brand clearance. Never render one unbroken lower line over a DS button. Preserve an intentional opaque lower materials surface even if no glow is active.
3. **Button seats (not replacement buttons)**: dpad seat angular, two/four face clusters, Start/Select/shoulders, Menu and one/two analog sockets, all drawn behind existing controls and centered on measured bounds. The approved physical buttons remain the actual hit zones; DO NOT alter their identities or pressed animations.
4. **DS utility strip**: use `controls/ds-aux-strip.svg` as a **conceptual master**, not literal fixed 393px widget coordinates. Map existing `MelonDS.kt` mic (`KEYCODE_BUTTON_L2`), screen function (`KEYCODE_BUTTON_THUMBL`), and real Menu input to left/centre/right seats. C-slot in a separate protected recess, not a button. Existing upper utility row L/R/Select/Start stays functional. Any actual composable relocation or layout change requires Daniel check of pointer/stylus/gesture mapping; #74 implementation slice after approved design. Do not create a second GL display.
5. **PS1 analog**: use `controls/analog-seat.svg` around existing PadKit analog(s), not as a new button. If analog isn't provided, omit seat cleanly. For cramped portrait, reduce nonfunctional detailing before shrinking actual controls or changing video bounds.
6. **Three editions**: reuse canonical `CartdeckEditionTokens` and Original/Pink/Green preset colors. Recolour named `data-cd-fill/stroke` tokens at render time. Special contrast: Pink decorative berry accent may be low contrast against berry bezel; reserve readable `onButton`/`focus` for critical glyphs. No extra geometry copies, image assets, presets or appearance migration.
7. **No P1-C3 leakage**: glow masks ON SVGs exist for future settings, but all zones default OFF and remain local to named physical regions; no toggle/brightness UI. Pattern/decals/stamps/faceplate editor, saved loadouts, movable control/game-type presets and one-handed mode are excluded.

## Layout math and clipping

```
R = root safe bounds after Android cutouts/insets
V = existing measured LibretroDroid viewPort bounds
Pi = existing functional PadKit control bounds (per composable)
B = protected C-slot/wordmark badge bounds
freeChassis = R \ (V union expand(Pi,4dp) union expand(B,8dp))
screenRim = inflate(V,7dp) \ V
deck = clamp(union(primary/secondary Pi)+hardware padding, R)
deckDecor = deck intersection freeChassis
glow = default OFF; futureOn = namedZoneMask intersection freeChassis
```

Use `layout-contract.json` exemplar 393×852 placements only to review intended proportions. **Never hardcode preview rectangles in dp**. Use measured `onGloballyPositioned` bounds as available; if PadKit group positions aren't accessible, introduce a narrow draw/measurement bridge **without changing hit-testing logic**.

### No overlaps
- Renderer: no shell, bezel, glass, glow or decal over any actual game pixel.
- Controls: hardware seats are under the real buttons; no accent line, C-slot, speaker or seam may cross touch area or legible label.
- Brand: original wordmark/C-slot geometry inherited from `design/cartdeck-v03/brand/`; no illumination beneath glyph.
- DS: menu/mic/screen-layout are distinct **controls**, C-slot decorative **badge**. Separate positions and z-order, no line through elements.
- Analog: socket follows real controller cap; doesn't float independent of the PadKit control.

## Proposed engineering PRs AFTER approval
- **P1-C2B-1**: manufactured body/7dp screen assembly + opaque integrated deck replacing `GlassSurface` visual + safe seam segmentation. Existing screen allocations untouched.
- **P1-C2B-2**: conditional control seats, DS utility strip + analog integration, Original/Pink/Green physical chrome and default-unlit glow source plumbing. No player-facing options.
- One combined staged ARM64 test APK only at the studio's designated build checkpoint. Do not build one per tiny visual adjustment. Cassia/owner review geometry, Daniel approves overlay/PadKit boundaries and Marcus tests actual device.

## Acceptance
**Design-only** asset tests: 48 SVG masters parse, manifest references valid, exactly six independent glow families with on/off/mask, palette path invariance, usable reference layouts, DS badge separated vertically from Menu. Runtime acceptance additionally needs real GB/GBA/4:3/PS1/NDS screenshots and controls, DS touchscreen mapping, full PadKit multi-touch/analog, Android safe insets and retained saves/upgrades. No software QA pass follows automatically from a design PR.


## Explicit stencil sources
Use `screen/standard-bezel-mask.svg` / `screen/ds-expanded-bezel-mask.svg` as shape references, and `chassis/front-body-mask.svg` / `chassis/control-deck-mask.svg` for opaque material allocation. A static SVG mask is NOT the measured actual Android renderer/PadKit exclusion. Compose computes intersections/differences from live bounds and safe insets before painting. Do not use the stencil files as full-screen white images.
