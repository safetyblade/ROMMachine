# CARTDECK — P0-A Android baseline

**Status:** BUILD / DEVICE VERIFICATION PENDING. This document deliberately records unknowns and failures rather than implying acceptance.
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

This is an **environment / network blocker**, not evidence that the Android source fails compilation. **There is no verified compiled APK at document creation.** The first real build attempt is the dedicated debug-only GitHub Actions workflow added by this PR. Read that run's logs before making any dependency changes.

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

## 3. CI evidence to complete (not yet verified)

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
