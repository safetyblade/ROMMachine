# GEM-02B — Offline Arcade DAT Research Importer

**Owner:** Roxy Quinn (catalogue) · **Architecture gate:** Daniel Mercer · **QA gate:** Marcus Reid

**Status:** Research tooling, draft. Does **not** change the Android app, publish metadata to an APK, download ROMs or BIOS images, or certify that a game runs on a device.

## Purpose

Replace manual source-file sampling with a reproducible, core-specific scan of the **full MAME 2003-Plus XML** and **FBNeo Arcade-only ClrMamePro XML** source registers. The importer accepts **local source DAT files only**, makes no network calls, and stages CSVs for manual quality and rights review.

Official source metadata (not game binaries):

- [MAME 2003-Plus XML metadata](https://github.com/libretro/mame2003-plus-libretro/blob/master/metadata/mame2003-plus.xml)
- [FBNeo Arcade-only DAT](https://github.com/libretro/FBNeo/tree/master/dats)
- [MAME 2003-Plus licence](https://github.com/libretro/mame2003-plus-libretro/blob/master/LICENSE.md)
- [FBNeo licence](https://github.com/finalburnneo/FBNeo/blob/master/src/license.txt)

Rights, provenance and commercial redistribution permissions must be evaluated before a full catalogue export is included with any distributed application. These files are for internal metadata research, **not** an authorization to distribute cores or licensed metadata.

## Local execution

Use a machine with Python 3.10 or later. Obtain the **source DAT files themselves** separately from the projects above; do not supply a ROM set or binary images.

```sh
python scripts/gem_arcade_catalogue.py \
  --mame /path/to/mame2003-plus.xml \
  --fbneo "/path/to/FinalBurn Neo (ClrMame Pro XML, Arcade only).dat" \
  --output /tmp/cartdeck-gem02b-review \
  --existing-ids /path/to/master_catalogue_ids.csv
```

The optional `--existing-ids` CSV contains existing source IDs in its first column, e.g., `M3P:sf2` or `FBN:sf2`. It is used strictly for **read-only reconciliation**, never to modify the supplied master or any app database. Omit it to stage all parents as new research candidates.

The output directory contains:

| File | Use |
|---|---|
| `manifest.json` | Input checksums, provenance, source counts and explicit distribution/QA gates |
| `parent_candidates.csv` | All source-defined parents, including editorial holds |
| `new_parent_candidates.csv` | Non-held parent IDs not present in existing catalogue IDs |
| `already_catalogued.csv` | Parent IDs already found in the catalogue |
| `hold_parent_candidates.csv` | Prototype, homebrew, hack or other non-standard game candidates for manual review |
| `clone_variants.csv` | Explicit `cloneof` entries, **not** separate parent games |
| `excluded_bios_devices.csv` | BIOS, device and non-runnable registry records |
| `cross_core_review.csv` | MAME–FBNeo shortname matches for manual identity confirmation |

**Important distinction:** `romof` can indicate a system BIOS dependency. It does **not** necessarily indicate that the game is a clone. Only `cloneof` drives the clone classification. Shortname matches across MAME and FBNeo do not prove ROM-set compatibility or the same release revision.

No ROM file names, individual ROM hashes, BIOS payloads, artwork, game download links, personal ratings or per-game device compatibility scores are exported.

## Controls and acceptance gates

1. Source hashes and exact source versions retained; no uncontrolled dataset changes.
2. Parent vs. clone vs. BIOS reliably differentiated, with ambiguous entries held for review.
3. Core-specific stable IDs, names and source attributes preserved; cross-core overlaps **never automatically merged**.
4. Exact source counts, previous-ID matches and collisions recorded.
5. Source/licence and commercial-distribution approval recorded before any bulk master import or APK usage.
6. Marcus verifies on-device compatibility independently; metadata is not playback evidence.
7. User GemRatings, DeckFit scores, shelves, save data, ROMs and artwork remain untouched.
8. Daniel reviews any future application integration against [#145](https://github.com/safetyblade/ROMMachine/issues/145) and [#194](https://github.com/safetyblade/ROMMachine/issues/194).

The connected [GEM Master Game Catalogue](https://docs.google.com/spreadsheets/d/12k2ZEmIO3mzo8z77FS6vMtsBjF8AxmOQ6Wf1DJVNXpo/edit) tracks manually reviewed records, source-file coverage and hold queues. This script does **not** write to Google Sheets automatically.

## Tests

```sh
python -m unittest discover -s scripts -p "test_gem_arcade_catalogue.py" -v
```

The small embedded XML fixtures confirm BIOS dependence vs. clone relations, exclusion of BIOS registries, ID reconciliation, cross-core collision queues, and metadata-only output. Full upstream DAT execution remains a separate live data gate and must be explicitly evidenced before calling the complete-registry pass finished.
