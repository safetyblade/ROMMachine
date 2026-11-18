# GEM-B1-D — Catalogue packaging checkpoint

## Production gate
The GEM Master Source Register records Title Screen CC BY 4.0 attribution terms, but the master contains mixed-source research and provisional identity matches. No blanket redistribution clearance is asserted. Do not package the master worksheet. Roxy must supply an independently reviewed record-level export and source attribution/approval register; exclude RoxyRating, player ratings, critic scores, unverified serials and artwork.

## Format
Schema 1 JSON: catalogueVersion, releaseCount, sources (id/name/url/licence/attribution/revision), releases (releaseId/systemId/title/sourceId/sourceRecordId, optional primaryGenre and verifiedSha256/verifiedSerials).
Export input: {"catalogueVersion":"v1","releases":[...]}. Independent approval input: {"catalogueVersion":"v1","sources":[...],"approvedRecords":[["sourceId","sourceRecordId","releaseId"]]}.
Run python3 scripts/gem/export_catalogue.py reviewed.json approval.json candidate.json.
The output is a candidate, not approval. Independently review its exact bytes and pin SHA-256, version and source declarations in trusted app configuration. Do not compute trusted approval from the import itself. Source/record review must inspect actual content, not just IDs. No live online service. A future signed release manifest or trusted local approval workflow will be needed to support newly issued packages without updating APK trust configuration.

## Import boundary
GemCataloguePackage.load receives bounded InputStream and independently trusted Approval. It rejects altered bytes, unsupported schema, mismatched source declarations, invalid platform IDs, duplicates, malformed identities and excessive size/count. Successful result exposes the validated index and match(game,evidence). InputStream lifetime belongs to caller. No active disk replacement or scanner mutation occurs. Persistent staged activation/rollback and storage-picker wiring remain B1-E/B5 work. No bundled production data or approval pins are included yet.

## Verification
Six package tests and ten existing matching tests authored; Android Gradle/JUnit not executed at this checkpoint. Test fixtures are synthetic, not hardware QA. Require CI, source rights clearance and production export review before declaring B1 complete. Existing cartridge UUIDs and save bindings are not edited.


## 2026-11-18 — Candidate seed and local execution

- Source: Title Screen public release pages (data build 2026.10.10); licence and redistribution statement: https://titlescreen.org/downloads ; attribution included in the package. The compilation is CC BY 4.0. Selected facts adapted; no copy/dump metadata or artwork redistributed.
- Review inputs: `scripts/gem/seed/reviewed.json` and `scripts/gem/seed/approval.json`. These record an engineering pilot review, not independent owner/legal sign-off. The source record IDs refer to individual release pages under https://titlescreen.org/release/. The two Advance Wars releases have directly reviewed genre evidence; the rest omit unverified genre fields. No strong identity fingerprints exported from incomplete source coverage.
- Candidate: `lemuroid-app/src/main/assets/gem/catalogue-v1.json`, 16 GBA commercial release rows, version `gem-seed-2026.11.18.1`. This is deliberately limited metadata, not a representative launch catalogue or a game compatibility claim.
- Trusted developer pin: `BundledGemCatalogue`, SHA-256 `2cf0d7bab4c9ac3b2a899b9aa23a6a04a544c22574ffd9f2418a6207311f3761`. Import is checked against preconfigured bytes, version and source declaration. The pin is not generated from a user-supplied file. Future packages need trusted manifest distribution; no live sync yet.
- Local verification: exact exporter and seed matched committed Git blobs; the exported seed rebuilt byte-for-byte. Nine Python export regression tests passed locally, using the same test source subsequently committed. Android Gradle/JUnit tests, ktlint, APK and device verification remain pending. No Android test result is implied by Python execution.
- The bulk archive was inaccessible from the execution environment. GEM Master research rows and native/legacy ambiguities were not silently promoted. The production coverage gate remains open.
- B1-E must wire persistent catalogue selection and matching to the installed library as read-only operations, preserve user correction state and fail safely if the catalogue cannot load. B5 delivers staged activation, rollback and transfer. B2 owns profile ratings and save-independent state.
