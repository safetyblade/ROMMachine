# CARTDECK — Independent Android preview APK / P1 bridge

Status: **DEV PREVIEW ONLY**. Not an approved public release.
Product owner: Jonathan Prestwich · Project Director: Cassia Rowan.
Development base: latest master; this change introduces opt-in separate Android ID while retaining existing emulator runtime.

## Why a separate build?

The existing phone P0 test line uses Android package `com.swordfish.lemuroid.debug` (inherited upstream debug key) and existing save/ROM library. Changing that app ID as an update would *not* migrate its private data; on Android it is a different application.

This build creates a separate CARTDECK application:
- **New installed package:** `dev.prestwich.cartdeck.debug`.
- **Visible label and icon:** CARTDECK Debug, approved C-slot launcher and CARTDECK TV banner.
- **Emulation core and saves:** unchanged; new app starts with an empty, separate data directory.
- **Existing test app:** remains installed with data/saves intact. Never uninstall until backups/export/import are independently tested.
- **Signing:** inherited upstream debug test key (fingerprint pinned by CI) **only for this private preview**; it is NOT an independent prestwich.dev production signing key. A permanent keystore must be generated securely, added to CI secrets and tested with a new signing/migration gate BEFORE external release. Do not ship this test key or claim a production distribution.

## Building

From the checked-out branch with recursive submodules:

```bash
./gradlew :lemuroid-app:assembleFreeBundleDebug \
  -Pcartdeck.dev.standaloneId=true \
  -Pcartdeck.p0.phoneArm64Only=true \
  -Pcartdeck.p0.testVersionCode=2001
```

The workflow `.github/workflows/cartdeck-standalone-debug.yml` adds an incrementing version and uploads an artifact called `CARTDECK_STANDALONE_ARM64_DEBUG.apk` inside GitHub's artifact ZIP. Read the CI build evidence before installing.

## User install / save safety

1. Keep the prior Lemuroid-derived test app installed. It contains your original game library and saves.
2. Install the new CARTDECK standalone preview APK as a separate Android app. Android should show two entries until migration is done.
3. Select your lawful ROM directory again; import permissions are app-specific.
4. Verify that CARTDECK launches and gamepad/touch controls and save states work.
5. To move progress, use the old app's supported export/backup **if verified**. Import into CARTDECK only after Marcus validates correct game-to-save mapping, cartridge RAM/state compatibility and rollback. Copying app-private files through unverified means is forbidden.
6. When a stable prestwich.dev release signing certificate exists, do not assume the preview upgrades in place; test signing/package transition and backup restoration first.

## Acceptance for this preview

- CI produces a real arm64 APK with package `dev.prestwich.cartdeck.debug`, CARTDECK label/icon, known SHA-256 checksum and signing fingerprint.
- The APK installs next to the existing app without clearing or altering previous app data.
- ROM import and GB/GBC/GBA gameplay (authorised homebrew), controller/touch input, SRAM and state persistence independently checked.
- The installed app shows approved launcher, TV banner and UI copy; Sienna may then proceed with presentation-only console shell work.
- No distribution without core licensing and signature security review.

Current unknowns: CI build completion, real-device gameplay and cross-app save migration. Do not mark passed from source code alone.

Related: `docs/CARTDECK_BRAND_MIGRATION.md` and `docs/PHONE_TESTING.md`.
