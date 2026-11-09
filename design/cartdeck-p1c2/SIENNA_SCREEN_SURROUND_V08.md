# CARTDECK — Sienna Vale · P1-C2 Screen and Surround V08

**Status:** Sienna-led DESIGN CANDIDATE, pending Cassia Rowan and Jonathan Prestwich visual approval. **Not an engineering execution approval.**

**Date:** 9 November 2026  
**Controlling parent:** [P1-C #14](https://github.com/safetyblade/ROMMachine/issues/14) · [Adaptive viewport #74](https://github.com/safetyblade/ROMMachine/issues/74) · [Future pattern/stamp faceplates #75](https://github.com/safetyblade/ROMMachine/issues/75)  
**Design authority:** Sienna Vale · **Experience:** Cassia Rowan · **Engineering handoff:** Elliot Shaw · **Architecture:** Daniel Mercer · **QA:** Marcus Reid.

## 1. Owner's new device feedback (v2101 / PR #76)

The owner supplied phone screenshots of a single-screen handheld game and a Nintendo DS dual-screen game in Pink CARTDECK:

- **Nintendo DS:** Both game screens now occupy the available tall aperture at a useful size. This uses the EXISTING core / LibretroDroid path. Preserve it. The side black bands are small and may be correct aspect letterboxing.
- **Single-screen GBA-style content:** The game frame is already almost the full width of the phone and therefore cannot get appreciably bigger at a 3:2 aspect without cropping. The `GameScreenLayout.kt` portrait `fillToConstraints` now assigns a very tall *renderer viewport*; the core centres a width-limited image inside it, leaving major black bands above and below the pixels.
- **The visual regression:** `CartdeckConsoleShell.kt` draws its massive 15dp bezel around the entire assigned renderer viewport, **not the actual displayed image**. As a result the black letterboxes look like an oversized black screen, the protected logo has no safe space and disappears, and the original CARTDECK character is lost.
- **Controls:** The owner's earlier physical test accepted the lower control area, responsive feel, game reload and retained progress. Do not redesign those controls to solve a display allocation problem.
- **Desired feel:** A manufactured CARTDECK handheld. Intentional space is welcome when it is styled chassis, not a fake inactive game display. Restore C-slot/wordmark, subtle machined trim and physical form without taking away useful game pixels.

### Controlling design statement

**The native video _content_ determines the screen opening; the remaining physical surface is CARTDECK.** Do not create a universal tall glass opening for every core and do not return to one fixed 3:2 screen for DS. The same shell family adapts its display surround, branding placement and decorative faceplate zones around each active game.

## 2. Sienna's recommended design language

**One physical device**, three factory materials (Original graphite/teal, Pink/berry, Green sage/jade/lime) and optional named custom palettes. Do not create Nintendo/Sega/Sony clone hardware skins.

- Keep the approved chamfered outer chassis, one-piece D-pad, tactile A/B / PS1 four-face and shoulder control language.
- **Narrow the black surround:** target a physically credible **6–8dp bezel**, instead of today's 15dp, with restrained 1dp edge lighting; verify actual pixel density and contrast. Treat these as design targets, not hard-coded universal values.
- Draw the bezel **immediately around the displayed content aperture**, not around unused core letterboxing. Separate any actual video bars from painted glass.
- Restore the permanent **C-slot + CARTDECK wordmark** on an opaque, protected surface whenever it can fit. Effects/trim never cross the badge; narrow DS configurations fall back to a compact C-slot on an alternative chassis region, not to an unbranded device.
- Use narrow mechanical seams, subtle accent rails, bolt/grille shapes and integrated material transitions to restore detail. The visual language remains manufactured, not neon HUD/overlaid UI.
- Avoid forcing a huge blank strip under a shorter image. Distribute *naturally unused* area into logo seat, surface finish and small engineered details, with optional future sticker / pattern safe zones.

## 3. Three adaptive screen arrangements

### A — Width-limited single-screen (GB/GBC, GBA, 4:3, PSP and variable PS1 when appropriate)

1. Let the single existing renderer provide its true current display aspect. At a given safe width, fit the **largest uncropped game rectangle** with contain scaling. The physical bezel follows this rectangle.
2. Place the content screen in the upper-to-middle portion of the usable screen-and-chassis area. Reserve only the physically available natural surplus for a slim logo seat above or below, not a mandatory fixed 92dp header.
3. Use the unused remaining vertical chassis to display the restored CARTDECK wordmark, restrained manufacturing details, and later player decals/patterns. Do not paint a black video window throughout that remaining surface.
4. An aspect or resolution change during play may reposition the visual bezel without resetting the core. Avoid a layout oscillation for tiny source fluctuations: validate what live frame geometry is actually available before adopting thresholds.
5. If a tall/portrait game becomes height-limited instead, choose height-limited arrangement (the screen remains first priority).

### B — Height-limited dual screen (Nintendo DS and compatible multi-screen output)

1. **Preserve the working DS presentation from v2101.** Both stacked screens are drawn by the existing single `GLRetroView`; do not insert two AndroidViews or split emulator output.
2. Fit the complete composite video into available height and width without cropping/stretching. Keep bottom-screen touch coordinate mapping, Pause/Back and system-complete face/shoulder buttons functional.
3. Use a **thin continuous bezel** and small side rails/chamfers; relocate C-slot brand detail to a narrow side rail, below the viewport, or the control-deck seam when the full wordmark cannot fit.
4. Do not force the wide-game logo/header into the tall DS allocation. DS being screen-heavy must still read as CARTDECK, not as plain black-border emulation.

### C — Landscape, tablet, floating and unusual aspect ratios

- Retain original `GameScreenLayout.kt` landscape overlay/non-overlay semantics and complete PadKit controls.
- Available height/width, Android safe insets and live core aspect determine actual aperture first. Side panels and slim engraved brand elements use the remaining material surface.
- All 25 configured systems share this adaptive mechanism; the matrix supplies conservative **fallback** ratios only where runtime geometry is unknown. Do not imply that each configured system is fully device QA accepted.
- Existing console/fullscreen/floating presentation-mode roadmap remains P1-C #14, separate from this visual correction.

## 4. Geometry: renderer bounds are not content bounds

Current:
`rootBounds → full GLRetroView → gameView viewport bounds → shell hole + 15dp bezel`.

Required:
`safe allocation → measured/known native content aspect → largest contained content rectangle → single GLRetroView.viewport → thin bezel around that actual rectangle → remaining chassis/branding/decal zones`.

**Do not infer true rendered game bounds merely from `viewportPosition`.** The current `gameView` box covers available space, so it includes potentially enormous vertical black bars. Either obtain the core-reported active aspect from the existing renderer and compute a contained rectangle, or demonstrate that a validated per-system fallback can safely drive the allocation until a live geometry feed is available. Ensure DS and PS1 dynamic video remain correct. Separate real game pixels from GL letterboxing in evidence.

Let `C = (contentWidth, contentHeight)` use **display aspect** (not pixel count if non-square pixels). Let `A = (availableWidth, availableHeight)` be Android-safe area above the functional controls. For a contained video rectangle:
`scale = min(A.width/C.width, A.height/C.height)`;
`videoWidth = C.width * scale`;
`videoHeight = C.height * scale`.
Use the original GL renderer for game pixels, **not** a Compose screenshot, second GL instance or image manipulation.

The available surround area is the set difference between the safe manufactured body and the padded video aperture plus PadKit hit regions. These are responsive **physical surfaces** for design—not black GL rectangles. Bezel, brand, trim, material and optional decals must never overlap the actual game or functional controls.

## 5. Permanent brand-seat placement hierarchy

1. **Full C-slot + CARTDECK wordmark** on a compact, opaque inset in an appropriate naturally free top/front panel.
2. If no top clearance, **relocate** the same protected badge to the interstitial front panel between display and controls, if available.
3. For tall DS/narrow devices, use **compact C-slot-only** on a side/bezel rail or lower seam; reserve safe clearance from trim.
4. If even the compact mark cannot fit on the upper body, use a recessed mark integrated with the **unoccupied lower shell/control deck**, away from input targets. Never claim hardware is intentionally unbranded merely because its display is tall.

Every placement is derived from the measured screen and control geometry. Never paint the badge under shell lines, glow, speaker grilles or decorations.

## 6. Future customisable faceplate zones (#75)

Plan the surfaces now; do **not** implement the import/editor or position persistence in P1-C2.

- Base material finish remains always present, with optional **CARTDECK-original camo / geometric / halftone patterns** applied only to exposed faceplate shapes.
- Optional badges/stamps include a smiley symbol, small collector marks and user-imported lawful artwork.
- User layer renders in clipped safe regions after current content, controls, protected wordmark and essential shell geometry are known. Reflow/reposition on DS and landscape; do not reserve permanent screen space for a decal.
- Save per factory/named appearance, not per ROM, game identity, autosave or emulator core. No network feed or broad device permissions.

## 7. Design deliverables and approval checkpoints

**Sienna to supply:** Original, Pink and Green concept comparisons for (A) GBA 3:2 / GB 10:9, (B) 4:3/variable PS1, (C) tall dual-screen DS, plus compact portrait and landscape. Show image-content rectangles, bezel thickness, exact brand fallback, and selected material/stamp zones. Draw a no-extras stock CARTDECK that still feels premium.

**Cassia and Jonathan to approve:** device look/feel and responsive composition, *not merely maximised ratio numbers*. Sienna then authors the definitive engineering prompt.

**Elliot / Daniel technical spike before application PR:** confirm the available live core display-aspect signal; if unavailable, record which core/system ratio fallbacks are safe; prove GL `viewport` and touch/stylus mappings remain correct. Keep input/storage/saves/ROMs/package/build signing unchanged.

**Marcus device checks:** compare content pixel bounding boxes before/after for GBA, GB/GBC, PS1, DS, representative wide/portrait source, both orientations, smaller phone; verify PS1 geometry changes and DS two-screen touch, functional buttons, progress retention and in-place APK upgrade. Record PASS / FAIL / NOT TESTED, not “all emulators passed” from a single game.

## 8. Not approved in this candidate

- No current engineering change, no live skin/stamp editor and no claim of a finished visual mockup.
- Do not introduce fixed top spacing, a universal 3:2 aperture, fake stretching/cropping/zooming, new emulation/touch mappings or additional verification loops.
- PR #76 remains the working regression fix; this V08 design replaces its **oversized black physical aperture** with a content-fitted bezel and reintroduces the necessary CARTDECK physical personality.

**Current decision requested:** approve an adaptive content-fitted screen + thin bezel + reflowing protected brand seat as the P1-C2 art direction. System fallbacks and exact dp measurements remain engineering validation work after concept approval.
