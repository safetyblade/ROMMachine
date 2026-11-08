# CARTDECK — phone APK channel and upgrade/save-test protocol

**Status:** P0 internal-only; not a public release. **Last edited:** 2026-11-08.
**Drive:** [prestwich.dev / CARTDECK / APK Test Builds](https://drive.google.com/drive/folders/16Rqr4oMRURWS3jyn79I3HKv2rgc1Pco4)
**Code:** [P0-A draft PR](https://github.com/safetyblade/ROMMachine/pull/5), [Issue #3](https://github.com/safetyblade/ROMMachine/issues/3).

## Android update identity — locked for the P0 DEBUG test chain

| Android field | Required value |
| --- | --- |
| App package ID | `com.swordfish.lemuroid.debug` |
| Signing certificate SHA-256 | `CE:3E:BF:1D:97:2C:09:58:34:70:E1:84:D9:B8:6E:DD:20:EB:46:79:FE:DC:63:2D:31:AC:9F:58:E8:74:1F:C8` |
| Certificate subject | `CN=Odyssey Debug` (inherited Lemuroid debug keystore, **not** prestwich.dev-owned; internal-only) |
| Original full baseline | VersionCode 252; [CI run 42455383634](https://github.com/safetyblade/ROMMachine/actions/runs/42455383634) |
| Phone build versionCode | `1000 + github.run_number`, monotonically increasing on new workflow runs; verify from each APK's manifest |
| Phone ABI | `arm64-v8a` only, using `-Pcartdeck.p0.phoneArm64Only=true`; no engine changes |
| Phone APK location | `CARTDECK_LATEST_ARM64_DEBUG.apk` in Drive folder, with exact build SHA/size/version recorded in a companion manifest |
| Public/release signing | **NOT CONFIGURED**. No inherited debug key is authorised for production or wide distribution |

**Important:** The **certificate SHA-256 fingerprint** identifies the signing certificate, not a replaceable signing setting. The **APK SHA-256 checksum** is a different value that changes every time the file changes. A valid Android in-place update requires the same application ID and compatible signing identity, and an acceptable (normally higher) versionCode. Never uninstall between save-retention tests: uninstall removes app data. Stable packaging preserves Android app data, but does not prove that the emulator actually flushes or restores SRAM correctly.

The current signing key is tracked in the inherited source. This is a P0-only testing continuity arrangement, **not a claim of exclusive prestwich.dev signing-key ownership or secure production signing**. A dedicated prestwich.dev key must be generated and stored privately in CI secret storage before public release; changing from this inherited test certificate cannot be treated as a seamless upgrade. Plan and test an explicit one-time migration (including save export/import) when product applicationId and signing identity change in a separately approved P1 milestone. Do not create, embed, upload, or commit a new release/private signing key in this work package.

## Phone install and upgrade test

1. Use a modern Android ARM64 phone. If the phone is 32-bit-only or x86, this test APK will not install; use the full baseline instead.
2. From Drive, download the **raw APK**, not a GitHub artifact ZIP. Verify its SHA-256 against the manifest. Allow one-time APK installation from the file manager if needed; use only this controlled test build.
3. Install the first phone APK. Note the versionCode, build SHA and signer fingerprint. Use author-licensed GB/GBC/GBA homebrew ROMs, with source URLs/licences recorded; do not import commercial game copies.
4. Using a fixture that supports persistent cartridge saves, play, perform an **in-game battery/SRAM save**, exit, and reopen. Record the exact checkpoint and whether it persists. Separately make a **manual emulator save state**, then restore it.
5. When the next phone APK is available, confirm its versionCode is **higher**, but package ID and signer SHA-256 are **identical**. Tap the downloaded APK and choose **Update**; do **not** uninstall or clear app data.
6. Reopen the exact same game from the existing library, validate previously saved in-game SRAM, last-played state, save-state slot and picker permission persistence. Record each result independently; do not equate a successful Android update with a passed save-regression test.
7. Repeat with GBC and GBA; test controller mapping/pause/resume and offline reopen. Document device model, Android API, date, APK hash, ROM fixture/licence, expected/actual behaviour, screenshot/logcat as safe and defect ID.

ADB test commands, if available:
```bash
adb install -r CARTDECK_LATEST_ARM64_DEBUG.apk
adb shell dumpsys package com.swordfish.lemuroid.debug | grep -E 'versionCode|versionName'
# Test licensed in-game cartridge save and manually restore state.
adb shell am force-stop com.swordfish.lemuroid.debug
# Reopen the same game without clearing app data.
```

## CI / Drive delivery

Workflow `.github/workflows/p0-a-debug-apk.yml` builds and checks the signed ARM64-only APK, pins certificate fingerprint and publishes a downloadable GitHub Actions artifact plus hash, source and version evidence. A P0 Drive upload is completed through a controlled Google Drive upload, with current `LATEST` file and versioned metadata, preserving old build evidence. **No unattended GitHub→Google Drive upload is configured**: this requires a scoped Drive service-account/CI credential managed privately and approved before automating. Do not claim every new GitHub run automatically appears in Drive until that integration is actually deployed and verified.

## Acceptance / stop conditions

- P0-A PR remains draft; no merge, release or tags.
- Marcus must independently install and verify save persistence on physical hardware. Build success is not a gameplay pass.
- Daniel reviews signing continuity, source licence exposure and ABI/runtime compatibility.
- Cassia reviews player-experience and save-preservation evidence before acceptance.
