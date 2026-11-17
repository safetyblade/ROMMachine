# CARTDECK Junior + SafeSave — Implementation Readiness, Delivery Slices, Marcus QA
**Prepared:** 2026-11-16 · Daniel/Sienna/Elliot/Marcus interface handover  
**Status:** DESIGN-READY; CODE/CI/DEVICE VALIDATION **NOT RUN**. No release approval. Preserve GC-01/CI-01 currently active.
**Required references:**
- `docs/design/jr01/JR01_PRODUCT_UX_SPEC.md` (Amendment 1 supersedes original multi-cartridge shelf)
- `docs/design/jr01/JR01_FINAL_SCREEN_INPUT_PARENT_CONTRACT.md`
- `docs/architecture/jr01/JR01_PROFILES_SAFESAVE_ARCHITECTURE.md`
- `docs/architecture/jr01/ENGINE_SAVE_SOURCE_AUDIT_2026-11-16.md`
- `docs/architecture/jr01/SAFESAVE_FORMAT_AND_API_V1.md`
- Active programme: `docs/engineering/CARTDECK_MASTER_ROADMAP.md`; original identity preservation #145.

## 1. Preimplementation control checks — evidence-based, not claimed CI PASS
| Check ID | Source inspected | Result as of this design audit |
| --- | --- | --- |
| RA-01 | `RetrogradeDatabase.kt`, `Migrations.kt` | **SOURCE VERIFIED**: current master Room v13, v12→13 retired cartridge ledger. No new migration run |
| RA-02 | `CartridgeSaveBinding.kt`, SRAM/state managers | **SOURCE VERIFIED**: cartridge-scoped copy/verify binding exists; profile isolation not implemented |
| RA-03 | `GameLauncher.kt`, `GameLoader.kt` | **SOURCE VERIFIED**: multiple launch paths/global native storage; policy/ownership service does not yet guard each path |
| RA-04 | `Armsx2Storage.kt`, `WiiuStorage.kt` | **SOURCE VERIFIED**: per-cartridge (not per-player) native roots; potential cross-player save leak if reused |
| RA-05 | `Ps3Storage.kt`, `Xbox360Profiles.kt` | **SOURCE VERIFIED**: engine-native user account constructs exist; need mapper and test |
| RA-06 | `XboxStorage.kt` | **SOURCE VERIFIED**: shared Xbox HDD + backup behavior; cannot label raw HDD save-only |
| RA-07 | `lemuroid-app-ext-play` SaveSyncManagerImpl + SaveSyncWork | **SOURCE VERIFIED**: legacy Google Drive sync and periodic worker; conflict guard required |
| RA-08 | Current signed APK, physical runtime, full-engine save inventory | **NOT TESTED** here — no APK generated, build/CI/device tests in this design-only task |
| RA-09 | All-engine save export and reimport | **NOT IMPLEMENTED / NOT TESTED** — adapters and SafeSave service planned |
| RA-10 | Cloud DB/object store/recovery | **NOT SELECTED / NOT IMPLEMENTED** — future optional, does not block offline implementation |
Source inspection is not proof that a given core can run, quiesce and restore on owner hardware.

## 2. Delivery order, each a separately reversible PR
**Gate 0 — baseline freeze:** At engineering start record master SHA, current schema, APK package ID/signature/version, actual installed ROM list, current stored games and legacy save hashes, native storage snapshots, applicable Google Drive config and controller mappings; preserve full verified compatible baseline backup. No uninstall or factory reset.

**PROF-01A — additive identity store:** next schema migration from HEAD (v13 as audited, use actual next version when implemented), stable immutable player IDs, default manager adoption, permissions/model, original save aliases unchanged, feature flag OFF. Unit migrations v11→v12→v13→next and schema exports; no UI/launch changes.

**PROF-01B — policy and switching:** universal player chooser, permissions at DAO, launcher and native host; Standard 'Hide' list and Junior 'Allow' list; session ownership/quiesce/close before switching; child gate. Existing adult shelf remains intact and unchanged.

**SAVE-01A — save ownership registry:** typed save-group and artifact inventory, lifecycle quiesce, memory-card and native-user mapping, journal, legacy collision quarantine. Preserve old save path as read-only source; bind new profile paths only on verified copy/restore. Add explicit per-engine `NOT_ISOLATED` failsafe. Source first.

**SAVE-01B — engine adapter increments:** common SRAM/states + PS1 card + DS first, then PSP/Dreamcast/GameCube/Wii, PS2 (Play! and ARMSX2), PS3, Xbox/Xenia, Wii U, remaining active engines. This is *test order*, not permission to drop engines or claim all-release completion. Each adapter requires save/reopen, other player isolation, standalone import/export and a coverage record.

**SAVE-02A — local SafeSave:** inventory UI, `.cartsave` versioned archive, manifest/checksums/limits, fully enumerated export of current/all players including memory banks, Android SAF destination, recoverable snapshot journal and honest skipped-group reporting.

**SAVE-02B — import / restore / player copy:** bounded import, ownership/game mapping and conflict preview, backup/atomic group restore/recovery, cross-player copy, external verification, wrong-ROM/engine protection. All test fixtures and independent roundtrip gates.

**JR-01C — Playroom & Grown-Ups:** Sienna's **single cartridge only** swipe/controller UI on portrait, landscape, tablet, TV, manager child allowlist/collections/theme/privacy/Save Centre, PIN/auth controls and clear error handling. No external catalogue or child-side ROM imports.

**SAVE-03 — future SafeSave Vault:** separate opt-in cloud security/product decision: account authentication, metadata database, private object storage, client encryption/recovery, quota, retention, upload/download conflict and cross-device device loss. Explicitly out of the current offline MVP and no backend deployment without separate authorisation.

## 3. QA matrix and gate contract
Marcus reports `PASS | FAIL | BLOCKED | NOT TESTED` with source SHA, app package/version, CI run ID, device model/OS, console/engine identity, evidence, and old/new save hashes where applicable. A source file's presence or mockup is NOT a PASS.

### A. Upgrade and integrity
- QA-001: Existing installed app upgrades without uninstall and retains ROM permissions, cartridge identity/cover/order, active engine selection and progress.
- QA-002: Room v11→12→13→next migration; direct v13→next; actual schema export and reopened DB + WAL coherence.
- QA-003: Same filename different ROMs, renamed/moved/re-imported files and retired cartridge ledger do not rebind saves incorrectly.
- QA-004: Default Manager adopts legacy saves without moving/deleting; ambiguous saves remain unassigned for manager manual resolution; no child inherits legacy saves.
- QA-005: Simulated interrupted migration/low storage/read-only source preserves original, resume rollback verified.
- QA-006: Reverse compatible roll-forward, no destructive downgrade to unopenable old Room schema.

### B. Profiles, permissions, UI
- QA-007: Manager, Standard, Junior (3 distinct) can coexist and switch, preferences/history/saves persist independently.
- QA-008: Manager sees shared ROMs; Standard hides individual ROM only for self; no ROM deleted.
- QA-009: Junior sees *only* approved games; new ROM import is default denied.
- QA-010: Attempt all bypasses: game search, recent, deep link, intent, widget/shortcut, in-game menu, native host, stale Activity intent, profile change and controller/system Back; no unauthorized launch or savedata exposure.
- QA-011: Junior→Standard/Manager requires gate offline; failed PIN never unlocks; recovery verified using parent-owned mechanism and attempt throttling.
- QA-012: One large cartridge only; swipe exactly one; controller D-pad/confirm/back; selection and save continuity after orientation and reconnect.
- QA-013: Validate Portrait Handheld / Landscape Handheld / Tablet (both) / Android TV 10-foot; focus ring, safe area, TalkBack, large text, reduced motion and child-safe empty/error states.
- QA-014: No extra games visible through Favourites, categories, save import, hidden metadata, previews or back-stack.
- QA-015: Active game profile switching quiesces and closes/blocks safely; next player cannot read old engine card/HDD/native user.

### C. SafeSave fidelity
- QA-016: Discover complete common SRAM and all state slots/previews plus correct missing/unsupported reports.
- QA-017: Raw PS1 memory card full 128 KiB card preserved and restored byte-for-byte; no duplicate misclassification.
- QA-018: DS .sav/.dsv and PSP SAVEDATA native directory round-trip with exact engine and no silent conversion.
- QA-019: PS2 ARMSX2 complete memcard and Play! memory-card persistence distinct, per profile; no BIOS included.
- QA-020: Dreamcast VMU, Dolphin memory card and Wii save-only, PS3 user SAVEDATA, Xbox/Xenia (native XUID), Wii U savedata adapter verification as *applicable*. Mixed-content HDD/NAND excluded from saves-only export and cloud.
- QA-021: Whole player export; all players export: includes every **verified** save group and shared memory card, lists every unverified/excluded group with reason/size when known. No false 'all protected' claim.
- QA-022: `.cartsave` manifest and checksum verifier rejects invalid hash, duplicate IDs/paths, traversal, absolute paths, symlink, bad magic, unexpected version, zip bombs, oversize/ratio.
- QA-023: Import same game and engine restores identical save bytes and in-game reopen; cross-engine savestate declared incompatible unless tested.
- QA-024: Conflicting restore takes verified destination backup; failed middle-step restores old target; no partial silent overwrite.
- QA-025: Copy save to different player preserves source byte hash, creates new writable target and requires owner/manager authorization; wrong player denied.
- QA-026: Low storage, blocked SAF provider, revoked permission, remote document provider without atomic rename/readback: truthful result and recoverable staged files.
- QA-027: No ROM images, BIOS, firmware, installed game binaries, account secrets, XUID Account or mixed-content console storage are in archives/cloud candidate.
- QA-028: Existing Play Google Drive auto/manual sync does not race profile save migrations, exports or restore; old remote save copies preserved; feature works in Free and Play flavors.
- QA-029: Engine save-write failure/retry and crash during quiesce/snapshot/activate never present stale success and never destroy prior save.
- QA-030: Backup created on Device A can import on Device B without ROM entitlement assumptions (requires matching user-provided ROM and compatible engine); provenance/game mapping safe.

### D. Optional Vault (later release gate)
- QA-031: Explicit per-profile opt-in; child cannot initiate upload without manager; logout does not delete local saves.
- QA-032: Storage and metadata per-account authorization, private object ACL, encryption, recovery on replacement hardware and invalid key failures.
- QA-033: Immutable versions, retry, quota, deletion/retention and two-device conflicts never silently overwrite last good snapshot.
- QA-034: Server does not receive ROM/BIOS/firmware, app logs, native account secrets or mixed-content virtual disks.

## 4. Mandatory build/proof gates
G1: design/source diff against protected baseline and no unrelated controller/emulator scope.  
G2: Kotlin format/lint/unit/Room migration/export schema + synthetic archive security tests.  
G3: one signed installable same-source ARM64 APK, unchanged package/signing and monotonic version; no fake PASS on skipped/cancelled jobs.  
G4: install-over, retained progress, source-based save round-trip and owner/Marcus actual physical testing on supported representative engines/inputs.  
G5: clean rollback/roll-forward and independent Marcus PASS/FAIL/NOT TESTED table. Only after G1–G5 all required paths pass is app release eligible.  
If any console's owned save type is not proven, its adapter is flagged UNSUPPORTED/UNVERIFIED and either profile switching/secondary launch blocks safely, or release is blocked if required product coverage cannot be met.

## 5. Preimplementation unresolved technical checks (engineering discovery tasks, not product indecision)
1. Map *every actually registered engine* to exact save directories, memory cards, native guest users, lifecycle flush events, settings and static resources. Capture evidence for each from source and a running APK.
2. Decide testable binding per native engine (system, card bank, cartridge, user) without assuming uniform one-ROM paths; PS3 guest user and Xenia XUID mapping requires special review.
3. Confirm any non-save content mixed with Wii U NAND/Xbox HDD before implementing saves-only export.
4. Audit parallel legacy Google Drive sync writer and define migration gating/worker cancellation/restart protocol.
5. Verify Android SAF streaming and recoverable write behavior with actual provider destinations, not only local files.
6. Confirm release scope of experimental consoles and whether their saves can be isolated; do not treat visibility or registry entry as device validation.

These are **implementation gate checks**, to be completed in the relevant PR and marked BLOCKED/NOT TESTED until demonstrated. The design decisions and acceptance contract are complete without inventing results.

## 6. Approved handoff wording
“Implement the CARTDECK universal profiles, Junior Playroom and SafeSave design in bounded PR increments. Read all five baseline specs, inspect current master and GC-01/CI-01 status, then start at PROF-01A after the active checkpoint. Never rewrite legacy SaveBinding or mass-move saves; use copied/hash-verified immutable ownership and per-engine adapters. Require a genuine profile-isolation proof before new player launches on any engine. Preserve signed APK, identity, legacy saves and existing Google Drive sync; build and verify every increment. Do not implement SafeSave Vault until separately authorised. Mark every QA line PASS/FAIL/BLOCKED/NOT TESTED with evidence.” 
