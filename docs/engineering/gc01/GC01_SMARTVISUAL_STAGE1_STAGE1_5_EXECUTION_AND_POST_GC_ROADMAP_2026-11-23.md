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

## 4. Owner-confirmed passes and remaining bounded work — 23 Nov 2026

**Owner testing on installed APK `91364396`, exact-source `9317b47a` (23 Nov):** **PASS** test IDs **1, 2, 4, 5, 6, 7, 11**: install-over data continuity; shelf sections/no phantom gaps; SafeSave header/dock; supported backup→restore→reopen; offline GEM ratings/cartridge link; curated Homebrew/My Sources; SIGNATURE intro framing, audible audio and Skip. **#183 shelf gap and #360 SIGNATURE intro closed at explicit owner direction** as accepted narrow defects; they are no longer Codex implementation quick wins. Retain these paths as normal automated non-regressions; do not rewrite their working UX solely to generate activity.

**Still NOT TESTED by owner on this APK:** ID **3** (retired Immersive option, fullscreen/display), **8** (Standard+GripArc), **9** (PivotArc+GripArc), **10** (PSP PivotArc independent direction controls), **12** (menu/controller-only navigation), **13** (Hold B + D-pad). **#135 remains an engineering completion blocker** until all six full GripArc source gates in §6 are proven, not merely a screenshot or one fixture.

**Partial acceptance is not whole-issue closure:** #290 SafeSave and #194 GEM retain separate missing engineering, including PSP snapshot inventory, active player/profile isolation, controlled save transfers and full validated catalogue/player integration. #248 and #262 stay open. Only the owner-tested, specifically passed portions are removed from this quick-win list.

| Priority | Source issue | Remaining independent engineering (avoid accepted UI rework) |
| --- | --- | --- |
| Q1 | #262 / closed #162/#167 | **Approved SmartVisual Stage 1/1.5**: replace confusing HD/filter terminology, retain legacy preferences, verify actual shader/renderer capability and non-stretch/non-crop. Not present in APK 91364396. |
| Q2 | #290 | **New work only**: PSP snapshot entry/inventory, real profile-owned Save Centre actions, fail-closed permission and coverage tests. Existing owner-PASS Save Centre chrome and supported backup/restore are regression baselines. |
| Q3 | #194 | **New work only**: authoritative active-player GEM session, profile/backup linkage, source coverage and genuinely missing provenance; tested offline GEM rating/catalogue/link UI is already owner PASS. |
| Q4 | #135 | **MANDATORY full engineering finish gate** in §6: both Standard/PivotArc+GripArc complete fit, all console buttons, missing WS/WSC/NDS/3DS layouts, real guest keys, persistence, full layout/input test matrix. Not a one-fixture quick win. |
| Q5 | #177 | Only missing neutral release, stick/trigger dedup and reconnect unit fixtures; no blind deadzone change without evidence. |
| Q6 | #146/#147 | Small storage/archive safeguards only: SAF cancellation/low-space, ZIP path traversal, malformed disc members; no unapproved archive-engine rewrite. |

Closed #303 (navigation), #308 (TV), #162/#167 (display foundation), #183 (shelf gaps), #360 (SIGNATURE), and #288/#289 (Junior/profile design documents) are accepted or existing baselines, not new standalone tasks. This does **not** waive GC code work for Junior profiles or independent TV testing. #365 is PERF/native 3DS input; PERF #295 FIFA deformation deferred. CARTMOD maker beta remains separately planned.

## 5. Post-GC roadmap — DOCUMENT ONLY; not GC acceptance work

**Later Stage 2 — PERF renderer capability adapters:** Prove each actual host's internal-resolution and texture-scaling contract, allowed values and save-safe behavior: PPSSPP `ppsspp_internal_resolution` and texture scaling, Citra resolution factor, Flycast `reicast_internal_resolution`, Dolphin `dolphin_efb_scale`, Play/ARMSX2 `play_res_multi` only where matching engine supports it. No global promise of 2×/3× on Xbox, X360, Wii U, PS3 or unsupported native paths. Expose improvements through the same Automatic/Enhanced user experience when genuinely supported; advanced diagnostics only for raw multipliers. PERF owns host/engine pin changes.

**Later Stage 3 — performance-adaptive SmartVisual:** Investigate real renderer timings, memory and thermal headroom, conservative hysteresis, fallback, a player's override and reproducible frame-quality/performance evidence. No runtime adaptation shipped until measured across devices and approved.

**Later optional work — curated visual profiles:** Only if validated stable, consider user-selectable specialist style/CRT presets under Advanced and a per-game visual preview; avoid a settings explosion or external services. This is not a condition of declaring GC engineering ready.

**Separate:** Remaining PC emulation and Xbox/PS3/native engine performance belong to PERF; CARTMOD maker beta follows the agreed creator roadmap. No new fourth top-level lane.

## 6. MANDATORY GC ENGINEERING FINISH GATE — GRIPARC IS NOT OPTIONAL

**Owner clarification, 23 November 2026:** The target is **SmartVisual Stage 1 + 1.5 AND all remaining executable GC work**. Neither the quick-win list nor a successful APK is an alternative to completing GripArc. Issue [#135](https://github.com/safetyblade/ROMMachine/issues/135) remains an **engineering FAIL**, not merely a physical-QA check.

**Execution order:** (1) recover/verify exact-head CI; (2) complete SmartVisual Stage 1/1.5 with code/tests; (3) **complete GripArc/PivotArc engineering in full, ahead of optional quick wins**; (4) finish the remaining GC A–H, SafeSave, profiles/Junior, GEM, SIGNATURE and TV engineering; (5) final full build and honest matrix. All verified units may be committed progressively, but do not mark GC engineering DONE while #135 is partial.

### GripArc engineering DONE requires ALL of these on PR #368

1. **Actual compact premium full-L addon** in the source-approved visual style, G1–G4 stable, left and right attachment, even with one assigned slot. Remove the bulky/detached corner and large wasted-gap layout shown in the owner's failure screenshots. Solve the entire native + addon deck composition rather than shrink a border or move labels. Preserve original native console button labels, arrangements and controls.
2. **Both Standard + GripArc AND PivotArc + GripArc must fit** using measured native button/hit bounds, video viewport, safe insets and available screen dimensions; portrait and landscape, compact phone widths, scaled tablet/TV where applicable, no hidden controls, no fake compression. All active touch targets >=48dp without overlapping or silently clipped areas. Keep original display aspect/crop policy, game visual area and Start/Select.
3. **Console-authentic inventory:** verify native control registration and visibility, especially GB/GBC/GBA, SNES, PS1, PS2, PSP, Dreamcast, GameCube, WonderSwan/Color, DS and 3DS. Implement remaining WS/WSC/NDS/3DS PivotArc profiles rather than labelling unfinished layouts device-only. No fixed H1–H3 in PivotArc; its Hold A/B and the separately approved Standard Hold Tray remain intact. Correctly distinguish Neo Geo Pocket Color system ID from GameCube.
4. **Real guest input contract:** every G1–G4 single/chord and held input, PivotArc Hold A/B, analogue/digital original keys, key-down/up/release and focus/reconnect are emitted to appropriate guest input routing; no stuck keys, phantom touch, swallowed native input or duplicate controls. Correct PSP primary-direction selection persists without stealing its independent D-pad and analogue. Source unit-only model satisfaction is not enough if the actual UI ignores the solver.
5. **Persistence and recovery:** side, vertical offset, game and player mapping, disable/enable, swap modes, launch/reopen, resume and install-over preserve control selections; unsupported combinations use a truthful safe alternative without losing the previous preference.
6. **Automated proof:** unit and Compose-render fixture matrix for Standard/PivotArc with GripArc on both sides and compact/normal/wide windows; per-console original-button inventory, painted bounds + hit target bounds, overlap/no-crop, all four programmable buttons, hold/release and launch/resume; minimum regression for screens matching the owner screenshot dimensions. Tests must exercise the rendering and input wiring, not only abstract polygon or renderer geometry.

A physical handset/Marcus check is **separate and remains NOT TESTED until performed**; nevertheless all six engineering criteria above are code-and-test requirements. Do not relabel incomplete geometry, missing guest actions, console profiles or failing fixture coverage as “physical QA pending”.

### Exact finish status and continuation

- Report **ENGINEERING COMPLETE** for GripArc only when all six criteria are demonstrably covered at the same source SHA, with CI tests and a verified signed ARM64 APK.
- If an input is blocked by a genuinely unavailable **external native emulator interface**, name the exact interface/engine, attempted integration and accountable PERF issue; finish every independent UI/geometry/host code requirement. An unimplemented Compose control or unresolved local geometry algorithm is **not** an external engine dependency.
- If execution capacity runs out before closing #135 or any other source requirement, report **ENGINEERING INCOMPLETE**, list precise remaining tasks and branch/test status, and resume from them in the next run. Never report “GC done,” “only physical testing remains,” or request merge on that basis.
- Do not stop on the first successful build, partial grip improvement, SmartVisual completion or optional quick win. No automatic owner QA PASS or issue closure.

## 7. Codex continuation contract

Read this file together with the original [GC owner handoff](GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md), [GC integration acceptance](../GC-INTEGRATION-ACCEPTANCE.md), GC issue #262 and PR #368. In the current active draft branch, first verify live CI/head and repair any failures, then **implement** Stage 1 + 1.5 (code/UI/policy/tests), **complete the six mandatory GripArc engineering gates above**, and finish all genuinely unblocked GC A–H engineering before optional quick wins. Never treat unfinished GripArc as device-only QA. Source tasks outrank repeated owner QA descriptions.

Build signed exact-head ARM64 debug APK, report its actual artifact URL, gate results, unresolved source/host dependencies and a **separate deferred physical owner checklist**. Do not stop at one successful build; do not claim physical passes, merge PR #368, release, close issues automatically, or start post-GC Stage 2/3 in this run.
