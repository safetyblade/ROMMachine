# ARCH-LIB-01 — Checkpoint C2A: durable retired-cartridge identity

**Status:** Draft engineering checkpoint; NOT RELEASED and NOT #145 acceptance.
**Base:** master `c305d7932a84ed8677f8f6a413498d7ba28127e1`; related #145, #115 and C1 PR #216.
**No changes to user ROM bytes, native engine stores, debug package/signing, or save-state files.**

## Why

C1 removes missing games after a complete successful SAF scan and removes their shelf positions in journal reconciliation. C2A retains identity records, the frozen save aliases and the exact shelf cell while a source is unavailable. It does not invent a ROM, search/download content, or assign legacy progress.

## Source and migration

- Add `retired_cartridges` Room entity/DAO in **v13**, with a 12→13 additive migration registered on the app DB and old-version migration chain. The snapshot contains immutable UUID, former numeric row ID, source URI/document, console, frozen save aliases and existing player metadata.
- On complete successful enumeration, retire every missing Game **inside C1's single Room transaction**, then remove only its live DB row and dependent indexed file rows. No external files or saves are deleted; any transaction failure rolls back both retirement and scan mutations.
- A later scanned candidate may recover the UUID, custom name, artwork, favourite, play history and frozen save aliases only if the same trusted `.documents` provider authority/document ID and console are unique, with no active claim. Original fileName/fileUri are not treated as enough. Ambiguous or unsupported sources remain unassociated, their retired record intact.
- New game records are inserted as normal if no verified recovery exists. No destructive overwrite or automatic title/size/basename matching.
- C1's journal now includes retired identity mappings before each scan. After commit, shelf reconciliation rekeys unavailable cells to reserved `retired:<cartridge UUID>` presentation keys, preserving original cell ID, section, facing, order, gaps, title and appearance. A different ROM later occupying the same URI cannot take this old shelf position. A verified return rekeys the old cell to its new launchable source. Crashed journal replay remains atomic/idempotent.
- Missing cells display `Unavailable` and cannot launch. There is no new file association action in C2A.
- Existing Wii controller preference reads fall back to frozen URI-based legacy keys; new settings use immutable cartridge UUID. GripArc already uses UUID identity.

## Boundaries and risks

- Former Room integer Game ID is recorded, but after a full removal the returned runtime row can have a different integer ID. C2A guarantees immutable cartridge UUID and save-binding continuity, **not** identity of the old integer SQL key. Native stores and any other game-ID-keyed integrations need explicit independent QA.
- A provider that reuses a supposedly persistent document ID for different file content cannot be distinguished solely through its opaque ID. The matcher therefore restricts itself to established `.documents` identity and unambiguous consoles. C2B will add user-controlled recovery for other situations; no unsafe fallback is allowed here.
- Identical ROM content in separate user cartridges is not merged. Sources without stable document identity, changed document IDs, ambiguous matches and shared-basename legacy saves remain separate and pending later explicit recovery.
- No automatic deletion, archive expansion, #146 operations, GEM work, engine changes or new native-store migration.
- `master` had an unrelated source lint failure in `Armsx2RuntimeProbeActivity.kt` before C2A. A standalone formatting-only fix is included solely to permit validation; it must not be attributed to C2A and does not change PS2 behavior.

## Verification required before merge

1. Verify generated and committed **v13 Room schema** from the actual KAPT processor; migrations 11→12→13 and installed v12→13 without losing data.
2. C2A synthetic tests: full missing scan, subsequent verified reimport, name/cover/favourite/legacy-save continuity, unique source IDs, no duplicate game, no false same-name match.
3. Journal: missing position retained; exact cell ID survives reattach and process restart, including different tree URI; no future/corrupt journal reset.
4. Save/controller: existing UUID-based bindings and frozen aliases unchanged; Wii legacy URI choice read-only fallback and per-UUID new choice survives return.
5. Existing full lint, Android Kotlin/JVM, Room schema and final engineering ARM64 APK jobs all green on exact head; do not bypass failing tests.
6. Marcus: install over real v11/v12 handset with real SAF grants and ROMs, remove/refresh/reimport, save/reopen, confirm shelf/appearance and console/controller behavior; log PASS/FAIL/BLOCKED/NOT TESTED. Native-store ownership still NOT TESTED until verified.
7. Keep #145 OPEN; C2B explicit historical save and unknown-source ownership choice remains a separate checkpoint.

## Reversion

Before merge, close draft. After merge, do not downgrade installed Room v13 to an older APK. If a source rollback is required, use an additive forward migration/new build and preserve retired rows, shelf journal, save bindings and external ROMs. Never instruct users to clear data, uninstall, or delete originals.
