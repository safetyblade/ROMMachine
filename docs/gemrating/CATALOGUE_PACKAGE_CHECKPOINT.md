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
