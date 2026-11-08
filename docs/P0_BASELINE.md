# CARTDECK — P0-A Android baseline

**Status:** DEBUG APK BUILD VERIFIED; EMULATOR / REAL DEVICE / GAMEPLAY QA PENDING. This document deliberately records unknowns and failures rather than implying acceptance.
**Date opened:** 2026-11-08
**Work package:** [CD-002 / Issue #3](https://github.com/safetyblade/ROMMachine/issues/3)
**PR branch:** `p0-a/debug-apk-baseline` from `master`. Do not merge before Daniel's architecture review, Marcus's independent QA and Cassia's acceptance.
**Controlled requirements:** [CARTDECK Drive 00–05](https://drive.google.com/drive/folders/1O4Bc1p9IAEkiSWCPuauhtDEirGYX0r_1)

## 1. Pinned source and known configuration

| Item | Baseline fact | Evidence |
| --- | --- | --- |
| Repo | `safetyblade/ROMMachine` | GitHub source |
| Starting master SHA | `029484893178dffa44e34db910c5a899257bcc69` | GitHub refs/heads/master, 2026-11-08 |
| Submodule | `lemuroid-cores` -> `fee2e824525daa22bcf318f96127fe43fa8a15ad` | Git tree gitlink / `.gitmodules` |
| Submodule origin | `https://github.com/Swordfish90/LemuroidCores` | `.gitmodules` |
| Wrapper | Gradle **8.10.2** | `gradle/wrapper/gradle-wrapper.properties` |
| Root Android Gradle Plugin | **8.4.0** | `build.gradle.kts` |
| Root Kotlin plugin | **2.0.21** | `buildSrc/src/main/java/deps.kt` |
| Required Java | **17** | Root configuration and CI |
| SDK | compile/target **35**, min **23**, build tools **34.0.0** | `buildSrc/src/main/java/deps.kt` |
| NDK | No project-pinned NDK identified in inspected configuration | **To record at CI runtime** |
| Runtime bridge | `com.github.Swordfish90:LibretroDroid:0.14.0` | `deps.kt` |
| App | versionCode **252**, versionName **1.17.0** | `lemuroid-app/build.gradle.kts` |
| Debug package | Expected `com.swordfish.lemuroid.debug` due to `.debug` suffix | **Verify from actual APK** |
| Signing | Debug type references tracked root `debug.keystore`; release config points to `release.jks` and inherited credentials | App Gradle config; **never use release signing** |
| Flavours | `free/play` × `bundle/dynamic` | App Gradle config |
| Candidate task | `./gradlew :lemuroid-app:assembleFreeBundleDebug` | Declared flavour names; **must prove by execution** |

## 2. Attempted baseline / exact reproduction

The current connector can read and change the GitHub repository, but does not execute Gradle. The available local container has **Java 21, no installed Android SDK**, and outbound GitHub DNS failed before a checkout:

```text
git ls-remote https://github.com/safetyblade/ROMMachine.git HEAD
fatal: unable to access 'https://github.com/safetyblade/ROMMachine.git/': Could not resolve host: github.com
```

This is an **environment / network blocker**, not evidence that the Android source fails compilation. **At initial document creation no APK was available; the verified CI build is now recorded in Section 3.** The first real build attempt is the dedicated debug-only GitHub Actions workflow added by this PR. Read that run's logs before making any dependency changes.

On a networked Android 35 + Java 17 machine, or in CI:

```bash
git clone --recurse-submodules https://github.com/safetyblade/ROMMachine.git
cd ROMMachine
git checkout p0-a/debug-apk-baseline
git submodule update --init --recursive
git rev-parse HEAD
git submodule status --recursive
java -version
./gradlew --version
./gradlew :lemuroid-app:assembleFreeBundleDebug --no-daemon --no-configuration-cache --stacktrace
```

If that Gradle task is absent, run `./gradlew :lemuroid-app:tasks --all` and record the actual task and error. Do **not** silently substitute a dynamic/Play/release variant.

### First remote build attempt — failed in SDK setup (not Gradle)

- **Run:** https://github.com/safetyblade/ROMMachine/actions/runs/42455131571
- **Commit:** `168db6277cf12b1799042dfaf85da2818a594cdc`
- **Command:** workflow `P0-A Android debug APK`, before Gradle task.
- **Observed:** recursive checkout and Java 17 setup passed; `android-actions/setup-android@v3` exited 1 after invoking `sdkmanager tools`. Exact log excerpts:

```text
Warning: Failed to find package 'tools'
Error: The process '/usr/local/lib/android/sdk/cmdline-tools/16.0/bin/sdkmanager' failed with exit code 1
```

- **Cause:** the action attempted an obsolete Android SDK package, even though GitHub hosted `ubuntu-24.04` already exposes Android SDK command-line tools in `/usr/local/lib/android/sdk`.
- **Minimal remedy:** remove that action only and run hosted `sdkmanager` directly to accept licences/install the project-declared SDK packages. Do not change AGP, Kotlin, Gradle, emulator cores or Android app source speculatively.
- **Status:** No compile attempt, APK, launch or game test completed in this run. A replacement CI run must prove the remedy.

### Second remote attempt — hosted SDK binary not on PATH

- **Run:** https://github.com/safetyblade/ROMMachine/actions/runs/42455273266
- **Observed:** checkout and JDK setup passed. The first command in the hosted SDK step, `command -v sdkmanager`, returned exit 1 with no other output. **Gradle was again not invoked.**
- **Cause:** removing the failing SDK setup action also removed its PATH setup, while the base runner SDK remained under `/usr/local/lib/android/sdk` (path confirmed in first run).
- **Minimal remedy:** explicitly use the runner's existing `/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager`, set `ANDROID_HOME` and export the command-line-tool path for later steps. No source or dependency changes.
- **Status:** Debug APK still not available. Check the replacement run's step log before declaring a compile success.

## 3. First successful debug APK — reproducible CI evidence

**Status:** Build succeeded; installing/running on emulator and physical devices are distinct QA gates.
**Build and download:** https://github.com/safetyblade/ROMMachine/actions/runs/42455383634
**Build source SHA:** `230991cbc03eccd402d2a5e3ea0d5c715d92c9a2`
**Base starting SHA:** `029484893178dffa44e34db910c5a899257bcc69`
**Core submodule revision:** `fee2e824525daa22bcf318f96127fe43fa8a15ad`, from `Swordfish90/LemuroidCores`.

| Evidence | Observed result |
| --- | --- |
| Gradle task | `./gradlew :lemuroid-app:assembleFreeBundleDebug --no-daemon --no-configuration-cache --stacktrace` |
| CI Build job | **PASSED**, `BUILD SUCCESSFUL in 4m 17s` |
| Artifact name | `cartdeck-p0-a-freeBundle-debug-42455383634` |
| Artifact ID | `13607950043` (Actions downloadable ZIP, not GitHub Release) |
| Evidence artifact | `cartdeck-p0-a-build-evidence-42455383634` / ID `13606534506` |
| APK filename | `cartdeck-p0-a-freeBundle-debug.apk` |
| APK size | **222012758 bytes** |
| APK SHA-256 | `176917ea40dc23c9a1b085529a84a940b8505e63c34eefeef3a2e195afd31fce` |
| App ID | `com.swordfish.lemuroid.debug` |
| Version | `1.17.0-DEBUG` / versionCode `252` |
| Android SDK | API **35**, build-tools **34.0.0**, minSdk **23**, targetSdk **35** |
| Java runner | **Eclipse Temurin 17.0.21+9** |
| Wrapper | **Gradle 8.10.2** (embedded Gradle Kotlin 1.9.24) |
| Project Kotlin/AGP | **Kotlin 2.0.21**, **AGP 8.4.0**; declarations not to be confused with Gradle's bundled Kotlin |
| Installed runner NDKs | **27.3.13750724**, **29.0.14206865**, **30.0.16248370**; no project-pinned/selected native recompilation shown |
| Debug signature | `apksigner verify` successful, v1 and v2 true; `CN=Odyssey Debug`, RSA 2048. This is an inherited debug key only, not a cleared release identity |
| Bundled GB/GBC/GBA ROM extensions | **None detected** in APK |
| Emulator job | **PENDING** at this evidence snapshot; see same run for eventual result |
| Marcus physical/game fixture tests | **NOT TESTED** (not inferred from compile) |

**Full native binary audit generated by CI:** `native-libraries.tsv` is in the evidence artifact. 88 `.so` entries: 22 each for `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. All 22 ARM64 entries have ELF PT_LOAD alignments >=16 KB. Gambatte, mGBA and LibretroDroid are >=16 KB aligned in all four ABIs. Four 0-byte entries are present outside ARM64: `armeabi-v7a/libcitra_libretro_android.so`, `x86/libcitra_libretro_android.so`, `x86_64/libcitra_libretro_android.so`, `x86_64/libppsspp_libretro_android.so`. melonDS armv7 and x86 binaries have 4 KB PT_LOAD alignment. The zero-byte entries and 4 KB alignments must stay visible as compatibility gaps; do not rewrite or replace cores without an approved subsequent package.

**ZIP and 16 KB gate:** All 88 native library entries are compressed; the invoked build-tools **34.0.0** `zipalign` rejected `-P 16` as an unsupported flag. This *check* is therefore **NOT VERIFIED**, not a demonstrated APK failure. No Android device with 16 KB page size has been tested. ARM64 ELF alignment passing alone does **not** clear overall 16 KB runtime compatibility. Source/core licences and distribution notices remain unreviewed for release, especially non-MVP bundled cores.

**APK verification:** independently downloaded the original artifact and confirmed its extracted SHA-256 matched the CI value. GitHub Actions artifact ZIP SHA-256 (`0cdb42a6038bb7d674bfced8bd37694e80b9b69c66574a3a60d1320a010c3d16`) is *not* the APK hash; do not confuse them.

**QA instruction:** Marcus must test the exact APK SHA-256 above and attach independent result. Do not mark cartridge SRAM, save-state, RTC, ROM import, touch/gamepad, TV or physical-device behaviour as passed without evidence. PR #5 is draft/unmerged.

## 3A. Evidence still to complete

Workflow: `.github/workflows/p0-a-debug-apk.yml`; build output: `lemuroid-app/build/outputs/apk/**`. It must publish only an internal GitHub Actions **debug** artifact and evidence, never a Release.

- [ ] Record **workflow run URL / ID**, result and exact failing log lines if failure.
- [ ] Record **PR head SHA** and recursively pinned submodule SHAs from that run.
- [ ] Confirm final **APK name, debug signature, package ID, build variant, size and SHA-256** from `build-evidence/`.
- [ ] Record Java, Gradle, Android SDK, build tools and NDK versions actually used; environment is not proven just by configuration declarations.
- [ ] Review `native-libraries.tsv`, `16kb-elf-summary.txt` and `zipalign-16kb.txt`. **16 KB compatibility remains unverified until all arm64 libraries and packaging pass and device evidence exists.**
- [ ] Record the **downloadable artifact URL** and have Marcus download that exact object, not an unrelated build.
- [ ] Attach full build failure logs if it fails, and propose only a blocker-specific remedy.

## 4. Source, licensing and native-code gate

Current foundation: Lemuroid-derived Android/Kotlin application (GPLv3, see `COPYING`); separate LibretroDroid (GPL-3.0) runtime; submodule LemuroidCores pinned above. Gambatte is identified for GB/GBC (GPLv2 in the Drive source register); mGBA for GBA (MPL-2.0). All core licences, transitive native binaries, source-binary correspondence, non-commercial restrictions and copyright notices **must be reviewed individually** with Daniel/legal before any public distribution. The Free flavour name is **not** proof that every included core is commercially redistributable.

The current `bundle` flavour includes the bundled-cores module, not only GB/GBC/GBA. The CI evidence inventories **every** APK `.so`, with ABI, SHA-256 and ELF PT_LOAD alignment; it does not assume all architectures or licences are acceptable. The tracked debug key is suitable only for internal reproducible debug builds; inherited release signing is forbidden.

No ROM, proprietary BIOS, manufacturer artwork, game data or ROM-download service may be introduced here. UI, package identity, scanner, LibretroDroid/Gambatte/mGBA, controller and save machinery are untouched by this P0 instrumentation-only change.

## 5. Device and homebrew QA — unverified

Marcus owns independent testing of the **same SHA and APK SHA-256**. Elliot may also run a separate author test; neither is automatically passed by successful CI.

| Test / fixture | GB (Gambatte) | GBC (Gambatte) | GBA (mGBA) |
| --- | --- | --- | --- |
| Fixture name, author, source URL and licence | NOT TESTED | NOT TESTED | NOT TESTED |
| Import single file via Android picker | NOT TESTED | NOT TESTED | NOT TESTED |
| Import directory; persist URI permissions | NOT TESTED | NOT TESTED | NOT TESTED |
| Start game / audio / video | NOT TESTED | NOT TESTED | NOT TESTED |
| Touch D-pad, diagonals, A/B, Start/Select | NOT TESTED | NOT TESTED | NOT TESTED |
| External USB/Bluetooth controller | NOT TESTED | NOT TESTED | NOT TESTED |
| Pause/resume and orientation | NOT TESTED | NOT TESTED | NOT TESTED |
| In-game cartridge SRAM -> force stop -> relaunch | NOT TESTED | NOT TESTED | NOT TESTED |
| Manual state save -> restart -> restore | NOT TESTED | NOT TESTED | NOT TESTED |
| RTC/homebrew time progression where applicable | NOT TESTED | NOT TESTED | NOT TESTED |
| Invalid/corrupt file and revoked permission | NOT TESTED | NOT TESTED | NOT TESTED |
| Android TV controller/focus/launch smoke | NOT TESTED | NOT TESTED | NOT TESTED |

On physical Android device, first capture: model/API/ABI/16 KB page-size if applicable, app package, installed APK checksum, current commit, input hardware, fixture URL/licence and expected cartridge save behaviour. Then:

```bash
adb install -r cartdeck-p0-a-freeBundle-debug.apk
adb shell monkey -p com.swordfish.lemuroid.debug -c android.intent.category.LAUNCHER 1
adb shell am force-stop com.swordfish.lemuroid.debug
# Reopen and observe whether cartridge SRAM and manual state actually persisted.
adb logcat -d > p0-a-device-logcat.txt
```

For every issue use CD-QA ID, reproduce steps, expected/observed, screenshots/logcat with personal data removed, severity, assignee and retest. Never use commercial ROMs as fixtures.

## 6. Stop / acceptance

**P0-A status: NOT ACCEPTED until:** successful clean-clone debug CI artifact, SHA-256/provenance, confirmed install/launch, independent GB/GBC/GBA lawful gameplay/save matrix, architecture review, 16 KB assessment, and Cassia acceptance are attached to the PR.

**Release: HELD.** This PR does not modify the tag-triggered inherited publication workflow. Do not push tags, use release keys, or merge without required reviews.
