# CARTDECK v0.5.2 — V1 Phone Shelf Correction (Sienna)

**Design-source candidate, not runtime integration.** Owner feedback from four real phone screenshots on 9 Nov 2026 drives this revision. Parent corrective Issue #40.

## Corrected intended hierarchy
- No tagline. Keep only a minimal brand/Library anchor.
- A single dominant **Cartridge Shelf**; Continue becomes one small last-played action, NOT a second 340dp-high row of spines.
- Add Cartridges prominent only when empty; compact when populated.
- One visible machined rail beneath the rack. All spines share a **fixed responsive physical size** and a common baseline, unaffected by font scaling; accessible names can grow/expand separately.
- Preserve the same Arc-Rail cartridge outline across all systems and editions, but the sideways face now includes original etched shoulder facets, inset side-rail grooves, a top crest seam and three small lower status segments.
- This is **not** a new manufacturer-inspired cartridge and never introduces per-system silhouettes.

## Screenshot-derived bugs to fix in the Kotlin implementation
- `CartridgeFront` in `Cartridge.kt` uses `game.customCoverUri` while pre-CARTDECK `LemuroidGameImage` used `game.displayCoverUrl()`. That method returns custom if set, otherwise `coverFrontUrl`. Restore that behaviour with Coil loading/error fallback. No new artwork lookup.
- Current `ContentScale.Crop` cuts custom portrait cover art in the provided screenshot. Design default is **contain** with a matching matte, later opt-in crop.
- `CartridgeSpine` currently multiplies its 94×340dp chassis and ALL offsets by `fontScale`. Physical chassis should use an independent responsive scale factor and keep title/badge clipping within their token-defined windows.
- `HomeScreen` currently displays an unwanted string `cartdeck_home_intro` and both Continue and Cartridge Shelf as full spine rows. Remove intro from the UI, not replace it with a slogan. Simplify Continue.

## Shell motion / tactile selection
Tap -> material press (~70ms) -> selected spine lifts from common rail (~140ms) -> keyed physical rotation/face presentation (~190ms) -> settle (~95ms). The source item and detail overlay must be one composited visual with one selected game identity and no flash of a plain dialog. Reverse Back where stable; preserve restored shelf position and TalkBack focus. Reduced-motion -> immediate static front. No fake game loading delay.

## Asset version handoff
`cartridge/spine-base.svg`: updated **same outer path and same title/badge windows**, just additional machined decoration. `cartridge/shelf-rail.svg`: original common lower rail, can be implemented as responsive Compose canvas if stretching this SVG is inappropriate. `shelf-presentation-v052.json`: viewport sizing, art fallback precedence, animations, semantics and native UI constraints.

Do not merge this source-only pass as production-art lock without Cassia/owner signoff. Elliot handles runtime via focused Issue #40 P1-A1R-1 and -2. Daniel preserves input/core/save boundaries. Marcus validates exact upgraded APK on phone.
