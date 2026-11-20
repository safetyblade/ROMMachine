# UNI-SIGNATURE-01 — Cassia Rowan approved global brand evolution

Owner-confirmed placement: additive UNI work, not a substitute for UNI-D/UNI-E/UNI-F or any active emulator, PC, PERF or GEM lane. This branch records the approved brief; assets and application-wide implementation are **not** delivered by UNI-D3.

## Global CARTDECK brand
- Midnight base: `#13131A`
- Neon pink accent: `#FF4FA3`
- Electric cyan accent: `#19F5DA`
- Update the base app icon, launch/splash/loading areas, and global brand touchpoints (not merely a selectable skin).
- Preserve approved CARTDECK logo geometry and the underlying product identity; do not relabel external providers or alter emulator identity.
- Do not overwrite Original, Pink, Green, other custom appearances or GEM and CARTMOD identity.

## Appearance
- **SIGNATURE** is an additional, independent, premium appearance with the same available device hardware, material and lighting controls as every other theme.
- Preserve selection, customization, owner saves, historical editions and rollback.
- New app-global palette and SIGNATURE appearance are distinct features and require separate regression checks.

## Acceptance
- App icon/launch presentation and global branding on handheld, landscape, tablet and TV.
- SIGNATURE present as selectable appearance without replacing Original.
- Verify no appearance-specific hardware forks and no regression of UNI-C/UNI-D control positions.
- Brand master reference: Cassia Rowan – SIGNATURE – MASTER (approved externally). The source asset has not yet been added to this repository; implementation cannot assume its artwork or geometry is embedded.

## Roadmap
- Record under UNI parallel brand/appearance workstream and in the GC presentation acceptance matrix.
- UNI-D3/D4 remain responsible for GripArc and adaptive controller geometry. UNI-E validates all emulator hosts. UNI-F validates global presentation and rollback.

## 20 November 2026 — implementation and source provenance

- Owner-provided video already exists on master at `lemuroid-app/src/main/res/raw/02_Loading2.mp4` (Git blob `5d1e9637...`, 6,773,125 bytes); D4 branch references that very blob at Android-compliant lowercase resource path `res/raw/cartdeck_signature_startup.mp4`. No duplicate upload or re-encoding.
- Cold startup video centres and aspect-preserving crops into the full display; handheld and TV share playback. Muted; skip/back and decoder error exits; maximum 12.5-second safeguard; only cold entry.
- C-slot launcher icon retains approved mark geometry; new colours apply globally to app icon, brand highlights and TV banner. Signature remains additive independent custom appearance.
- Confirm video first-frame and centre-C framing on physical phone/tablet/TV, launcher mask/safe zone and monochrome, existing themes, navigation and gameplay/resume before merge.
