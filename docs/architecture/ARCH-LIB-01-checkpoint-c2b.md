# ARCH-LIB-01 — Checkpoint C2B: explicit source and historical save recovery

**Status:** DRAFT / PARTIAL / NOT RELEASED. Related issue #145 and #115.
**Base:** `d44b9018a86398148c6058920cbb9aae0e9305c4` on master, incorporating merged C2A PR #236.
**Owner:** Jonathan · **Architecture:** Daniel · **Engineering:** Elliot/Codex · **QA:** Marcus.

## Scope delivered in the candidate

### A — Manual retired-cartridge source recovery

- Available in **Settings → Cartridge and save recovery**.
- Players select an **unavailable retired cartridge**, then an already scanned ROM of the same console, then actively acknowledge and confirm ownership. A ROM must first be indexed via the normal Add Cartridges / refresh flow. No ROM download or file mutation is performed.
- No automatic filename/title/content-size matching. The manager refuses candidates with play history, favourites, custom name/cover or existing cartridge-scoped SRAM/state/preview data or save-binding manifest. Other previously configured per-source settings should be reviewed before use.
- Source confirmation uses `LibraryIndexWork.refreshLock`, a committed C1 journal containing a user-selected rebind plan, a single Room transaction for UUID/data reassignment and retired-ledger deletion, and journal replay from actual Room state. A failed/rolled-back DB change cannot authorise removal of a source shelf cell.
- The existing retired shelf cell keeps its original identity, facing, section, order, spacing and title. If the selected ROM has an untouched default Unsorted shelf cell, that one **newly generated** cell is removed during the one atomic shelf update; customised or ambiguous cells fail closed and retain the journal for recovery. No source URI alone authorises a duplicate-cell deletion.
- The candidate retains the installed integer Game.id and datafile references. The retired cartridge UUID, frozen legacy save names, custom cover/name, favourite, history and per-UUID control settings are attached to this already indexed candidate. Existing game/card runtime paths are unchanged. No ROM file is deleted, downloaded or overwritten.
- Failed journal reconciliation remains recoverable at startup. A prepared journal without committed Room changes is replayed harmlessly.

### B — Explicit historical shared SRAM ownership

- The same dialog offers **Legacy saves**. The player selects a currently installed game, reviews the matching legacy shared SRAM file name, byte count and SHA-256 prefix and must explicitly confirm.
- `CartridgeSaveBinding.pendingSharedSram()` does not import anything. `confirmSharedSramRecovery()` rechecks that the cartridge and source are unchanged and verifies the complete SHA-256 before and after copying into the cartridge UUID / shared-SRAM namespace.
- Copying is staged and verified, with the original never modified. Conflicting UUID-scoped progress fails closed, and repeated same-digest confirmation is idempotent. Save binding retains the `savebinding-v1` compatibility prefix with a `user-confirmed-shared-sram` status.
- Supported candidate is only the app's legacy shared `.srm` inside its existing saves directory; no user-selected arbitrary filesystem path. Max source size 32 MiB. Malicious filename traversal and changed fingerprints are rejected.
- No core-engine-owned memory card, PS1/PSP card image, Wii NAND, Xbox HDD, DS `.dsv` conversion, SRAM from isolated engines, manual state, auto state, state metadata or preview is automatically assigned by this code.

### Explicitly pending (C2B completion)

1. Full states/previews and native engine-specific store ownership contracts, user review and verified recoveries in separate bounded slices.
2. Manual recovery from an external newly chosen SAF document that is not yet indexed; currently the player must import/scan first. This protects the existing managed ROM and permission flows.
3. Complete candidate physical-device QA and all-console/TV/tablet smoke results.
4. Additional UI polish for large catalogues and explicit support handling for a source with a user-customised shelf position. This candidate intentionally fails closed rather than overwrite layout.

## Tests and release gates

Added synthetic test coverage for manual restoration, player metadata and shelf cell retention, rejection of used sources, restart replay after Room commit, ambiguous legacy SRAM assignment only on explicit confirmation, changed-source hash rejection, idempotence, preservation of original file and refusal to overwrite distinct existing progress.

The first review must also verify:
- Existing Room 11→12→13 and 12→13 migration tests, generated schema v13 and unchanged installed package identity.
- Kotlin production/test compilation, full JVM tests, source lint, native engineering builds and final ARM64 APK on **exact PR head**.
- Interrupted journal before Room commit, after Room commit, during shelf atomic write and after file copy before manifest update. Record PASS/FAIL/NOT TESTED truthfully.
- Marcus installation-over without uninstalling, SAF grant retention, shelf slot preservation, Wii/DS/PS1/PSP/Dreamcast gameplay and saves, controller settings and original backups.
- No unmanaged ROM deletion, GEM change, archive work, emulator replacement or new permissions.

### Baseline corrections

The branch also fixes pre-existing C2A defects: the indexing worker now passes retired records to the post-scan shelf reconciliation, and three source formatting violations inherited from merged C2A are corrected separately. Those were present in the prior red APK pipeline and are not a new C2B functional feature.

## Recovery and rollback

Do **not merge, release, close #145 or start destructive #146** on this draft. Close/revert the draft on failure; no C2B schema changes are introduced. The installed Room v13 must be preserved, never downgraded. If a newer application has already performed an explicit recovery, preserve its journal and new UUID-scoped saves during any forward-fix; do not uninstall, clear app data, overwrite originals or silently attach a different ROM.
