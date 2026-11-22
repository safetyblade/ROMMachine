# CARTDECK GC-01 — Whole-team design review and decision recommendations
22 November 2026 | Draft PR #362 | **Design guidance, not source implementation or role sign-off**

This document presents a role-by-role design assessment of already documented CARTDECK contracts. It does **not** claim that Cassia, Daniel, Sienna, Elliot, Roxy or Marcus have actually attended, signed or approved. The recommendations are prepared for Jonathan's explicit owner decision. Original GC A–H requirements (#262), control #304 and independent QA #248 remain authoritative.

Sources: the existing [cross-feature design supplement](GC01_FULL_COMPLETION_DESIGN_SUPPLEMENT_2026-11-22.md); GC issue #262; original Sienna 25-screen M00–M24 A–H design on the engineering/gameplay-consistency-gc01 branch; #290 SafeSave and its JR01 architecture, source audit, technical API and Junior UX contract; #308 controllers; #172 Modern Function; #85 Wii; #135 GripArc; #194/#210 GEM; #360 SIGNATURE. Source engineering and physical tests are separate from these recommendations.

**Decision state:** OWNER DECISION means a current conflicting choice or new policy needs Jonathan/Daniel approval before that aspect is changed. DESIGN CLARIFICATION means this specifies an existing approved requirement and can be implemented against live source. QA GATE means engineering evidence, not another product decision. Never merge master automatically.

## 1. Cassia Rowan — programme and overall product

**C1 — Scope (DESIGN CLARIFICATION):** Treat GC-01 as the full original eight workstreams: A one CARTDECK game menu; B game-first display; C physical controllers; D Wii-specific controls; E PivotArc/GripArc; F Modern Function game actions; G Xbox360 guest continuity; H honest engine/UI quality. SafeSave, GEM, player profiles/Junior, TV input and SIGNATURE are integrated deliverables, not replacements for A–H. Do not mistake a green APK or a merged historical issue for a completed workstream.

**C2 — Delivery (DESIGN CLARIFICATION):** Keep **one GC draft PR #362**, one latest same-head ARM64 build, reversible commits and the previous signed working APK as rollback. PERF #367 remains separate; CARTMOD website search and mod patching remain their own lane. No premature issue closure/merge.

**C3 — Priorities (DESIGN CLARIFICATION):** Build a visible, usable SafeSave Centre and its initial real native-save/state restore route first; complete the rest of supported-engine coverage, guided physical controller mapping/TV, GEM user journeys, and all other A–H. Intermediate commits are engineering checkpoints, not permission to stop early. Each delivered action must have real UI, service, adapter, tests and honest unsupported status.

**C4 — Unverifiable engine features (OWNER DECISION):** Recommend truthful capability-limited acceptance, not fake all-console savestates. Every active console must be inventoried; native saves must be protected where supported; engines without real savestates show Unavailable with the reason. Owner decides release eligibility when a genuine backend limitation remains. Never silently exclude a registered system.

## 2. Daniel Mercer — architecture and the six unresolved technical/product choices

### D1 — SafeSave database strategy (OWNER DECISION)
**Recommend:** Retain the already installed **separate SafeSaveDatabase v1** for immutable snapshots and operation journals; make a **forward-only additive migration in main Room** only for profiles and ownership/binding data actually needed. Use immutable cartridge/player/engine keys and crash-safe idempotent cross-store journal phases; independent databases cannot participate in a single SQLite transaction. Preserve existing v1 snapshots, v11→v12→v13 migration history, stable SaveBinding aliases, existing Play Google Drive sync and data after failed migration. No destructive recreation or new empty DB replacing existing content.

**Proof required:** Old populated data → upgraded and reopened profiles/snapshots → failed migration recovery, background sync conflict handling, reinstall-over and verified rollback of save files. Daniel confirms schema version and any live branch conflicts before code writes.

### D2 — Unassigned imported saves (OWNER DECISION)
**Recommend:** Allow a safe, fully validated .cartsave archive for an uninstalled or ambiguous game to remain in a **private read-only Unassigned Backups** list. Keep immutable source metadata and file; later manually **Link to Cartridge** only after verified platform/engine/region/edition ownership, permissions and conflict preview. Never assume identity from filename/title, invent a ROM/launch target, or grant Junior access. No silent expiry or background auto-deletion; explicit user cleanup with size/space warnings. Reject malicious and unsupported archives outright.

**Proof required:** Restore after cartridge later imported; wrong-region and lookalike-title refusal; preserved original backup; explicit delete only.

### D3 — Junior parent authentication and recovery (OWNER DECISION)
**Recommend:** Before enabling Junior, Manager must set up an actual locally supported verification and recovery method. Prefer Android's device-backed credential confirmation where supported; otherwise a rate-limited Manager PIN protected with an appropriate secure verifier **plus pre-enrolled recovery proof**. Do not promise TV/system-lock compatibility without testing, or invent a forgotten-PIN bypass. If verification cannot be established or recovery proof is lost, retain user data but deny protected elevation and Junior exit to unrestricted screens. CARTDECK Junior is not an OS-level kiosk.

**Proof required:** Back, deep link, auto-resume, OS lifecycle, import, profile switching and native launch cannot leak adult access. Offline works, false PIN attempts limited, save/profile data never erased to recover a forgotten secret.

### D4 — Physical and game-action mapping precedence (OWNER DECISION)
**Recommend this three-layer model:** 
1. Physical controller normalisation: Android button/axis/hat positions, calibration/deadzone, device identity and capabilities.
2. Guest translation: selected engine, original console button/stick/trigger output and physical gameplay port. Device-wide saved authentic profile is the default.
3. Optional per-cartridge mode: explicitly saved **Custom override** has precedence; otherwise **user-enabled and exact-release-verified Modern Function**; otherwise **Console Authentic**. Keep device calibration and menu device ownership separate from guest port and CARTDECK player identity.

**Preserve current mappings:** If existing stores behave differently, record precise migration proposal; do not overwrite working player mappings or impose a global A/B swap.

**Proof required:** Physical button -> correct guest input -> observed real game action, press/hold/release, same descriptor reconnect, two-player port isolation and TV mapping without touch.

### D5 — Fit versus Fill semantics (OWNER DECISION; historical conflict)
**Recommend:** **Original proportions / Aspect Fit as the default** with no crop or stretching, including TV/landscape, while preserving any previously saved explicit choice. Offer **Stretch to Fill (may distort image)** only as an explicit renderer-backed opt-in; never crop the game to masquerade as Fill. This proposal reconciles newer #210/original no-stretch intent with older #162/#167 default-Fill/stretch instructions **only after Jonathan approves it**. Do not change current renderer policies as part of unrelated SafeSave/controller work.

**Proof required:** Actual renderer output/GL or Vulkan source pixels, real 4:3 TV ratio, GB/GBA, DS/3DS dual screens and Wii U touch transforms. Do not claim an AndroidView size change implemented native Fill.

### D6 — Save states versus native game saves (DESIGN CLARIFICATION)
**Recommend:** Every engine adapter separately reports native saves, manual savestates, autosave/resume states, full shared memory cards/VMUs, archive portability and actual restore/reopen. Native game saves can be backed up even if that engine cannot create emulator savestates. Xbox360/Xenia is not advertised as supporting savestates. Never package mixed NAND/HDD, installed games, BIOS, OS or user account secrets as save files.

**Proof required:** One capability matrix per actual registered SystemID and active backend; distinguish SOURCE VERIFIED, DEVICE VERIFIED, NOT TESTED, UNSUPPORTED and BLOCKED.

## 3. Sienna Vale — UX and interaction design

**S1 — SafeSave home (DESIGN CLARIFICATION):** Five player-friendly paths: **Protect My Progress**, **Games & Consoles**, **Backups & Recovery**, **Import/Export**, **Other Players** (Manager only after authorisation). Each game clearly separates **In-game Saves**, **Save States** and **Shared Cards/VMUs**. Backup ≠ live Save State Now. Display owner, supported/unverified/excluded/locked coverage, file counts, total size, engine compatibility and restore consequences. Back Up Game / Console / Player / Everything / Full Card must be real actions; otherwise show a factual unsupported state, never a fake green button.

**S2 — Restore (DESIGN CLARIFICATION):** Select archive/version -> inspect/hash -> target player and exact cartridge -> compare incoming versus existing -> Skip / New Slot where supported / Replace After Verified Backup -> stage -> verify -> activate -> reopen -> receipt. Default must not overwrite. For a shared memory card, warn explicitly that multiple games are affected. For SAF destinations where readback is unavailable, say "Written; provider verification unavailable". For incomplete Backup Everything, say "All verified supported saves backed up", and list omissions; never imply absolutely everything was protected.

**S3 — Controller setup (DESIGN CLARIFICATION):** Route **Controllers → Select Device → Player Port → All Games / This Game → Authentic / Modern / Custom → Map → Test → Save**. Distinguish **Your button** (physical position), **Original console button** and **Game action** (only if verified). Capture press AND release; show actual native output, stick directions/neutral, analogue trigger range, chord conflicts and held input. Actions Conflict/Swap/Cancel/Undo/Reset and unsaved-change prompt. Unknown pads get optional first-use button calibration; known working pads use safe auto-map. Reserve a working Back/Menu escape. TV remote navigates the editor but is never silently assigned as gamepad.

**S4 — Profiles/Junior (DESIGN CLARIFICATION):** Reuse existing **Who's playing?**, Junior's **exactly one cover**, **Grown-Ups** and manager-gated save/export and exit. Include explicit error states: no approved Junior games; authentication unavailable; orphaned save; wrong ROM version; guest engine cannot isolate another profile; interrupted restore. Child cannot see another player's private backup names or absolute paths.

**S5 — Navigation and display (DESIGN CLARIFICATION + D5 owner decision):** Five adult destinations **Shelf, GEM, SafeSave, CARTMOD, Settings**, profiles from header, controller settings normal not Advanced, advanced technical diagnostics only under Advanced. GEM catalogue search is offline; CARTMOD owns website search/My Sources (current PR #362). TV/DeX focus and full-width shelf, controller-only navigation, single active input owner, Back returns to prior focus. For display labels, use "Original proportions" and "Stretch to fill (may distort image)" **only if owner approves D5**.

**S6 — SIGNATURE (QA GATE):** Original approved intro video/audio and actual wordmark geometry, adjustable startup audio, truthful media volume/focus, one cold-start replay, remote skip and safe framing. Source hash/host decoder pass ≠ real speaker/Bluetooth/HDMI or TV approval.

## 4. Elliot Shaw — engineering integration and failure handling

**E1 (QA GATE):** Reuse and mount existing SafeSaveSurface, PlayerChooserSurface, JuniorPlayroomSurface, GEM and controller UI components **with actual services**, not demo callbacks. No dead top-level destinations. Build real path from touch/controller to service to engine/file operation to persisted result.

**E2 (QA GATE):** For each active emulator, derive actual native-save location and state locations from runtime paths, not guesses. Snapshot complete metadata/sidecars and shared banks after engine quiesce, validate SHA and scope; keep old save until new journal commits. On failure try rollback; on rollback failure preserve all recovery material and show Recovery Required. Test low-space, symlink/traversal/zip bomb, wrong core, active sync writer and interrupted process.

**E3 (QA GATE):** Deliver the full native controller path: Android device event -> physical normalisation -> game mapping -> correct backend/guest port -> actual pressed/released game action. No duplicate event delivery to shelf, menu, Libretro and native host. Handle hotplug/disconnect/stale held axes, P1/P2 as supported, reserved menu escape, active capture and real TV/DeX no-mouse testing.

**E4 (QA GATE):** Wii Sideways/Nunchuk/Classic only where actual bridge supports each, with explicit unsupported IR/motion information. 3DS sensor/tilt (issue #365), DS stylus and Wii U GamePad touch require their own native capability; gamepad button mapping alone does not prove motion/IR/stylus.

**E5 (QA GATE):** Keep full original GC A–H source coverage, existing game renderer and save bindings, Android ID/signer, same-head native receipt and independent PERF #367. No new code branch or relaxed tests; publish exact downloadable ARM64 APK and test matrix. Partial PSP repair and green Robolectric unit tests are not end-to-end SafeSave acceptance.

## 5. Roxy Quinn — GEM catalogue, DeckFit and discovery

### R1 — My DeckFit precision (OWNER DECISION)
**Recommend:** **Whole-number 1–5 for My DeckFit v1**, compatible with the existing integer rating field; show simple five-step visual input and Clear. Half-star increments can be introduced only with an owner decision, versioned storage/migration and tests. Null means unrated, never zero. This is a recommendation, not an approved override of earlier speculative half-point artwork.

### R2 — Provisional editorial coverage (DESIGN CLARIFICATION)
Every eligible catalogue release has **DeckFit presentation**, but not necessarily a fabricated numeric rating. Use **Provisional 1–5** only where sourced evidence on controls, legibility, short sessions, orientation and peripheral requirements supports a reasoned estimate. Otherwise show **Not yet assessed**. Distinguish **My GemRating** (enjoyment 1–10), **Editorial DeckFit**, **My DeckFit** and **Runtime Verified**. A high DeckFit never promises the game runs.

### R3 — GEM user journeys (DESIGN CLARIFICATION)
Unrated newcomer -> neutral offline Browse/Rate a Game (not fake For You). Installed cartridge -> details -> rate/edit/clear -> reopen same installed cartridge. Uninstalled title -> local catalogue details/rating/wishlist -> Not in Library, no acquisition link. Rated player -> For You with sourced 2–3 reasons, console/install preferences, Not Interested and Surprise Me; preserve existing wildcard preference (current repository default documented 30%, no forced reset). All per profile; remain offline and deterministic.

### R4 — Source integrity (QA GATE)
88,246 are *release records*, not unique games or tested playable titles. DOS/FBNeo/MAME2003Plus gaps require lawful compatible sources and actual matching; uncertain title-only identities are not auto merged. Data-pack updates must not overwrite personal ratings, game saves, ROM UUID, custom art or SaveBindings. Include provenance, licence and last reviewed.

## 6. Marcus Reid — independent QA and acceptance contract

**M1:** Fresh and populated install-over, same signer/package, exact APK SHA; preserve original ROM identity, all native saves/states, SafeSave v1 snapshots, old migrations, preferences, controller config, artwork and GEM user ratings.

**M2:** Per-registered-console/backend matrix with separate **native in-game saves / manual states / auto-resume / full card or VMU / archive export-import / save-reopen / profile isolation**. Source supported and physically PASS are different fields. Unknown/unsupported/mixed guest content explicit.

**M3:** SafeSave backup Everything with a good PSP group, locked second engine and a third system without savestates reports partial coverage accurately. Test source/target corruption, provider no-readback, journal write failure, process death at every phase, wrong title/region/core, interrupted copy, cross-player leakage, low storage and entire two-game shared memory card restore.

**M4:** Unknown Bluetooth pad mapped using only controller UI: select, calibrate, reassign per-cartridge, map every supported button/stick/trigger, test, Save, launch and see actual guest action, reconnect retain binding, undo/reset. Repeat on handheld, DeX and native TV; 5-button remote can navigate configuration but does not replace gameplay pad. Real P1/P2 game test where backend supports it.

**M5:** Original A–H: one native CARTDECK menu, renderer geometry/DS/Wii U, Wii attachments, GripArc/PivotArc fit with all original controls and 48dp targets, verified Modern Function title action, Xbox360 native guest save/reopen, honest status across all systems, and startup speaker/Bluetooth/HDMI/TV video/audio.

**M6:** Every result needs exact Git SHA, APK artifact/version/signing, device OS, actual controller, legal game/save fixture, step/result and PASS/FAIL/BLOCKED/NOT TESTED. Do not promote device PASS from source compilation, host video decoding or tests alone.

## 7. Owner ratification register: six decisions

| ID | Default recommendation | Owner status | Interim safe rule |
|---|---|---|---|
| **OD-01** | Aspect Fit/Original Proportions default, optional explicit Stretch to Fill, never crop; retain existing saved display choices | **RECOMMENDED, NOT APPROVED** | Do not change current renderer or longstanding user settings |
| **OD-02** | Manager device credential or secured PIN + pre-enrolled recovery proof before enabling Junior | **RECOMMENDED, NOT APPROVED** | Never enable insecure parent bypass or destroy progress |
| **OD-03** | My DeckFit whole 1–5 v1; later halves require schema migration | **RECOMMENDED, NOT APPROVED** | Preserve existing integer rating storage |
| **OD-04** | Physical normalisation -> guest/port -> per-game Custom > verified opted-in Modern > Authentic | **RECOMMENDED, NOT APPROVED** | Preserve pre-existing physical/game mapping and escape |
| **OD-05** | Valid safe unassigned backup kept private/read-only indefinitely until explicit user deletion or verified cartridge link | **RECOMMENDED, NOT APPROVED** | No arbitrary name-match or auto-expiry |
| **OD-06** | Retain SafeSave v1 DB; additive main Room profile/binding tables, cross-store durable journal | **RECOMMENDED, NOT APPROVED** | No destructive database migration or snapshot loss |

**Ready for Codex:** Implement clearly approved existing scope and design clarifications immediately without a fresh design loop. Surface a focused decision only for six owner ratifications or a specific verified source conflict. Do not interpret this record as an approval to merge or close issues. One PR #362; exact-SHA APK; all original A–H retained.
