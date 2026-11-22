# CARTDECK GC-01 — Owner lock: GEM, default controller swap, Junior PIN, Wii/3DS motion
**Owner decisions:** Jonathan, 22 November 2026 (AEDT)
**Execution location:** Draft GC PR #362, original GC-01 #262; PERF core/host work under #60, issue #365 retained for native 3DS sensor bridge. No master merge, release, new GC PR or automatic issue closure authorised.

**Precedence:** This owner update supersedes conflicting older text in GC01_TEAM_DECISION_RECOMMENDATIONS_2026-11-22.md, GC01_OWNER_APPROVED_DECISIONS_2026-11-22.md and GC01_GEM_CONNECTOR_READY_DATA_ARCHITECTURE_2026-11-22.md regarding mapping conflict default, Junior verification UX and catalogue-update friction. All prior A–H, SafeSave/data integrity, signed install-over and independent QA safeguards remain.

## 1. Locked owner decisions

| ID | Approval / current intent | Engineering implication |
| --- | --- | --- |
| **GEM-C01/C03** | YES to connector-ready ingestion and scalable indexed offline database. Connectors can improve game characteristics and catalogue coverage; no online dependence for normal GEM. | Prefer versioned local indexed source/feature packs, reusable provider-neutral importer contract, stable release IDs/ratings; choose SQLite/sharding by measured tests, not fashion. |
| **GEM-C05 user editing** | The historic catalogue is **finite**: old platforms are not producing new official releases. Owner does not require onerous governance for personal metadata modifications or enrichment. | Personal changes allowed freely, separately stored; no rights/source approval UX for user's own tags/notes. Existing ROM/save/ratings identity still protected; source redistribution/licensing for shipped bundles still legally necessary. |
| **OD-03** | My GemRating **whole 1–10**, My DeckFit **whole 1–5**, always. Both are player-entered; GemRating is primary recommendation preference signal. | Rating null=unrated; no half increments, inferred user GemRatings or editorial overwrite. |
| **OD-04** | **Direct two-press mapping, LIVE visual, automatic reciprocal SWAP by default.** | If physical RT previously sends action A and physical X sends action B, choosing the RT assignment then X results in X→A, RT→B. Do not unassign B or leave duplicate bindings. |
| **OD-02/Junior** | **Simple parent PIN** to exit Junior mode to adult views, or modify Junior settings/permissions. | One simple 4–6 digit local PIN setup/confirm/change; ask once per protected action/grouped authenticated session, no unnecessary Android credential/Biometric requirement. Store salted protected verifier with modest rate limits; no plaintext and no child-accessible bypass. Owner has not defined lost-PIN recovery; make verified device-credential recovery optional where supported and document remaining unsupported TV case rather than promise OS-wide kiosk mode. |
| **OD-05/06** | Approved SafeSave console dashboards and Export All with maximum verified supported native save and state inclusion, **visible exceptions for remaining research**. | Do not block all exports merely because a native core cannot expose savestates; count games-with-saves, actual state slots, VMU/card groups, orphaned archives per console; no fabricated counts or fake completion. |
| **FG-01/03/04/05** | Approve manual Export All, optional opt-in on-exit backup where safe, no silent retention deletion; truthful archive privacy/compatibility; legal/source checks for redistributed package. | No unasked pruning, no mandatory cloud, cross-core state load only when proven, source verification only for redistributed bundles, not casual personal edits. |

### Boundary: finite historical game releases vs metadata evolution
Treat past released games as bounded in scope (aside from previously missed releases, revisions, translations, homebrew and user editions). **Game identity catalogue** can approach completion; gameplay metadata, mechanic tags, controller requirements, compatibility confidence and personalised ratings remain open to enrichment. Future connectors must be extensible but need not run continuously. Keep bundle approval conservative, personal edits simple and offline.

## 2. Controller mapping — reciprocal swap contract (owner-locked)

The first two presses are **source and new physical button**, not "map all actions" and not "replace and unmap original."

**Two-press flow, usable with one pad alone and on TV:**
1. Choose actual connected controller and optional game/console/port scope. Display a capability-accurate pad outline with glowing active button and current mappings.
2. "Press the button whose action you want to move." Example **RT**. Its button and **current guest action A** illuminate.
3. "Press the button that should perform that action." Example **X**. Its **current guest action B** illuminates.
4. **Preview automatic SWAP** with two arrows: "X will do A" / "RT will do B." Both old actions remain available. If there was no B binding, X gets A and RT becomes unassigned only if the user explicitly consents; default preservation is preferred, or offer a reversible duplicate only if engine supports.
5. One **Confirm Swap / Cancel** action. Default confirmation can be one press; do not require a third remapping operation. Show live controller diagram with old and new labels, distinct **physical button**, **guest native button** and **verified game action** layers.
6. **Test** with the controller produces real backend guest down/up and analogue axis values; **Save** persists scoped mapping; **Undo** restores previous swap, **Reset** original device/game defaults. Warn on leaving unsaved changes.
7. No action can steal the only tested menu/Back escape, override gameplay port without explicit permission, leak captured event to Shelf/game, or transform a required analogue trigger into on/off input silently. Preview loss of analog pressure if a digital X cannot replicate it; disallow unsupported analogue↔digital swaps until compatible semantics are proved.

**Current spec conflict resolved:** The previous "Swap / Replace / Cancel (default Cancel)" is superseded. **Swap is default**; Replace/Unassign is not a normal default action. No automatic apply without an explicit Confirm/Save. All controller types, unknown VID/PID, TV remote-only navigation and exact active native button inventory are still required.

**Model:** Physical normalisation → guest backend/port → per-cartridge Custom overrides > explicitly enabled verified Modern Function > Authentic. Reciprocal swap affects the selected scope, not every game. Persist original old mappings/compatibility metadata for true Undo. Preserve existing player mappings non-destructively on upgrade.

## 3. One GC Motion & Peripheral Inputs package — Wii + 3DS user experience

**Routing decision:** Pull **3DS motion/tilt PLAYER EXPERIENCE** (owner request #365) into existing **GC workstream D/C controller/input** on PR #362, alongside Wii mapping and special controls. **Do not transfer original Wii GC UX to PERF.** PERF #60/#365 continues ownership for any core, native host, Libretro sensor API, Android ABI/EGL/Vulkan/Dolphin/Citra/Azahar motion bridge changes, which are engine support—not player-facing layout/controls. Keep #365 traceable as a cross-lane dependency; do not open another independent GC feature PR or merge unrelated native changes blindly.

### 3.1 Shared motion modes and truthful capability
In **Game Controls → Motion & Pointer**, show only capability-relevant modes for active console/game:

- **Phone Motion**: use Android device sensors routed into actual guest sensor/motion interface when supported, orientation calibration, re-centre, sensitivity, deadzone, smoothing, hand orientation and paused lifecycle. For raw acceleration-dependent titles, orientation-only rotation vector does not suffice.
- **On-screen Motion Control**: independent, accessible virtual tilt/aim joystick, optional virtual pointer with drag/tap/click, and dedicated interaction gestures (including **Pull/Launch** for Beyblade where verified); these emit real guest sensor/pointer events, not circle-pad/D-pad masquerades.
- **Controller / Stick Pointer**: where actual Wii/3DS backend explicitly supports translated IR/pointer/tilt from stick; indicate sensitivity and button binding. Ordinary analog remapping must not be called motion sensor support.
- **Off / Use Game Defaults**: preserves normal native buttons/touch; correct mode defaults only where a real capability is proved.

**Owner preference:** try device-sensor motion for relevant 3DS/Wii titles where core supports it; always offer a user-selectable visual on-screen input alternative where technically feasible; modes saved **per game and per profile**. Do not show a functional-looking motion toggle if running backend ignores its events.

### 3.2 Console-specific surfaces
**Wii:** Keep original GC-01D Sideways Remote, Remote+Nunchuk, Classic attachment picker and per-game explicit choice. Optionally offer normal Wii Remote/peripheral profiles only if backend supports them. Map actual physical buttons/held state as designed for GC; add right-stick/on-screen IR pointer and sensor-to-gyro acceleration only through verified native Dolphin/Libretro path. Show "This game requires pointer/motion input not provided by the current engine" if no safe emulation. Do not assume a Wii Classic-capable title covers an IR-required title. Keep actual Wii NAND/save paths distinct and unchanged.

**3DS:** For *Beyblade: Evolution*, design distinct sensor input (phone movement) and virtual pad fallback that can cover **aim + pull/ripcord launch**. Both need native guest gyro/accelerometer signal proof; an ordinary virtual analog stick sending D-pad events is insufficient. Preserve circle pad/D-pad/C-Stick/stylus, dual-screen touch, portrait and landscape transforms; no overlay hiding required input. An available Citra core with no sensor interface is not evidence of working motion. Native sensor capability blocked under PERF is displayed as unavailable, not claimed done.

### 3.3 Motion HUD and calibration UX
- On first launch of a title needing motion, show concise choice: "Move phone / Use on-screen control / Off" with **Test Motion**, simple 3-position directional indicator and **Calibrate/Center**. If native sensor interface unavailable, explain why and offer only actually supported controls.
- The on-screen stick/pointer should be compact, movable and safely docked outside active touchscreen/dual-screen targets, >=48dp touch, no compulsory thick bezels or lost game pixels. GripArc and PivotArc layout solver must reserve space and avoid overlapping native console controls.
- Use high-contrast axis/tilt visual feedback with direction, recenter and input status; do not require using both physical sensor and virtual stick simultaneously. Switching neutralises previous guest sensor values.
- Android foreground only, pause/background/disconnect stops sampling, release events and reset tilt to neutral. Rotation compensation 0/90/180/270, sensor timestamp processing and calibration retained per session or opted per game. No telemetry or sensor history stored.

### 3.4 Evidence and boundary
- **GC test**: mode chooser, virtual controls, profile persistence, non-overlap, controller maps, per-console attachment and visible disabled states, handset/TV layout, no input leak.
- **PERF test**: actual physical phone gyro+accel and virtual signal reach chosen 3DS/Wii core backend, host sensor negotiation and unit conversion, no ABI or renderer regression, actual game responds, save/exit/reopen and Citra/Dolphin rollback. No unverified core swaps or engine source merges into GC by default.
- **Marcus owner device acceptance**: Beyblade aim BOTH directions + actual launch gesture; second 3DS motion title; a Wii title requiring IR pointer and a distinct Nunchuk/Classic title; phone sensors/virtual touch/physical controller, rotation, background/resume, sensor-unavailable fallback. All source/native status **NOT TESTED** until real device.
- Culdcept black-screen issue #311 is separate: motion support is not a fix for boot/render.

## 4. Remaining decisions and non-design blockers

**Owner product choices now settled:** connector-compatible GEM and scalable offline catalogue, integer-only user ratings, reciprocal controller swaps, simple Junior PIN, SafeSave visible console totals/Export All, conservative non-destructive migrations, Fit default with optional explicitly warned stretch. No re-opening A–H scope.

**Still narrowly undefined, recommend simple engineering defaults:**
1. **Junior lost-PIN recovery on TVs without a secure Android credential:** prefer pre-enrolled offline recovery code held by parent; no hidden universal reset. Keep Junior data recoverable; exact secure recovery mechanism needs Daniel security review but does not block PIN UI.
2. **Analogue trigger↔digital button swap:** warn "pressure control changes to on/off" and refuse unless game/engine action semantics are known compatible; otherwise allow explicit digital-only mapping override with test.
3. **Motion game compatibility when core lacks guest sensor support:** track PERF host/core integration separately and display unsupported; do not ship visual motion stick that merely animates without affecting gameplay.
4. **Scalable GEM index selection:** Daniel benchmark read-only versioned SQLite vs indexed segmented packs at 100k–500k release features; do not add network/update system merely because connector architecture is approved.
5. **Roxy data completeness:** prioritise per-release mechanics/playstyle/subgenre and controls metadata, provenance from source; personal GemRating always remains only user-set preference input.

All five are small implementation/security or capability-proof questions, not another major design stage. Codex should keep proceeding with the approved GC #262 A–H scope, SafeSave, GEM and controllers, with PERF native engine work linked not duplicated. **No PR merge, physical QA PASS or release approval** from this guidance.
