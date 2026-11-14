# ARCH-LIB-01 — Checkpoint B engineering and QA handover

Status: **DRAFT / HOLD — do not merge, do not start #146**  
Primary: #145. Related: #115. Baseline: merged PR #201, master `e9cfbdf39aea84faecff8fb8cfaf3ed760866610`.  
Review PR: #206, branch `arch/arch-lib-01-checkpoint-b`.

## Controlled implementation

- Room v12 retained with the original v11→v12 migration and UUID, frozen `legacyFileName` and `legacySourceUri` fields from PR #201. The *actual annotation-processor-generated* `12.json` is committed, with identity hash `de28f470fb057436fd1fa7b3d18cb85d`. Generated schema evidence: Actions run 43525440325, completed successfully. Hash has not been invented from the SQL.
- `CartridgeSaveBinding.kt`: per-UUID SRAM, states/metadata, and preview directories; core/namespace kept distinct. A legacy file is never overwritten or deleted. An entire legacy group is copied to private cartridge storage and SHA-256 verified, with a temporary file and rename, **before** the atomic activation manifest is written. Incomplete or conflicting prior copies throw without activation. Missing active storage is an error, not an empty new save.
- Ownership decision: only one currently indexed cartridge may match the frozen legacy filename (SRAM uses basename). When zero or multiple indexed records match, existing legacy progress is unassigned; new writes are isolated. A durable per-cartridge manifest records `fresh`, `verified-copy` or `ambiguous-unassigned`. This is **not sufficient to prove historical ownership** of saves left by cartridges already removed from the library; see hold gates below.
- `SavesManager.kt`, `StatesManager.kt`, `StatesPreviewManager.kt`, `DirectoriesManager.kt` and `LemuroidApplicationModule.kt`: wire the coordinator to actual production managers. Save errors propagate, and preview writes are atomic. Existing manager constructors used by prior tests retain their unbound default. No ROM or engine directory was moved.
- `DesmumeMigrationHandler.kt` and `GameLoader.kt`: deny raw global `.srm`/`.dsv` fallback for ambiguous DS ownership. Verified or already bound DS data retains existing format conversion and timestamp behavior.
- `CartridgeSourceMatch.kt`, `LemuroidLibrary.kt` and `DataFileDao.kt`: after successful scan enumeration, reconcile **only** a uniquely matched SAF authority + opaque document ID, within the same console, where the old source was not observed in this scan. Re-use old Room integer id, UUID, alias, artwork, custom name and favourite, re-parent new datafiles and discard only the transient new row. No name/title-only matching; no large-ROM hashing.
- `LibraryIndexWork.kt` re-keys shelf cells on such verified relocation preserving section, position, and cell ID; it rejects collisions rather than overwriting an existing shelf cell. `GripArcProfiles.kt` uses durable UUID keys for new writes, with read-only fallback to both prior URI-keyed and legacy source keys.
- Four inherited baseline compile faults addressed separately: GameMenuRoute enum order, suspend shelf read in Compose, Xbox Compose semantics imports, and missing save-error string `R` import. These were already failing on merged master, Actions run 43512019756; repairs are not evidence of successful integration.

## Save ownership / recovery contract

1. Never change the meaning of a previously activated binding when a ROM is renamed, moved, or the legacy folder is altered.
2. Do not activate a migrated group until **all existing members** in that group have verified byte-for-byte copies. Originals remain untouched, including after errors.
3. Do not copy an ambiguous global legacy group into two cartridges. Future writes go to separate UUID locations; ambiguous originals remain available for later explicit recovery.
4. If a partial target differs, fail closed, retain both it and the original, and require a controlled manual choice. Never promote the partial file.
5. Native cards, PPSSPP savedata, Wii NAND, and Xbox HDD are **not recursively copied or rewritten** by this implementation; preservation relies on their unchanged engine-specific persistence contracts and requires device verification.
6. Rolling back the PR before merging is a source rollback. **Do not downgrade a device after new UUID saves** and assume the older APK will see the latest progress. An export/recovery procedure must handle the UUID copies explicitly; no automatic reverse overwrite is allowed.

## Evidence and tests

- Room schema generation: **PASS**; compiler output and committed JSON identity hash as above. The Room compilation alone does **not** prove any installed v11 database opens correctly.
- Added `CartridgeSaveBindingTest.kt`: same-filename SRAM isolation, verified legacy SRAM plus manual/automatic state restore after rename, ambiguity isolation, incomplete-copy fail-closed, and DS global fallback denial.
- Added `CartridgeSourceMatchTest.kt`: unique SAF identity after tree URI change, present-original guard, ambiguous matching guard, and no title/filename/file-scheme/cross-console matching.
- Existing `CartridgeMigrationTest.kt` (actual synthetic v11→v12 Room reopen), `SearchMigrationTest.kt` (prior migration chains) and cartridge/shelf/controller regressions remain required. **Do not mark PASS until full CI test logs contain successful assertions**.
- Integrated ARM64 workflow must include source lint, app compilation, Kotlin unit tests, all native engines, both Wii and Wii+Xbox APK outputs, and retained failure logs. An Actions job labelled skipped/cancelled is not a PASS.
- Hardware installation over the existing v11/v12 app, ROM folder, original saves and customisation is not tested through the connector. Marcus must independently verify on a real device, without uninstall or reset.

## Unresolved release blockers for #145 / Marcus

- **Full identity continuity:** only unique SAF document-identity moves are linked automatically. File-system moves or provider changes that allocate a new document ID cannot be proven without a retained fingerprint or explicit confirmation. This branch does not implement a user-facing ambiguity resolution UI.
- **Removed-then-reimported content:** current cleanup drops missing Room game rows and the shelf prunes missing cells. There is no durable tombstone/fingerprint ledger to restore UUID, customisation and shelf position after an entirely separate completed refresh. This gate remains **OPEN**; do not claim reimport acceptance.
- **Historical legacy collisions:** a uniquely named *currently indexed* cartridge is not necessarily the original owner of a file left behind by a removed game. Recovery provenance and user selection are still required to eliminate that ambiguity.
- **Native persistence:** real PS1 native cards, PSP savedata, Wii NAND, Xbox HDD and other system-specific storage must be assessed for independent title ownership and regression; this PR has not migrated those formats.
- **Error recovery:** failure is visible to the caller, but the app lacks an owner-facing recovery selection flow for ambiguous legacy and incomplete migration.
- **Full CI and device evidence:** no hardware PASS until Marcus records exact handset/app install-over/source ROMs and successful save/reload. Full Kotlin and ARM64 results must be checked against the precise PR HEAD, including distinguishing pre-existing faults from added regressions.

## Marcus independent QA sequence

1. Back up existing application data, ROM folder, SRAM, states, previews, native persistent directories and shelf settings; record package, signing identity, build SHA and installed DB version without changing them.
2. Install over v11 (and an already-installed v12 test fixture), reopen the app and confirm Room migration hash, original UUID stability and preserved shelf/cover/favourites/GripArc/appearance.
3. Launch an unchanged legacy GB/GBC/GBA/DS/PS1/PSP save, verify previous progress **and** a newly written save survives force-stop/reopen, with original legacy files unchanged.
4. Install two different game contents with identical filenames; assert both future SRAM/manual/automatic state and previews remain distinct, and ambiguous original progress is not silently assigned.
5. Rename a document, move between SAF trees with stable document ID and refresh; check old Room id/UUID, shelf section/index, cover, controller settings, engine choice and saves remain. Verify missing or duplicate document evidence never reconciles incorrectly.
6. Test failure simulation (I/O denial, interrupted copy, stale target, missing active directory), with no partial authority, error visible, retry-safe original.
7. Regression all existing console launch, PivotArc, GripArc, portrait/landscape/tablet/TV appearance, folder refresh and native persistent saves. Record any skips or unverified paths instead of passing them.

## Feature boundaries

This commit only defines prerequisite identity and save contracts. **Do not start** managed import/delete #146, archives #147, authorised download #148, backup/restore #7, GEM-01 ratings or GEM-02 catalogue matching, and do not merge #206 until blockers and independent QA gates are closed.
