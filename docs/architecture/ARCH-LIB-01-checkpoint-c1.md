# #145 Checkpoint C1 — atomic refresh and recoverable shelf reconciliation

Status: **PARTIAL / draft candidate**. Related #115. No #145 completion or deletion-readiness claim.

## Recovered checkpoint

Base master: `7bfd14a28fd9b2ff6fd02b8af9c8881a6327aced` (14 November 2026).
Reuses merged #201/#206 Room v12, cartridge UUIDs, frozen legacy aliases,
verified SaveBinding copies, SAF document matching, existing shelf AtomicFile
and per-game controls. #213 archive/patch engines and #214 compile-first CI
are now merged; neither is reimplemented here. No schema change.

The final #214 head `42738d82604ac84a04c3d2d21b15c30dca8422d5` passed
[full CI](https://github.com/safetyblade/ROMMachine/actions/runs/43548930157)
and [schema CI](https://github.com/safetyblade/ROMMachine/actions/runs/43548930184).
Earlier run 43548752316 failed lint; it is historical, not a current master failure.
These are baseline evidence, not results for this candidate.

## Implemented source

- Enumerate and identify into bounded metadata staging before library mutation.
  New-game validation, duplicate URI rejection and BIOS provider reads precede Room commit.
- Existing-row refresh, additions, verified source relocation and stale game/datafile
  cleanup share one Room transaction. Re-read current player metadata at commit
  to retain edits performed during slow enumeration. Cancellation/failure rolls back.
- Atomically write an identity-map journal before indexing. Before every subsequent
  scan and at main-process startup, replay shelf reconciliation from committed Room
  UUIDs. This works whether Room committed or rolled back; no guessed commit bit.
- Reconciliation runs under ShelfStore's existing lock, preserving latest appearance
  and arrangement. Rekey existing cells using cartridge UUID ownership, retain cell
  IDs, reject a destination conflict, and clear intent only after atomic shelf success.
  Replaying an already-written shelf is idempotent. Corrupt/future journals fail closed.
- Prevent automatic UI reconcile and Undo from adding stale/destination cells while
  recovery is pending; completed work reloads the shelf through its existing observer.
- #115 failure snackbar has Copy Report. Report includes run UUID, actual operation
  phase and sanitized category; no raw exception, title, filename, path or URI.
  Permission and enumeration currently share one phase because provider Flow owns both.
- BIOS formats/locations and all native persistent stores stay on existing routes.
  BIOS import is an existing external side effect, not a transaction with Room; originals
  remain in source storage. BIOS cleanup failures retain the shelf recovery intent.

## Validation checkpoint

- **PASS:** `./gradlew ktlintFormat ktlintCheck --no-daemon --no-configuration-cache --max-workers=2`.
- **PASS:** `python3 -m unittest discover -s scripts/ci -p 'test_ci_kotlin_gate.py' -v` (4 checks).
- **PASS:** `git diff --check`.
- **BLOCKED locally:** production/test Kotlin compile, JVM assertions and APK assemble:
  dependency `com.github.Swordfish90:LibretroDroid:0.14.0` POM returned JitPack HTTP 403.
  Attempt to retrieve the successful CI pinned-host artifact also returned HTTP 403
  from this execution environment. No dependency or TLS/security gate was weakened.
- **NOT TESTED at initial submission:** new JVM tests until candidate CI executes.
  Added real Room late-enumeration and transaction-rollback tests, restart/rollback/
  shelf-conflict journal tests, and sanitized-report test. Existing migration, disc,
  save-binding, shelf, controller and appearance tests remain enabled.
- **NOT TESTED:** v11/v12 handset install-over, physical process kill, saves/reopen,
  gameplay/audio/video, controllers, TV/tablet and native stores. No candidate APK
  built locally; baseline artifacts are not presented as candidate artifacts.

## Limits and next bounded package

This is the first independently reviewable part of PR-A, not all Checkpoint C.
Removed-then-reimported cartridges still lose their Room identity after successful
cleanup; durable tombstones/content evidence and explicit recovery UI remain required.
Historical legacy save ownership remains uncertain where removed cartridges may have
used a shared basename; no new automatic assignment policy is introduced here.
Native persistent-store ownership still needs engine-specific QA. No managed import,
deletion, archive expansion, Find Games, artwork provider, backup or Gem UI is enabled.

C2 should add a versioned durable retired-cartridge/source ledger and preserve shelf
positions for unavailable cartridges, followed by explicit identity/legacy-save recovery
choices. Do not infer ownership from title or filename. Review generated schema and
actual migration tests before enabling source moves/reimports. GEM-01/02 remain gated.
Only after source/migration safety evidence should PR-B start; PR-C destructive actions
retain independent ownership and deletion tests. Then PR-D, E, F and G as in owner brief.

## Marcus QA handover

1. Snapshot original v11/v12 app, SAF grants, shelf, covers, controls, progress and native
   stores; record package/certificate/version. Install over, without uninstall/reset.
2. Refresh unchanged library; preserve UUID, custom names/covers/favourites, shelf
   section/position/spacing, appearance and controls. Exercise all existing key paths.
3. Fault provider access/enumeration late in a >200-entry scan: no partial new rows or
   renamed rows, original library remains playable; Copy Report contains phase only.
4. Inject DB cleanup failure after inserts: all Room changes roll back. Retry succeeds.
5. Kill before DB commit, after DB commit, during shelf write and after shelf write
   before journal deletion. Reopen offline/revoked permission: recovery derives from
   Room without rescanning; cell identities/positions and appearance survive.
6. Rename/move a supported stable SAF document, refresh, save/reopen. Test duplicate
   destination conflicts: shelf/journal retained, visible error, no silent overwrite.
7. Verify Wii/Dreamcast and proven legacy/DS/PSP paths, engine fallback, audio/video,
   PivotArc/GripArc, phone/landscape/tablet/TV. Record PASS/FAIL/BLOCKED/NOT TESTED.
8. Removed/reimport and ambiguous-legacy selection are **not accepted** by this PR.

## Rollback

Before merge, close draft and discard its branch. After merge, revert this source commit
and roll forward an APK with unchanged app ID/signature and increasing version code.
No database version change or save-path migration is introduced. Complete pending shelf
recovery with this version before installing code that lacks the journal; preserve the
journal and shelf for support if recovery conflicts. Never downgrade/reset app data or
copy legacy saves over newer UUID progress. No automatic merge/publication authorised.
