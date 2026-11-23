# GC-01 — Image Quality and SmartVisual: approved Stage 1 / 1.5 and post-GC plan

**Decision:** Owner approved 23 November 2026. **Execution lane:** active draft [GC PR #368](https://github.com/safetyblade/ROMMachine/pull/368), under [GC #262](https://github.com/safetyblade/ROMMachine/issues/262). **Engineer:** Codex/Elliot. **Acceptance:** tests and a new exact-head APK first; owner/Marcus physical QA separately. This document adds to the full GC A–H, SafeSave, GEM, Junior, SIGNATURE and control obligations; it supersedes only conflicting presentation of HD/scaling controls.

**Product goal:** Players choose a game, not an emulator graphics configuration. Deliver a **unified user experience** while safely admitting that native renderer implementations differ. Do not ship a new per-engine settings dashboard, universal 2×/3× control, or graphics option that does not genuinely affect the chosen host.

## 0. Source baseline and no-repeat rules

The verified pre-addition GC head `9317b47a7d2032c4a95b073d8d5a5258dbc252ee` built debug APK versionCode `91364396`, package `dev.prestwich.cartdeck.debug`; Kotlin, Room, GEM, 804 Android tests, and the connected UI/APK gate passed. Re-read the actual branch head at execution time; **documentation commits after this head require new CI evidence**. No approval to merge or release.

**Already implemented:** Legacy Libretro ambient-color `Immersive Mode` has been removed from mobile/TV settings and force-disabled in `GLRetroViewData`; **do not** delete fullscreen gameplay, aspect controls, physical shell appearance or legacy preference read compatibility. A source regression test covers TV. The existing `DisplaySettingsStore` persists global values plus per-cartridge overrides for `hd`, `quality`, `filter` and alignment. `ShaderChooser` currently uses CUT/CUT2/CUT3 and CRT/LCD/Smooth/Sharp/Auto. Host choice distinguishes Libretro shaders from native renderer-owned graphics.

## 1. GC Stage 1 — one Image Quality choice (APPROVED; code not yet delivered)

### Player presentation

Make one compact **Image Quality** choice, accessible from the single shared Display/Graphics route in Settings, the cartridge's setup, and the universal game menu:

- **Automatic (recommended; default)** — choose an appropriate, validated presentation for this console/game and renderer. The default must not force costly enhancement.
- **Original** — preserve the verified unenhanced image path. Do not call a smoothing or CRT shader “original” without proving its pipeline output.
- **Enhanced** — enable the existing supported image enhancement path with capability-safe settings. Do not imply a higher native 3D resolution.

A separate optional **Visual Style** control may include Auto, Sharp Pixels, Smooth, CRT and LCD **only where the current host genuinely supports those outputs**; this is optional presentation, not five required startup questions. A small **Advanced** pane may retain power-user shader quality and diagnostic settings. Hide Low/Medium/High from the primary screen. No new permanent setting in each console/engine menu.

**Separate concepts clearly:** Screen Fit / Native aspect and Hardware / Minimal / Off surround determine display geometry. Image Quality determines appearance. Internal 3D render resolution and texture scaling are *not* synonyms for Image Quality. Never couple changing a quality choice to stretching, cropping, moving game controls, changing game speed, resetting saves, or switching cores.

### Migration, applicability and fallback

- Prefer a thin shared UI/resolver over existing stable `DisplaySettingsStore` values; add a versioned optional field only if genuinely necessary. All existing per-cartridge/global preferences and customized filters must survive install-over.
- Preserve the actual previous result for players with explicit `HD` or CRT/LCD/Sharp/Smooth choices; when the new three-state UI cannot faithfully represent a legacy combination, retain it as a visible “Custom / existing preference” under Visual Style or Advanced until explicitly replaced. Do **not** silently map every saved filter to Automatic.
- No unprompted mass migration, overwrite of player overrides, or forced game restart. `Use Global Default` and per-cartridge override/reset continue to work.
- Resolve selected backend **before** presenting available effects. On a native host without a proven common shader bridge, preserve its original renderer, show honest availability and do not toggle an inert global “Enhanced” switch.
- Apply a safe renderer pipeline with a genuine pass-through/known-good fallback when shader support is absent or compilation/runtime fails. An unsupported requested mode must not crash or replace the last working setting.
- Changing quality can apply at the next game launch if that is how the renderer is constructed; indicate this plainly.

### Stage 1 source tests

Test Android shared menu, Settings, cartridge override and Android TV focus where relevant; old `hd/filter/quality` combinations and preferences across upgrade; accurate labels/effective mode/unsupported host; Reset/Use Global; fullscreen/native aspect and surrounds unchanged; source-only no-codec/no-ROM/no-SafeSave/no-cart-ID regression. Implement the real presentation and persistence code, not a mock screen.

## 2. GC Stage 1.5 — SmartVisual common quality resolver (APPROVED; code not yet delivered)

Create a **small testable internal SmartVisual policy/resolver** behind the Image Quality choice, reusing the shaders already shipped. No new AI service, remote model, new image engine or emulator upgrade.

- **Console-aware output:** classify broad, source-proven presentation types (low-resolution pixel art, handheld LCD, 2D/3D mixed and 3D) conservatively, with established `SystemID` defaults and verified real shader availability; do not pretend static metadata can infer each game's frame content. Preserve manual overrides.
- **Automatic:** use suitable current CRT/LCD/Sharp/Smooth/pass-through defaults and a conservative existing HD filter only when validated to improve that path. Where no reliable choice is proven, return current safe default rather than invent enhancements.
- **Enhanced:** select from CUT/CUT2/CUT3 based on supported GLSL/device limits and validated safe tiers. Where GLSL is below the supported threshold, the existing lower-quality fallback remains. No automatic GPU clock manipulation or per-engine override.
- **Image integrity:** avoid halos, shimmering, unwanted smoothing, oversharpening, CRT artifacts on LCD systems, 2D text blurring, washed-out colour or distortion. Do not apply a 3D-looking preset universally to 2D systems.
- **Deterministic:** a chosen renderer/system/profile produces the same policy result when loaded again; implement unit tests covering all supported system IDs, override precedence and unsupported hosts. Preserve game/cartridge identifiers.
- **Performance:** prohibit runtime per-frame shader switching or thermal/FPS-feedback tuning in Stage 1.5. Only use static capability/validated-cost checks and conservative default/caps. If unsupported, degrade deterministically to an existing proven path and record diagnostic detail under Advanced only.

Use representative programmatic image/render checks for GB/GBC, GBA, SNES, PS1 2D/3D, PSP and DS plus one native-host non-regression. Validate no aspect or touch hit-target change, shading fallback, correct alpha, viewport sizing, shader support and save/reopen preference persistence. Real-device visual correctness remains a separate owner acceptance check.

### Stage 1.5 exit criteria

1. One shared Image Quality choice produces **actual** changes on supported Libretro paths, not just toggled labels.
2. Safe, honest native-host handling without emulator-specific user settings.
3. Automatic/Original/Enhanced, saved override inheritance, legacy preference migration and rollback are automated-test green.
4. No regression to original aspect, gaming controls, input latency, frame pacing, save data, ROM library or decoder.
5. Signed ARM64 exact-head build passes all mandatory gates; owner device screenshots are **NOT TESTED** until supplied.

## 3. Existing GC A–H work — must not be reclassified as “physical QA” to avoid code

The 23 Nov Codex checkpoint delivered PSP PivotArc direction control + both-hand GripArc fit, DeSmuME Standard Hold Tray mapping, SafeSave destination recheck (changed-byte/new-file/symlink regression), SIGNATURE final-frame/surface loss correction, Homebrew and 88,246-record GEM integrity. **It did not declare GC A–H engineering complete.**

| Lane | Engineering still outstanding (inspect current source and implement unblocked work) |
| --- | --- |
| A Menus | Host-specific shared-menu ownership, recovery, unpause and Back paths. Closed #303/#308 and earlier menu successes are **regression baselines**, not authorization to rebuild them. |
| B Display | Existing DS/3DS, tablet, external-display safe layout and automated regressions; add **Stage 1 / 1.5 SmartVisual** here without renderer geometry rewrite. |
| C Controllers / TV | Prove P1/P2 actual guest dispatch, distinct controllers, reconnect and automatic remote/gamepad launch→menu→shelf paths in available tests. |
| D Wii / 3DS | Finish the verified app-side Wii pointer/motion and 3DS tilt routing; exact native motion bridge owned by PERF #365, not a GC engine fork. |
| E GripArc / PivotArc | Complete WS, WSC, NDS, 3DS console layouts; compact combined controller geometry, native button retention, four G controls, primary PSP selector/hold-release and two-hand/one-hand source tests. #135 physical FAIL remains open. |
| F Modern Function | Exact console+title+edition profiles, input dispatch, unambiguous authenticity/verified labels and safe fallback; do not declare a 16-title research table to be executable mappings. |
| G Xbox 360 continuity | XUID-bound player save-only mapping, verified native storage adapters, safe resume/restore, or specific external host dependency. |
| H Integration | Exact-source build, install-over/data migration and shared shell/display/menu/TV regressions, source + unit + rendered evidence. |

**Cross-cutting:** SafeSave PSP snapshot-to-Save-Centre integration, bounded native adapters, profile/Junior isolation and PIN recovery, manager-authorised transfers, GEM active-player/ratings/export scope. SIGNATURE #360 remains owner physical FAIL until tested. Do not force unavailable native inputs or automatic “fully backed up” labels.

## 4. Small, low-risk backlog candidates for the NEXT Codex execution

These are **candidate quick wins, not automatic issue closures**. Implement only when source audit confirms a missing behavior and the work is bounded; prefer tests + small fixes before deeper host dependencies:

| Priority | Source issue | Safe bounded unit of work (do not claim the entire issue closed) |
| --- | --- | --- |
| Q1 | GC #262 + closed DISPLAY #162/#167 | Make displayed “HD scaling” wording truthful and replace contradictory/duplicated settings paths via Stage 1. Verify renderer-host capability, legacy reset and no stretch/crop/override regressions. |
| Q2 | GC #262 + #194 | Add tests for original/game-specific cartridge preview, exact-ID explicit link, safe fallback on missing installed ROM, offline GEM and changed player rating while protecting 88,246 catalogue records. Avoid new database work. |
| Q3 | #290 | Add Save Centre PSP snapshot listing/entry, accurate archive coverage and profile/auth fail-closed unit tests where current identity model supports it; no synthetic universal whole-console backup. |
| Q4 | #135 | Fix one measurable cramped portrait/landscape combined-deck fixture, then test 48dp and original button/action preservation with left/right placements. This does NOT close physical geometry failure. |
| Q5 | #177 | Add source-only stick/trigger dedup, neutral-release, right-stick/deadzone and reconnect regression fixtures. **Do not** silently change controller deadzones or promise a gameplay quality fix without evidence. |
| Q6 | #146 / #147 | Add small, isolated guard tests for read-only SAF folders, cancel/low-storage on managed import, ZIP path traversal and malformed multi-disc members where existing staging already supports it. No new 7z/native archive stack in this run. |
| Q7 | #360 | Add repeatable startup clip surface-loss/cold-start/rotation/skip/audio-focus state-machine tests, keeping the approved `02_Loading2.mp4` and source audio. Not a device audio PASS. |

Closed #303 (navigation), #308 (TV), #162/#167 (display foundation), and #288/#289 (Junior/profile design) contain retained requirements but should not be reopened or reimplemented from scratch. #365 is PERF/native, not a new GC engine branch. PERF #295 FIFA deformation remains a deferred bug, not an easy win. CARTMOD maker beta remains separately scheduled.

## 5. Post-GC roadmap — DOCUMENT ONLY; not GC acceptance work

**Later Stage 2 — PERF renderer capability adapters:** Prove each actual host's internal-resolution and texture-scaling contract, allowed values and save-safe behavior: PPSSPP `ppsspp_internal_resolution` and texture scaling, Citra resolution factor, Flycast `reicast_internal_resolution`, Dolphin `dolphin_efb_scale`, Play/ARMSX2 `play_res_multi` only where matching engine supports it. No global promise of 2×/3× on Xbox, X360, Wii U, PS3 or unsupported native paths. Expose improvements through the same Automatic/Enhanced user experience when genuinely supported; advanced diagnostics only for raw multipliers. PERF owns host/engine pin changes.

**Later Stage 3 — performance-adaptive SmartVisual:** Investigate real renderer timings, memory and thermal headroom, conservative hysteresis, fallback, a player's override and reproducible frame-quality/performance evidence. No runtime adaptation shipped until measured across devices and approved.

**Later optional work — curated visual profiles:** Only if validated stable, consider user-selectable specialist style/CRT presets under Advanced and a per-game visual preview; avoid a settings explosion or external services. This is not a condition of declaring GC engineering ready.

**Separate:** Remaining PC emulation and Xbox/PS3/native engine performance belong to PERF; CARTMOD maker beta follows the agreed creator roadmap. No new fourth top-level lane.

## 6. Codex continuation contract

Read this file together with the original [GC owner handoff](GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md), [GC integration acceptance](../GC-INTEGRATION-ACCEPTANCE.md), GC issue #262 and PR #368. In the current active draft branch, first verify live CI/head and repair any failures, then **implement** Stage 1 + 1.5 (code/UI/policy/tests), tackle available Q1–Q7 bounded wins, and work through all genuinely unblocked GC A–H engineering. Source tasks outrank repeated owner QA descriptions.

Build signed exact-head ARM64 debug APK, report its actual artifact URL, gate results, unresolved source/host dependencies and a **separate deferred physical owner checklist**. Do not stop at one successful build; do not claim physical passes, merge PR #368, release, close issues automatically, or start post-GC Stage 2/3 in this run.
