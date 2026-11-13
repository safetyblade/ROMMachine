# CARTDECK — Master product and delivery roadmap

**Authoritative current checkpoint: 13 November 2026 · v2.6**
**Owner:** Jonathan Prestwich · **Project Director:** Cassia Rowan · **Design:** Sienna Vale · **Engineering:** Elliot Shaw · **Architecture:** Daniel Mercer · **QA:** Marcus Reid
**Controlled primary roadmap:** [CARTDECK Product & Delivery Roadmap v2.6 (Drive)](https://docs.google.com/document/d/1wlURBkCoASXTAOFbXrNQWzODEbnVuMJBUz4l68Vac4Q/edit)
**Repository:** `safetyblade/ROMMachine` · source checkpoint `master a0f3a1eed0a87700cc1c640a4d65a21d2f97482f`.
**Purpose of this file:** short, current engineering mirror of the controlled Drive roadmap. Older logs and issue conversations remain historical evidence. Update this summary whenever owner physical test outcomes supersede earlier source claims.

> **RELEASE BLOCKER — GRIPARC STILL FAILING.** The two owner screenshots from 13 November show **Standard + GripArc** (GBA and PS2-class), NOT PivotArc + GripArc. Standard + GripArc draws and the owner says the buttons work, but housing is too big and required native controls disappear when other consoles load. **PivotArc + GripArc does not work on the owner's handset**. Neither combined mode has complete device acceptance. Merged source PR #157 is not a PASS; [#135](https://github.com/safetyblade/ROMMachine/issues/135) remains open and BLOCKING.

## 1. Delivery board — source status and acceptance truth

| Lane | Owner decision / goal | Source milestone | What is still open / proof needed |
| --- | --- | --- | --- |
| **GripArc V2.2 — BLOCKER** [#135](https://github.com/safetyblade/ROMMachine/issues/135) | Slim *italic GripArc* premium L; only slight native console control displacement, no missing buttons; both Standard+ and PivotArc+ safe | Joint solver #152 and compact native inventory #157 MERGED | **FAIL** PivotArc+GripArc on handset; Standard+GripArc partial render/buttons only, too large; other console native buttons disappear. Needs owner/Marcus matrix |
| **Tablet and TV UX** [#162](https://github.com/safetyblade/ROMMachine/issues/162), [#165](https://github.com/safetyblade/ROMMachine/issues/165), [#166](https://github.com/safetyblade/ROMMachine/issues/166) | Responsive tablet menus and TV 10-foot library/settings, remote/gamepad focus, no touchscreen buttons on TV | Screen/eligibility policy and MENU button #163 MERGED | Full tablet layouts, branded TV focus/navigation, basic TV remote menu path and actual hardware QA **NOT ACCEPTED** |
| **Actual fullscreen video / Native ratio** [#167](https://github.com/safetyblade/ROMMachine/issues/167) | TV & landscape **Fill** by default (stretch 4:3 if required; no crop), Native aspect option from controller/remote game menu, live switch | Display policy and initial touch/shell hiding #163 MERGED | Native LibretroDroid C++/JNI Fill vs Native renderer, persistence, portrait fullscreen choice and source-pixel tests **NOT IMPLEMENTED/ACCEPTED** |
| **Controller connected = buttonless fullscreen** [#162](https://github.com/safetyblade/ROMMachine/issues/162) | In fullscreen on all screen classes hide Standard/PivotArc/GripArc/button wells/shell when enabled gamepad connected; touch controls return on phone/tablet unplug; TV never overlays; remote remains accessible | Foundation #163 + #170 source MERGED | Physical connect/disconnect, DS/touch fallback, complete portrait/fullscreen UI/renderer and game-menu escape QA **OPEN** |
| **Modern Function Controls** [#172](https://github.com/safetyblade/ROMMachine/issues/172) and [#176–181](https://github.com/safetyblade/ROMMachine/issues/176) | Map original *game action* to physical modern control location based on console+genre+verified title, not just original letters; reversible profiles | Research/documentation PR #175 MERGED: 16 games / 44 proposed mappings | **NO runtime action resolver or general hardware QA**; controller normalizer, exact core key bridge, title identity, UI, catalogue and Marcus tests OPEN |
| **Shelf editing / reserved holes** [#183](https://github.com/safetyblade/ROMMachine/issues/183) | Remove all spaces, Auto-arrange A–Z and by console, Done/Tools/Undo; drag must not manufacture vacancies; retain intentional explicit spaces | #184 MERGED, lint-only repair #185 MERGED, hotfix branch source preflight PASS | Current merged-master integrated ARM64 APK and actual owner Remove/Undo/Sort/drag/rescan/restart acceptance **OPEN** |
| **Emulator & ROM quality** [#143](https://github.com/safetyblade/ROMMachine/issues/143), [#145–148](https://github.com/safetyblade/ROMMachine/issues/145) | Preserve Wii/Dreamcast known gameplay, recover PS2 native frame, Xbox no-white frame, support reliable multi-track/ROM and protected saves | PS2 JavaVM crash fix #182 MERGED; isolated Xbox diagnostics #168/#169/#171 MERGED | Owner PS2 new-build retest not confirmed; Xbox still white in prior owner report; ROM management / archive / library integrity independent open lanes |

**Evidence key:** `MERGED` = source present; `CI PASS` = specific run passed; `OWNER DEVICE PASS` = exact gameplay on a named APK verified; `MARCUS QA PASS` = independently documented. Never substitute one for another. As checked, [latest merged-master build #43413014935](https://github.com/safetyblade/ROMMachine/actions/runs/43413014935) was **in progress**, while the hotfix branch [#43412688553](https://github.com/safetyblade/ROMMachine/actions/runs/43412688553) passed **source preflight**, not the whole device gate.

## 2. GripArc V2.2 — PRIMARY RELEASE BLOCKER

**Actual owner observed (13 Nov):**
- Standard + GripArc **renders on device** and present GripArc buttons respond; render/input partial PASS only.
- Module still consumes too much space, especially its mostly empty right vertical leg and horizontal leg with G1-only.
- Changing to other consoles can **remove native game buttons**; this remains a failure regardless of PR #157 native-inventory test results.
- **PivotArc + GripArc did NOT fit/render as required**; no physical PASS. Previous claims of PivotArc device success are expressly superseded.
- No blanket G1–G4, left/right, Console/PS2/GC/DC, profile restore, theme or device-fit acceptance.

**Design target locked:** GripArc should be a compact physically attached premium **full L**; italic *GripArc* engraving and subtle CARTDECK lit/relief materials. Four stable G1–G4 button slots, 1–4 active, no ghost buttons, ≥48dp nonoverlapping real hit areas. Reserve smaller L dock first, slightly scale **visible** native controls, nudge original console controls only bounded up/away from dock (initial target 8–12% visual compactness, 10–24dp movement only if measured fit). Never rearrange into generic PlayStation grid. All genuine D-pads, face buttons, triggers, analogs, utilities, console seats and accents remain. Game aperture unchanged. Standard-only and PivotArc-only remain unchanged.

**Next focused fix:** reconcile runtime **active native controller inventory** for each actual selected console/core (PS1/PS2/PSP, GameCube, Dreamcast, GB/GBC/GBA, SNES, supported Wii/Xbox where applicable); detect & reject missing controls before drawing; fix PivotArc dock with Hold A/B, shoulders and utility; align artwork and touch regions; no changes to G button ownership/chords/saves or ROM/emulator launch paths. Deliver real owner device QA and Marcus sign-off. [Issue #135](https://github.com/safetyblade/ROMMachine/issues/135); per-cart ARC [#123](https://github.com/safetyblade/ROMMachine/issues/123), [#125](https://github.com/safetyblade/ROMMachine/issues/125).

**Exit gate:** Standard+GripArc right/left and PivotArc+GripArc right/left render and work on owner handset; no missing native buttons; G1–G4 input, chords, held releases, toggles, theme light, 48dp safe region, saved profiles and replay/reopen/install-over verified. If a device cannot safely support both, show a usable explicit fallback without silently changing saved preference. Until this gate: **FAIL / OPEN**.

## 3. Display / tablet / TV experience — OPEN

**Layouts:** Tablet medium window ≥600dp / expanded ≥840dp: CARTRIDGE shelf responsive panels, sorting, details, accessible setup and game menus; no phone UI inflated to ten-foot television. TV Leanback: focused cartridge cards, readable settings, remote D-pad/OK/Back, discoverable access to Game Menu even on a minimal remote and a controller. Connect/disconnect/multiplayer tested; TV remote is navigation rather than full gamepad.

**Gameplay:** TV always **no visual buttons/shell**, regardless of controller connection; paired physical pad can play a supported game and can always open menu. Fullscreen on ANY device with an enabled gameplay controller hides **all** virtual Standard/PivotArc/GripArc touch buttons and decorative device shell; when last pad disconnects on touchscreen phone/tablet, re-enable playable on-screen controls without restarting. Never automatically force portrait phone into fullscreen because of a pad; explicit portrait fullscreen selection and native shell default preserved.

**Video:** TV and non-DS landscape default actual **Fill entire display** (stretch 4:3 to 16:9 if required, no crop). Menu offers **Native aspect** without restarting or losing game state; native source proportions with bars. Fill must be implemented in LibretroDroid host GL/JNI, not just AndroidView geometry. Preserve native game controls and coordinate transforms.

**DS:** Nintendo DS/3DS dual-screen games **incompatible** with TV or landscape-fullscreen gameplay; clearly label this when opening, but preserve shelf listing, portrait handheld/tablet two-screen + stylus and all DS saves.

**Tracking:** [Parent #162](https://github.com/safetyblade/ROMMachine/issues/162), [Tablet #165](https://github.com/safetyblade/ROMMachine/issues/165), [TV #166](https://github.com/safetyblade/ROMMachine/issues/166), [Renderer #167](https://github.com/safetyblade/ROMMachine/issues/167). Source policy [PR #163](https://github.com/safetyblade/ROMMachine/pull/163) merged; full UX and real video scaling/device QA not complete.

## 4. INPUT-02 — action-based universal modern controllers — OPEN

**Goal:** the SAME **FUNCTION** uses familiar modern **PHYSICAL location** even if original console labelled the input differently. Modern bottom face = Jump in relevant platformers or primary Pass/Kick in verified football titles; Right Trigger = Accelerate and Left Trigger = Brake in approved racing profiles; L-stick = Steer (to digital D-pad only when game expects that).

**Critical counterexample:** SNES B = **JUMP** in *Super Mario World* → physical South, while SNES B = **ACCELERATE** in *Super Mario Kart* → physical RT. GBA Mario Kart A = Accelerate → RT emits actual GBA A, PS2 Gran Turismo 3 Cross = Accelerate → RT emits Cross; Dreamcast Crazy Taxi already uses native analog RT/LT. Never map `SNES B -> RT` or `GBA A -> RT` across all games.

**Controller physical normalizer:** Xbox 360-style, Switch Pro, supported PS3/DualShock and generic Android gamepads share SOUTH/EAST/WEST/NORTH, triggers/sticks/dpad/shoulders. Preserve correct Android→RetroPad conversion; controller device identity, remap preferences, 0..1 analog supported outputs, multiple ports, disconnect/press ownership, game MENU and Xbox/GC/DC/Wii unusual native inputs.

**Modes:** Console Authentic (safe fallback), Modern Function (player opt-in then **verified per-title/edition** mapping automatic), Custom (saved per-cartridge). Confirmed console+genre candidates are **explicit suggestions**, never silently imposed; current Game metadata lacks reliable genre/action semantics. Show labels and confidence; one-click Undo; digital RT->face key must hold/release with hysteresis, original face key accessible and no false analog claims. Profiles keyed to actual stable cartridge/game/region/core, not an inferred screenshot or raw filename.

**Research status:** [PR #175](https://github.com/safetyblade/ROMMachine/pull/175) MERGED documentation only; 16 game examples, 44 proposed mappings; [#172](https://github.com/safetyblade/ROMMachine/issues/172), [#176](https://github.com/safetyblade/ROMMachine/issues/176) source validation, [#177](https://github.com/safetyblade/ROMMachine/issues/177) physical normalizer, [#178](https://github.com/safetyblade/ROMMachine/issues/178) resolver, [#179](https://github.com/safetyblade/ROMMachine/issues/179) UI, [#180](https://github.com/safetyblade/ROMMachine/issues/180) catalogue, [#181](https://github.com/safetyblade/ROMMachine/issues/181) independent QA ALL remain OPEN. No new live action mapping has been accepted.

## 5. Shelf edit / cartridge library — MERGED SOURCE, OWNER QA OPEN

**Screenshot (13 Nov):** "Reserved" holes among games, text wraps. Root cause in `ShelfBoard` drag target: dropping into visual last-row columns persisted empty `ShelfCell()` slots; successful ROM cleanup removes game-linked cells but preserves explicit/reserved cells. [PR #184](https://github.com/safetyblade/ROMMachine/pull/184) merges:
- Edit toolbar **Done / Tools / Undo**, Tools for Add section, **Remove all empty spaces** (keep order), Auto-arrange **A–Z** and **by console** within original sections.
- No implicitly persisted vacancy when dragging onto visual padding; **Empty** label instead of multi-line Reserved.
- Explicit manually inserted spaces still permitted; standard library refresh does not silently wipe intentional holes.
- Atomic `ShelfStore` and existing Undo; no ROM, cartridge, game-progress/save, VMU or core modification.
- [PR #185](https://github.com/safetyblade/ROMMachine/pull/185) fixed source/test lint after early #184 merge and is merged.

**Remaining:** Integrated current-master APK complete build, owner phone Remove spaces/Undo/A-Z/by-console, drag-to-padding restart, actual cartridge removal/complete refresh, failed scan protection, install-over and no missing game/saves. Keep [#183](https://github.com/safetyblade/ROMMachine/issues/183) OPEN until demonstrated.

## 6. Parallel architecture and emulator recovery — preserve all working paths

- Historical owner Wii Mario Kart and Dreamcast Soul Calibur CDI gameplay are known regression anchors, **not** all-games support.
- PS2 Play! JavaVM native crash fix [PR #182](https://github.com/safetyblade/ROMMachine/pull/182) merged, but owner new-build playable first frames, controller and memory-card reopens still unverified.
- Xbox hakuX GPU/white frame owner fault remains a separate unresolved issue; merged instrumentation #168/#169/#171 does not certify game frames.
- Dreamcast full multi-track/descriptor support, original-console library, ROM management, import/delete/archive and save isolation keep independent issue scopes: [#143](https://github.com/safetyblade/ROMMachine/issues/143), [#145](https://github.com/safetyblade/ROMMachine/issues/145), [#146](https://github.com/safetyblade/ROMMachine/issues/146), [#147](https://github.com/safetyblade/ROMMachine/issues/147), [#148](https://github.com/safetyblade/ROMMachine/issues/148). Do not regress preservation and launcher/setup paths.

## 7. Cross-stream delivery and acceptance gates

| Gate | What is required |
| --- | --- |
| **G0 Baseline / rollback** | Exact integrated APK, package/signing, master commit, existing ROMs/library/saves, controller mappings, known working games; preserve install-over |
| **G1 Source correctness** | Native button inventory, layout fitter, screen class/render policy, control action evidence/title identity, safe shelf transformations; pure tests/fixtures |
| **G2 CI / APK** | Kotlin lint + unit + actual ARM64 native host/core packaging + final installable artifact from SAME source and engine versions; don't count only preflight |
| **G3 Owner device** | GripArc BOTH combined modes including PivotArc, no missing buttons, console-specific inputs; TV/landscape Fill vs Native with remote; controller hotplug; shelf actions and Undo; actual game action mapping on sampled titles |
| **G4 Marcus independent QA** | PASS/FAIL/NOT TESTED matrix including source SHA, APK SHA/version, device/OS, console/core/controller/pad, real gameplay frame/action, saves/reopen and theme/48dp/gesture clearances. Failed or untested remains OPEN |
| **Release** | Cassia/owner acceptance + architecture/licensing and release review; no forced cross-lane sequencing, no disabling working functionality to satisfy one feature |

**Execution recommendation:** GripArc's failed physical integration #135 is a live blocker and must not be lost in new feature work. In parallel, complete #167 native Fill, #165/166 tablet/TV focus, #176/177 control evidence + normalizer (catalogue parallel), and post-merge shelf #183 device QA. Advance other emulation/ROM lanes separately with rollback.

**Primary truth and update mechanism:** Drive v2.6 controls owner decisions; GitHub source and issue links give implementation/PR/CI status; actual owner/Marcus testing gives physical acceptance. Update both roadmap surfaces after any new device result—especially GripArc, which remains **FAILED** as of this checkpoint.
