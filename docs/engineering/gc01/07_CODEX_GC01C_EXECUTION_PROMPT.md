# CODEX — CARTDECK GC-01C full controller source implementation

## Mission
You are the source implementer for CARTDECK by prestwich.dev. Execute **GC-01C Physical Controller Consistency**, according to `docs/engineering/gc01/06_GC01C_CONTROLLER_DESIGN_IMPLEMENTATION.md` and the approved Sienna/Daniel specifications `docs/engineering/gc01/02_DANIEL_SIENNA_DESIGN.md` and `04_SIENNA_COMPLETE_UX_DESIGN.md`. **Write code and tests, not more planning documents.** Work in context-window-safe source chunks, and do not ask Jonathan to do hardware QA until the whole A–H lane is ready.

## Baseline and consolidation (non-negotiable)
- Canonical consolidated parent: `integration/gc01-ps3-controller-candidate-20261116` @ `95c23db2de649dd3111fd3bd9dc2bde932d5d205` (two parents PS3 #276 and GC-01 #277). Integration PR #279. Current handoff branch `engineering/gc01c-codex-handoff-20261116` includes only these design files on top.
- Treat open historical PRs #271–#277 as **already represented** in integration tree, not seven separate future merges. Do not merge them individually or overwrite consolidated paths. PR #276's PS3 seven-port firmware/storage work is likewise represented.
- HEAD may advance. Before source edits, inspect commit graph, actual files, current CI and exact merged conflict resolutions. If someone updated the integration branch, reconcile deliberately and report it.
- No new emulator integration, console removal, Room migration, ROM identity rewrite or save-root rename. Keep exact working package identity and rollback. Never force-push, merge to master, publish APK or declare gameplay tested.

## Execution — produce source commits in these bounded chunks
**C1 — deterministic host Menu/Guide/Back ownership.** Audit Libretro `GameViewModelInput`, original Xbox SDL `NativeCoreActivity`, Xenia `Xbox360Activity`, PS2 `Armsx2RuntimeProbeActivity`, Cemu `WiiuVulkanRuntimeProbeActivity`, PS3 `Ps3RuntimeActivity`, TV `TVGameActivity`/`TVGameMenuActivity`. Introduce small pure event-edge state/helper only if it removes duplicated bugs. Consume host menu key **matched** down/up pair, ignore repeats/unmatched UP, clear on disconnect/cancel, fire once on release if opening a new Activity. No duplicate long-hold toggles. Preserve Android Back, Start/Select, PS3 seven-player and emulator shortcut semantics. Unit tests for doubles and unmatched events.
**C2 — shared focus/route semantics.** Use `SharedGameMenu` and `XboxGameMenu` as canonical style. Correct first focus, disabled Resume recovery focus, DPAD/OK/Back, Guide/menu escape, TV remote-only navigation, nested dialog cancel, large text / scroll. No emulator/game input while menu has focus. Compose tests for touch/TV/controller navigation, safe exit and recovery.
**C3 — device detection and reconnect.** Reuse `GameplayControllerPresence`, `ControllerHardwareProfile` and existing device/profile stores; use stable descriptor for preference and fresh per-session device ID for event state. Preserve port mapping and native PS3 user slots 1–7, exclude TV remotes as gameplay pads. Connected -> disconnected -> reconnected rebinds without stuck keys; restore mobile touch or TV connect-controller guidance with remote Back/Menu exit. Tests for same-name devices, remote, ephemeral IDs, hotplug, enabled preference.
**C4 — mapping/test/capture.** Implement approved first-pad and controller map/test journey in `PhysicalControllerMappingPanel` and existing native mapping panels. Capture physical location mapped to authentic native key; show live down/up/held, multiple simultaneous chord combinations, DPAD diagonals, trigger analog/digital duplicate, axes min/max/neutral, player port. Avoid pixel art redesign. Offer Capture/Cancel/Reset/Undo with safe state. Distinguish `Console Authentic` from supported `Modern Function` and Custom; unsupported functions show truthful disabled explanations. No conflicting binding overwrite or save mutation. Tests are essential.
**C5 — integrated source regression.** Run Kotlin lint, pure unit and Compose tests, exact SHA connected ARM64 APK pipeline (including PS3 real engines, Xenia, ARMSX2, Cemu and previously shipped cores), native ABI, 16 KB alignment, Room schema, save aliases, controller tests, DS touch, Wii mode, GripArc/PivotArc. Fix CI failures in source, not by disabling checks. Preserve setup-only PS3 `resume=false`. No fake emulation performance claims.

## Output acceptance packet (not physical QA)
1. One GC-01C PR based on the handoff branch, with commits linked to C1–C5. Keep master untouched and the PR draft while unverified.
2. For each chunk: source file diff, tests added/run, code ownership unchanged, risk, rollback and exact HEAD SHA.
3. On green CI: integrated APK artifact run/ID and SHA-256, versionCode/package signing identity, included engines and PS3 distribution-gate status; **an APK build success does not prove the games work**.
4. If CI blocked on pinned upstream binaries/expiry/licensing, give exact failing workflow step, log and remediation, do not fabricate an APK or weaken gate.
5. Handoff to Jonathan at end of **the entire GC-01 A–H lane** for physical-device QA. Marcus records independent evidence then; not now.
6. Return a consolidated baseline recommendation and rollback SHA with outstanding issues. Do not mark #262 or #266 complete solely on source compilation.

## Specific hazards to avoid
- PS3 `GameMenuCapabilities.resume` and GC `GameMenuPauseGate.retryResumeAfterFailure` were explicitly merged. Do not replace one with an older version. `GameViewModelInput` opens MODE on release, unlike older PS3 branch; verify input edge contract against focus-transfer bounce.
- Xenia/offline profiles, hakuX virtual HDD, PS2 memory cards, Cemu NAND, PS3 RAP/firmware, Libretro save slots and cartridge SaveBinding remain untouched.
- Cemu Vulkan surface/swapchain and Wii U GamePad letterbox pointer transform remain host-owned. DS dual screens/stylus are not controller axis inputs.
- No new global listener that double-sends a pad key to SDL/JNI/Libretro.
- No visible emulator performance diagnostics in normal play; diagnostic/test screen is voluntary only.
- Android TV remote is navigation-only; don't count it as connected gameplay controller.
- Existing controller profiles and mappings must round-trip. Unsupported mappings must not silently become active.

## Stop condition
Deliver source and automated test results, a complete candidate APK where permitted, and an accurately classified remaining list. **Do not merge/release to master** until owner authorises consolidation and end-of-lane hardware acceptance.
