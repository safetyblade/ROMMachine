# GEM Data Improvement Delivery — source and integration checkpoint
**23 November 2026 | PR #368 | Engineering implementation; not owner/device release acceptance**

## Source-of-truth facts (read-only, all 53 pinned shards)
- **Original corpus remains:** 88,246 release records on 30 systems; 14,216 VDG and 74,030 Title Screen. No source IDs, signed source registry, game hashes, installed ROMs or player records were rewritten.
- **Source-genre filled:** 79,946; genuinely missing: 8,300. Current canonical taxonomy maps **79,612 game releases** plus **four explicitly marked non-game releases**, leaving **330 Miscellaneous** source-genre entries intentionally unknown (99.59% disposition coverage). Three known-genre records are held for promotional/firmware-title review.
- **Recommendable source genre, before:** 50,461 records, 57.18% of catalogue, due to narrow legacy allow-list/title regex.
- **New projected recommendable source genre:** **79,609 records, 90.21%**; **+29,148** additional release records. This is a static projection of the same native genre/titled review rules, not device/gameplay QA or a count of guaranteed personalised recommendations. Player preferences and rating signals still determine actual output. Some contribution-only genres can add a small number of valid entries.
- **Exact-ID gameplay-feature pack:** **1,235 records (497,782 bytes)**, with 1,200 additional Title Screen genre-derived features clearly identified as **not individually gameplay-tested**, 30 prior editorial seeds, and five prior Nintendo publisher-description source records. The 1,200 are source-derived game facets, not 1,200 independently researched reviews. Rights and original per-game source genre/revision are preserved. **Feature pack SHA-256:** `564d602a11bca8f54919b148317f4cb7eca4ef091497687a4329395912b7914f`.
- **Source identities:** 6,468 cross-source same-system/title collision **groups remain candidates, not proven duplicates**. No speculative work merges. 0 source-verified ROM hashes or serials; absent identity evidence is accurately reported unknown.
- **DeckFit:** 88,246 inherited system baselines, 91 exact-release reviewed overrides, 14 source-qualified special-control Input review holds that remain unscored, all untouched. No external-controller bonus. Runtime playable/tested remains a separate, truthful state.

## Functional changes
1. `GemGenreTaxonomy.kt` establishes one source-driven, controlled canonical genre/crossover/playstyle mapping. Known false negatives like *Prototype 2* and *Urban Trial Freestyle* are no longer blocked by plain words in their titles; compilations can remain recognised game collections. Truly unknown and promotional-content cases are reviewable, not fabricated.
2. `GemDiscovery.kt` supplies canonical genres and explicit licensed mechanics to existing offline `GemRatingRecommender`. It accepts a verified work relationship only with separately provided evidence; it never links works by title.
3. `GemFeatureContributions.kt` extends the existing exact release-ID and title importer with optional source licence, independently evidenced work identity and provenance fields. All 1,235 bundled records are source-and-title checked.
4. GEM Browse, search, filters, For You and intelligence use the shared canonical classification. The catalogue UI displays source names and original source genres to distinguish ambiguous editions.
5. `GemDeckFit.evidenceTraits()` maps only narrowly sourced, explicit gameplay facts to existing permitted touch modifiers. Source genre `First-person shooter` alone is **not** misinterpreted as proven dual-stick aim; personal My GemRating/My DeckFit remain separate.
6. `scripts/gem/audit_data_quality.py`, `validate_taxonomy.py`, `verify_feature_pack.py` provide one reproducible pre/post quality/rights report and reject out-of-catalogue rows and speculative identity.

## Exact automated evidence
- [GEM integrity and source taxonomy/feature verification run 45163987699](https://github.com/safetyblade/ROMMachine/actions/runs/45163987699): **PASS** across 53 pinned catalogue source shards, taxonomy mapping, 1,235-row feature pack, per-record original source-genre match, revisions and rights.
- Kotlin, Room, full JVM/Robolectric and ARM64 APK results must be attached for the final exact PR head. Source-integrity PASS does not automatically certify compilation or app behaviour.
- Physical Android touch input, orientations, Junior/profile isolation, install-over, saves and runtime compatibility remain independent Marcus/owner acceptance, **not tested by this source pass**.

## Outstanding honest boundaries
No blanket verified work IDs or source ROM fingerprints are fabricated. No assertion of game-level compatibility, operation of untested special controls, or independently researched mechanics for every game. Source data gaps in DOS/FBNeo/MAME 2003-Plus and manual editorial expansion remain sensible *post-closeout* maintenance; they should not cause a new verification loop. PR #368 remains open draft until the single consolidated acceptance.
