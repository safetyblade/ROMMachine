# GC-01 — CARTDECK Gameplay Consistency: Full Engineering Execution Contract
Owner directive: 15 November 2026. Status: approved scope, NOT implementation or QA acceptance.
Lead Elliot Shaw | Director Cassia Rowan | Architecture Daniel Mercer | UX Sienna Vale | QA Marcus Reid | Product acceptance Jonathan.

## Superseding architecture decision
The entire user-facing in-game menu is NATIVE TO CARTDECK. No user-facing Libretro, SDL, Xbox, PS2, Wii, PSP, or other emulator-specific menu, no grey platform diagnostic dialog, and no alternating UI ownership. CARTDECK draws and navigates one branded native menu, control configuration, display options and save/load journey. An engine adapter reports supported capabilities and executes operations through the original backend; native engine timing, game state, saves and input ownership remain protected. Advanced engine options may be presented inside CARTDECK, not as a competing UI. Do NOT rewrite functioning emulator internals to achieve presentation consistency.

## Programme-wide delivery standard
Each engineering assignment must cover the complete user outcome, not one code change. Perform code/PR/CI archaeology, technical research, dependency reconciliation, architecture and UX review, implementation, deterministic tests, exact-SHA APK packaging, actual device execution where available, rollback proof, independent QA, and Cassia/owner decision. Do not treat a build, merged code, a test mock, screenshots alone or source documentation as proven gameplay.

## Prework required before coding
1. Recover exact master HEAD, branch base, submodule SHAs, active/draft/merged/reverted PRs, issue mapping and overlapping implementations. Reconcile baseline rescue #246, owner closeout #247, independent physical QA #248, identity continuity #145, and cross-console work. Report what IS implemented, only proposed, blocked or unverified.
2. Identify controlled latest actually successful integrated ARM64 debug APK, CI job URL, exact SHA/hash/versionCode/package ID/signature/ABI, and compatibility with owner's installed version. Preserve an immutable baseline and viable revert. Do not uninstall or clear data.
3. For each actually packaged runtime, inspect launch, audio/video, pause, menu, save, resume, background, focus, exit, crash and reconnect: GLRetroView/Libretro, native SDL Xbox, PSP/PS1, Wii, PS2, Wii U and Xbox 360. Clearly distinguish advertised system, source integration, packaged engine and demonstrated Samsung gameplay.
4. Trace input delivery and ownership from Android and touch through existing GameViewModelInput, GameViewModelTouchControls, PadKit, GLRetroView and per-native SDL/JNI bridge, including axes, hats, triggers, held, repeats, chords, multiplayer and hotplug. NO new competing collector.
5. Trace actual renderer pixel aspect, framebuffer dimensions, safe insets, Android surface ownership and DS multi-screen/touch geometry, orientation, TV overscan and top-line bleed. NO crop or stretch to claim full screen.
6. Audit Room migrations, cartridge UUID and frozen SaveBinding aliases, SRAM, emulator save states, memory cards, native profile roots, SAF permissions, artwork, editions, appearances, shelf and preferences. Establish upgrade and rollback tests.
7. Check pinned NDK, native ABI, source dependencies, build scripts, test fixtures and legally usable ROMs; resolve clear blockers before escalating. Make evidence ledger and architecture dependency diagram.

## Daniel architecture review and Sienna UX review
Design minimally invasive interfaces: CartdeckMenuHost and MenuState; EngineCapabilities and EngineCommandAdapter (pause/resume/save/load/advanced/exit, errors and unsupported); ControllerIdentity/InputProfile/InputTranslator (existing dispatcher stays authoritative); ViewportPolicy (aspect-fit and correct coordinate transforms); PersistenceContract (cartridge identity and engine save roots). Define failures, focus, rotation, background and disconnect. Sienna must specify the universal menu layout, focus/Back/DPAD/touch behaviour, disabled actions, controller mapping/test, settings, game-first video and TV. Do not alter native bridge contracts before Daniel signs off.

## Full eight workstreams and issue mapping
A. CARTDECK NATIVE UNIVERSAL MENU — #191 PR #200: one menu, Resume, Save/Load if supported, Controllers, Display, Advanced engine options and Exit; eliminate competing user-facing dialogs and avoid duplicate pause and lost state.
B. UNIVERSAL SCREEN — #74 #162 #167: maximise safe game video without crop/stretch; native ratio, optional native integer scaling, no thick bezels/top-line bleed; portrait/landscape/tablet/TV; retain DS dual-screen and touchscreen alignment, slim brand trim; auto-hide controls only with viable physical pad, restore on disconnect.
C. PHYSICAL CONTROLLERS — #177 #179 #181 PR #231: capability discovery/VID PID where trustworthy, position-based defaults, generic calibration, player and device profiles, remap/test/save/reset, held inputs/diagonals/simultaneous buttons/triggers/axes, reconnect, fallback, native system parity.
D. WII — #85: sideways Wii Remote, Remote + Nunchuk, Classic profiles. Validate original engine requirements; do not invent gyro/motion/pointer capabilities; safe mapped fallback if technically supported, otherwise plainly disclose limitation.
E. PIVOTARC + GRIPARC — #135: reproduce actual Samsung failure while Standard + GripArc works; fix combined geometry, routing, held-state arbitration and native-console coverage, not a second input framework.
F. MODERN FUNCTION — #172 #176 #178 #180 #181: research-only action matrix is NOT runtime PASS. Verify source controls by title+console+region+edition/identity; opt-in action mapping with authentic/native fallback, no global A/B flip; test real game actions.
G. XBOX 360 PROFILE AND SAVE — #197 #255: durable profile ID, per-cartridge save root, settings, reopen/upgrade/controller continuity, no save PASS until real Xenia write/read demonstrated.
H. PS1/PSP AND ENGINE QUALITY — #56 #114 #261 #247: launch, video, audio, frames, stability, suspend/resume, save correctness; same-file SHA A/B where applicable; retain known working engine and rollback; do not misrepresent PS2/Wii U/X360 development as playable.

## Dependency-aware implementation
Gate 0 baseline/reconciliation and Daniel+Sienna shared contract decisions. Increment A native menu/engine adapters; increment B screen; increment C controllers; D Wii and E PivotArc/GripArc after canonical mapping, with device work in parallel; F title-specific Modern Function after identity+mapping; G X360 profiles once save runtime is proven. H emulator regression analysis starts immediately and runs in parallel, then follows normal integration checks. Each increment is independently reviewable but complete as an end-to-end user behaviour. Do not allow disjoint PRs to race or overwrite the current combined APK.

## Mandatory verification per increment
- Exact upstream/base/head commits and changed paths; research and dependency decisions.
- Passing relevant static/unit/integration tests including unsupported operations and failure paths, Room migration tests, no regressions, and exact-SHA full integrated APK CI, not a standalone toy build.
- Artifact URL, APK SHA256, versionCode, package ID, ABI and signer evidence; install-over preservation and rollback route.
- Actual licensed game fixture identity/checksum, Samsung model/OS, controller type, display orientation; recorded launch, frames/audio/input, pause/menu, native save and save-state separately, exit/reopen, crash/rotation/reconnect as applicable. Only mark handset PASS after device evidence.
- Hardware not available = NOT TESTED, not PASS. Marcus independent acceptance and Cassia release recommendation. Jonathan final acceptance.
- Stop if games/saves/UUIDs/profiles/artwork/editions/appearance/permissions regress; if native image crops/stretches; if input sticks or menu leaks; if rollback unproven. No mainline merge/release on green CI alone.

## Prompt construction rule for whole programme
Future prompts must be full execution briefs: objective; all affected engines/OS/UX; existing evidence; source inspection; prerequisites/research; architecture approvals; complete implementation incl edge cases; integration/regression tests; full APK and installer; physical QA; rollback and artifact provenance; defect register; acceptance matrix. Always run feasible prework first and report precise blockers rather than sending a speculative roadmap or leaving known gaps for the owner to discover.
