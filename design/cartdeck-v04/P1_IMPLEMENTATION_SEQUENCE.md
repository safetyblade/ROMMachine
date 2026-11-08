# CARTDECK — Engineering implementation handoff (Elliot Shaw)

**Connector-first audit complete:** Existing app is Compose + AndroidView Libretro renderer + PadKit radial control inputs. Do NOT rebuild emulator.

## P1-A1 — Identity and useful Home (Issue #12)
Verify merged brand PRs #11/#20, keep legal attribution in About. Keep `HomeViewModel` / `StorageFrameworkPickerLauncher` unchanged. Fixed CARTDECK semantic colours instead of Android dynamic wallpaper colours; branded top/bottom areas; compact empty state; Continue from existing recent list. Proof: actual phone screenshots.

## P1-A2 — Bookshelf / Cover / Customise (extend Issue #12)
Use `HomeScreen.kt`, `GamesScreen.kt`, `FavoritesScreen.kt`, `SearchScreen.kt`, `LemuroidGameCard.kt` and `GameInteractor.onGamePlay`. Add selectedGame navigation state (restored through orientation/process death) and a CoverDetail composable. Spine shelf uses tightly grouped sideways cartridge books (fixed spine widths, controlled focus and horizontal scroll). Selection **opens front cover, never launches**; explicitly tap Open or Resume for original launch. Cover customization integrates existing `CustomCoverManager`, not unlicensed online scraper; local image crop/position/reset, permission denied/cancel fallback. Keep game metadata, progress, favorite and ROM records stable. Cover detail and editor must work for missing art, long titles and user-licensed images.

## P1-B1 — Shared shell / controls (Issue #13)
`MobileGameScreen.kt` and `GameScreenLayout.kt`: keep same `createRetroView` AndroidView, `viewport` math, PadKit input owner, settings and Menu. Add edition-driven visual chassis/background/bezel and responsive viewport bounding frame. Use corrected cross from v03 with `ControlCross`; do not use decorated SVG for hit targets. Keep original A/B/X/Y, shoulders, analogs, Start/Select and menu key IDs. Test GB/GBC/GBA.

## P1-B2 — Any game in CARTDECK handheld (extend #13)
Reuse the exact same shell family for PSX/PSXDualShock and other supported systems. Default vertical handheld shell permitted for PS1, **no special PS1-themed console skin**. Compute displayed image contain bounds from runtime core game video geometry; fallback ratios only when runtime dimensions unavailable. For PS1 provide four-face + shoulder/trigger and optional dual analog within adaptive control workspace; if too cramped on short phones, offer screen-maximal/landscape/floating accessibility without losing required inputs. Any changes to PadKit mapping out-of-scope. Two-screen systems need a coherent multi-display layout, not destroyed frames.

## P1-C — Modes, editions, preferences (Issue #14)
Console Shell default phone/tablet, Fullscreen and Floating alternatives. Reuse `TouchControllerSettingsManager` size/margins/rotation; add appearance-only alpha/mode tokens. Create a player-facing colour editor according to `appearance-editor.json`: modify ONLY nine cosmetic edition fields, preview shelf/shell/4-face controls, enforce legibility and preserve user preference across upgrades. Custom colours must flow through reactive Compose drawing, not a one-tint PNG or a new emulator skin. Persist outside ROM/saves, change without resetting core. Derive native Compose colours from existing canonical v03 edition manifests, with v04 semantic geometry and Android-safe vector conversion; no duplicate brand or colour values. Original/Pink first, optional future Green/Mono only after Jonathan approves.

## P1-D — Boot / pause / settings / TV (Issue #15)
Nonblocking WAKE → SLOT → MARK → GAME; reduced motion/mute/skip. Structured Pause hierarchy but preserve existing `GameMenuHomeScreen` actions; refactor settings progressively without removing BIOS/input/core setup. Android TV remains controller-first fullscreen.

## Critical release gates
- #9 **OWNER OBSERVED PASS** for one played game with retained progress; independent GB/GBC/GBA save and upgrade matrix still open. No P1 runtime merge until Marcus/Cassia/Daniel approve explicit bounded gate.
- Debug signing continuity, current package ID and version monotonicity, storage / SAF grants, in-game SRAM and manual states MUST survive an in-place update. No signing/package rename here.
- Every slice should have visual screenshot matrix, Gradle/ktlint reports, direct device review and clear PASS/FAIL/NOT TESTED. PRs remain small; no protected ROM/BIOS/cover art in repo.
- Do not treat any HTML or SVG design demonstration as actual native implementation.
