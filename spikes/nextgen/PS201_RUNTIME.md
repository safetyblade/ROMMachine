# PS2-01 — executable Android host gate and two-engine decision

Issue #84. **This is an Android test harness, not a PS2 system registration or a playable claim.**

## Executed source path

- Play! pinned to `1f27232772e484bfb79364b751b84f41f4c87481`. `.github/workflows/cartdeck-ps2-01-android.yml` now attempts a **real Android ARM64 `play_libretro` build**, dynamic symbol test and 16 KiB ELF alignment. A failing compile MUST be reported, not re-labelled success.
- Private debug `PlayStation2RuntimeProbeActivity` uses current CARTDECK LibretroDroid `GLRetroView` and persistent app-private `files/ps2-lab/saves` and `system` directories, loading only a developer-staged binary and lawful disc. Run `bash scripts/ci/ps201_device_probe.sh DEBUG_APK PLAY_ARM64_SO LAWFUL_DISC_DIR DISC_NAME DEBUG_PACKAGE` on a physical device. Media remains developer-selected; THIS IS NOT AUTOMATIC PLATFORM DETECTION.

## ARMSX2 direct competitor

- Previously compared `fc4ab0b8b4f386617773cf1916e9bc6911dba59b`; Linux arm64 Libretro and standalone Android are evidenced in upstream, **Android ARM64 Libretro core not proven**.
- The November 2026 Libretro documentation lists both Vulkan and GLES 3.2 routes for ARMSX2; this differs from the pinned fork's original Vulkan-only core metadata. Obtain an exact later source SHA before enabling an alternate Android host.
- Its separate `pcsx2/bios`, `pcsx2/resources` and memory-card persistence are not compatible with generic Play! save-state assumptions. Never silently switch an existing PS2 library identity between engines or overwrite memory cards.

## Decision rule

**Provisional experimental host: Play!**, only because the pinned Android ARM64 Libretro build path is explicit and is being executed. **Final engine decision deferred until measured, actual device gameplay.** ARMSX2 remains a leading performance/compatibility competitor; if Play! fails lawful gameplay or emulation speed, implement an in-process JNI/Vulkan or GLES3.2 host rather than launch its external application.

Not tested yet: real LibretroDroid GLES3.2 context, game boot/match, audio, both sticks and triggers, genuine memory-card roundtrip, 16 KiB device load, in-place update, sustained thermal/FPS. No public system entry, BIOS/core/ROM packaging, changes to existing user games or saves.