# CARTDECK — ASSET-H1 Native Resource & Semantic Colour Foundation

**Engineering:** Elliot Shaw · **Product acceptance:** P0 accepted by Jonathan Prestwich for P1 progression · **Source review:** Sienna Vale / Cassia Rowan · **Architecture:** Daniel Mercer · **QA:** Marcus Reid  
**Tracking:** [ASSET-H1 #30](https://github.com/safetyblade/ROMMachine/issues/30) · [Cartridge family #27](https://github.com/safetyblade/ROMMachine/issues/27) · [P1-A #12](https://github.com/safetyblade/ROMMachine/issues/12)

## Implemented scope (resource-only, no visual rollout)

- Generator: `scripts/generate_cartdeck_native_assets.py`
- Typed, immutable Compose colour model (unreferenced by current screens): `lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/shared/compose/ui/CartdeckEditionTokens.kt`
- Gradle task: `:lemuroid-app:generateCartdeckNativeAssets`. Hooks `merge*Resources` for ordinary Android builds and emits Android `res` source set at `lemuroid-app/build/generated/cartdeck-native/res/`. Python 3 is required; override executable with `-Pcartdeck.assets.python=python` when needed.
- CI: `.github/workflows/cartdeck-v04-assets.yml` and `scripts/ci/test_cartdeck_native_assets.py`, with source-to-native JSON traceability artifact on success/failure.
- No current native screen references these assets or uses `resolveCartdeckEdition`. **The current Material dynamic-colour theme is unchanged.** This is not a visual app feature release or game QA pass.

## Canonical sources and precedence

| Contract | Canonical inputs | Native handling |
| --- | --- | --- |
| Brand and small/status icon | `design/cartdeck-v03/brand/` | **Reuse** existing packaged launcher, monochrome notification and TV banner; don't duplicate canonical mark in ASSET-H1 |
| D-pad | `design/cartdeck-v03/controls/dpad-master.svg` | Export cosmetic Original/Pink vector. Same ring-free path and four shapes, untouched PadKit hit targets |
| Console hardware | `design/cartdeck-v04/shell/`, `controls/`, `boot/`, `navigation/` | Per-edition vectors; no game renderer or input binding integration here |
| Cartridge family | `design/cartdeck-v05/cartridge-family.json`, six `cartridge/` SVGs | Source of truth for spine/front/badge/fallback. **Supersedes** old v0.4 shelf/book/cover SVGs. Geometry shared across GB/GBC/GBA/PS1 |
| Skins | `design/cartdeck-v03/editions/{original,pink}.json` | Typed `CartdeckEditionTokens`, generated `values/cartdeck_edition_colors.xml`; Green/Mono NOT generated for distribution |
| Local game cover | `CustomCoverManager` and `GameInteractor` (existing) | Not changed. New art window 40,113,240×200, radius 8 in 320×390 cover; clipping to be handled by P1-A runtime |

## Accessibility and editable palette contract

Nine source tokens are immutable design inputs: `chassis, secondary, bezel, glass, button, accent, onChassis, onButton, focus`.

ASSET-H1 validates text contrast `onChassis/chassis` and `onButton/button` at **4.5:1** minimum. UI focus has a **3:1** target against the actual material on which drawn. The Pink `focus=#FFF3F7` does not reach 3:1 over its light `chassis=#F2A9C9`. Do **not** change canonical Sienna's Pink values or hide the issue. Generate **derived** `focusOnChassis` (Pink uses `onChassis` dark) and `focusOnSecondary` (Pink keeps light focus where contrast passes) for future P1 consumers. Accessibility precedence when custom editor arrives: high contrast overrides material preferences; geometric and touch input models stay locked.

Android VectorDrawable previews are compiled as drawables but never tinted over user artwork. A/B labels and GB/GBC/GBA badges remain runtime text and are not baked into native SVG vectors. Art clip/crop and pressed/focus states are work for P1-A/B/C, not an ASSET-H1 implementation claim.

## Reproducible build and output

Pure asset generation from repo root:

```bash
python3 scripts/generate_cartdeck_native_assets.py --output lemuroid-app/build/generated/cartdeck-native/res
python3 -m unittest discover scripts/ci -p test_cartdeck_native_assets.py -v
./gradlew :lemuroid-app:assembleFreeBundleDebug --no-daemon --no-configuration-cache
```

Generated Android source set:

- `drawable-nodpi/cartdeck_h1_original_*.xml`
- `drawable-nodpi/cartdeck_h1_pink_*.xml`
- `values/cartdeck_edition_colors.xml`
- Adjacent evidence outside `res`: `native-assets-manifest.json` with canonical master SHA-256, native resource SHA-256, geometry SHA-256 and edition manifest SHA-256.

Every asset generator rerun must produce byte-for-byte identical results, and Original/Pink native pathData must match for each master. No images/assets/ROMs downloaded at build time. No generated files committed alongside canonical source; source changes are validated in CI and built through a Gradle task.

## P1 follow-through and acceptance

- **Sienna / Cassia:** lock final v0.5 Arc-Rail appearance before merging a live cartridge UI. The current v0.5 JSON has `state=design-candidate-awaiting-asset-lock`, despite the merge of PR #29. Review art-window placement, identical silhouettes, badge socket, fallback art and Pink/Original snapshots.
- **Daniel:** audit Gradle generated source-set integration and D-pad/PadKit boundary; verify no package/signature/SAF/library/renderer/state changes.
- **Elliot:** CI vector/source consistency, Android resource compile, source SHA and APK checksum; run actual Android screenshots only when a distinct P1-A/B runtime PR uses assets.
- **Marcus:** ASSET-H1 has no gameplay/input behaviour to mark PASS. Perform per-device P1 runtime regression at the first feature slice.

**Release hold:** This is P1 preparation authorised by owner P0 acceptance, not proof that #9 independent matrix, #16 per-core licensing, #17 production signing or #18 16KB native runtime have passed. P1 runtime rollout follows separate explicit PRs. 
