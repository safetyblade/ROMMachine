# GEMRating offline recommender — isolated implementation

Source contract: [GEM Master Game Catalogue](https://docs.google.com/spreadsheets/d/12k2ZEmIO3mzo8z77FS6vMtsBjF8AxmOQ6Wf1DJVNXpo/edit), tabs `GEMRating Feature Index`, `GEMRating Ranking Contract`, `GEMRating Test Fixtures`, `GEM Verified Work Links`.

## Scope of this PR

- Pure Kotlin `GemRatingRecommender` accepts an already-validated list of game metadata and explicit owner `Rating` records, calculates deterministic ranked results offline and returns evidence-based explanations.
- It **does not** read Google Sheets, ingest licensable content, mutate ROM bindings, migrate the Room database, edit saves, write player scores, modify UI, or assert console compatibility.
- It **does not** substitute RoxyRating, critic scores, or DeckFit for owner preferences.
- No catalogue entries are bundled with the APK in this PR. Research-only and excluded records are not eligible for playable recommendations.
- Work ID deduplication is permitted only for externally *verified* canonical identities; same-text titles and expanded editions remain separate.

## Integration interface

Pass `GemRatingRecommender.Game` from an explicitly reviewed, locally licensed GEM metadata import; use the game's stable release/catalogue identifier, canonical work ID **only when independently confirmed**, normalized genre, sourced crossover, playstyle, mechanics and evidence tier. Treat missing fields as absent, not positive preference evidence. Never guess eligibility from a historical catalogue row. `Eligibility.GAME` means *product eligibility has been confirmed*, **not** a game boot test. Separate runtime capability and ownership checks are mandatory.

Pass `GemRatingRecommender.Rating` only from explicit local user ratings in a future versioned rating repository keyed by cartridge/release scope. Never read them from editorial catalogue columns or auto-inherit between uncertain work records.

`recommend(catalogue, ratings, limit, wildcardShare)` returns score, source-feature explanation and optional wildcard flag. Cold start returns empty; the UI must provide clearly labelled non-personalised browsing separately.

## Verification

Local independent Kotlin compilation and a lightweight temporary test runner executed 16 deterministic JUnit-style test functions on 2026-11-16 (14 policy fixtures plus 2 samples using real GEM catalogue IDs and facet names): **16 PASS / 0 FAIL**. The temporary test runner and JUnit shim are **not** repository sources. Run the actual Android-module test task in CI to independently confirm Gradle/JUnit compatibility:

```sh
./gradlew :lemuroid-app:testFreeBundleDebugUnitTest --tests '*GemRatingRecommenderTest' --tests '*GemRatingCatalogueFixtureTest'
```

If that Gradle variant name differs, inspect `./gradlew :lemuroid-app:tasks --all` first.

## Integration gates deliberately left open

1. Approved/redistributable GEM metadata export with source attribution, canonical source IDs and evidence confidence.
2. Local rating persistence, edit, delete, export/import and controlled migration; preserve `SaveBinding` and cartridge UUID continuity.
3. Repository integration adapters and player-facing GEMRating/GemFinder screens.
4. Offline library and gameplay eligibility filtering, plus hardware/Marcus QA.
5. CI green, Android APK build and user physical verification.

This PR is a safe computation checkpoint **only**. It must not be represented as finished GEMRating UI, finished GEM data licensing, a complete game catalogue or a release-ready APK.
