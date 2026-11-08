# One CARTDECK APK workflow and failure review

Inspected 88 failed runs across all four workflow names with failures at the start of this cleanup. Logs were retrieved for all 88, including job-log fallback for older runs. Counts below classify the primary observed failure; some runs also reported secondary style or memory errors. This is a snapshot, not a claim that unrelated branches stopped running during the review.

| Observed primary failure | Runs | Resolution |
|---|---:|---|
| Kotlin style violations | 37 | Format the offending Kotlin/Kotlin DSL files; keep the full root ktlintCheck gate. |
| Gradle distribution download | 30 | Keep Gradle 8.10.2; pin its official SHA-256, verify the archive, use curl with bounded retries and cache only the verified archive. |
| Maven dependency HTTP 504 | 5 | Keep dependency versions; allow longer transport timeouts and one complete retry only for recognised transport errors. Configuration cache stays disabled. |
| Generated-resource task dependency | 3 | Use a typed resource-output property and Android Components addGeneratedSourceDirectory so every consuming task depends on generation. |
| Emulator Android service broken pipe (ADB 224) | 2 | Emulator input/overlay/activity services failed before app launch. Device boot is no longer a prerequisite for producing the ARM64 APK; retain fail-closed harness unit tests and separate real-device acceptance. |
| Emulator boot timeout | 2 | Separate device acceptance from building the single ARM64 APK. No emulator/device success is claimed. |
| Gradle/Kotlin heap exhaustion | 2 | Build only the standalone ARM64 debug variant with two workers and the in-process Kotlin compiler; give the unit-test JVM 1536 MiB. |
| Bash options passed to sh | 2 | Use Bash explicitly throughout the single workflow; retain executable shell harness regression tests. |
| SDK manager missing from PATH | 2 | Use the hosted SDK manager absolute path and export SDK tool paths. |
| Obsolete SDK tools package | 2 | Install only platform-tools, platforms;android-35 and build-tools;34.0.0; remove the old setup action. |
| Undeclared asset colour token | 1 | Already corrected upstream in the token generator. Preserve the source/native-asset tests in the single workflow. |

## Current pipeline

The only workflow file is `.github/workflows/cartdeck-standalone-debug.yml`, displayed as **CARTDECK APK**. It runs on PRs to master, pushes to master and manual dispatch. The file path is retained so the existing workflow run number, and therefore the `2000 + GITHUB_RUN_NUMBER` development version progression, continue.

One job runs brand/design/native asset tests, AAPT2 resource compilation, shell harness and retry-policy tests, full root ktlintCheck, the existing 19 Compose/Robolectric tests, and exactly `:lemuroid-app:assembleFreeBundleDebug` with standalone ID and ARM64 enabled. No broad assembleDebug/all-flavour build or production release/signing path remains.

One Actions artifact, **CARTDECK_STANDALONE_ARM64_DEBUG**, contains one `CARTDECK_STANDALONE_ARM64_DEBUG.apk` plus an `evidence/` directory. Staging the APK requires successful style/tests/build, exactly one expected variant output, standalone package/version, the established debug signer, ARM64-only native inventory and the existing size bound. Failed runs retain diagnostics in the same artifact without claiming a usable APK.

Historical Actions runs remain available as audit evidence. Obsolete definitions are removed; their old run history is not deleted. No tag-triggered public release workflow remains.

## Local validation

The consolidated command passed full root ktlintCheck, all 19 Compose/Robolectric tests (0 failures/skips), and the standalone ARM64 debug build. The brand/design/native checks and AAPT2 resource compilation passed. The four retry-policy tests passed; the APK verifier rejected an incorrect version and a second APK, then accepted and staged the valid signed standalone APK. A fresh Gradle archive download matched the pinned official SHA-256; a repeat bootstrap verified and reused that archive.

## Reproduce

Install Java 17 and SDK 35/build-tools 34.0.0; initialise recursive submodules. Run:

```sh
bash scripts/ci/bootstrap_gradle.sh
# Bootstrap prints the version and exports GRADLE_BIN through GITHUB_ENV in Actions.
export GRADLE_BIN="${RUNNER_TEMP:-/tmp}/cartdeck-gradle/gradle-8.10.2/bin/gradle"
bash scripts/ci/build_standalone_apk.sh 2012
CARTDECK_VERSION_CODE=2012 bash scripts/ci/verify_standalone_apk.sh
```

Use a version strictly newer than the installed standalone app. Verification retains package `dev.prestwich.cartdeck.debug` and certificate SHA-256 `ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8`. No uninstall, save migration, core changes or package/signing migration is involved. Formatting-only changes remove the existing lint failures without changing SQL/save/cover logic.

APK compilation, framework/unit tests and resource validation do not establish real-device install-over, gameplay, SRAM/manual states, controller input or 16KB runtime acceptance. Those gates remain independent.

## Reviewed failed runs

| Run | Workflow | Observed primary failure |
|---|---|---|
| [42470076989](https://github.com/safetyblade/ROMMachine/actions/runs/42470076989) | Checkstyle | Gradle distribution download |
| [42469922169](https://github.com/safetyblade/ROMMachine/actions/runs/42469922169) | Checkstyle | Maven dependency HTTP 504 |
| [42469619743](https://github.com/safetyblade/ROMMachine/actions/runs/42469619743) | P0-A Android debug APK | Generated-resource task dependency |
| [42469619757](https://github.com/safetyblade/ROMMachine/actions/runs/42469619757) | CARTDECK standalone preview APK | Generated-resource task dependency |
| [42469619758](https://github.com/safetyblade/ROMMachine/actions/runs/42469619758) | Checkstyle | Maven dependency HTTP 504 |
| [42469488768](https://github.com/safetyblade/ROMMachine/actions/runs/42469488768) | Checkstyle | Gradle distribution download |
| [42469488686](https://github.com/safetyblade/ROMMachine/actions/runs/42469488686) | P0-A Android debug APK | Gradle distribution download |
| [42469298558](https://github.com/safetyblade/ROMMachine/actions/runs/42469298558) | Checkstyle | Maven dependency HTTP 504 |
| [42469298547](https://github.com/safetyblade/ROMMachine/actions/runs/42469298547) | CARTDECK standalone preview APK | Maven dependency HTTP 504 |
| [42469298548](https://github.com/safetyblade/ROMMachine/actions/runs/42469298548) | P0-A Android debug APK | Generated-resource task dependency |
| [42469164696](https://github.com/safetyblade/ROMMachine/actions/runs/42469164696) | Checkstyle | Gradle distribution download |
| [42469164670](https://github.com/safetyblade/ROMMachine/actions/runs/42469164670) | CARTDECK standalone preview APK | Gradle distribution download |
| [42469164657](https://github.com/safetyblade/ROMMachine/actions/runs/42469164657) | P0-A Android debug APK | Maven dependency HTTP 504 |
| [42469147950](https://github.com/safetyblade/ROMMachine/actions/runs/42469147950) | Checkstyle | Gradle distribution download |
| [42468989406](https://github.com/safetyblade/ROMMachine/actions/runs/42468989406) | Checkstyle | Gradle distribution download |
| [42468989383](https://github.com/safetyblade/ROMMachine/actions/runs/42468989383) | CARTDECK standalone preview APK | Gradle distribution download |
| [42468786487](https://github.com/safetyblade/ROMMachine/actions/runs/42468786487) | Checkstyle | Gradle distribution download |
| [42468715232](https://github.com/safetyblade/ROMMachine/actions/runs/42468715232) | Checkstyle | Gradle distribution download |
| [42468620554](https://github.com/safetyblade/ROMMachine/actions/runs/42468620554) | Checkstyle | Gradle distribution download |
| [42468620447](https://github.com/safetyblade/ROMMachine/actions/runs/42468620447) | P0-A Android debug APK | Gradle distribution download |
| [42468459962](https://github.com/safetyblade/ROMMachine/actions/runs/42468459962) | P0-A Android debug APK | Gradle distribution download |
| [42468459948](https://github.com/safetyblade/ROMMachine/actions/runs/42468459948) | Checkstyle | Gradle distribution download |
| [42468298061](https://github.com/safetyblade/ROMMachine/actions/runs/42468298061) | Checkstyle | Gradle distribution download |
| [42468275964](https://github.com/safetyblade/ROMMachine/actions/runs/42468275964) | Checkstyle | Gradle distribution download |
| [42468275929](https://github.com/safetyblade/ROMMachine/actions/runs/42468275929) | P0-A Android debug APK | Gradle distribution download |
| [42468275915](https://github.com/safetyblade/ROMMachine/actions/runs/42468275915) | CARTDECK standalone preview APK | Gradle distribution download |
| [42467303887](https://github.com/safetyblade/ROMMachine/actions/runs/42467303887) | Checkstyle | Gradle distribution download |
| [42466987064](https://github.com/safetyblade/ROMMachine/actions/runs/42466987064) | Checkstyle | Gradle distribution download |
| [42466705098](https://github.com/safetyblade/ROMMachine/actions/runs/42466705098) | Checkstyle | Gradle distribution download |
| [42466705046](https://github.com/safetyblade/ROMMachine/actions/runs/42466705046) | P0-A Android debug APK | Gradle distribution download |
| [42466183063](https://github.com/safetyblade/ROMMachine/actions/runs/42466183063) | Checkstyle | Gradle distribution download |
| [42466183069](https://github.com/safetyblade/ROMMachine/actions/runs/42466183069) | P0-A Android debug APK | Gradle distribution download |
| [42466179914](https://github.com/safetyblade/ROMMachine/actions/runs/42466179914) | P0-A Android debug APK | Gradle distribution download |
| [42465973478](https://github.com/safetyblade/ROMMachine/actions/runs/42465973478) | Checkstyle | Gradle distribution download |
| [42465761653](https://github.com/safetyblade/ROMMachine/actions/runs/42465761653) | P0-A Android debug APK | Gradle distribution download |
| [42463060564](https://github.com/safetyblade/ROMMachine/actions/runs/42463060564) | Checkstyle | Kotlin style violations |
| [42463050270](https://github.com/safetyblade/ROMMachine/actions/runs/42463050270) | Checkstyle | Kotlin style violations |
| [42463038131](https://github.com/safetyblade/ROMMachine/actions/runs/42463038131) | Checkstyle | Kotlin style violations |
| [42462861329](https://github.com/safetyblade/ROMMachine/actions/runs/42462861329) | Checkstyle | Kotlin style violations |
| [42462605341](https://github.com/safetyblade/ROMMachine/actions/runs/42462605341) | P0-A Android debug APK | Emulator Android service broken pipe (ADB 224) |
| [42462605328](https://github.com/safetyblade/ROMMachine/actions/runs/42462605328) | Checkstyle | Kotlin style violations |
| [42462543922](https://github.com/safetyblade/ROMMachine/actions/runs/42462543922) | P0-A Android debug APK | Emulator Android service broken pipe (ADB 224) |
| [42462543910](https://github.com/safetyblade/ROMMachine/actions/runs/42462543910) | Checkstyle | Kotlin style violations |
| [42462541130](https://github.com/safetyblade/ROMMachine/actions/runs/42462541130) | Checkstyle | Kotlin style violations |
| [42462257065](https://github.com/safetyblade/ROMMachine/actions/runs/42462257065) | Checkstyle | Kotlin style violations |
| [42462239311](https://github.com/safetyblade/ROMMachine/actions/runs/42462239311) | Checkstyle | Kotlin style violations |
| [42462215093](https://github.com/safetyblade/ROMMachine/actions/runs/42462215093) | Checkstyle | Kotlin style violations |
| [42462184930](https://github.com/safetyblade/ROMMachine/actions/runs/42462184930) | Checkstyle | Kotlin style violations |
| [42462146186](https://github.com/safetyblade/ROMMachine/actions/runs/42462146186) | Checkstyle | Kotlin style violations |
| [42461830372](https://github.com/safetyblade/ROMMachine/actions/runs/42461830372) | Checkstyle | Kotlin style violations |
| [42461732149](https://github.com/safetyblade/ROMMachine/actions/runs/42461732149) | Cartdeck visual asset catalog | Undeclared asset colour token |
| [42461586278](https://github.com/safetyblade/ROMMachine/actions/runs/42461586278) | Checkstyle | Kotlin style violations |
| [42461321525](https://github.com/safetyblade/ROMMachine/actions/runs/42461321525) | P0-A Android debug APK | Emulator boot timeout |
| [42461321484](https://github.com/safetyblade/ROMMachine/actions/runs/42461321484) | Checkstyle | Kotlin style violations |
| [42460917208](https://github.com/safetyblade/ROMMachine/actions/runs/42460917208) | Checkstyle | Kotlin style violations |
| [42460831643](https://github.com/safetyblade/ROMMachine/actions/runs/42460831643) | Checkstyle | Kotlin style violations |
| [42460783881](https://github.com/safetyblade/ROMMachine/actions/runs/42460783881) | Checkstyle | Kotlin style violations |
| [42460585764](https://github.com/safetyblade/ROMMachine/actions/runs/42460585764) | Checkstyle | Kotlin style violations |
| [42460573437](https://github.com/safetyblade/ROMMachine/actions/runs/42460573437) | Checkstyle | Kotlin style violations |
| [42460570030](https://github.com/safetyblade/ROMMachine/actions/runs/42460570030) | Checkstyle | Gradle distribution download |
| [42460565485](https://github.com/safetyblade/ROMMachine/actions/runs/42460565485) | Checkstyle | Kotlin style violations |
| [42460560204](https://github.com/safetyblade/ROMMachine/actions/runs/42460560204) | Checkstyle | Kotlin style violations |
| [42460551296](https://github.com/safetyblade/ROMMachine/actions/runs/42460551296) | Checkstyle | Kotlin style violations |
| [42460531075](https://github.com/safetyblade/ROMMachine/actions/runs/42460531075) | Checkstyle | Kotlin style violations |
| [42460480903](https://github.com/safetyblade/ROMMachine/actions/runs/42460480903) | Checkstyle | Kotlin style violations |
| [42460421473](https://github.com/safetyblade/ROMMachine/actions/runs/42460421473) | Checkstyle | Kotlin style violations |
| [42460371699](https://github.com/safetyblade/ROMMachine/actions/runs/42460371699) | Checkstyle | Kotlin style violations |
| [42459787200](https://github.com/safetyblade/ROMMachine/actions/runs/42459787200) | Checkstyle | Kotlin style violations |
| [42459639951](https://github.com/safetyblade/ROMMachine/actions/runs/42459639951) | Checkstyle | Gradle/Kotlin heap exhaustion |
| [42459342252](https://github.com/safetyblade/ROMMachine/actions/runs/42459342252) | Checkstyle | Kotlin style violations |
| [42458956124](https://github.com/safetyblade/ROMMachine/actions/runs/42458956124) | Checkstyle | Kotlin style violations |
| [42458941131](https://github.com/safetyblade/ROMMachine/actions/runs/42458941131) | Checkstyle | Gradle distribution download |
| [42458582157](https://github.com/safetyblade/ROMMachine/actions/runs/42458582157) | P0-A Android debug APK | Bash options passed to sh |
| [42458582132](https://github.com/safetyblade/ROMMachine/actions/runs/42458582132) | Checkstyle | Kotlin style violations |
| [42458320616](https://github.com/safetyblade/ROMMachine/actions/runs/42458320616) | Checkstyle | Kotlin style violations |
| [42458028774](https://github.com/safetyblade/ROMMachine/actions/runs/42458028774) | P0-A Android debug APK | Emulator boot timeout |
| [42456576863](https://github.com/safetyblade/ROMMachine/actions/runs/42456576863) | Checkstyle | Kotlin style violations |
| [42456185371](https://github.com/safetyblade/ROMMachine/actions/runs/42456185371) | Checkstyle | Kotlin style violations |
| [42456185352](https://github.com/safetyblade/ROMMachine/actions/runs/42456185352) | P0-A Android debug APK | Bash options passed to sh |
| [42455387544](https://github.com/safetyblade/ROMMachine/actions/runs/42455387544) | Checkstyle | Kotlin style violations |
| [42455371618](https://github.com/safetyblade/ROMMachine/actions/runs/42455371618) | Checkstyle | Kotlin style violations |
| [42455276172](https://github.com/safetyblade/ROMMachine/actions/runs/42455276172) | P0-A Android debug APK | SDK manager missing from PATH |
| [42455276184](https://github.com/safetyblade/ROMMachine/actions/runs/42455276184) | Checkstyle | Kotlin style violations |
| [42455273266](https://github.com/safetyblade/ROMMachine/actions/runs/42455273266) | P0-A Android debug APK | SDK manager missing from PATH |
| [42455263085](https://github.com/safetyblade/ROMMachine/actions/runs/42455263085) | Checkstyle | Gradle/Kotlin heap exhaustion |
| [42455178484](https://github.com/safetyblade/ROMMachine/actions/runs/42455178484) | P0-A Android debug APK | Obsolete SDK tools package |
| [42455178518](https://github.com/safetyblade/ROMMachine/actions/runs/42455178518) | Checkstyle | Gradle distribution download |
| [42455131571](https://github.com/safetyblade/ROMMachine/actions/runs/42455131571) | P0-A Android debug APK | Obsolete SDK tools package |
