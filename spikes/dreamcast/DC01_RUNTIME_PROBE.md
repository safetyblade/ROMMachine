# DC-01 — LibretroDroid device runtime probe (not a Dreamcast release)

Source base: PR #87; target: Issue #82. This branch is intentionally stacked on the
Dreamcast research PR, not on hardware P1-C2 or GC/PS2. It adds a DEBUG-ONLY,
unlisted activity to the existing CARTDECK Android application and a local
device script. No Flycast binary, Dreamcast BIOS, disc or game is committed,
downloaded, embedded or uploaded.

## What this actually exercises

The debug activity constructs **the same `GLRetroViewData` and `GLRetroView`
from `com.swordfish.libretrodroid` used by CARTDECK's normal gameplay view.
It feeds the locally staged Flycast Libretro core, lawful content, persistent
system/save folders, and physical gamepad key/analogue events to the real
library. The activity observes LibretroDroid's `FrameRendered` events and
records diagnostics under tag `CARTDECK_DC01`.

It DOES NOT enable Dreamcast in Settings, `CoreID`, the regular library,
automated core updater or product gameplay launcher. It does not touch the
existing user's ROMs, covers, game IDs, saves, app ID or release signing.
No custom EGL adapter is claimed or added before a trace identifies the
specific missing hardware-context operation.

## Run only from a local developer workstation

Prerequisites: physical arm64-v8a Android phone, developer-owned reproducibly
compiled Flycast Libretro ARM64 .so from pinned SHA
`1b7c7d63b73ea6987fb23f9334a7d597401a50f8` / NDK r29, legal
Dreamcast homebrew program, ADB, and *debug* CARTDECK APK whose package ID
matches the fifth argument. A GDI must have all companion tracks together.

```bash
bash scripts/ci/dc01_device_probe.sh \
  /path/to/cartdeck-debug.apk \
  /path/to/flycast_libretro.so \
  /path/to/homebrew-disc-folder \
  homebrew.gdi \
  <actual-debug-package-id>
```

The core binary is never shipped by this branch. The script stages it under
app-private `files/dreamcast-lab`; it does not clear this directory between
launches. The real product continues using its own directories.

Evidence: `dc01-device-evidence/{input-sha256.txt,device-abi.txt,page-size.txt,
install.txt,start.txt,logcat.txt,dc01-filtered.log,screenshot.png,result.txt}`.
Do not publish screenshots/logcat containing personal data or proprietary
fixtures. Each clean smoke result requires **actual visual inspection** for
moving frames plus audible sound and controller response; `FRAME_RENDERED`
alone proves only an event, not correct pixels or audio.

## Current evidence / hard gates

- Flycast pinned ARM64 binary and static libretro export / ELF alignment:
  **PASS on PR #87, research run 42602584330**.
- Debug activity runtime compilation: **PENDING CI**.
- Flycast native dependency loader and GL/EGL context: **NOT TESTED**.
- On-device homebrew video, audio, input, clean re-launch: **NOT TESTED**.
- VMU save round trip, multi-disc and 16-KiB physical page runtime:
  **NOT TESTED**.
- GPL combined distribution: **NOT CLEARED**.

If native context negotiation fails, collect the precise logcat request and
the `RETRO_ENVIRONMENT_SET_HW_RENDER` context type/version. Change only the
smallest Flycast-specific render bridge; do not replace the shared renderer.
No retail APK with Flycast core and no master merge before Daniel, Marcus and
Cassia review. Do not use normal PS1/GBA/DS saves as test fixtures.
