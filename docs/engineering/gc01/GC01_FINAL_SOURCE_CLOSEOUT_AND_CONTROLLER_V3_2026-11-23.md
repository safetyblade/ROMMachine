# GC-01 — FINAL SOURCE CLOSEOUT: PivotArc/GripArc V3 + SmartVisual + all remaining A–H

**Owner direction:** 23 November 2026. **Execute on existing DRAFT PR #368** (`engineering/gem-b7b-deckfit-20261122`). **Do not create another GC PR or leave a second open engineering lane.** Original GC authority: #262, #135, #290, #248, the 22 Nov authoritative owner implementation handoff, and SmartVisual Stage 1/1.5 roadmap. This file is the **latest additive execution contract** where prior prose about the design or feature state is stale. All previously approved input semantics and physical test boundaries remain.

**Product finish definition:** Ship a source-complete GC candidate. **Only physical owner/Marcus acceptance should be left pending at the end of this engineering pass.** No missing UI code, screen geometry, profile security, saved-state routing, input dispatcher, model migration, fake placeholder settings, stub engines, incomplete console profiles or failed automated test may be relabelled as “needs physical QA.” If an external native engine feature cannot be implemented in GC, name its exact unavailable symbol/backend, working host boundary and PERF issue with evidence; coordinate the dependency now rather than silently counting it as finished.

## 1. Review of latest coding window — measured 23 Nov

At pre-closeout GC head `0e7871cc5c84ce35ac2e0e4baa49a1e9f16a84e7`, comparison from prior known-good exact-head APK `9317b47a7d2032c4a95b073d8d5a5258dbc252ee` found **12 intervening commits and 25 changed paths**. Source includes:
- **SmartVisual first implementation:** `SmartVisual.kt` selects Automatic/Original/Enhanced and a console-aware conservative renderer policy; `ShaderChooser.kt` maps supported Libretro outputs to existing LCD/Sharp/CUT shader variants; `DisplaySettingsStore.kt` versions global/per-cartridge preferences, and `DisplaySettingsScreen.kt` exposes the three choices with legacy custom handling. `SmartVisualTest.kt` covers per-SystemID, GL capability and legacy behaviour. This is **real code**, not a proposed feature; remaining acceptance is verifying policy/fallback/persistence/navigation and exact-head CI/physical appearance.
- **Additional C/E work:** revised `PivotArcControlGroup` and baseline controls, PSP direction, combined fit and actual rendering tests. **Not accepted:** owner screenshots still show an opaque oversized solo/combined Pivot housing and an accessory that appears detached.
- **Safeguards:** Storage Access Framework folder tests and ongoing source-level regression coverage.

**New immediately following connector changes on this same branch** remove the full opaque solo PivotArc background, replace the combined island fill with open machined rails/conditional short joint, protect both render policies with source tests, and correct a misleading “Screen aspect: Auto / Original” header to “Screen fit and surrounds.” These are **incremental design fixes, NOT evidence that all six #135 gates or SmartVisual are complete**. Preserve any concurrent Codex work; inspect live current HEAD and reconcile file changes rather than overwriting.

## 2. ARC V3 — decisive physical design reset (NO giant black panels)

The five owner images are an actual presentation regression: GBA Standard+GripArc (two frames), GBA PivotArc+GripArc (two frames), and PlayStation-family solo PivotArc. Original conversation images are not part of the GitHub blob; issue [#135 owner screenshot description](https://github.com/safetyblade/ROMMachine/issues/135#issuecomment-7096981706) is the precise witness statement. The images **do not** independently prove button inputs or an emulator black-screen fault.

### V3 rendering contract — every mode

**One CARTDECK console chassis, not a second faceplate.** Preserve the existing branded header and viewport. The lower deck remains the theme-coloured console body, with local machined button wells, restrained material rails, sparse accent seams, controlled relief, engraved PivotArc / GripArc labels, and original native button artwork. The painted shape must derive from **actual control placement**, not a huge opaque bounding rectangle. The decoration is pointer transparent.

**Solo PivotArc:** do not use the full control hull as a solid visual plate (the hull is only a safe-clearance envelope); draw only precise local control pockets, a compact labelled shoulder/utility rail, and optional safe machined connector where measured. A/B and their existing Hold A/B stay near their faces; D-pad/analog stay independently mapped; shoulder/utility controls remain coherently mounted rather than three orphan pills below a slab. Original/Pink/Green/custom remain semantic. **The PS-looking image is not permission to activate unverified PS1/PS2 PivotArc support**: first inspect actual SystemID and native inventory.

**Standard+GripArc:** keep the fixed premium **full-L** (G1–G3 upright, G4 foot) on selected LEFT or RIGHT with **four fixed source target positions**, even when count is 1. Native D/A/B, shoulders, Start/Select/Menu, analogs, triggers and genuine per-console controls always remain. Improve **physical vertical/horizontal positioning** relative to bottom rim and control clusters; align the L's bottom foot to the shell without blank 100dp gutters, overlaid native hardware or offscreen buttons. Restore a actually legible **GripArc** engraved identity with a measured reserved text zone; do not use a clipped 10dp caption as proof.

**PivotArc+GripArc:** solve one joint control composition using real video/safe bounds, both 48dp G touch regions and real native button/well geometry; draw an **open joined lower seam** with the L foot rather than two neighbouring independent rectangular solids. Move/group controls **only as required by the approved one-hand design**, never to make an oversized decorative slab look acceptable. Preserve native face button ordering and names, source key IDs, independent PSP D-pad/analog, Hold A/B, G chords and simultaneous holds, and utility buttons. If genuinely impossible to fit at constrained 320dp width/orientation: **explicit session-only Standard fallback**, saved selection/mappings unchanged; report such a case as UNSUPPORTED/NOT ACCEPTED, not full fit PASS.

**No** fixed H1–H3 on PivotArc. Standard handheld optional Hold Tray is separate from PivotArc Hold A/B and GripArc G1–G4.

### Finish implementation packages — all engineering on #368

| Gate | Real implementation, not documentation |
| --- | --- |
| ARC-01 | Shared measured screen/paint/48dp-hit envelopes; no opaque full-hull fill; pocket/hardware-seat drawing and no native button loss in solo and combined. Fix `CartdeckPivotArcModule.kt`, `CartdeckHardwareSeats.kt`, `CompositeControlDeckHousing.kt`, `GripArcControlGroup.kt` as one render path. |
| ARC-02 | Fix `GripArcCombinedLayout` / `GripArcPositionSolver` actual Standard native constellations, left/right, fixed four-G L and durable side/offset/mapping. Real 48dp buttons, visual bounding/engraving and no viewport changes. |
| ARC-03 | Fix `CompositeControlDeckSolver` placement/scoring and genuine compact joined seam for PivotArc+GripArc. Simultaneously verify Solo PivotArc: neither gets an opaque full-width slab, buttons detached below, or fake placeholder controls. |
| ARC-04 | Complete twelve handheld identities GB/GBC/GBA/GG/LYNX/NGP/NGC/WS/WSC/PSP/NDS/3DS; source-validate `TouchControllerID` and `NativeControlInventory`. **WS/WSC are currently missing from `PivotArcLayoutProfile.activeFor` AND `GripArcInputCatalog.forSystem`**. Map both directional groups/orientation as actual guest inputs before advertising; NGC means Neo Geo Pocket Color, not GameCube. |
| ARC-05 | Guest key DOWN/UP, diagonals, analog, double-owner G chords, Hold A/B toggles, focus loss, pause/background/reconnect and input release. No mapping-only mock; trace real PadKit/guest dispatch. Full Controls & Setup in Settings, in-game shortcuts only. |
| ARC-06 | Add repeatable **ACTUAL rendered** Compose screenshot fixtures matching owner image ratio 709×1536 and 320/360/393dp widths, portrait/landscape as supported; Standard+GripArc, Solo PivotArc, Pivot+Grip both sides, representative handheld/Sony/GC/DC native inventories; assert control/paint/viewport geometry, seam and label existence and apply pressed/released inputs. No source-only solver pass as a replacement for UI draw proof. |

**Engineering conclusion must cover all six gates.** Screen layout and movement are **design engineering**, not deferred acceptance. Only after verified source can Jonathan/Marcus physically PASS/FAIL actual comfort/input.

## 3. SmartVisual — finish NOW, not “post-GC”

The new three-choice model exists. Finish remaining source, migration and CI work in this pass:

1. One **Image Quality** choice Automatic (default for previously unconfigured)/Original (actual pass-through)/Enhanced (actual proven CUT effects) in existing global, game and supported settings entry points. Remove overlapping HD sliders as ordinary user options; keep Visual Style/Advanced only when supported and honest. Do not add generic 2×/3× internal-resolution knobs or a console menu zoo.
2. Legacy filter/hd/quality overrides remain exactly effective until the player explicitly changes them; app update preserves per-cartridge and global settings and “Use Global Default.” Ensure changed setting actually reaches `GLRetroViewData.shader` on the next supported game launch. No inert switches or false “native renderer Enhanced” claim.
3. Source tests for all system IDs, GL ES 2/3, shader fallback/compile and unsupported host, manual Original and Enhanced, stable border/viewport/press mapping, offline/persistence/install-over, saved legacy CRT/LCD filters and Reset. If the runtime cannot recover a shader build failure, implement the bounded safe fallback in the existing Libretro host (not a new shader engine).
4. Keep renderer-native internal-resolution/texture scaling under *post-GC PERF Stage 2* and thermal/FPS dynamic tuning under *post-GC PERF Stage 3*. Stage 1/1.5 are **current GC engineering requirements**. Confirm actual source tests, not visual assertions only.

## 4. Remaining GC A–H — integrate all executable work before claiming engineering complete

| Scope | Engineering requirement to implement/verify in **this PR** |
| --- | --- |
| A · Menus | Complete ownership/Back/recovery/close/pause paths across supported game hosts; no fake successful close while in-game input remains locked. Closed navigation issue #303 is baseline, not a new design. |
| B · Display | Finish SmartVisual above, correct tablet/external display/DS/3DS safe geometry and navigation; do not revive Immersive Mode or alter true fullscreen/surround. |
| C · Controllers/TV | Actual P1/P2 source guest routing, swap/Undo persisted, reconnect, focus release and repeatable remote/gamepad-only shelf→cartridge→play→menu→shelf. |
| D · Wii/3DS | Gamepad-first Wii, real virtual IR pointer and guest motion path, independent 3DS tilt/sensor/guest bridge where available. Audit native PERF #365 bridge requirements explicitly; do not mark GUI switches complete if dispatch is inert. |
| E · Controls | Entire ARC-01–ARC-06 design/source/combined/handheld/control acceptance, not only the printed G1–G4 slots. |
| F · Modern Function | Exact title + edition + console action identity and verified guest dispatch; no metadata-only fake working actions. |
| G · Xbox 360 | Correct XUID-bound, active-user storage for saves and bounded save-only restore, without mixing Xbox runtime PERFs or texture pins. |
| H · Integration | Full install-over continuity, library/cartridge UUID/legacy aliases, live Save Centre and game launch/resume, manufacturer shell/appearance, all mandatory CI, exact-head signed ARM64 APK. |

**Cross-cutting (still engineering):** PSP SafeSave snapshots listed and restored through Save Centre; supported native adapters verified without false universal coverage, authenticated active-player profiles, manager/Junior ownership and PIN/recovery, permission checks on restore/export/navigation, GEM active-player-rating identity and backup integration, reliable title/library/cover links and offline changes. Build bounded tests, avoid destructive save migration or unsupported game-native memory claims.

## 5. Owner QA / issue reconciliation

**Owner PASS on APK 91364396**: checklist 1, 2, 4, 5, 6, 7, 11 (install-over/shelf, Save Centre chrome & one supported restore, GEM ratings/browse/link, Homebrew, SIGNATURE intro). Narrow issues **#183 and #360 closed**, no rework without regression.

**Remaining owner physical checks:** 3 Display, 8 Standard+GripArc (PARTIAL visually), 9 Pivot+GripArc (VISUAL FAIL), 10 PSP PivotArc, 12 game menu/controller-only, 13 Hold B+D-pad; owner device input still unverified by five screenshots. **Do not auto-close #135, #290, #194, #262 or #248** or infer independent Marcus PASS from source.

## 6. Execution contract for Codex — no more “plan complete” stops

1. Read **live** branch head and its exact-head CI, this contract, source comparison, screenshots issue #135, the original A–H acceptance and SmartVisual Stage 1/1.5 spec.
2. **Finish** all source engineering in order: final SmartVisual pipeline → real ARC-V3 visual/input and all 12 handheld profiles → independent A–H, SafeSave/Junior/GEM source requirements → automated device-proxy render/integration regression. Do not spend build capacity redoing accepted #183/#360 or changing PERF engine pins.
3. For each true work unit: inspect existing implementation, implement connected code, add real tests, commit, continue; build gates after meaningful milestones and after the final cumulative candidate. **No stopping after first green build** or calling an item complete because a Settings label exists.
4. If a *specific external native host API* is unavailable, document the symbol, proof, host-side code and separate PERF owner issue, then continue unrelated work. A mispositioned G module, missing handheld layout, GUI stub, unfinished SafeSave import or insecure profile is NOT an external engine blocker.
5. Complete Kotlin/Room/GEM, SmartVisual tests, controller actual-render/guest-input matrices, SafeSave transaction and profile security, startup/TV/GC regressesions and exact-source connected APK. Include executable instrumented path where available, not only structural tests. Do not claim a build is verified until its Actions run is successful and its artifact belongs to the same SHA.
6. Return exact head, diff/commits, test counts, signed ARM64 artifact link/versionCode, **ENGINEERING COMPLETE / INCOMPLETE** per A–H, any precise unresolved source/native dependency and the **short physical-QA-only** checklist. PR stays draft/unmerged until Jonathan accepts. If engineering remains, keep implementing on this PR until complete. No new GC lane.

**Source engineering target is ALL current executable GC work; physical checks are the eventual remaining gate, not an excuse to defer controller design.**
