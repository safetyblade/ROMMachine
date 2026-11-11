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
## Integrated build recipe and frontend contract (10 November 2026)

Use `ANDROID_NDK_HOME=/path/to/ndk/29.0.14206865 bash scripts/ci/build_ps2_upstream_core.sh /private/output`.
The workflow now calls this identical recipe. It emits `libplay_modern_libretro_android.so`,
source/patch/submodule provenance, SHA-256, exported symbols, dependency and 16 KiB ELF evidence.
The output is private and is not uploaded by the workflow.

The pinned upstream had two integration defects addressed by
`scripts/ci/patches/play-android-private-storage-gles.patch`:

- Android initialisation used `EXTERNAL_STORAGE` instead of the frontend save directory.
  The patch requires `RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY` before constructing Play's
  config/VM. Play's folder memory cards live beneath `<save directory>/Play Data Files/vfs/mc0`
  and `mc1`; Play exposes no `RETRO_MEMORY_SAVE_RAM`. Keep this directory persistent and
  separate from other systems and engines. Do not rely on generic SRAM backups or states
  for memory-card protection.
- `RETRO_HW_CONTEXT_OPENGLES3` denotes ES 3.0 and ignores the requested minor version.
  The patch requests `RETRO_HW_CONTEXT_OPENGLES_VERSION` 3.2 and propagates a rejected
  graphics callback as a load failure. The frontend must validate the actual EGL context;
  accepting the callback alone does not establish GLES 3.2 support.

The physical-device runner now launches the actual PS2 Activity, detects `PS201_BLOCKED`,
and quotes private staging inside `run-as` rather than redirecting as Android's shell UID.
Its mocked-adb regression tests exercise the Activity/staging contract and failure markers;
they are not device or gameplay evidence. The debug input route includes both sticks and
L2/R2 joystick triggers, retaining existing button and D-pad routing.

ARMSX2 reassessment: upstream HEAD observed as
`0aecc97c89da773fb66076319b0173ef6a682c5f`. At that exact revision,
`pcsx2-libretro/Main.cpp` has `ENABLE_OPENGL` and Android GLES 3.2/3.1/3.0 negotiation;
the old Vulkan-only assessment no longer describes current source. It also requires a
shared graphics context for its worker GS thread. No Android ARMSX2 binary or compatibility
claim follows from this source inspection. Its user-owned BIOS/resources/cards remain a
separate persistence contract from Play!.

Local native execution result: **NATIVE BUILD PASS** for the pinned, patched Play!
source using NDK 29.0.14206865, Android API 28, Release ARM64. All 443 build steps
completed; a repeat invocation completed with `ninja: no work to do`. The core is
5,430,464 bytes after `llvm-strip --strip-unneeded`, SHA-256
`bf93d8581054ef4541931780c2b660159ef22de2bb82f71f5977730a178a9b9d`.
All three ELF LOAD segments have `0x4000` alignment and all eight required checked
Libretro entry points are exported. Three mocked-adb regression tests pass.
**ANDROID RUNTIME PASS and GAMEPLAY DEVICE-TESTED remain unproven.**

The original 48,775,112-byte unstripped library remains in the native build tree for debugging. ELF alignment and required dynamic exports are verified on the stripped packaging copy before hashing.
