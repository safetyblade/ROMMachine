# CARTDECK P1-C2B — C11 native hardware acceptance handoff

**Issue:** #74. **Design:** approved SVG sources from merged PR #80. **Engineering:** P1-C2B-1 PR #88 and the stacked P1-C2B-2 source PR. **Presentation gate:** C11. **Product owner:** Jonathan. **Independent QA:** Marcus.

## Engineering safety contract

- The only emulator renderer is the inherited AndroidView/GLRetroView. Standard 4:3, NDS expanded composite video and landscape overlay/non-overlay ConstraintSets are **not changed**.
- The entire manufacturing shell and opaque control deck are passive visual layers. `GlassSurface` is not used by gameplay PadContainer. PadKit and the current radial layouts still own all interactions, pressed feedback, input IDs, scaling, customisation preferences and haptics.
- Actual `onGloballyPositioned` bounds for D-pad, face groups, shoulders, Start, Select, analog, DS mic, screen/layout and Menu are reported without modifying touch modifiers' measured constraints or PadKit pointer behaviour. A missing/overlapping seat is **suppressed**, never moved to a fabricated coordinate.
- Existing `SecondaryButtonSelect(position = 2)` and `SecondaryButtonStart(position = 2)` in GBA, DS and PSXDualShock must remain unchanged. The parent input-angle definitions in `layouts/shared/Controls.kt` remain unchanged.
- Hardware source resources are derived from the 52 reviewed editable SVG masters by `scripts/generate_cartdeck_native_assets.py`. Three colour-resolved editions, one geometry source. Static SVG masks are delivered in Android raw resources, but the native painting exclusion is based on measured bounds, not static canvas reference geometry.
- All six glow zones provide raw mask + unlit + illuminated resource IDs. `CartdeckGlowFamily.enabledByDefault` is always false; no glow on, toggles, brightness or customisation UI until #75.
- No ROM/library migration, database changes, firmware bundling, core changes, package identity, signing, version code or save path edits.

## C11 single-build rule

**Do not generate experimental APKs for minor art/source corrections.** After the two PRs receive focused code and design review, invoke the existing combined ARM64 debug workflow **once at C11**, using the current verified package/signing identity and an increasing versionCode. Install **over** the existing APK. No uninstall and no data reset.

CI at C11: asset source test, new `CartdeckHardwareGeometryTest` and `CartdeckHardwareSeatPolicyTest`, all existing Kotlin/style tests, relevant device-target build, resource generation, 16KB native packaging audit, APK identity/signing/version, and artifact SHA.

## Required visual and interaction evidence

Capture baseline and integrated Android screenshots with identical device/orientation/touch scale/margins/rotation. Recommended evidence names:
- `c11-standard-ab-before-after.png` (4:3 single viewport, GBA)
- `c11-standard-abxy-before-after.png` (4:3 with four face buttons)
- `c11-ps1-dual-analog-before-after.png` (both analog rings where configured)
- `c11-nds-dual-screen-before-after.png` (same dual-screen height, stylus target)
- `c11-original-pink-green.png` (same geometry, three material palettes)
- `c11-narrow-portrait-and-landscape.png` (safe insets, both overlay modes)
- `c11-start-select-rectangles.json` (actual functional root bounds BEFORE/AFTER, standard/GBA, NDS and PS1, same device and settings)

QA interactions: D-pad diagonals; simultaneous A+B; L/R and L1/R1/L2/R2 where exposed; PS1 left/right analog + press; NDS mic and layout function, stylus at both screen regions; Start and Select intentional press vs accidental lower thumb-rest touches; Menu/Pause; existing user touch customisations; game launch, save, force-stop, reopen, install-over version upgrade and retained progress. Separate automated rendering/source checks from actual hardware and gameplay observations.

Acceptance requires no decorative seam, seat, glow mask, C-slot or wordmark over active game pixels, labels or touch targets; 4dp visual clearance around functional controls and 8dp around protected branding. If the image cannot fit without a collision, **omit the decorative element**, never move the existing working controls.

## Review routing

Daniel: verifies root/viewport/input coordinate spaces, Compose z-order, PadKit pointer invariance, 16KB native packaging and cross-edition resource status.

Sienna/Cassia: accepts physical joining, 7dp surround, protected brand, DS auxiliary strip and no-glow default across Original/Pink/Green.

Marcus: independent device tests and screenshot/rectangle evidence; he alone signs gameplay/release gates.

**Status at source handoff:** engineering source written; unit tests and screenshots **not yet executed/captured**. C11 APK intentionally deferred. Do not mark device acceptance or public release without evidence.
