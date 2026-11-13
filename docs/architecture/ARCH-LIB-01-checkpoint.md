# ARCH-LIB-01 / #145 — checkpoint A, NOT engineering complete

## Baseline and concurrent work

Base: `57ba2869398ca832aa68e724931ce389d89f93a0` (fetched master).
The starting workspace was clean at `a9b6e4b7cc9674e52f70ce667440047445b81f84`;
implementation branched from fetched master, not that older checkout.
Read #145 and #115 including their comment collections (both empty).
Read the linked architecture and roadmap via Google Docs.
Open PRs initially considered: #192 (appearance collections) and #196 (stacked adaptive patterns).
During implementation master advanced to `6aeade19ed1cfdf9c052ef3bd45685468348d8ab`,
merging #192 and #200 (shared pause menu). That master was merged into the checkpoint
branch without conflicts. Their changes are inherited, not part of this implementation diff.
Master already includes Dreamcast controller clearance #190 and Xbox submission #189;
no controller geometry, appearance, emulator, registration or native code changes here.

Baseline integrated workflow: https://github.com/safetyblade/ROMMachine/actions/runs/43501389633
Both Wii and Wii/Xbox APK jobs failed at `compileFreeBundleDebugUnitTestKotlin`:
`GripArcRenderingTest.kt:72:56 Unresolved reference 'oversized'`.
Source preflight, patched host and native engine build jobs passed. This is inherited,
not evidence that the candidate compiles or passes integration. Do not bypass that gate.

## Recovered working contracts

* Room v11: integer `Game.id`, UNIQUE `fileUri`; DataFile foreign key to integer game ID;
  FTS4 and title search triggers; migration chain 7→8→9→10→11, committed schemas 8–11.
* `LemuroidLibrary`: provider enumeration, disc-family merging, metadata lookup, concurrent
  batch writes, then game/data-file pruning. Incomplete enumeration prevents cleanup but
  does not roll back earlier batch writes. BIOS import is an external filesystem side effect.
* `LibraryIndexWork`: Room scan, then separate `ShelfStore` AtomicFile reconciliation,
  then core scheduling under one catch. No durable cross-store checkpoint.
* Shelf keys hash system/URI; cells, hidden/presentation state, order, appearance and sections
  reside in separate JSON. Custom name/cover, favourite/history reside in Game. Cover files
  use integer Game.id. URI-keyed GripArc profiles, controller configs and engine preferences
  are separate stores; emulator compatibility and shortcuts also use integer IDs.
* SavesManager SRAM uses basename `.srm`; legacy engines share root, modern candidates use
  engine-save-ram/core. Existing PCSX modern raw-card seed verifies format and size.
* StatesManager automatic/manual files use full filename under engine/version namespaces;
  metadata is a separate atomic file. Previews use filename and engine namespace.
* melonDS `.sav` import and DeSmuME `.dsv` compatibility use filename-derived names.
* GameLoader also supplies persistent filesystem directories: legacy root, native/system,
  or engines/core. PSP savedata seeding, native memory cards and Xbox virtual HDD are not
  equivalent to SRAM and cannot be globally moved by a filename migration.
* SAF grants and provider scheme selection remain external source contracts; launch resolves
  `fileUri`, datafiles, descriptor families and staging caches. Current disc classifiers,
  CachedRomStager verification, registered systems and fallback paths remain in place.

## Implemented schema and identity changes

Room v12 adds only `games.cartridgeId` (non-null UUID string, unique index),
`legacyFileName` and `legacySourceUri` (nullable compatibility aliases).
Migration 11→12 generates independent random UUIDs inside Room's migration transaction,
freezes each existing filename/source, and preserves all existing columns, integer IDs,
DataFile relations and indexes. The migration is registered in the application builder.
New Game objects generate UUIDs; data-class copies preserve them and the aliases.
Room's schema-12 export still requires successful kapt/build before merge.

No identity is inferred from filename, title, CRC or a regional catalogue match. Same-name
rows receive distinct UUIDs. This checkpoint DOES NOT implement source relocation matching,
content fingerprints, alias history, missing-record tombstones, archive-member source keys
or retaining identities across deletion/reimport. Current fileUri uniqueness is unchanged.
An installed cartridge is not a catalogue title or release.

## Save compatibility and error reporting actually implemented

`Game.saveFileName()` uses the frozen legacy filename. SRAM, manual/automatic states,
previews, melonDS and DeSmuME compatibility lookup use it. A rename with the same SAF document
URI therefore retains filename-based progress; refreshed Game.fileName remains current for
launch. No old save is renamed, copied, deleted or reformatted.

SavesManager.setSaveRAM and StatesManager state writes now rethrow exhausted retry failures.
Manual slot, exit-save and background-save flows show a storage failure message; failed
exit-save does not report successful finish. The loading guard resets in finally.
Existing namespace/version incompatibility checks and PCSX/PSP seeding remain unchanged.

CRITICAL GAP: this checkpoint does NOT isolate same-name legacy SRAM/states or native
persistent files. Historical collisions cannot be resolved by guessing which cartridge owns
shared progress. Both originals and their source aliases must remain available when the
next slice introduces explicit save binding and copy→verify→activate. The alias fields here
are compatibility scaffolding, not a completed SaveBinding or collision-resolution system.

## Refresh consistency and #115

No refresh transaction or diagnostic architecture is claimed in this checkpoint. Existing
incremental writes, cleanup and post-commit shelf failure risks remain. No crash-recovery
journal, user-copy diagnostic report or complete stage classification is implemented yet.
The phone's #115 cause remains UNPROVEN; its generic SCAN_FAILURE cannot distinguish DB,
shelf, metadata or scheduling exceptions. The source risks above are confirmed by code,
not reproduction of the owner's failure. Prior #116 stream/disc improvements are retained.

## Tests and evidence

Added Robolectric tests:
* independent UUIDs for identical filenames and preserved aliases on Game.copy;
* installed SRAM/manual/automatic state reads after a same-document rename;
* exhausted SRAM/state writes propagate and preserve a sentinel storage failure fixture;
* actual committed v11 schema → v12 retains duplicate-name rows, customisations and aliases,
  generates distinct UUIDs and retains IDs on database reopen.
Updated existing actual-v10 migration/search test to traverse v12 and verify frozen aliases.
All ROM fixtures are synthetic; no commercial titles or game files are required.

Local attempts:
* Initial wrapper run: BLOCKED (default Gradle home lacked distribution; DNS unavailable).
* Cached Gradle with default Java: BLOCKED (default Java 21 lacked compiler capability).
* Cached Gradle + workspace Temurin 17 + SDK: unit/build resolution BLOCKED, missing cached
  `com.github.Swordfish90:LibretroDroid:0.14.0` and other dependencies in offline mode.
* Standalone ktlintCheck: BLOCKED resolving uncached ktlint runtime dependencies
  (including kotlin-logging and sarif4k). No lint or Kotlin test PASS is claimed.
* Existing Python Dreamcast descriptor fixtures: PASS (9 tests); this does not execute Kotlin or gameplay.
* `git diff --check`: PASS. This is whitespace validation only.
* Initial candidate run 43510195913: fast source contracts PASS; Kotlin lint FAIL in new
  test formatting. Corrections committed; replacement run must pass before any merge.
* Integrated ARM64: baseline FAIL as above; candidate CI must be inspected separately.
* APK: none produced locally. No install-over or gameplay success claimed.

## Functional regression matrix / Marcus handoff

| Path | Status | Required evidence |
| --- | --- | --- |
| Actual v10/v11 DB upgrade, aliases, UUID persistence | NOT TESTED | Run added Robolectric tests after dependencies/build gate recover |
| Duplicate filename identity | NOT TESTED | Added fixture; save isolation remains unimplemented |
| Existing SRAM/state, same-document rename | NOT TESTED | Added fixture plus install-over handset test |
| Save write failure and retry/exit UI | NOT TESTED | Added failure fixture plus low-space/revoked storage device path |
| Retro/native registration and launch prep | NOT TESTED | Representative existing paths with integrated native bundle |
| Disc families and engine fallback/version | NOT TESTED | Existing synthetic fixtures plus device paths |
| Shelf/art/favourites/order/search | NOT TESTED | Refresh and launch existing customised library |
| Controls/appearance persistence | NOT TESTED | Install-over and restart on owner handset; no layout edits here |
| Interrupted/revoked SAF/DB/shelf recovery | BLOCKED | Transactional refresh/recovery slice still required |
| Native cards/filesystem/Xbox persistent progress | NOT TESTED | Existing paths retained; binding work still required |
| ARM64 APK/install-over | BLOCKED | Candidate integrated workflow and signing/version verification |
| Hardware gameplay and release | NOT TESTED | Marcus independent technical QA; Jonathan gameplay acceptance |

Do not ask Marcus or Jonathan to uninstall, clear data, rename ROMs or sacrifice saves.
No hardware acceptance is inferred from any source/build check.

## Remaining work and Daniel decisions

Before #145 completion: stable source/content relationships; uniquely verified move matching;
missing-source retention; explicit engine save bindings and duplicate-name conflict recovery;
transactional inventory/commit; durable shelf recovery; scoped private diagnostics/copy UI;
fault-injection tests; generated schema; passing integration and install-over/device evidence.
For large discs use bounded candidate fingerprints only to select verification candidates;
never auto-merge on samples, filename or title. Full verification may be deferred to an
explicit move/import operation rather than mandatory multi-GB reads every refresh.

Daniel review questions for the next slice:
1. Preserve intentionally duplicated cartridges as separate library instances even with equal
   full hashes? Proposed default: yes; only unique vanished-source matches can relocate.
2. How should the player explicitly assign historically shared/ambiguous legacy save files?
   Proposed default: retain original unassigned; never duplicate/activate guessed ownership.
3. Which native persistent stores require engine-wide bindings versus cartridge bindings?
   Do not split platform cards/HDDs by cartridge without proven engine compatibility.

## Rollback / recovery

Before installing any migration candidate, retain an app-private/data backup through the
existing authorised process. Do not uninstall or clear data. This draft has no approved APK.
Revert the source commits before release if this checkpoint is rejected. Once v12 has been
installed, an unmodified v11 APK cannot open the upgraded Room database: DO NOT attempt a
blind downgrade or enable destructive fallback. Roll forward with the same package/signing
and a higher version code, retaining v12 fields; or restore the complete pre-install snapshot
under a tested recovery procedure. Do not copy only a live SQLite main file without WAL.
Old save files remain at original paths. No copy migration was activated in this slice.

## Changed source files

ApplicationModule, Game, RetrogradeDatabase, Migrations; SavesManager, StatesManager,
StatesPreviewManager, MelonDsSavesMigrator, DesmumeMigrationHandler; BaseGameScreenViewModel
and the save failure string. Tests: CartridgeIdentityTest, CartridgeMigrationTest,
SearchMigrationTest, TitleSearchTest SQL fixtures (new required UUID), actual schema-11 fixture. No other functional code is changed by this lane.

## Delivery boundary

Draft checkpoint only. No merge, release, future-feature implementation or automatic #146
work is authorised by this document. #145 remains OPEN and NOT engineering complete.
Readiness for #146: BLOCKED until the remaining identity/save/refresh contracts and QA pass.
