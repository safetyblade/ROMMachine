# P1-A1 native cartridge library — review and independent QA handoff

Scope: Issue #27, parent #12. Base: master `1a35f9b971f2ab116c86ae5ab2cac5b93b2b3657`.
Branch: `feature/p1-a1-native-cartdeck-cartridge-shelf`. No merge or public release authorised.

## Implemented journey

Home uses Original graphite/teal tokens independently of Android wallpaper colours. Add Cartridges calls the existing HomeViewModel folder-picker API. Existing permission actions, scan state, recent records (Continue), favourites and discovery are retained. Permission cards use compact spacing; scan progress does not hide existing cartridges.

Home, Search, Favourites and Systems render the same horizontally browsable Arc-Rail spines. Runtime titles and system badges sit above native vector layers. Spine dimensions grow with font scale; full title/system semantics remain available when visible text ellipsizes. Keyboard focus has a separate highlight.

Every ordinary game selection reaches one activity-level cover dialog, storing only the selected game ID in saved instance state. The underlying destination and shelf state remain mounted. Dialog Back returns to that destination; lazy shelf scroll state supports saved-state restoration. The dialog reads the existing database record and observes invalidation, including asynchronous cover writes. Explicit Open consumes a one-shot UI guard and calls the unmodified GameInteractor.onGamePlay exactly once. No Resume label is inferred from last-played time: the existing launcher still controls automatic resume. Long-press context actions remain available.

Change Cover uses Android GetContent and the existing GameInteractor.onSetCustomCoverUri/CustomCoverManager path; cancel does not write. Reset Cover calls the existing clear callback. Replaced local files bypass stale image caches. Artwork lives inside the approved 40/113/240/200 art window with center-crop and rounded clipping. Only locally supplied custom artwork is displayed here; absent/broken artwork uses the original atlas fallback. Crop/zoom/position editing remains P1-A2.

## Source and asset boundary

No emulator, LibretroDroid version, submodule revision, native rendering, PadKit, scanner, storage permission implementation, database schema/entity/DAO, save/state format, game IDs, launch implementation, package-ID logic or signing configuration changed. Test-only dependencies and Android resource-aware unit tests were added. The standalone CI workflow now runs the cartridge tests and retains reports; existing identity/signature/native checks remain intact.

`scripts/export_cartridge_vectors.py` reuses the existing reviewed SVG converter and resolves semantic edition tokens from v03 JSON. Canonical front, overlay, spine, fallback, badge, back and C-slot paths become native VectorDrawables for Original and Pink. Both editions have identical path geometry and viewport bounds. `scripts/test_cartridge_vectors.py` checks native geometry, fallback and art-window bounds; existing v05 design tests also remain applicable.

Original is the only active runtime edition. Pink is available to native component previews/resources without adding edition settings. The generated resource comparison images in [p1-a1/previews](p1-a1/previews) are **VectorDrawable-derived reference renders, not Android device screenshots**. `scripts/render_cartridge_previews.py` reproduces them with optional CairoSVG; this dependency is not used by the application.

## Reproduction

Use Java 17, SDK platform 35, build-tools 34.0.0 and the repository wrapper; initialise recursive submodules. Network destinations include JitPack, Google Maven, Maven Central and the Gradle distribution/plugin services. Preserve TLS/checksum validation.

```sh
python3 scripts/export_cartridge_vectors.py
python3 scripts/test_cartridge_vectors.py
python3 design/cartdeck-v05/test_cartridge_family.py
python3 scripts/audit_cartdeck_brand.py
./gradlew :lemuroid-app:assembleFreeBundleDebug :lemuroid-app:testFreeBundleDebugUnitTest \
  -Pcartdeck.dev.standaloneId=true \
  -Pcartdeck.p0.phoneArm64Only=true \
  -Pcartdeck.p0.testVersionCode=2005 \
  --no-daemon --no-configuration-cache --max-workers=4
```

Local validation used a writable Robolectric test home and the managed proxy/CA, supplied through a local Gradle init script. The standard CI runner has a writable home. In restricted cloud shells, Kotlin's in-process compiler strategy avoids attempts to create daemon files under the read-only home. These are environment overrides, not dependency or assertion changes.

Workflow: `.github/workflows/cartdeck-standalone-debug.yml`. It chooses `2000 + GITHUB_RUN_NUMBER`; always inspect the actual artifact version before installation. Local candidate version 2005 is above the stated v2001 baseline and the latest observed CI v2004. The earlier local v2101 build was diagnostic and is not the handoff candidate.

Expected artifacts: `cartdeck-standalone-arm64-<run_id>` (APK), `cartdeck-standalone-evidence-<run_id>` (commit, checksum, package/version/signature/native inventory), and `cartdeck-cartridge-tests-<run_id>` (JUnit XML and HTML). The local staged evidence directory is `/workspace/artifacts/p1-a1` and is not committed. Use its source-SHA file and APK hash rather than assuming a filename establishes provenance.

## Validation and acceptance limits

**Local result: standalone ARM64 debug build PASSED; 19 Compose/Robolectric tests PASSED (0 failures, 0 skips); 4 native-vector tests and 2 v05 family tests PASSED; brand audit PASSED.** Local APK: versionCode 2005, 90,533,029 bytes, SHA-256 `b4ec9712532124d8552436fcd68a5a270302581ca3233b6f278722c24dcb0edd`; package and signer match the standalone baseline.

Compose/Robolectric tests exercise empty/import, populated multi-system shelves, full accessible long titles, selection versus launch, one-shot Open, Back callback, picker cancellation and successful result, reset callback, local-cover manager persistence, fallback availability, Continue, narrow layouts, large text, saved shelf position, and actual database-backed Search/Favourites/Systems selection. A headless test is not independent device QA. Existing save interfaces are protected by source-boundary review; gameplay/save persistence is not inferred from that review.

Initial JitPack/GitHub API access failed with policy HTTP 403 and recovered after the owner enabled full access. Initial Robolectric execution failed before assertions because its lock file targeted a read-only home; a writable test home corrected the harness. A compiler error in the first artwork-refresh parameter pass was fixed and rebuilt. Kotlin/AGP deprecation warnings remain upstream warnings. No CI requirements were weakened to hide failures.

No physical device is attached; `/dev/kvm` is absent. Real before/after Android screenshots, update-over-v2001, URI-grant persistence across an actual Android upgrade, complete TalkBack/keyboard traversal, and GB/GBC/GBA gameplay, SRAM, manual states and controller regressions are **NOT TESTED** here. The APK is an internal review candidate, not runtime acceptance.

## Sienna / Cassia review

- Compare the native front/spine contours, art clipping and C-slot with the v05 masters; both editions retain exact paths.
- Review system typography and two-line rotated title legibility. The header uses native Material typography rather than converting the custom SVG wordmark's transformed text.
- Static selection-to-cover is the reduced-motion default; no artificial rotation delay or new animation system is introduced.
- Review Original foreground/surface contrast, focus treatment, large text, compact permission messaging and landscape scroll.
- Pink resource previews are prepared; there is no shipping edition switch or gameplay-shell change.

## Marcus / Jonathan independent QA

1. Verify source SHA, APK SHA-256, package `dev.prestwich.cartdeck.debug`, candidate version and debug signer `ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8` from the exact artifact. Capture the installed version first. Reject a candidate that is not newer than the installed build.
2. Install **over** the standalone baseline (`adb install -r` or the normal Android update UI). Never uninstall, clear data or use a downgrade flag. The separate legacy application ID does not share this app's data.
3. Capture baseline and candidate Home, empty import, 3+ GB/GBC/GBA cartridges, selected front, long title, landscape/tablet and large-font screenshots using lawful fixtures.
4. Confirm selecting a spine never launches; explicit Open launches once. Back preserves the prior route/shelf position. Test Home/Continue, Search, Favourites and Systems plus long-press actions.
5. Change a local cover, cancel, change it again using the same file type, restart, reset, and verify persistence and fallback without changing the game ID. Revoke a source grant after import and confirm the private copy still works.
6. Verify existing ROM picker grants/import, notification/microphone recovery, GB/GBC/GBA startup, audio/video, touch/gamepad, cartridge SRAM, manual state, process restart and install-over-save persistence independently. Separate SRAM from auto-resume/manual states in the results.
7. Record Sienna visual review, Cassia experience acceptance, Daniel boundary review and Marcus independent results against the same SHA/APK. Keep the PR open until applicable #9/P0/P1 gates are recorded.
