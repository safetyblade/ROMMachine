# CARTDECK P1-C2A — Production Hardware Pack v1.0

**Status: SOURCE ART COMPLETE / VISUAL APPROVAL PENDING.** Owner and Cassia must approve the compositions before Elliot changes app layouts. This is not runtime acceptance.

## Recovered native baseline
Latest source is merged PR #78, master `10056e350073eb33f8dc9ba45a2e1ceb64555539`. `CartdeckConsoleShell.kt` currently draws a chassis with 7dp bezel and a near-bottom full-width structural seam, except when the DS compact badge splits it. `GameScreenLayout.kt` allocates a responsive 4:3 single-screen window (22dp side margins, 84dp top margin) and an expanded flexible dual-screen viewport (18dp side/top margins). `MobileGameScreen.kt` owns the measured AndroidView and PadKit control composition. `MelonDS.kt` has functional mic and screen-layout buttons attached to secondary groups; `PSXDualShock.kt` has conditional dual analog controls. We do NOT change these source files in this PR.

## Three connected manufactured zones
1. **Display assembly:** external bezel, thin glass lip, edge/corner trim, consistent **7dp** structural surround following existing measured `viewportBounds`. The vector is a local source master only; Compose must derive actual outer ring from game viewport to avoid stretching corners or masking content. Standard and DS have matching edge thickness, not identical glass dimensions.
2. **Main chassis:** solid front-body, exposed shoulders and side rails; protected brand seat and optional speakers or machining in genuine residual space. Customisable residual mask exists but no skin editor.
3. **Control deck:** opaque solid attached panel with edge and lower seam; integrated hardware seats drawn **behind** PadKit. Lower decorative seam is segmented around live controls and DS C-slot, not drawn straight through a working button, label or badge. This removes the existing lower translucent band visually without collapsing safe unused space.

## Screen, controls and DS
- **Standard:** existing 4:3 opening preserved; GB/GBC/GBA/PS1/N64/PSP game is contained by core renderer; no alteration to emulator frame. Two-face and four-face share chassis.
- **PS1:** circular analog socket is legitimate mounting detail around the existing analog stick — not a new interactive element. One or two instances only when PadKit supplies them; no detached floating analog.
- **Nintendo DS:** a cohesive lower auxiliary strip: microphone left, screen/layout function centre, Menu right. C-slot stays in a separate small protected recess **above the utility line** so it is never mistaken for a button. Upper L/R and Start/Select remain. The lower strip reflows from measured PadKit bounds; fixed artwork is only a reference. Preserve both DS screens and stylus coordinates.
- **Future layouts:** controls are data-driven seat types (dpad, twoFace, fourFace, analog, shoulder, startSelect, menu, mic, screenLayout, genericAux). Asset geometry does not assume a fixed count of inputs; racing, stylus-heavy, one-handed and custom presets remain future scope.

## Six independent lighting families
Each has `-mask.svg`, `-unlit.svg` and `-on.svg`. Unlit is a physical engraving, *on* is a distinct editable tintable layer. Zones: top shoulder, bezel edge, badge underglow, speaker/indicator, analog ring and lower deck seam. Each zone has a documented color token, protected clipping region and z-order. **Default all illumination OFF**. No glow settings or brightness UI is implemented in this design task.

## Edition and physical-finish contract
Every SVG uses original CARTDECK semantic colour identifiers, with **one shape shared by Original/Pink/Green**. Source colours default to graphite, but native renderer binds approved `design/cartdeck-v03/editions/*.json`. Custom named finishes can later remap tokens without cloning geometry. Do not redraw the canonical C-slot, wordmark, button master, D-pad or PadKit hits. Pink accent against berry bezel is too low-contrast for critical text; use the accessible resolved `onButton` / `focus` for labels and focus.

## Design-only safety
- No input handlers, fake functional controls, additional AndroidView, direct emulator calls, cartridge-library changes, save modifications, package ID/signing work or branch merge into runtime.
- Draw visuals with pointer-transparent `Canvas`/VectorDrawable. **Control seats behind PadKit**; user interactions remain the original control components.
- Display ring and glass lip must never cover game pixels. The screen aperture is measured dynamically from allocated renderer viewport, not a static PNG hole.
- Draw materials with unlit state complete; lighting, masks, stamps, camo, loadouts and movable controls are later #75/#34/#58 lanes.

## Review and implementation order
1. Sienna source + standard, PS1, DS and three-edition previews — **this PR**.
2. Cassia and Jonathan: art, geometry, DS utility strip and analog-seat signoff.
3. Daniel: measured bounds, overlay input isolation and segment exclusion.
4. Elliot: focused Compose hardware placement in `CartdeckConsoleShell.kt`, `GameScreenLayout.kt`, `MobileGameScreen.kt`, preserving active PadKit layout IDs.
5. Marcus: actual 4:3 screen; DS both panels + touch; dual analog; A+B/diagonals/shoulders/Menu; shell per-edition, no collision; performance, saves and update continuity.

**This deliverable intentionally does not produce an APK.** Existing PR #78 remains the working runtime baseline.
