> **LATEST OWNER REFINEMENT — 22 NOV 2026:** Read [GC01_INPUT_MOTION_GEM_JUNIOR_OWNER_LOCK_2026-11-22.md](GC01_INPUT_MOTION_GEM_JUNIOR_OWNER_LOCK_2026-11-22.md). Owner **approves** provider-neutral connector-ready design and scalable indexed offline storage. Historic game releases form a finite catalogue, though under-covered releases and metadata facets can be enriched. Allow ordinary users to edit personal metadata without a heavy approval workflow. Legal redistribution of *bundled* outside data still requires validation; preservation of ROM UUID, SaveBinding and player GemRating is non-negotiable. Pending-decision language about GEM-C01/C03 below is historical.

# CARTDECK GEM — Connector-ready, offline-first data and recommendation architecture
**Owner directive 22 Nov 2026. Status:** design/architecture guidance on draft GC PR #362, not implementation or release. Original GC-01 A–H #262 and source checks remain required.

## 1. The owner outcome

CARTDECK GEM must be a **database that can continuously be expanded, corrected, enriched and optimised by compatible connectors**, not a fixed application-bundled game-name list. It still must be **fast and useful entirely offline**. A connector enriches **game metadata**, not private player ratings; only the player's explicitly entered whole-number **GemRating 1–10** is the personal recommendation training signal. **My DeckFit 1–5** is also whole-number and separate from enjoyment.

Build this extension capability once, avoiding a custom one-off importer per data provider. Do not require login or an internet service for basic GEM.

## 2. Three layers with strict boundaries

**A. Source connector / ingestion (offline developer pipeline or explicitly authorised future updater)**

Provider adapter fetches/reads *metadata only* and returns a normalized, immutable evidence receipt with source ID/revision, raw record ID, timestamp, source licence/attribution, field-level provenance, optional creator verification and content hash. Candidate sources can include permitted game catalogues, ROM set metadata, curated mechanics/tags, reviewed game characteristics, and Roxy's lawful annotations.

**B. Approved catalogue/feature store (local app authority)**

Staged data is reviewed, reconciled and accepted into a **versioned local base catalogue plus an additive gameplay-feature layer**. Existing registered release IDs, verified work/edition identity, ROM cartridge UUIDs, SaveBindings and player ratings **must not change** because a new connector names a game differently. Corrections are field-level revisions with source evidence, not destructive global overwrites. At least one prior approved version is retained for rollback. The app queries an indexed local snapshot, so searching/recommending works with Wi-Fi disabled.

**C. Per-player private preference layer and scoring**

GemRating ledger, explicit Clear, My DeckFit, wildcard/genre preferences, wishlist and Not Interested are independently stored against authorised player and exact release/cartridge identity. Never put user ratings or private save metadata in source connector ingestion or uploaded metadata requests. GemFinder derives deterministic ranked suggestions from **that player's ratings**, source-backed gameplay facets and explicit candidate filters. No cloud account or AI provider is necessary.

Connector failure can never break local search, remove ratings or corrupt the active catalogue.

## 3. Generic metadata connector interface — architecture direction (not pre-approved executable API)

Design one provider-neutral interface:
- **ConnectorIdentity:** stable connector ID, schema/API version, operator, supported platforms, data types, declared licence, rate/cost limits, permission class, capabilities, attribution.
- **discoverDelta(cursor, sinceRevision)**: returns bounded paged records/change receipts or a full version, with digest, ETag/revision/cursor; idempotent retries, no network requirement to read already-approved local data.
- **normalize(sourceRecord)**: maps source-specific keys into controlled fields and preserves original unmodified external IDs, aliases, provenance, source locale/region and uncertainty; does **not** commit or auto-link ROMs.
- **validate()**: rejects invalid schema/values, licence gaps, gross outliers, injection/non-game payloads, title-only identity conflation, ambiguous platform and suspicious record volume changes.
- **previewDiff()**: provides per-field additions/changes/deprecations, conflicts, confidence/rights, effect on ranking, size/memory estimates and record quarantine; no surprise live writes.
- **stage() / publishApproved()**: source material may be staged, but **only a separately trusted publisher** can approve/sign a catalogue package. App receives read-only approved packages. Never let a remote connector sign its own trusted update.
- **recover() / rollback()**: rollback prior verified version, keep tombstones/retired source IDs so previous player ratings remain linked.

This may be implemented as a JVM/Kotlin adapter model plus standalone offline importer/CI job; it does **not** require user-installable plugin execution inside Android. A future provider connector can be added without schema rewrite.

## 4. Metadata model (versioned, extendable, searchable)

### Identity and relations

- Work (a verified conceptual game; never guessed from matching title)
- PlatformRelease (stable release ID, platform/SystemID, region, revision, edition, language)
- ExternalSourceRecord (source provider, source record ID, version, licensing)
- TitleAlias (source/locale, canonical-or-unverified status)
- CartridgeLink (immutable installed cartridge UUID → verified release reference; explicit manual correction if uncertain)
- Edition/Port relationships (verified edges only, not automatic merge)
- Parent/clone set identifiers for FBNeo/MAME where applicable, **not ROM downloads**.

### Source-backed gameplay feature facts

A flexible, typed **GameplayFeatureRecord** keyed by release and/or independently verified Work with:
- key/tag (normalized vocabulary), value(s) and optional confidence;
- original raw source value, source URL/record ID/revision, evidence tier, attribution/rights, reviewer and review timestamp;
- feature category **genre, crossover/subgenre, mechanics, playstyle, pace/session length, solo/co-op/competitive modes, theme, complexity, control/peripheral requirements**, accessibility/legibility and optional era, creator and franchise;
- allowed scoped application (work-wide or edition/platform-specific). A PSP edition and DS edition can differ in controls/DeckFit despite the same work.
- no blank guessing: absent or disputed fields remain **Unknown**, visible for enrichment research, not scored as disliked.

**P0 enrichment** for immediate recommendation improvement: exact release/work identity, primary genre, crossover genre, mechanics, playstyle, evidence/eligibility. P1 after that: mode/player count, session length/pacing, theme, input demand, verified series links. P2: developer, era, region language. A DAT may improve identity without meaningful genre/mechanics—this distinction is required when evaluating connectors.

### Private user data

Separate profile-authorised GemRating integer 1–10 and explicit Clear tombstones, My DeckFit integer 1–5, player choice/preferences, wishlist, Not Interested and optional played history. Do not store these in the signed public catalogue; user's data survives any approved metadata upgrade, rollback or collector failure.

## 5. Field conflict, trust and update rules

1. **Never title-only join** independently named catalogue releases; exact serial/hash/verified identity first, unresolved ambiguous candidate quarantined.
2. Source priority is **per field, not per database wholesale**: explicit human-reviewed verified evidence > directly authoritative source fields > rights-cleared secondary source > provisional editor annotation > Unknown. Provide a rule table with exceptions (e.g. publisher for release year vs core DAT for arcade shortname), rather than declaring one all-powerful provider.
3. If two providers disagree on control requirements, platform, genre or mechanics, preserve both source claims, mark a conflict, use the last approved value until review, and show a reversible proposed correction. Changes to immutable identifiers require a new ID and crosswalk, never reassignment.
4. Source deprecation produces a tombstone/retired record, with retained rating references and prior version availability; cannot silently erase user profiles.
5. Connector input is **untrusted content**: bounded paging/byte count, strict schemas and safe string/path handling, HTTPS and explicit trust, source licence/redistribution review. Do not execute remote scripts or hidden automatic background scraping, bypass provider ToS or import game binaries/ROMs/BIOS/box art.
6. **Connector policy:** initially connectors run in controlled ingestion/review tooling, not app-start live queries; in-app optional update checking may reuse existing signed GEMC1 preview/download/activate/rollback only once independent signing keys and approved origin are provisioned. No generated signing key in repository, no invisible updates, no personal ratings sent to source.

## 6. Real current source architecture to extend, not bypass

Inspected code on GC branch:
- `GemCataloguePackage.Release`: release ID, SystemID, source and sourceRecordId, primaryGenre, optional verified hashes/serials. No full gameplay-feature vector in base schema.
- `GemGameplayFeatures`: small hard-coded feature overlay; convert/grow via versioned feature pack with source provenance and stable key matching, not a giant hardcoded list.
- `GemDiscovery.prepare`: projects existing release+feature values into `GemRatingRecommender.Game`. Must take the new verified feature records and preserve exclusions.
- `GemRatingRecommender`: pure offline model of primary/crossover genre, mechanics and playstyle, explicitly user-rating driven, deterministic; extend only with tests. Internal decimal relevance score is **not** user whole-number GemRating.
- `GemProductionCatalogue`: existing approved source shards, current 88,246 rows across 30 systems.
- `GemSignedCatalogue` / `GemCatalogueVersions`: existing signed GEMC1 envelope, bounded JSON, pinned key/update origin optional, preview/activate/rollback. Currently external signer and update origin **not provisioned**; do not invent or self-sign publisher authority. Preserve backwards compatibility, old source IDs and active/previous versions.
- `GemRatingLocalStore`, `GemProfileBackup` etc. remain independent; no mandatory network and no destructive migration.

**Critical engineering limit:** 88,246 bundled rows approach the existing 100,000-row package cap; connector-driven expansion can exceed it. Do not silently bump MAX_RELEASES/MAX_BYTES to arbitrary huge values. Engineer a bounded indexed **multi-shard database or versioned indexed SQLite/Room-backed catalogue** (not the shared player/ROM Room schema), with proven efficient lazy load and search, documented schema migration, integrity/signed manifests and per-shard hashes. Benchmark actual 100k, 250k, 500k rows on target Android hardware, including cold start, filters, rating lookup, memory, APK size and rollback. Choose storage architecture on evidence.

## 7. Connector quality metrics and GEM ranking improvements

For every connector/data release produce a machine-readable review report:
- New Work / PlatformRelease / feature rows; valid, duplicate, ambiguous, quarantined, removed, conflicted; source provider/date, rights verdict and per-field evidence.
- **Per-system P0 completeness**: % valid eligible games with primary genre, crossover, mechanics, playstyle and reliable identity before/after; no invented counts.
- Features indexed and synonyms normalized; distribution of Unknown vs reliable; stale data age/revision.
- Recommendation quality fixture: 100–300 diverse games, synthetic *test* profile ratings (high/low/neutral/clear), profile isolation, novel/matching wildcard, explanation accuracy, cold start, wrong edition/series dedupe, Not Interested suppression, not-installed metadata only.
- Runtime: p50/p95 searches, cold/open indexing time, memory peak, source pack bytes, crash/rollback/install-over. Record actual hardware benchmark and acceptable thresholds as engineering proposal, **not untested promise**.
- Connector repeat runs are idempotent; failed partial update leaves old catalog and all ratings intact; future connector replacement/removal doesn't orphan prior source IDs or corrupt saves.

**Before/after recommendation tests:** A player rating Advance Wars highly should increase evidence-backed mechanics-similar games; a low rating for a disliked racing title should reduce relevant racing-style similarity, not ban every racing game; the recommender must not substitute Roxy's DeckFit or editorial taste for explicit GemRatings. Unrated player sees neutral Browse/Rate a Game.

## 8. Roxy sprint and owner decisions that still need stronger guidance

**Roxy next data pass:** Audit actual P0 completeness; pick first feature-rich, rights-cleared source connector (not necessarily the largest FBNeo DAT), produce an ingestion adapter contract, source provenance and a 100–300-release enrichment pilot; separately research FBNeo/MAME2003Plus/DOS coverage. No master merge, no ROM/media download, no product release based on research only.

| Decision ID | Recommended default | Status and why |
| --- | --- | --- |
| **GEM-C01 Connector execution boundary** | **Controlled source-ingestion pipeline first; optional explicitly requested signed local catalogue updates in Android later.** No per-user automatic live search or arbitrary connector code execution. | **OWNER DECISION PENDING**: product scope of optional future in-app connector management versus publisher-only connectors. |
| **GEM-C02 Connector publishing authority** | Single owner-controlled catalogue approval/signing service (outside repo/app), independent source/rights review, multi-source provenance. Connectors submit candidates only. | **Architecture/operations decision pending**: where controlled key/origin and data licence review physically live. No token/key creation or spending now. |
| **GEM-C03 Data-store growth strategy** | Versioned, indexed, bounded local multi-shard pack or dedicated read-optimised GEM SQLite store separated from canonical ROM/saves/user profile DB. Benchmark 100k–500k candidates before choosing. | **Architecture decision pending**: key/index/data volume/rollback format; avoid premature huge JSON list. |
| **GEM-C04 Ratings-data access** | User GemRatings stay private/on device; sources/ingesters see no user identifiers. Optional aggregate quality telemetry only if independently authorised opt-in later. | **OWNER DECISION PENDING only for future opt-in telemetry**; no telemetry now. |
| **GEM-C05 Field arbitration** | Owner-reviewed per-field provenance/authority rules; conflicts quarantined, preserve prior approved field until review; source removal never deletes player ratings. | **Architecture/Roxy evidence review pending**, safe default applies now. |
| **GEM-C06 Recommendation optimisation** | Offline deterministic scoring using explicit ratings only for preference, documented relevance reasons, fixture-tested low/neutral/high feedback; feature-rich metadata P0 first. | **Engineering/QA gate**, not a new high-level decision. |
| **GEM-C07 Update UX and cadence** | On-demand **Check for catalogue updates** with preview new/updated/conflicts and explicit Install/Roll Back; no background network by default. | **OWNER DECISION PENDING** for optional automatic updates or in-app connector UI later. |

**Whole GC boundary:** This design is additive to the owner-approved original A–H, SafeSave, direct controller mapping, Junior, SIGNATURE and TV. GEM source ingestion must not hijack PERF/CARTMOD or block implementation of approved GC elements. Independent hardware QA remains NOT TESTED until performed.
