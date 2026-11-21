# SAVE-A01 — First SafeSave activation (PSP, PPSSPP Modern)

Status: draft implementation slice for review. Not merged. No device PASS claimed.

## Scope delivered in this PR

- Separately versioned Room database `SafeSaveDatabase` (DB name `safesave`, v1)
  holding snapshot/file/journal metadata only. Save bytes live on disk, never
  in database rows. Main `RetrogradeDatabase` stays at v13 untouched, so
  rollback is: revert this PR / delete the `safesave` database and snapshot
  root. No main-database migration, no downgrade risk.
- **PENDING ARCHITECTURE APPROVAL:** the separate-database decision still
  needs Daniel's approval. This PR flags it; it does not claim it.
- PSP-only adapter (`PspSavedataAdapter`) for one native savedata group under
  the PPSSPP Modern layout `PSP/SAVEDATA/<group>/`, verified against
  `GameLoader` / `PSPSavedataSeed` source. Symlinks rejected.
- Explicit association only: the caller supplies the immutable cartridge UUID
  and the group name. Ownership is never inferred from filename/title, and a
  group already bound to a different cartridge UUID is blocked.
- Capture only after engine closure/quiescence (caller-supplied gate),
  staged copy, per-file SHA-256 verification, then registration.
- Restore with destination backup, staged restore, hash verification and a
  recoverable journal in the index. Failures journal `FAILED` and preserve the
  original data. Nothing is silently overwritten.
- Minimal in-app access: `SafeSavePspAccess` finds verified PSP snapshots for
  one cartridge UUID and initiates capture/restore. No startup scans or
  hashing of all saves/ROMs; hashing covers only the one group in flight.
- Metrics: capture/restore return duration, file count and bytes. Peak memory
  is NOT measured in this slice (see checklist).

## Hard exclusions honoured

No Vault (#293), no cloud upload, no `.cartsave` export claim, no
memory-card/VMU claims, no all-console coverage, no changes to merged UNI
#358/SIGNATURE, cartridge IDs, frozen save aliases, existing user saves, or
Play Google Drive sync. Draft PRs #361 and #362 were inspected: no shared
files with this PR (they touch CI workflows, `FileKtTest`, `MobileGameScreen`,
`DisplaySettingsStoreTest`).

## Tests

`SafeSaveStoreTest` (Robolectric/JUnit): identity (UUID enforcement),
multi-file integrity, wrong-game association block, restore round-trip with
destination backup, corruption refusal with live save preserved, interrupted
restore journal/original preservation, install-over rediscovery, and Room
persistence across database reopen. Low-storage behaviour is guarded in code
(free-space check before capture/restore) but is NOT TESTED in the unit suite.

## Marcus QA checklist

| Check | Result |
| --- | --- |
| Identity: non-UUID / filename-based association refused | PASS (unit) |
| Multi-file integrity (SHA-256 per file) | PASS (unit) |
| DB persistence across reopen (separate SafeSave DB) | PASS (unit, Robolectric) |
| Interrupted restore preserves original + journals failure | PASS (unit) |
| Corruption detected, restore refused, live save preserved | PASS (unit) |
| Wrong-game association blocked | PASS (unit) |
| Low storage guard | NOT TESTED (code guard only) |
| Install-over rediscovery of verified save | PASS (unit simulation) |
| Capture/restore latency + bytes measured | PASS (metrics returned; values are device-dependent) |
| Peak memory measured | NOT TESTED |
| CI build / APK | BLOCKED until CI runs on this PR head |
| Physical PSP save → snapshot → restore → reopen in game | NOT TESTED — owner device acceptance (Jonathan) |

## Owner device checklist (Jonathan)

1. Play one PSP game in PPSSPP Modern, save, close the game fully.
2. Capture the game's savedata group against its cartridge UUID.
3. Change progress in game, save again, close the game.
4. Restore the snapshot.
5. Reopen the game and confirm the restored progress loads.
