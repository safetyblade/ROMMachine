# GC-01 — PivotArc Handheld Expansion + GripArc V2.2 Repair
**Owner delivery contract | 22 November 2026 | Existing draft PR #362**
**Status:** Authoritative source-backed implementation and QA handoff for GC C/E. No new Kotlin implementation, APK, physical PASS, issue closure or master merge is represented by this document.

## 0. READ FIRST — correct existing UI and current failures

**Design and original source**:
- [PIV-HH-01 — console-authentic one-hand PivotArc layouts and wireframes](../../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) (full **12** registered handheld SystemIDs: GB/GBC/GBA/GG/LYNX/NGP/**NGC**/WS/WSC/PSP/NDS/NINTENDO_3DS).
- [HHOLD-01 — separate Standard handheld optional hold tray](../../design/gc01/GC01_HANDHELD_UNIVERSAL_HOLD_CONTROLS_DESIGN_2026-11-22.md).
- [Current controlling full GC A–H implementation contract](GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md).
- [GC Sienna source UX M10 Touch Layout, M11 GripArc Studio, M12 Unfit Fallback](04_SIENNA_COMPLETE_UX_DESIGN.md).
- [GripArc V2.2 physical acceptance matrix](../griparc-device-acceptance.md).
- [GripArc physical defect #135](https://github.com/safetyblade/ROMMachine/issues/135), original [#123](https://github.com/safetyblade/ROMMachine/issues/123), cartridge profile [#125](https://github.com/safetyblade/ROMMachine/issues/125), held-input [#122](https://github.com/safetyblade/ROMMachine/issues/122), PivotArc [#58](https://github.com/safetyblade/ROMMachine/issues/58), original GC [#262](https://github.com/safetyblade/ROMMachine/issues/262) and QA [#248](https://github.com/safetyblade/ROMMachine/issues/248).

**Current owner device result from #135 (do not overwrite with green source tests):**
- **Standard + GripArc:** physical module renders and mapped controls respond in the owner's screenshots, but the full-L **still looks oversized** and at least some system-specific native buttons disappear on console switch; **PARTIAL ONLY / FAIL on original control preservation**.
- **PivotArc + GripArc:** **PHYSICAL FAIL** on owner device, no full combo acceptance.
- **Source code** already includes GripArc V2.2 art/manifest and attempted real-footprint/combined solvers; the existence of this code or merged old PR #152/#157 is **NOT** a complete fit/pass.
- Design lock: compact premium physically attached **full L** on the selected side, refined italic "GripArc" engraving, original theme materials, fixed G1–G4 geometry **even when active count = 1**, complete mapped native controls, min **48dp** nonoverlapping hit regions, no viewport crop/shrink, original Standalone Standard/PivotArc unchanged.

**Scope correction:** Do **NOT** add permanent H1/H2/H3 buttons to PivotArc. Preserve current Hold A/B. Standard HHOLD-01 optional 1–3-slot tray is a *separate* Standard touch feature, not GripArc or PivotArc geometry. The new PivotArc handheld expansion is legitimate new scope, but a new profile is only available with a complete verified source-native controller and measured fit.

## 1. Exact source integration map — start here, NO new control framework

| Existing production source | What is already available | Required bounded repair |
| --- | --- | --- |
| \`GripArcGeometry.kt\`, \`griparc/griparc-v22-manifest.json\` | V2.2 manifest, full-L footprint from **two arms**, four prepositioned G-slot touch bounds, fit/error return | Retune **measured painted material and arm thickness/scale** (not shrink hit targets) for owner's "L too large" report. Actual L footprint union and four fixed touches reserve geometry, not filled bounding-box rectangle or a fake floating "G" line. |
| \`GripArcCombinedLayout.kt\` | Dock-first and measured native control constellation, complete controls/visuals checking, attempted close-fit/whole-group fallback | Repair **Standard+GripArc** using actual on-device observed native control rectangles including wells, utility rail and analogs; preserve exact grouped face and native input identity, never remove a button to fit. Test target console changes and loading transitions. |
| \`CompositeControlDeckSolver.kt\` | Combined dock+PivotArc notched island, Hold A/B and future X/Y reservations, 48dp source constraints | Repair **PivotArc+GripArc** physically, using new PivotArc profile only where source-inventory and measured geometry actually complete. Reuse one dock-first placement truth for outline and hits; reject fit honestly rather than overlapping controls or hiding gameplay. |
| \`NativeControlInventory.kt\` + \`TouchControllerID.kt\` + original PadKit layouts | Source-native control specification for supported IDs, but does **not** yet cover all handheld/new console variants | Close console native inventory gaps before rendering them as supported. \`SystemID.NGC\` is **Neo Geo Pocket Color**, separate from \`GAMECUBE\`. Respect Game Gear visible 1/2 vs key A/B, NGP/NGC label-vs-key differences, WS portrait/landscape dual directional groups, Lynx Options/Pause, PSP analogue + Sony compass and DS/3DS stylus and bridge limitations. |
| \`GripArcProfiles.kt\` \`GripArcInputCatalog\`, \`GripArcProfileStore\` | Profile per stable cartridge; left/right side, G1–G4 count and chord mappings; source examples for PS2/GC/DC/GB/GBA/SNES/PSP; **\`SystemID.NGC\` already has a distinct Pocket Color branch** | Audit uncovered \`GG\`, \`LYNX\`, \`NGP\`, \`WS\`, \`WSC\`, \`NINTENDO_3DS\` before enabling their GripArc; existing \`else -> emptyList()\` means no assignable keys, and must not silently claim success. Preserve all previously saved profile keys/settings, G1–G4 even inactive, mapping on Open/Resume/Restart. |
| \`PivotArcLayoutProfile.kt\` / \`PivotArcControlGroup.kt\` | Currently GB/GBC/GBA/SNES owner geometry; existing Hold A/B and safe per-control owner key IDs | Implement 12 real handheld one-thumb profiles per PIV-HH-01 **without overwriting existing GB/GBC/GBA/SNES settings**, preserving original controls, mirroring, 48dp and all special console inputs. No extra H1–H3. |
| \`PivotArcHeldInputGate.kt\`, \`GripArcEditorScreen.kt\` | First-owner DOWN/last-owner UP and four chord slots with source-owned press/release; editor already exists | Validate touch + physical + held A/B + G chords simultaneously; no duplicate DOWN/early UP/stuck keys on menu, focus loss, layout switch, background or game stop. **Controller setup and GripArc configuration in Settings**, in-game only quick actions/shortcut. |
| \`MobileGameScreen.kt\`, \`GameScreenLayout.kt\`, \`GripArcCombinedLayout.kt\`, \`CompositeControlDeckSolver.kt\` | Actual viewport/safe insets, layout measuring, deck composition and fallback | **One frame / one geometry truth** shared by painted housing, native input hit regions and accessory touch targets. No first-frame visible controls disappearing on backend reload, changes of cartridge or rotation. |
| \`MainGameContextActions.kt\`, shelf cartridge context | Per-cartridge hold/layout chooser and library context | Migrate full setup route to normal Settings → Controls & Setup, preserving a short route from cartridge detail/in-game and per-cartridge ARC marker only when enabled. Do not replace main menu or create second editor. |

## 2. Mandatory GripArc repair scope from owner #135

### GA-01: Repair **Standard+GripArc** native-control loss and oversized L
1. Measure actual PadKit control **painted group rectangles and 48dp hit targets** for gamepad/console variant with selected GripArc active. Avoid solver tests with empty fake occupied sets as primary proof.
2. Reserve the *actual* two L arms + all G1–G4 occupied geometry, even when active G count is one; keep G positions **fixed**. Arrange D-pad, face constellation, shoulders, triggers, sticks, Start/Select/Menu and native special controls as intact constellations, never arbitrary fake ABXY grids.
3. Make the **full L visually more compact** with restrained arm/material proportions and correct machined joint/italic engraving, while every active G touch area remains **at least 48dp**, physically distinct, accurately highlighted. A smaller text label alone does not fix a full-L that's too large.
4. When source-native inventory cannot fit, show a readable **M12 Unfit Fallback** with explicit **Standard for this session** and Back/Reset; preserve saved GripArc preference and all original game controls. Do NOT overlay game pixels, truncate error behind A/B or silently flip L side.
5. Test switching systems while active: GB/GBC/GBA/SNES, PS1 digital/DualShock, PS2, PSP, Dreamcast native trigger pair, GameCube Z and C-stick/half-trigger, and other actual registered engine variants. If an inventory is missing, report exact unsupported capability, not a successful fit.

### GA-02: Repair **PivotArc+GripArc** complete notched-island fit
1. Resolve one **joined module**: correct full-L material union, G touch bounds and one notched Pivot island outline with complete original **D, A/B (or original labels), L/R, utility rail, existing Hold A/B where actually native, and future X/Y envelope where required**.
2. Current source PivotArc systems **GB/GBC/GBA/SNES** first for regression and owner physical proof. New 12 handheld profiles may combine only after their **own** precise inventory + safe geometry is evidenced; never infer a new combined PASS from a solo profile.
3. Both left- and right-hand accessory placements, real measured 320dp/360dp/393dp widths and asymmetric cutouts. If impossible, make **session-only** Standard safe fallback without altering the saved mode, mappings, profile/side or original launcher state.
4. Never change actual game viewport ratios or crop/scale to make hardware appear to fit. No hidden Hold A/B, no blocked D-pad diagonals, no cart detail/freezes on controller attachment.
5. Keep #135 in **FAIL/engineering ongoing** until owner device confirms whole visual/input path, Marcus independently validates.

### GA-03: Complete programmable G1–G4 controls and per-cartridge UX
- One permanently complete full L with 4 fixed slot positions; choose 1/2/3/4 active Gs, remaining inactive slots stay blank, not ghost input. No G geometry shift across active counts.
- Any G maps to **one actual console-native button or simultaneous chord** (not timed macro) with per-cartridge exact guest support; original keys remain available. On press, native DOWN for all chord members; release final source UP once, even when Hold A/B or another G shares the same key.
- Side LEFT/RIGHT, vertical offset, count and all four mappings persist by stable cartridge identity; disabled preserves configuration; re-enter via Open/Resume/Restart without leaking last game's G holds. ARC marker only on enabled games.
- Use normal **Settings → Controls & Setup → GripArc**, with live native input inventory, G1–G4 mappings, preview, Reset, active count and selected side. Cartridge context/in-game menu may shortcut to the same Settings; do not put full edit panels in gameplay.
- Reuse owner-approved machined material/semantic palettes Original/Pink/Green/custom; no new art system, no "button chip" replacing bolted-on full L.

### GA-04: Reality-check new handheld PivotArc profiles before joint solver assumes support
- PIV-HH-01 now contains **12 registered handheld IDs including NGC (Pocket Color)**, not 11. Correct any other old docs/status counts, and code map NGC to validated NGP-family layout without losing its separate game/persistence ID. \`NGC != GAMECUBE\`.
- Systems with especially difficult layout: WS/WSC *both independent X and Y directional sets*; PSP both real D-pad and analog; Lynx real Option1/2 and Pause; DS/3DS two screens/stylus and conditional sensor/tilt; GG original button 1/2 labels; NGP/NGC A/B original click-stick. Source-native keys must not disappear when GripArc is enabled.
- New **One-Thumb Arc** and optional **Hold & Move** must be measured/engine-tested as separate layouts; cannot claim a game is "one-hand compatible" just because directional petal geometry renders.
- Do not add H1–H3 to PivotArc; do not mix Standard HHOLD-01 up-to-three Hold Tray with attached GripArc G1–G4. Existing PivotArc Hold A/B and GripArc four G keys remain independent.

## 3. Required acceptance suite — same HEAD and real hardware

**Test hardware**: owner's Samsung SM-S721B first, plus controlled width fixtures **320×640, 360×740, 393×852 and 393×852 with asymmetric cutouts**, left/right, portrait and genuinely supported landscape, Original/Pink/Green/custom, all 1–4 active G counts, 48dp+ touch and entire painted housing.

| Gate | Verification and required recorded result |
| --- | --- |
| **G-01 Standard original inventory** | On each tested system compare *before/after* all real buttons, original labels, analog, shoulder/triggers, well art, utility and touches; any hidden/displaced source-native control => FAIL. |
| **G-02 Standard+GripArc** | Physically smaller visual full L (but full footprint and 48dp touches), 4 fixed G slots at counts 1–4, actual left/right, no viewport/OS gesture collision, source owner test. |
| **G-03 PivotArc+GripArc** | Actual dock-first joined notched island and Hold A/B, correct all original keys+future envelope, both sides and safe explicit fallback; no physical PASS until owner actually tests. |
| **G-04 Input ownership** | Hold B + D-pad + ordinary B + overlapping G chord; second G shares B; physical pad adds B. Exactly one DOWN on first owner and one UP after last owner, safe pause/background/core stop. |
| **G-05 Console switching** | GB/GBA/SNES, Sony PSX/PS2/PSP, GC, DC and new supported handheld profiles. Check entire native inventory **after** switching launch/restart/resume, not only first render. |
| **G-06 Configuration/settings** | ARC per game, count 1–4, all mappings, side/height, enabled marker and disabled retention, saved after install-over/reopen, full editor Settings-only; no accidental mapping overwrite. |
| **G-07 Small screens** | All real controls and addon >=48dp, artwork hit region aligned, true source viewport, useful error + Standard session fallback on impossible fit, no silent side switch/profile mutation. |
| **G-08 HH-PIV twelve-family** | Original GB/GBA/SNES no regression; GG+NGP+NGC, Lynx, WS/WSC orientation, PSP real analog, NDS/3DS stylus & motion only where native bridge verified; one thumb and source console inputs physically tested. |
| **G-09 Install-over/data** | Same package ID/signer, keep cartridge mappings, native saves/state/VMUs, ROM bindings, appearance/Junior/GEM; no destructive reset. |
| **G-10 Release evidence** | Exact Git SHA + Android APK version/signature + direct GitHub Actions ARM64 artifact + tests + owner/Marcus measured photos/input trace with PASS / FAIL / BLOCKED / NOT TESTED per actual engine/device. |

**Source fixture tests are NOT independent physical acceptance.** Any old PASS must be labelled precisely (e.g., Standard combined input only, not all-native layout).

## 4. Codex implementation order, avoid duplicated planning

1. **FIRST** fix owner-reproduced #135 failures before treating enlarged PivotArc coverage as usable with GripArc: Standard+GripArc L proportion and native controls; PivotArc+GripArc measured fit for already-supported GB/GBA/SNES.
2. In parallel reuse validated source/native inventory and PIV-HH-01 data-driven profile design for **GG, NGP, NGC**, then Lynx, WS/WSC, PSP, DS/3DS. Keep source's approved GB/GBC/GBA/SNES layouts and original saves untouched.
3. Extend combined solver **only for a new handheld after** its own source inventory, true full native controls and solo PivotArc physical/geometry tests pass. No promise that every layout+GripArc can fit narrow screens; explicit safe Standard fallback is valid but marks combo as UNSUPPORTED/BLOCKED, not PASS.
4. Connect **Settings** full GripArc + One-Hand layout, existing cartridge context shortcut and G source-owned chords/held-state, avoid new framework/multiple branches.
5. Run tests for mixed touch/physical holds, layout safe bounds, left/right, disabled slots, exact guest key presses, launch/reload/restart and change of console. Produce the next same-head APK and honest owner/Marcus physical acceptance matrix.
6. Keep full GC original A–H, SafeSave/GEM/controller/Wii+3DS/Junior/TV/SIGNATURE work continuing on PR #362; only this bounded C/E scope is addressed here, not new baseline for full GC completion.

**DO NOT**: open independent new GC PR, merge/close #135, auto-merge child #368, claim source success as owner physical PASS, replace the owner-approved full-L art with dots or shrink 48dp controls, delete original native buttons, shrink video, invent gyro or actual emulator support, or write yet another broad architecture document in place of code.
