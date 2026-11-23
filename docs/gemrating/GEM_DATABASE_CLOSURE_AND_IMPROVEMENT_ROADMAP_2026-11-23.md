# GEM — Database closure and improvement roadmap
**23 November 2026. Roxy/Architecture/Engineering draft for owner acceptance.**
**Active issue:** #194. **Implementation lane:** existing open draft PR #368, based on master. Former GC parent PR #362 is merged.
**Scope:** shipped static offline GEM game reference and recommendation catalogue. Not installed-device personal data; no ROM hosting, live scraping or universal runtime-game certification.

## Decision

Freeze raw record-count expansion for initial GEM closure. Improve the present 88,246 release records / 30 systems / 53 approved source shards first. Never condition "Done" on individually researching 88,246 entries, resolving every possible duplicate, or adding DOS/FBNeo/MAME 2003-Plus metadata. Those remain separately bounded enhancements.

Finish stages A through D **in one continuous implementation assignment**, without stopping at each verification checkpoint. Then execute **one consolidated final gate** (E). Developer tests may run throughout, but every green subtest, partial APK, sample data pack or code commit is NOT a reason to pause for owner approval. Repair identified defects on the same branch.

## 1. Exact read-only catalogue evidence

Evidence: [GEM quality census, successful run 45151634744](https://github.com/safetyblade/ROMMachine/actions/runs/45151634744), artifact gem-quality-census. Script: scripts/gem/audit_data_quality.py. It reads all pinned shipped source assets and features/v1.json without modifying any release or player records.

| Metric | Count |
| --- | ---: |
| Records / systems / shards | 88,246 / 30 / 53 |
| VDG / Title Screen | 14,216 / 74,030 |
| Records with source genre | 79,946 (90.59%) |
| Missing source genre | 8,300 |
| Genre recognised by current recommendation filter | 50,488 (57.21%) |
| Currently eligible for recommendations | **50,461 (57.18%)** |
| Not currently eligible under recommendation rules | **37,785** |
| Known but unrecognised genre | **29,458** |
| Exact-ID gameplay feature records | **35 (0.04%)** |
| Verified ROM hash / serial records | **0 / 0** |
| Candidate cross-source same-system/title collisions | **6,468 groups** |
| Candidate within-source same-system/title collisions | **15,822 groups** |

**Collision counts are not confirmed duplicates.** Preserve regions, editions, revisions and every source-qualified release ID. A single published work can have multiple records.

High-frequency unrecognised source genres:
- Platformer 4,819; Action-adventure 4,002; First-person shooter 1,495.
- Educational 1,277; Board/card/tabletop 1,021; Music 968.
- Third-person shooter 956; Party 933; Action role-playing 900; Shoot 'em up 856.

The current GemDiscovery code recognises only 11 broad genre strings and filters potential non-games using title words including prototype and trial. **Prototype 2** and **Urban Trial Freestyle** demonstrate false-negative risk; legitimately playable compilations can be excluded by double pack. Do not interpret title matches as conclusive content-type evidence.

The source-release schema has optional primary genre but no native full-game mechanics/playstyle/edition/work relationship fields. The app already includes the exact-ID JSON contribution parser, a deployed 35-record feature asset, and integration with GemDiscovery and GemFinder. **Reuse those.** Current GEM intelligence displays contributed tags, but DeckFit's engine uses the hard-coded feature seed rather than contributed mechanics/playstyle for its complexity adjustments: connect the two through narrowly trusted traits, not a new estimator.

The 88,246 provisional console QuickScore baselines are already assigned. **91** previously reviewed exact release scores take priority; **14** required-input review records retain their base but suppress the public numeric score. No catalogue entry implies launch/playability verification. The repository cannot show private installed-device user ratings; protect their profile-local persistence and migration paths.

## 2. A — Reconcile genre taxonomy and eligibility (P0)

**Roxy + Daniel design; Elliot implements.**

Create one controlled canonical genre + crossover/subgenre/playstyle mapping layer on the existing versioned feature overlay, preserving unaltered original source genres. Include explicit unknown, confirmed non-game and needs-review states. Examples: Platformer to Platform; first-person shooter to Shooter + first-person; Action-adventure to Action with Adventure crossover; source-declared turn-based strategy to Strategy + Turn-based; recognise music, educational, card/board, party, visual novels and compilations with source-backed policies rather than silently eliminating them.

Make source content type (game/compilation/application/firmware/promotional/unknown) an explicit data field. Treat risky title keyword matches as review flags only. Centralise the same normalisation for Browse search, genre filters, GEM recommendation eligibility and the feature scorer.

**Completion condition:** All distinct raw genre labels have a documented mapped/explicitly excluded/needs-source-review disposition. At least **95% of the 79,946 genre-filled release records** obtain a canonical category or an intentional evidenced non-game treatment, with the exact unresolved count in the audit. Regression fixture includes Prototype 2, Urban Trial Freestyle, Castlevania Double Pack, a genuine firmware record and a real demo. Never assign a made-up genre to any of the 8,300 genuinely blank records.

## 3. B — Source identity, provenance and trustworthy collision handling (P0)

**Daniel owns identity; Roxy owns source rights.**

Preserve all 88,246 immutable release IDs, pin/hash registry, local cartridge identity, SaveBinding and personal ratings. Publish provenance per contributed field: original source ID/record, revision, source rights/licence, evidence URL or reference, reviewer/date, and confidence. Put verified Work/Release/Edition relationships into a separate optional overlay, populated **only when independent identifiers** establish the link. Do not title-only merge the 6,468 collision groups.

Use rights-cleared DAT serial/hash identities when available and exactly applicable. Do not fabricate hashes or imply that stable Title Screen source IDs are ROM hashes. Unresolved work links remain source-qualified separate entries and a visible deduplication research queue.

**Completion condition:** Zero changed original release IDs; zero automated title-only work merges; all promoted work relationships carry provenance and rollback; no rating, SafeSave, scanner or Room migration needed for catalogue metadata. Missing verified fingerprints remain clearly unknown.

## 4. C — Targeted feature enrichment and data-pack quality (P0)

**Roxy curates; Elliot reuses GemFeatureContributions.**

Retain all 35 current exact-ID features. First author a **250-record representative batch** across all major platform/gameplay types and use it to finalise taxonomies. Then grow to **1,000+ high-value distinct release records** with evidence-backed crossover, playstyle and mechanics where those details are genuinely sourced. Prioritise recognisable, widely played and installed/library-relevant franchises and genre diversity instead of filling random records. Do not invent controller schemes or turn-based traits from a vague RPG genre. Keep unknown fields absent.

Make a source-owned versioned feature pack with stable exact-ID+title checks, controlled tags, per-field provenance and signed/pinned integrity. Reuse existing one-megabyte/2,000-row importer boundaries or deliberately revise bounds with Daniel review if necessary. Produce one candidate manifest reporting accepted/changed/rejected, source rights and digest; prevent a rejected record from overwriting an existing accepted record. Preserve the existing 91 touch reviews and 14 special-input holds; optional motion alone does not require a hold.

**Completion condition:** 1,000+ accepted source-backed enriched records, no unjustified identity transfer or invented evidence; readable explanation fields, additive rollback and no destructive changes to signed 53 base shards.

## 5. D — Make offline GEM search, recommendations and DeckFit use the same data (P0)

**Elliot implements; Sienna owns presentation.**

Wire the existing source-genre normalisation, feature pack, GemDiscovery and GemRatingRecommender all the way through Browse, details, filters, Search, For You, and My Ratings. Correct release-level eligibility and false negatives; distinguish same-title records safely in UI. Add contributed mechanics/playstyle to DeckFit only where an explicit, evidenced trait corresponds to an existing permitted adjustment. Preserve the single public provisional touchscreen-only DeckFit 1–5 estimate in 0.5 steps under latest QuickScore; player-editable My DeckFit stays whole-number 1–5 and personal GemRating whole-number 1–10. Runtime works/failed/unknown evidence is separate. RoxyRating cannot train users' preferences.

Fictional high/low/clear user GemRatings must visibly affect deterministic source-grounded recommendation order and grounded reasons. With no rating or no usable fields, show neutral discovery and encourage rating without fabricating personalised results. Preserve offline mode, current active profile, Junior filters, existing installed-game associations, import/export and install-over.

**Completion condition:** Search, filter, game details, ownership and recommendations agree on taxonomy/evidence; no false runtime claim, no data leakage, no invented personal ratings and no genre-only fake source reasons.

## 6. E — One consolidated release candidate and acceptance gate

**Elliot integrates; Daniel reviews schema/identity; Roxy signs source and rights; Sienna signs UX; Marcus executes independent QA; Jonathan approves final acceptance.**

One integrated package delivered at an exact commit:
1. Source-and-feature manifest with digests, licences, original/canonical genre tables, 1,000+ feature row count, collision/unresolved report, 91 reviewed scores, 14 holds and rollback.
2. Read-only before/after census: release totals unchanged, source genre coverage, canonical mapping %, actual recommendation eligibility, rejected/unknown, duplicate-work treatment and provenance.
3. One **final** CI run encompassing formatting, code/tests, all 53 source integrity checks, 88,246 baseline projections, contributor importer, recommendation changes, profile privacy, signed catalogue compatibility, Room/install-over/save integrity and offline behaviour.
4. One ARM64 APK and a single physical owner/Marcus QA packet: normal and special-input cases, portrait/landscape, offline lookup, ratings/DeckFit and non-impairment of existing gameplay/saves. Physical untested entries are never silently passed.

Do not open a new PR per batch; do not stop after sample tests or ask the owner for interim approval. Fix final-gate defects in-place, then re-run the final gate. No automatic merge, issue closure or release.

## 7. Not blocking initial GEM Done

- Newly sourced DOS, FBNeo and MAME2003Plus content with rights review and proven core-aligned identifiers.
- More 1,000-record curation releases using the **same** schema/pack importer.
- Richer ratings preferences, themes/era, sessions, multiplayer modes, verified compatibility and mod recipes only when sourced and implemented.
- Optional signed user-controlled catalogue updates. No always-online/cloud inference dependency.

## 8. Ownership and Codex continuation contract

| Owner | Work |
| --- | --- |
| Roxy | Canonical genre map; source/licence evidence; 250-then-1,000 high-value feature records; collision queue; input holds; final data census |
| Daniel | Additive sidecar contract; proof of ID continuity; source provenance; work-link safety; signature/rollback |
| Elliot / Codex | Implement A through D **end to end on the existing PR**, then E. No intermediate verification stop. |
| Sienna | Current GEM Browse, details, For You, Search and badge wording; no UI rebuild |
| Marcus | Predefine automated test matrix; one independent final integrated test and physical packet |
| Cassia | Safeguard integration with merged GC and other open lanes, keep owner briefing single-source |
| Jonathan | Accept final scope and one final release/merge gate after supplied evidence |

**Codex direction:** Review live master and PR #368 first; reconcile existing GC merge. Execute A+B+C+D consecutively, reusing all current code and the versioned feature pack. Do not stop after data sample, test, documentation, partial APK or successful CI. Return only when a complete A–D implementation and E evidence packet are available, with source counts, honest unknowns and one APK. Do not merge/close without owner authority.
