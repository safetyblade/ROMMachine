# B7 — Visual completion candidate

Base: verified B6 PR #333 merged at 052ccbc6e90512335768ffe9ec3fc48c108003bb.
B6 candidate build #837 verified; exact master build remains a required release gate.
No engine, controller dispatch, save, Room schema, signing or CI packaging changes.

## B7-01
Source Sans 3 upstream static TTF faces, unmodified. Source: https://github.com/adobe-fonts/source-sans/tree/release/TTF (Adobe release branch). Each copied blob hash matches upstream:
- Regular c5dd0b88cc8ff4edd7ebdcefee0290df15529657
- Medium fbc97bf32b11d8bf1760c40b1f36e4041a2014a7
- Semibold 4aff9a3aea9b5f1e746f03ed40e1b293740dc8a2
- Bold 85693ccc852fbd7fa4ae9d89d6ccbf6d38f8e0f0
- SemiboldIt 74c566168c2ba07db9c88647738bfc4565998910
OFL 1.1 license: assets/licenses/source-sans-3-OFL.txt; upstream LICENSE.md. No online font service.
Shared Compose type roles use the bundled family while preserving existing sp and line heights.
No change to cartridge or logo geometry. Platform glyph fallback and real-device font scaling require acceptance.
Legacy native Android/Leanback text retains its platform font in this candidate.

## B7-02 / B7-03
Approved B4 action path conversion to Android vectors for shared actions; Shelf search/context actions, main header and GEM management use adopted vectors. Existing product marks unchanged, unavailable product surfaces not mounted.
Compact shared menu action semantics and growable context row height; GEM cards 12dp corners. Further consistency/accessibility review of every runtime menu is required during acceptance. TV keeps its B6 Leanback navigation.

## B7-04
Local picker validates bounded input via BitmapFactory, stages a unique local file; online and local share canonical cartridge identity and compare-and-set cover-only DB update. Reset commits null before deleting the previous file. Failures retain previous pointer and report a failure. Only app-owned matching cover files are removed. Existing low-level importCover legacy helper retained for existing callers/tests; production GameInteractor uses stageLocalCover instead.
Regression cases: invalid local image retains prior file; stale identity and stale reset reject; successful replacement/reset retain favourites and other metadata. No ROM or save mutations.
Manual upload uses existing picker without a separate new preview screen; online preview remains.

## Gates
Pinned repository Kotlin source, Room, connected APK, unit/UI tests and exact master artifact before merging. Local Gradle unavailable. B8 must include real device and TV verification (all appearances, fonts/long titles and scaling, D-pad focus, all mounted actions, upload invalid and replacement/reset, install-over progress). No claim of completed physical acceptance.
