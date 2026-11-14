# MOD-01 execution evidence — 14 November 2026

**Not a completed CartMods delivery. Do not close #212 or distribute this branch as a working mod manager.**
The owner has authorised full implementation; this document records actual work and missing evidence,
not an architecture approval gate. The source currently contains file-engine groundwork only.

## Recovered baseline

- Clean starting checkout: `59dedc2d66f172e3c810ee9038e02dcc1c2cc8fc`.
- Fetched master and branched `feat/cartmods` from `2024f058c2f202b66fc3b1ec4e7a1ea8319fee3b`.
  This includes #206 checkpoint B and #207 adaptive appearance work.
- Read the owner override in #212 and related #145/#146/#147/#148/#93. Earlier research-only
  instructions are superseded. PERF-05 remains owner-confirmed fixed; no engine changes here.
- Room v12: random immutable `Game.cartridgeId`, frozen legacy source/name aliases, exported
  schema and existing save binding. Custom names/art, favourites, shelf and source reconciliation
  remain owned by their current components. This change does not alter Room or its migrations.
- `GameLoader` computes modern PSP system root at `filesDir/system-engines/ppsspp_modern` and
  modern save root beneath the supplied saves root at `engines/ppsspp_modern`. Fallback uses the
  existing roots. `PSPSavedataSeed` preserves engine separation. Neither texture nor savedata
  deployment may write into pinned system assets.
- Inspected cartridge detail/context UI, launch data, PPSSPP assets/defaults, directory manager,
  database identity, both ARCH-LIB checkpoint documents and existing APK workflow.
- Connected PR searches returned no open results. Local `gh pr list` and `gh run list` were
  forbidden by the API transport, so an exhaustive active-PR inventory is **not verified**.

## Existing failures (before any CartMods integration)

Local baseline command:

```sh
./gradlew :lemuroid-app:compileFreeBundleDebugKotlin :lemuroid-app:testFreeBundleDebugUnitTest \
  --no-daemon --max-workers=4
```

Failed resolving `com.github.Swordfish90:LibretroDroid:0.14.0`: JitPack HTTP 403. The configuration
cache serialization error wraps that dependency failure. Log: `/workspace/cartmods-baseline.log`.
An offline shared-module compilation also cannot resolve multiple uncached Android dependencies;
log: `/workspace/cartmods-shared.log`. Neither attempt proves Android source compilation.

Inherited [Actions run 43528531739](https://github.com/safetyblade/ROMMachine/actions/runs/43528531739)
for #207 head `98b77d21c0d596a4f7284a6b8ae5d9078a1b4028`:

- Source preflight and all five native engines, patched host and hakuX build: success.
- Both integrated APK jobs: failure at `compileFreeBundleDebugUnitTestKotlin`.
- Concrete error: `LibretroGameMenuTest.kt:11:33 Unresolved reference 'onNode'`.
- APK verification/upload gates did not pass. No release artifact from that run.

The pinned-host artifact is available through the GitHub connector, but downloading its returned
storage URL into this execution environment is also rejected by the proxy (HTTP 403). No dependency
substitution, weakened TLS, test removal or native-engine replacement was attempted.

## Implemented source

- `ModArchive`: bounded ZIP central/local header inspection, path validation, case-collision and
  file/directory collision checks, Unix link/special-file rejection, overlap rejection, compression
  and expansion budgets, file count/storage reserve limits, per-file size/CRC verification, explicit
  file allowlist callback, cancellation, isolated temporary extraction and failure cleanup.
- `ModPatch`: IPS literal/RLE/optional truncate and all four BPS commands, including overlapping
  target copies. Uses bounded buffers and seekable files rather than loading disc images into RAM.
  Requires source **and output** SHA-256. Verifies all BPS CRCs. Caps output at 8 GiB by default,
  checks storage reserve, honours cancellation and commits a new file only after verification.
  Existing source, patch and pre-existing edition files are not overwritten by these checks.
- `ModFiles`: common safe relative paths, bounded copy and cancellable SHA-256 verification.
- A JUnit wrapper registers the synthetic fixture suite with the existing app unit-test source set.
  A standalone compiler runner executes those same production classes without Android dependencies.

Caller contracts still matter: archives and staging parents must be app-owned, package imports must
be serialised, destinations must be managed edition paths, and the eventual persistent transaction
must own orphan recovery and cross-operation storage quotas. These file engines alone do not supply
those service-level guarantees. ZIP64, encrypted/split ZIP, xdelta/VCDIFF and PPF are rejected.
There is no executable installer support and no third-party game/mod asset bundled in this change.

## Verification

- Kotlin lint for app and shared-module production/test sources: **PASS** (`ktlintCheck`).
- Standalone Kotlin 2.0.21/JVM 17 compilation of the three production engines: **PASS**.
- Synthetic engine checks: **37 PASS**, including valid ZIP, traversal/absolute paths, Unix symlink
  metadata, local header mismatch, case collisions, parent-file collisions, expansion/file-count/
  compression limits, cancellation, simulated storage reserve failure, truncated archives, corrupt
  extracted payload, IPS literal/RLE/resize, BPS source read/literal/source copy/overlap/long varints,
  base/target/patch checksum failures, target-copy bounds, source retention and partial-file cleanup.
- The storage test is deterministic reserve-budget rejection; it is not an Android ENOSPC test.
- No Android instrumentation, Room migration, integrated unit-suite, gameplay, save-continuity,
  install-over or independent Marcus QA pass is claimed.

Reproduce the isolated checks using a populated Gradle Kotlin compiler cache:

```sh
JAVA_HOME=/workspace/toolchains/jdk-17.0.17+10 \
GRADLE_USER_HOME=/workspace/.gradle python3 scripts/ci/test_cartmods_engine.py
```

The runner accepts `KOTLIN_COMPILER_CLASSPATH` and `KOTLIN_STDLIB` for other installations.
It is additional evidence, not a replacement for the repository's existing release gates.

## Outstanding delivery and acceptance

| Required result | Actual status |
| --- | --- |
| Room ModPackage/Binding/Profile/Deployment, additive migration and generated schema | Not implemented |
| SAF import, inspection, durable installation/recovery record | Not implemented |
| Cartridge Mods UI and protected save choices | Not implemented |
| PPSSPP session texture option, actual deployment and per-profile save roots/locking | Not implemented |
| Capability adapters and cross-console independent cartridge editions | Not implemented |
| Crash recovery, removal, enable/disable, rollback and install-over continuity | Not implemented |
| Existing-functionality gameplay regression | Not run; existing production paths unchanged by this diff |
| Signed ARM64 QA APK with artifact verification | Not produced |
| Marcus independent QA | Not performed |
| Synthetic rendered texture proof and WWE match acceptance | Not performed |

There is currently **no per-console player-facing CartMods capability** and **no accepted WWE mod
packaging workflow in the app**. Internal IPS/BPS engine tests must not be presented as cross-console
integration, compatible WWE import, or a finished feature. No new user-facing menu or placeholder
has been exposed.

The owner will perform hardware tests on a Samsung Galaxy. Exact model, installed app identity,
Android version, supplied base-game revision and creator package remain to be recorded against an
actual candidate APK. There is no attached adb device or `/dev/kvm` here. A future device handover
must cover rendered replacement on/off, independent original/modded progress, same PSP game ID,
engine fallback, full close/reopen and upgrade-over-install before WWE entrance/full-match proof.
Do not advance to Roxy's creator-pack selection handover or close MOD-01 from these file-engine checks.
