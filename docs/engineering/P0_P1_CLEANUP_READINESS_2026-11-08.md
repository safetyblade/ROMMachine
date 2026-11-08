# CARTDECK — P0/P1 Cleanup & Skin Integration Readiness

**Audit:** 8 November 2026 · **Engineering:** Elliot Shaw · **Review:** Cassia Rowan / Sienna Vale / Daniel Mercer / Marcus Reid  
**Read-only source:** `safetyblade/ROMMachine` `master` commit `41d06cfd0b95b6bdb361c6fdf8bd34767e495fa2`.  
**Controlling specification:** CARTDECK Roadmap v1.2, [Drive roadmap](https://docs.google.com/document/d/1wlURBkCoASXTAOFbXrNQWzODEbnVuMJBUz4l68Vac4Q/edit), [UX spec](https://docs.google.com/document/d/1qpLQiUYQ9bAazeZI8T4zZWOo8A3p5Ffux3CO35Abp9k/edit), [engineering Issue #23](https://github.com/safetyblade/ROMMachine/issues/23).  
**Document class:** connector-audited implementation plan, NOT an APK/device test, code refactor or P0 gate approval.

## Decision and evidence reconciliation

**Outcome: DESIGN-SOURCE READY / P1 RUNTIME MERGE HOLD.** Existing app is a functioning Lemuroid-derived emulator; preserved P0 owner evidence covers one played game and retained progress, plus owner confirmation an Android update installed correctly. Independent per-system GB/GBC/GBA, SRAM vs manual-state, controller, RTC and upgrade-save evidence remains open in [#9](https://github.com/safetyblade/ROMMachine/issues/9). Cassia's formal P0 decision is CONDITIONAL; Daniel's [#16](https://github.com/safetyblade/ROMMachine/issues/16), [#17](https://github.com/safetyblade/ROMMachine/issues/17), [#18](https://github.com/safetyblade/ROMMachine/issues/18) remain independent distribution/signing/native gates. No production release.

Merged PR history is **not** equivalent to accepted native runtime: [#5](https://github.com/safetyblade/ROMMachine/pull/5) P0 build; [#6](https://github.com/safetyblade/ROMMachine/pull/6) v0.3 design; [#8](https://github.com/safetyblade/ROMMachine/pull/8) P0 phone channel; [#10](https://github.com/safetyblade/ROMMachine/pull/10) corrected v0.3.1 D-pad; [#11](https://github.com/safetyblade/ROMMachine/pull/11) visible Android identity; [#19](https://github.com/safetyblade/ROMMachine/pull/19) screenshot-based source mapping; [#20](https://github.com/safetyblade/ROMMachine/pull/20) locales; [#21](https://github.com/safetyblade/ROMMachine/pull/21) optional standalone-preview app ID; [#22](https://github.com/safetyblade/ROMMachine/pull/22) v0.4 DESIGN-C asset catalog. The v0.4 README explicitly says design assets are NOT wired into Android UI.

## KEEP / CHANGE / REMOVE / VERIFY register

| Classification | Component or evidence | Decision and reason |
| --- | --- | --- |
| KEEP | LibretroDroid, pinned `lemuroid-cores` native submodule, Gambatte/mGBA, core launcher | Emulation baseline, no unreviewed core/ABI updates; per-core licensing separately #16. |
| KEEP | `GameInteractor.onGamePlay`, `GameLauncher`, `MobileGameScreen.createRetroView`, `RetroGameView.viewport` | Game startup and original pixels. New shell wraps, never replaces renderer. |
| KEEP | PadKit `ControlCross`/`ControlFaceButtons`, device controller configs, `GB.kt`/`GBA.kt`, touch hit regions | D-pad/face-button **appearance** may change; diagonals, simultaneous A+B, L/R, menu and key IDs must not. |
| KEEP | `HomeViewModel`, library database `Game.id`, `StorageFrameworkPickerLauncher.pickFolder`, existing scanner/permissions | Preserve ROM grants, discovery, saves, favourites and loading. A cosmetically hidden notification permission must not break `GameInteractor.ensureNotificationsPermissionAvailable`. |
| KEEP | `CustomCoverManager`, imported local cover URLs, `GameInteractor.onSetCustomCoverUri` / `onClearCustomCoverUri` | Shelf cover editor must reuse the existing app-private copy path; no new copyright scraper or ROM/metadata changes. |
| KEEP | `com.swordfish.lemuroid.debug` old P0 install, v1015/v1016 archive, original test signing SHA, historic saves | Existing user data protected; no ID/signature migration in visual cleanup. |
| KEEP SEPARATE | `dev.prestwich.cartdeck.debug` standalone preview opt-in ID from #21 | Different Android private-data sandbox. CI artifact pass does **not** migrate old saves or prove device install. Production key separate #17. |
| KEEP | `COPYING`, `THIRD_PARTY_NOTICES.md`, upstream copyright, Kotlin packages/internal `lemuroid_*` resource identifiers, `lemuroid:` deep links | Attribution, internal contracts and shortcuts; rename only after dependency and data contract audit. |
| KEEP / REUSE | Sienna canonical v0.3 branding/controls and v0.4 universal source, editable edition JSONs | One product DNA; never duplicate SVG master per colour or use artwork as input geometry. |
| CHANGE — P1-A | Compose `HomeScreen`, `LemuroidGameCard`, `MainTopBar`, nav destinations | Add compact permissions, Continue, sideways spine shelf, cover/detail, explicit Open/Resume, local artwork editor. Preserve search/systems/favourites. |
| CHANGE — P1-A/C | `LemuroidTheme.kt` dynamic Android wallpaper colours and legacy Material colour constants | Map Original/Pink manifests to semantic Compose colours. Do not accidentally ship unapproved Green/Mono. |
| CHANGE — P1-B | Grey circular native controls in `LemuroidCrossForeground.kt`, `LemuroidControlBackground.kt`, `LemuroidPadTheme.kt` | Use corrected ring-free v0.3.1 cross/approved A/B, with visual press/focus feedback, no PadKit gesture remapping. |
| CHANGE — P1-B/C | `MobileGameScreen.kt`, `GameScreenLayout.kt` visual constraints only | One adaptive portrait/landscape CARTDECK chassis, bezel and 10:9 GB/GBC, 3:2 GBA contained viewport. PS1/other systems reuse same identity and appropriate existing controls, not different-console skin. |
| CHANGE — P1-D | `GameMenuHomeScreen.kt`, progressive `SettingsScreen.kt`, TV focus/branding | Nonblocking boot and clearer Resume/Save/Load/Display hierarchy; preserve commands, advanced/BIOS/core menus and controller-first TV. |
| VERIFY before removal | `ic_lemuroid_tiny.xml`, `lemuroid_launcher*.xml/png`, `lemuroid_tv_channel.png`, `ic_banner.png`, legacy Fastlane screenshots | Files remain in source alongside CARTDECK icon/banner. Manifest references new CARTDECK assets; no proof unused for notifications, old API/TV, shortcuts, alternate flavours. Reference and APK resource audits required before deletion. |
| VERIFY before simplification | Home notification/mic/DS cards, bottom navigation, Save Sync, advanced settings | Some are conditional functional affordances. Reduce default friction but do NOT simply remove required permission/recovery/BIOS/input routes. |
| NO DELETE | Android native libraries, old save files, ROM index metadata, custom covers, registered URI grants | Not housekeeping artifacts. |
| NO CHANGE IN THIS TASK | `applicationId`, signature, release workflow, save formats, ROM layout, `Game.id`, frame pixels | Architecture and migration #16–#18/#7. |

## Consolidated Sienna asset inventory — master is authoritative

**Canonical v0.3 / v0.3.1** `design/cartdeck-v03/`:
- Brand (three editable masters): `brand/cslot-master.svg`, `brand/cartdeck-wordmark-master.svg`, `brand/logo-lockup-master.svg`.
- Controls (nine editable graphics): `controls/dpad-master.svg` (PR #10 approved *candidate* chamfered cross / short bars / **small** hub; no circular mounting disc), `action-a-master.svg`, `action-b-master.svg`, four separate `arrow-{up,down,left,right}.svg`, `glyph-a.svg`, `glyph-b.svg`. Arrow art is optional and separate from the default D-pad.
- Palettes: `editions/original.json` Original graphite/teal and `pink.json` Pink blush/berry **approved direction**, exact rendered asset lock still needs Sienna/Cassia/Jonathan. `mono.json` accessibility reference; `green.json` proof of edition interchange, NOT shippable.
- Token contract: `chassis`, `secondary`, `bezel`, `accent`, `onChassis`, `button`, `onButton`, `focus`, `glass`; derived `secondaryHi`, `accentHi`, `accentLo`. Identity, system/viewport, controls and hit-zone guardrails invariant.

**Additive v0.4** `design/cartdeck-v04/` (23 new SVG masters; no duplicate brand/D-pad):
- `shell/`: `chassis-portrait.svg`, `chassis-landscape.svg`, `screen-bezel.svg`.
- `controls/`: `button-face.svg`, `four-face-cluster.svg`, `six-face-cluster.svg`, `shoulder.svg`, `trigger.svg`, `analog-base.svg`, `analog-cap.svg`, `start-select-pill.svg`, `stylus.svg` (nine).
- `shelf/`: `book-spine.svg`, `cover-case.svg`, `cover-placeholder.svg`, `cartridge-edge.svg` (four).
- `boot/`: `slot-pulse.svg` (one). `navigation/`: `shelf.svg`, `cover.svg`, `insert.svg`, `edit-cover.svg`, `display.svg`, `pause.svg` (six).
- Contracts: `catalog.json`, `system-viewports.json`, `shelf-journey.json`, `SCREEN_CHANGE_MATRIX.md`, `P1_IMPLEMENTATION_SEQUENCE.md`.
- Generators/tests: `build_assets.py`, `export_android_vectors.py`, `test_assets.py`, `test_android_export.py`. These create **generated design previews**; the master tree contains no v0.4 output wired as `res/drawable` or as a Compose scene. Reviewed native VectorDrawable/Canvas conversion, layout constraints, focus/pressed/disabled and APK resource compile/screenshot tests remain required.

**Android already wired:** adaptive and fallback `cartdeck_launcher`, C-slot vector, monochrome icon, TV banner, product label. Existing `cartdeck_brand.xml` defines launcher colours only; no native Original/Pink runtime edition picker yet. Current runtime `LemuroidTheme.kt` still adopts dynamic Material wallpaper colours on Android 12+.

## Current screen → target implementation map

| Current surface/source | Target Original / Pink | Design assets / token dependencies | Delivery & invariant |
| --- | --- | --- | --- |
| Launcher, app label, TV banner / Manifest + drawable/mipmap | Existing CARTDECK C-slot remains; inspect notification small icon and locale overlays | v03 brand, native adaptive/TV vector | A0 safe audit; do not rename package or signing. |
| Home empty / `HomeScreen.kt`, `HomeViewModel.kt` | Single clear Add Cartridges, concise contextual notification/mic/DS warnings | C-slot, v04 shelf, `chassis/onChassis/accent` | P1-A1 #12; call existing picker, keep permissions available. |
| Scan/import progress / HomeViewModel + LibraryIndexScheduler | Compact progress/cancel/retry; no fake readiness | v04 insert, focus/error colours | P1-A1 #12; no scanner/SAF rewrite. |
| Home recent/favourites/discover / `HomeRow`, `LemuroidGameCard.kt` | Continue direct action + dense horizontal **sideways** book spines | v04 book-spine, v03 brand, `secondary/accent/focus` | P1-A2 #12; preserve list ordering IDs and recents. |
| Spine click / `MainActivity.kt` `onGameClick` | Select/show front cover instead of auto-launch; Back returns to same shelf focus | v04 cover-case, cartridge-edge, cover glyph | P1-A2 #12; `Game.id` stable; explicit Open/Resume uses existing `onGamePlay`. |
| Custom cover / GameInteractor + CustomCoverManager | Local art pick, crop/position/reset and original placeholder | v04 cover-placeholder, edit-cover | P1-A2 #12; no external scraping, do not overwrite existing artwork. |
| Systems, search, favourites and navigation / `MainNavigationRoutes` | Same destinations with matching shelf vocabulary | v04 shelf/nav glyphs, `focus/onChassis` | P1-A2 #12; cannot remove routes just for cleaner visuals. |
| GB/GBC game / `MobileGameScreen`, `GameScreenLayout` | Shared console shell, 10:9 contained 160×144 image | v04 portrait/landscape chassis/bezel, v03 D-pad/A/B, `bezel/glass/button` | P1-B1 #13; same AndroidView, PadKit/GL, A+B/diagonals. |
| GBA game / same | Same CARTDECK frame, contained 3:2 240×160 image, L/R reachable | shell/bezel, v04 shoulder, v03 controls | P1-B1 #13; no crop/stretch or pixel/saved-state change. |
| PS1/other games / PadKit PSX/PSXDualShock and other core config | **Same original handheld console identity**; 4/6-face, triggers, sticks or stylus when required | v04 universal controls, `system-viewports.json` | P1-B2 #13 separately; core-reported aspect contains image, no remapping. |
| Landscape/tablet / `GameScreenLayout` | Adaptive shell, bounded video and safe controls | landscape shell, bezel, focus/pressed | P1-B1; rotation safe, 48dp targets. |
| Gameplay options / MobileGameScreen + TouchControllerSettingsManager | Console Shell(default), Fullscreen, Floating; Original/Pink selectable | v03 edition JSONs, v04 layout assets | P1-C #14; change in place, no core reset/save change. |
| Pause/save/menu / `GameMenuHomeScreen.kt` | Resume / Save State / Load State / Controls / Display / Exit hierarchy | pause/display/nav icons, edition tokens | P1-D #15; preserve GameMenuContract, SRAM vs state distinction. |
| Boot/insert / current direct game launch | WAKE→SLOT→MARK→PLAY without delay, skip/mute/reduced motion | v04 slot-pulse, v03 C-slot | P1-D #15; no forced startup wait or game core replacement. |
| Settings/permissions / `SettingsScreen.kt` + routes | Progressive Appearance, Gameplay, Storage, Saves, System/Advanced | v04 display/nav, `focus/onChassis` | P1-C/D; keep advanced, BIOS, input, rescan paths. |
| Errors/crash/help / strings, crash UI | CARTDECK wording and recovery, real failure visibility | brand/focus/error colours | P1-D; no silent fallback or data erase. |
| Android TV / `MainTVActivity`, TV fragments/game view | C-slot TV identity, ten-foot focus; fullscreen play, no touch overlay | v03 brand, v04 nav/frame only in menus | P1-D; controller first, protect TV controls. |
| ROM folder/export/backup / SAF + settings | Kept unchanged in P1-A through P1-D | none required now | **P1-E #7, separate.** |

## CI, signing and packaging — verified classifications

1. **Working:** [v0.4 design catalog workflow](https://github.com/safetyblade/ROMMachine/actions/runs/42462257086) PASS on audited master. [Brand Contract](https://github.com/safetyblade/ROMMachine/actions/runs/42462239335) PASS on design PR #22; these are static source/generator checks, not Android gameplay.
2. **Working but separate identity:** standalone preview build [run 42461321501](https://github.com/safetyblade/ROMMachine/actions/runs/42461321501) PASS, source `ba838a363092f5f1e6df395c5c202127143ab121`, package `dev.prestwich.cartdeck.debug`, versionCode 2001, APK SHA256 `70ccf3e8f59ffc670ed40a273a6fdcfd15c6b4c9202a478c8db15b8119205576`, inherited Odyssey Debug certificate. No old-app data migration/physical install evidenced; new P1 source changes after that SHA not covered.
3. **Original test identity:** old P0 phone archive v1015/v1016 `com.swordfish.lemuroid.debug`; 1016 APK SHA256 `eb8b0b8892e75db57c8bbcb1986f7feb69cb7496b32e2a210c3ebaae6315f52d`. User confirmed Android in-place update installed as expected. Treat in-game SRAM and state preservation as independently unverified. Stable Drive [APK channel](https://drive.google.com/drive/folders/16Rqr4oMRURWS3jyn79I3HKv2rgc1Pco4) is controlled, not automated GitHub-to-Drive publishing.
4. **Checkstyle fails inherited ktlint:** `.github/workflows/checkstyle.yml` runs `./gradlew assembleDebug ktlintCheck`. Pre-CARTDECK source [run 42455387544](https://github.com/safetyblade/ROMMachine/actions/runs/42455387544) failed `:lemuroid-app:ktlintMainSourceSetCheck`; newer [run 42461830372](https://github.com/safetyblade/ROMMachine/actions/runs/42461830372) fails the *same task*. Do **not** call entire current failure newly introduced; obtain exact per-file ktlint report and compare to baseline before classifying any additions. Do not blanket-reformat all upstream source.
5. **Emulator CI false green:** [run 42458956177](https://github.com/safetyblade/ROMMachine/actions/runs/42458956177), job `128664924908`, says success, but log contains `adb shell am start -W ...` followed by `Status: timeout`. Script calls `/usr/bin/sh -c` per line; `adb ... | tee` can mask failing child status and a separate `set -eu` does not establish whole-script failure propagation. Prior [run 42458028774](https://github.com/safetyblade/ROMMachine/actions/runs/42458028774) genuinely timed out boot. These are **CI evidence defects**, not proof of native app crash or launch PASS. Fix with a single bash executable, `set -euo pipefail`, explicit `adb install` result, `am start` result and healthy PID/activity check with preserved logs; compare on hardware.
6. **Signature/licences:** `lemuroid-app/build.gradle.kts` has an inherited checked-in debug keystore and legacy release-signing path; not a secure prestwich.dev release identity. Do not re-sign P0 APKs. Default freeBundle packs out-of-scope native systems; #16 per-core bill of materials/licence exclusion and #17 release key/ID/migration remain open. Compressed native libraries and actual 16KB page-size runtime unresolved #18.
7. **Source build evidence != screenshot evidence:** latest full Android compile for the audited v0.4 `master` commit and native screenshots are **not established** by asset-generator CI. Existing older standalone build predates v0.4. Inspect exact SHA/build before issuing an APK for a P1 native slice.

## Genuinely safe immediate cleanup (no live UI/core/source code changes)

- Docs-only report and reconcile **source vs shipped** status in Issue #23, Roadmap v1.2 and Drive; correct old references saying merged PRs still draft and CI status unknown. Keep historic test evidence intact.
- Read-only icon/resource, locale, notification, TV, Fastlane and shortcut reference maps; identify **proven** dead legacy assets before proposing a deletion diff. Do not delete from pattern matching names.
- Add a separate narrow **CI-harness-only** draft PR under #18 to make emulator assertions fail closed and attach logcat/screenshot; keep Android runtime, game and save code untouched.
- Establish a machine-readable native-asset export/traceability manifest (v03/v04 master → semantic edition → generated native vector → consuming Compose component), static checks and non-runtime previews. Do not claim a generated SVG is packaged as a native drawable.
- Capture a reproducible **ktlint baseline report** for an inherited SHA and a matching current-SHA run, then fix only newly introduced violations in future P1 files. Existing upstream debt needs separate scope/approval.

## Ordered small PR recommendation — DO NOT merge runtime UI before P0/pilot gate

| Seq | PR scope | Files/bounds | Evidence required |
| --- | --- | --- | --- |
| 0 | **This audit** — documentation-only | `docs/engineering/P0_P1_CLEANUP_READINESS_2026-11-08.md`; no app files | Source refs, keep/change register, issue/Drive links; safe before gate |
| 1 | **CI-H1** — emulator fail-closed harness | Only `.github/workflows/p0-a-debug-apk.yml` + tested `scripts/ci/android_smoke.sh` | APK exact SHA, install/launch explicit exit, PID, logcat, emulator version; Daniel review |
| 2 | **ASSET-H1** — asset registry/native export proof, NO runtime wiring | `design/cartdeck-v03`, `design/cartdeck-v04` tests and generated resource contract | Original/Pink geometry/tokens, corrected D-pad, Android parser/compiled resources; Sienna+Daniel |
| 3 | **P1-A1** — brand/Home and permission UX | Home, theme, top/dock, empty/progress, Continue | Before/after real device screenshots, import permission tests, unchanged DB/launcher |
| 4 | **P1-A2** — spines + cover + editor | shelf/cover/detail state; existing GameInteractor, CustomCoverManager | Click opens cover not game, Open invokes existing launch; Back/focus/restoration, custom-cover migration unaffected |
| 5 | **P1-B1** — GB/GBC/GBA shell & angular controls | MobileGameScreen layout + visual PadKit layer | GB 10:9/GBA 3:2, D-pad diagonals, A+B, Start/Select/L/R, save/resume, no clipped game pixels |
| 6 | **P1-B2** — PS1 and other systems in *same shell* | v04 visual control profiles; no mappings | PSX/PSXDualShock real core output, 4/6-face/analog/shoulder reach, safe aspect and no manufacturer UI |
| 7 | **P1-C** — Console/Floating/Fullscreen + Original/Pink | preference/appearance boundary, same renderer | Mid-game mode/edition switch no core reset; fallback Original, safe pause and accessible control placement |
| 8 | **P1-D** — nonblocking boot, menu/TV polish | slot/boot, GameMenuHomeScreen, SettingsScreen, TV UI | Skip/mute/reduced-motion, state/menu intact, TV controller focus; same licensed-game regression |
| Separate | **P1-E #7** and production release architecture #16/#17/#18 | scoped storage, portable backup/restore, owner-held release identity | Explicit data-export test and secure key, legal/native-device approval, independent QA |

**Every native PR:** test APK + package/version/source commit + SHA256 + exact manifest/source-assets version + Android portrait/landscape/tablet/TV before/after screenshots as applicable + pass/fail/not-run GB/GBC/GBA input/saves and licensed fixtures + Sienna visual approval, Daniel change-boundary review, Marcus independent QA handoff, Cassia acceptance. Preserve fallback no-shell. No production/public release before legal, signing and 16KB gates.

**Release/governance hold:** P0 owner pilot observation is accepted; complete P0 and public release are NOT. The next authorised action is read-only/design/CI-harness preparation, then explicit Marcus/Daniel/Cassia P0 pilot review before runtime merge. Do not silently migrate app-private data to the separate standalone package.
