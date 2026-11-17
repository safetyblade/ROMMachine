# SafeSave — Technical API, archive format, restore protocol and cloud contract
**Version:** proposed 1.0 contract · Daniel Mercer · 2026-11-16  
**Status:** implementation design, not shipped. Related [SOURCE AUDIT](ENGINE_SAVE_SOURCE_AUDIT_2026-11-16.md), #290 and #293.
**Non-goal:** emulator ROM/BIOS sharing or repackaged installed-game environments.

## 1. Model of record
`CartridgeId` is installed cartridge UUID, **not** a catalogue game/title/filename. `LocalPlayerId` is immutable UUID, **not** PS3 user or Xbox XUID, device account or cloud user subject. Session manager resolves `(LocalPlayerId, CartridgeId, EngineId)` into a verified `SaveOwnershipBinding` before an engine can open writable storage.

```text
PlayerProfile(id, type, permissions, disabledAt?)
ProfileCartridgePolicy(playerId, cartridgeId, mode[ALLOW|HIDE])
GameSession(sessionId, playerId, cartridgeId, engineId, startedAt, state[OPEN|FLUSHING|CLOSED])
EngineNativeIdentity(playerId, engineId, nativeIdentityRef, proofStatus)
SaveGroup(groupId, ownerPlayerId, platformId, engineId, scope[GAME|CARD_BANK|USER|ENGINE],
          nativeIdentityRef?, revision, isolationStatus)
SaveArtifact(artifactId, groupId, cartridgeId?, saveKind, slot?, pathRef, contentHash, bytes, createdAt)
Snapshot(snapshotId, ownerPlayerId?, scope[GAME|PLAYER|ALL_PLAYERS], stage, manifestHash, createdAt)
OperationJournal(opId, actorPlayerId, operationType, beforeSnapshot, targetRef, phase)
CloudLink(playerId, accountSubject, scope, consent, encryptionRecoveryState) // FUTURE
```

Every operation includes an actor/authorization context; `playerId` is server-independent and generated locally. All store access flows through `PlayerSessionContext` and `SaveBindingResolver` (no implicit global current-user singleton across coroutines). A new profile does not see or write to legacy paths unless a trusted migration establishes ownership. If adapter isolation is unverified, launch fails safe in that additional profile.

## 2. Adapters: required capability interface (conceptual Kotlin)
```kotlin
interface SafeSaveAdapter {
    val engineId: String
    suspend fun inspect(context: PlayerSessionContext): SaveCoverageReport
    suspend fun quiesce(context: PlayerSessionContext): QuiesceResult
    suspend fun discover(context: PlayerSessionContext): List<SaveGroup>
    suspend fun snapshot(group: SaveGroup, destination: Path): SnapshotResult
    suspend fun verifyImport(source: VerifiedSnapshot, target: PlayerSessionContext): RestorePlan
    suspend fun stageRestore(plan: RestorePlan, destination: Path): StagedRestore
    suspend fun activateRestore(staged: StagedRestore): ActivationResult
    suspend fun recover(operationId: UUID): RecoveryResult
}
```
Must classify `SUPPORTED_VERIFIED | DETECTED_UNVERIFIED | SKIPPED_LOCKED | UNSUPPORTED | EXCLUDED_MIXED_CONTENT | LEGACY_UNASSIGNED`; indicate `saveKind` and `scope`. ROMs, firmware, proprietary installed binaries, Xbox XUID Account and unclassified WiiU/PS3 NAND contents excluded. `SUPPORTED_VERIFIED` requires engine/format/physical test evidence, not source pathname alone.

## 3. User-visible SafeSave operations
- `Export Game`: game save artifacts plus **whole shared card bank** when the game's progress is co-located in it; disclose if export includes other titles' saves, require approval and preserve original bank.
- `Export Player`: all supported save groups owned by selected player, across every console.
- `Export Everyone`: Manager-authenticated all players and optionally clearly marked unassigned legacy artifacts; shows count, size and per-engine exclusions.
- `Export Memory Card`: raw supported complete memory card / VMU image with card descriptor; PSP and PS3 directories retain metadata/tree.
- `Import`: scan/validate/preview -> select destination player -> conflicts -> protected restore; never accept implicit full-library admission by importing a save.
- `Copy to Player`: take immutable snapshot, then restore under destination owner identity; no shared writable pointer. Manager required for child/cross-player.
- `Restore Backup`: select existing version, show affected games and conflict rules, backup current before replacing.

## 4. Archive (.cartsave) V1
Transport is a ZIP-compatible, bounded, streamed archive; extension is marketing, **not** a security proof. v1 format location names:
```
manifest.json
manifest.sha256
artifacts/<random-safe-artifact-uuid>/content.bin OR tree/<relative-safe-path>
```
A representative manifest shape (values illustrative):
```json
{
  "format": "cartdeck.safesave",
  "schemaVersion": 1,
  "snapshotId": "6dfbe310-a77d-48ad-a272-6e6e9d711999",
  "createdAtUtc": "2026-11-16T08:30:00Z",
  "scope": "PLAYER",
  "source": {"appSchema": 13, "deviceLabel": null, "player": {"exportAlias": "Player 1", "id": "db7e4a82-8eac-4bd0-8bc0-9d7e8e2bd909"}},
  "coverage": {"verified": 2, "skipped": 1, "skippedReasons": [{"engineId": "example-engine", "reason": "DETECTED_UNVERIFIED"}]},
  "saveGroups": [{
    "groupId": "1e4f2526-cc24-4477-9371-82a4edc0c137",
    "scope": "CARD_BANK",
    "platformId": "PSX",
    "engineId": "pcsx_rearmed",
    "engineVersion": "source-version",
    "nativeUserRef": null,
    "linkedCartridgeIds": ["d9ad9717-fc64-4c0c-bce2-1780ff57e9fe"],
    "artifacts": [{
      "artifactId": "4dd2d17e-1f15-4e97-afd1-acb9e6a32937",
      "saveKind": "MEMORY_CARD",
      "slot": "1",
      "path": "artifacts/4dd2d17e-1f15-4e97-afd1-acb9e6a32937/content.bin",
      "byteLength": 131072,
      "sha256": "SHA256_HEX_FROM_REAL_CONTENT"
    }]
  }]
}
```
Manifest syntax above is a contract *example*, not bytes of a genuine export. Export alias/name should be optional and removable for privacy. Protect metadata of minors and allow encrypted local archive mode; cloud snapshots ALWAYS client-side encrypted. A package containing personal savedata is user-owned private data; never log it.

### Validation rules
- Bound file count, per-file size, uncompressed totals, compressed-to-uncompressed ratio, manifest nesting depth, decompression and parsing time.
- Reject absolute paths, `..`, backslashes as separators, NUL/control chars, symlinks, duplicate paths, duplicate IDs, unexpected extensions, and paths resolving outside staging.
- Require validated SHA-256, expected sizes and declared format version before restore. SHA-256 is *integrity*, not authentication; do not treat a checksum as a signature or trust a remote uploader's identity.
- Unknown/future versions stay import-preview-only or fail safely; never silently downgrade an archive. Cross-engine/core savestate import allowed only if explicit compatibility rule and tests exist.
- For a split game source (disc set / region / cartridge replacement) require stable identity match or authenticated manual mapping; no fuzzy title or filename matching.

## 5. Snapshot / import state machine
```text
DISCOVER → QUIESCE → STAGE → HASH_VERIFY → MANIFEST_COMPLETE → SAF_WRITE → DEST_VERIFY → COMPLETE
                      ↘ FAIL: retain original + staged recovery record, report incomplete
IMPORT_OPEN → BOUNDED_VALIDATE → PREVIEW → AUTHORIZED_MAP → DEST_BACKUP → STAGE_RESTORE
→ VERIFY_RESTORE → ATOMIC_ACTIVATE_GROUP → REOPEN_ENGINE_VERIFY → COMMIT_JOURNAL → COMPLETE
                            ↘ FAIL → rollback affected group to verified backup → report
```
- On Android SAF, destination provider can be remote and may not support atomic rename, fsync, total-length readback or reliable length reporting. Show `Written to provider; verification unavailable` instead of verified complete if no readback. Prefer create temporary local snapshot first; protect against low storage and never delete final backup until verified.
- No engine may write to an artifact while a snapshot is being taken; quiesce via engine flush/close and exclusive lock. Avoid scheduled sync worker touching restored files during operation.
- Multi-engine all-player export can be **complete with declared exclusions**, not an atomic global stop-the-world snapshot unless all sessions quiesced and verified. Per-group restore atomicity is required; multi-group restore may be partial with clear journal/recovery.
- Source is immutable during copy-to-player; target backup and conflict preview required. Same SAVE path must never belong to two active player IDs.
- After restore, update binding manifest and runtime index only after verifying bytes; no overwrite of frozen legacy alias originals. Remapping imported saves never automatically grants cartridge access.

## 6. Existing Play flavor Google Drive sync compatibility
Legacy SaveSyncManager may mirror `saves`, `states`, `state-previews` in Google Drive `appDataFolder` with a periodic 3-hour background worker. That sync is **not SafeSave Vault** and is not profile-aware. New profile-owned save paths MUST be excluded from this legacy worker, and the worker cannot write concurrently with SafeSave migration/import.
- On upgrade do not silently turn off user's configured sync or erase prior cloud files. Provide a clear compatibility state / migration choice, pause conflicting operations when necessary, and preserve default legacy player's original folder semantics until opt-in migration.
- Cloud Vault may migrate/copy existing cloud data only through explicit authenticated consent and verified restore; do not silently treat Drive files as SafeSave snapshots.
- Free and Play variants must have consistent local SafeSave logic; cloud capability remains explicit.

## 7. Remote Vault contract (future only, not authorized)
Use private object storage for encrypted binary save snapshots and an authenticated relational database for linkage, manifest hashes, snapshot indexes, retention and audit. Remote access per *authenticated account subject*, with per-profile access binding and parent-managed Junior profiles. Server-side row scope + object access checks; never rely on UI security or client-supplied user IDs. End-to-end confidentiality requires client-side authenticated encryption and a cross-device key recovery design, not just TLS or Android Keystore. Immutable snapshot versioning, quotas, opt-in manual upload/download first; conflict resolution; deletion/export and recovery policy. No background monitoring or automatic sync in first release.

## 8. Versioning and rollback constraints
- New Room profile tables must be added to actual current schema v13+; new numbered migrations depend on branch-head schema at implementation time. Retain prior `games.id`, cartridge UUID, all Room indices and retired ledger, ShelfStore and user SAF grants.
- Migrating default player's saves must be non-destructive `legacy -> copied -> hashed -> staged binding -> activated`; unresolved legacy files stay unassigned and manager-recoverable. Never silently assign to a child.
- Actual downgrade to an older APK incompatible with Room schema is NOT rollback; rollback means revert source pre-merge, or installed-compatible roll-forward / complete tested snapshot restore.
- Test exact app ID/signing, installed version continuity, Room DB/WAL integrity, partial staging and save reopen. Require Marcus device PASS for each engine before promoting adapter.
