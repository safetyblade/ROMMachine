# GC-01 GEM — User GemRating as recommendation learning signal and catalogue enrichment
**22 Nov 2026. Owner clarification from Jonathan Prestwich.** Existing draft PR #362; design/data research handoff, not code, source ingestion, or QA acceptance.

## Product rule — approved

- **GemRating belongs to the user**, is deliberately entered as a **whole-number enjoyment score 1–10**, and should guide the offline personalised GEM recommendation programme.
- **My DeckFit** is separately entered by the user as whole-number **1–5**, reflecting how well the game plays on CARTDECK. Never use half numbers.
- Roxy's editorial views, a provisional editorial DeckFit score, critic/popularity rankings and runtime compatibility **are not substitutes for user GemRatings**. Never automatically create, override or infer a user's rating.
- User ratings, favourites, Not Interested and optional preferences belong to the current authorised player profile. No cross-profile leakage or inherited favourites without explicit action.

## 1. Actual inspected source and why ranking is data-limited

On current draft PR #362:
- File: lemuroid-app/src/main/java/com/swordfish/lemuroid/app/gemrating/GemCataloguePackage.kt. Catalogue Release currently defines releaseId, systemId, title, sourceId, sourceRecordId, optional primaryGenre and optional verified hashes/serials. The main release schema has **no per-release mechanics, crossover genre or playstyle**.
- File: lemuroid-app/src/main/java/com/swordfish/lemuroid/app/gemrating/GemRatingRecommender.kt. The scorer accepts verifiedWorkId, primaryGenre, crossoverGenre, playstyle, mechanics, eligibility and evidence. Those last four gameplay facets are the actual sparse feature-vector similarity inputs. The recommender already uses explicit user ratings and excludes Roxy, DeckFit and critics as training signals.
- File: lemuroid-app/src/main/java/com/swordfish/lemuroid/app/gemrating/GemDiscovery.kt. The release-to-recommender projection obtains additional features through GemGameplayFeatures.find(releaseId,title); when none exists, crossover, playstyle and mechanics are empty and evidence is weak.
- File: lemuroid-app/src/main/java/com/swordfish/lemuroid/app/gemrating/GemGameplayFeatures.kt. A small manually authored, exact-ID-and-title list has examples for card games, tactical games, kart racing and similar groups; it is not full 88,246-release coverage.
- Current 19 Nov B7-A bundle: **88,246 release records across 30 mapped systems**, not 88,246 independently verified games. DOS/FBNeo/MAME2003Plus gaps. Neither the presence of a release nor a good DeckFit score proves game launch or in-game compatibility.

**Critical priority:** Enrich existing eligible records with **genre + crossover/subgenre + playstyle + mechanics** before treating another raw record-count increase as completion of GEM recommendations. Do not claim data coverage percentages until audited.

## 2. Database fields to fill in — staged contract

| Priority | Field | User/recommender value | Existing support |
| --- | --- | --- | --- |
| P0 | Stable source-qualified game/work/release/edition/region/platform ID, aliases, matching confidence | Safely associate ratings from the same actual title; avoid wrong variants and repetition | Existing release ID and system; work linking only when independently verified; preserve identity |
| P0 | Primary genre from controlled taxonomy | Broad style preference and basic discovery | Present, must audit missing/placeholder coverage |
| P0 | Secondary or crossover genre | Meaningful cross-genre bridges for wildcard recommendations | Scorer supports; broadly missing from main Release schema |
| P0 | Gameplay mechanics, multi-valued canonical tags | Strong reasons such as deckbuilding, collecting, grid tactics, platforming, turn-based combat, exploration or racing | Scorer supports; currently only small feature overlay |
| P0 | Playstyle / gameplay tempo | Match turn-based, real-time, methodical, reflex, arcade-style and similar user choices | Scorer supports; currently small feature overlay |
| P0 | Eligibility, evidence, per-field source/rights/review date | Reject non-games/firmware and avoid invented similarity claims | Source-level categories exist; more complete provenance needed |
| P1 | Single/co-op/competitive multiplayer modes and player count | Better context and matchmaking user preferences | Requires schema and scorer/filter expansion |
| P1 | Sessions and pacing (short rounds, campaign, open-ended) | Handheld, quick-play vs longer-play recommendations | Requires schema and scorer expansion; do not estimate from title |
| P1 | Theme, setting, mood, narrative emphasis | More precise discovery beyond gameplay mechanics | Requires schema/scorer expansion |
| P1 | Complexity/difficulty, controller/touch/motion requirements, orientation, readability | Better contextual recommendations and DeckFit evidence | Must keep separate from emulator support; requires verified metadata |
| P1 | Series/franchise and verified Work/Edition relationship | Avoid repetitive remaster/edition results, build variety | Never title-only merge |
| P2 | Release era, developer/publisher, supported languages, region and subgenre | Optional discovery and filters | Field-specific rights and evidence |
| P2 | Per-player wishlist, ratings, Not Interested, favourite, played/recent, wildcard preferences | Personal feedback, eligibility and candidate filtering | Local player store, never bundled editorial catalogue |

No invented or hallucinated metadata to make cells nonempty. Store unknown rather than guessing. Source data is historical game metadata, not actual Android runtime capability proof.

## 3. Recommended additive feature-pack architecture (design handoff)

Keep the signed/versioned base catalogue and all immutable IDs unchanged. Add an independent versioned sparse **GameplayFeatures** layer keyed to exact source-qualified releaseId and optionally a genuinely verified workId. Each classified field carries source revision, original record key, date/reviewer, licence/attribution and evidence tier. Preserve the player's private rating ledger and old source identities on metadata refresh; no Room/SaveBinding or ROM scanner mutation just to attach features. Daniel must review actual on-disk pack schema/version constraints and migration strategy.

Keep multi-valued mechanics/tags as a controlled taxonomy with source aliases and explicit normalization tests. An absent feature is **unknown**, not a negative preference. Maintain source-backed, editorial-provisional and weak-context as distinctly represented evidence. A title-only uncertain match must not fill missing game attributes.

Some future P1/P2 columns are valuable to store but **will not improve recommendations until the actual GemRatingRecommender projection and scoring are extended**. Engineering must provide working before/after tests, not just new JSON columns.

## 4. Ratings-to-recommendations behaviour

1. Every explicit high user GemRating teaches positive affinity for the game's **sourced** genres/mechanics/playstyle. Low scores contribute negative/avoidance weight for *relevant* attributes but should not permanently ban every matching title; neutral/mid-range scores contribute little. Clearing a rating removes that signal.
2. User ratings of both installed and uninstalled catalogue titles may train the same current profile **only after identity/eligibility is verified**; unrated or ambiguous matches never enter training.
3. Never display a computed fractional GemRating. In the inspected GemDiscovery.prepare code, multiple cartridge ratings linked to one release are currently averaged as Double for **internal ranking projection**. Preserve **each original whole-number rating** and do not persist that mean as if the player gave it; test conflicts, multiple cartridges, edition differences and reimports. Internal numerical ranking relevance is not itself a user rating, and may remain a decimal.
4. Only explain similarity using actually sourced shared mechanics/subgenre/style; example: "You rated Advance Wars highly; both use turn-based tactics and resource management" **when those facets are independently sourced**. No fabricated explanations.
5. With no ratings or no usable metadata, show Browse/Rate a Game and neutral discovery, never fake a personal "For You". User may control wishlist, genre/platform scope, owned vs uninstalled, Not Interested and wildcard share. Not Interested is a filter signal, **not** an automatically created 1/10 rating.
6. Separate eligibility: a catalogue game may be recommended as "Not in Library" with metadata only; never imply it is installed or its native engine is verified. Junior restrictions and profile limits filter candidates before ranking.
7. Preserve deterministic offline operation, stable work dedupe only with verified workId, opt-in Modern Function and controller/motion limitations separately.

## 5. Roxy's two-hour database sprint — rebalanced priority

The earlier source assignment investigated FBNeo DAT to fill the arcade gap. FBNeo can add platform names, core shortnames, ROM set identity and clone/parent links, but **may not supply rich gameplay mechanics, playstyle, themes or subgenres**. Therefore the sprint now has **two distinct deliverables**:

**A. Existing catalogue enrichment audit (highest recommendation priority)**
- Inspect exact current base release schema + small GemGameplayFeatures overlay.
- Measure per-system/source **total eligible releases** and non-null count/percentage for primary genre, secondary/crossover genre, mechanics, playstyle and reliable verified identity, as well as placeholder/non-game/ambiguous classes.
- Build a candidate controlled taxonomy with valid synonyms; sample source extracts and source-licensed fields; describe any new optional schema/scorer fields needed.
- Pilot a **100–300 diverse-game** fixture from *lawfully sourced metadata* (card/turn-based/tactics, platformer, puzzle, racing, sports, RPG, adventure and cross-genres). Attach individual per-field source citations or provenance, stable ID and evidence. Unknown values remain unknown.
- Compare before/after ranking on explicit fictional **test** player ratings only (whole 1–10), disliked games, sparse data/cold start, Surprise Me/wildcards, duplicate editions and rich reasons; fixtures are not real user ratings or user analytics.
- Record exact match/conflict counts, provenance, changed release IDs (must be zero), source rights/attribution, staged JSON digest and rollback. Do not claim completed data integration without evidenced repo/source changes.

**B. New source feasibility**
- Core-aligned FBNeo DAT first, then rights-cleared MAME2003Plus/DOS candidate. Separately audit dataset redistribution/attribution rights; engine code licence is not a guarantee for the data pack.
- Rate each proposed source on **coverage plus gameplay-field richness**; prioritise a legitimate source with mechanics/genre/playstyle where feasible, not the largest count.
- Do not download ROMs, BIOS, firmware, box art, copyrighted media or game payloads. No unsanctioned web source registration, no master merge and no fabricated compatibility PASS.

## 6. Next Codex GEM acceptance — not just another source count

**User-visible:** rate installed and uninstalled verified titles; clear/edit and reopen rating, whole numbers only; proper per-profile isolation; For You with reasons and wildcards from player ratings; independent My DeckFit, provisional editorial DeckFit and runtime evidence; offline use on phone/TV; no fake AI/online recommendation claim.

**Data:** auditable percent of P0 facet coverage per system; valid taxonomy; source licence/attribution; release and cartridge ID continuity; scorer regression fixtures demonstrating meaningful changes with high, low and cleared ratings; no false explanation when facets absent.

**Engineering boundary:** Scope remains GC #362 with GEM #194, original GC A–H #262 and Marcus #248. This design note adds **data and ranking requirements**, not a code implementation claim or team-authenticated approval.
