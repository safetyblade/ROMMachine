# CARTDECK P3-PS2 — PlayStation 2 ARM64 integration readiness

**Owner direction:** PlayStation 2 titles should join the SAME CARTDECK library and Arc-Rail cartridge presentation, badge **PS2**, existing custom/indexed/fallback cover, selection and explicit Open/Resume, and shared game-first console shell. No second PS2 app, manual import list or different branding.

This is **research and executable safety preparation only**, not user-visible system registration, not a packaged PS2 core and not a functioning PS2 game. Parent: https://github.com/safetyblade/ROMMachine/issues/81 ; platform: https://github.com/safetyblade/ROMMachine/issues/84 .

## Two parallel engine routes — do not mix their requirements

### Route A — Play! (first Android Libretro bridge experiment)

- Maintained upstream: https://github.com/jpd002/Play- ; pinned source `1f27232772e484bfb79364b751b84f41f4c87481`.
- Official upstream `build_retro/android_build.sh` explicitly supports `arm64-v8a`, `-DBUILD_LIBRETRO_CORE=yes`, builds `play_libretro` and emits Android .so. Core described as MIT-licensed in Libretro documentation; verify third-party dependency notices.
- OpenGL ES 3.2 requirement. BIOS high-level emulation means a separate Sony PS2 BIOS dump generally isn't required for this candidate, but game compatibility/performance must be demonstrated rather than assumed.
- Initial use: minimal legal-fixture video/audio/input/core load through isolated LibretroDroid, with an explicit test of renderer negotiation and save/memory-card persistence. No guarantee that Play! will run the owner's desired PS2 titles.

### Route B — ARMSX2 (native ARM64 performance and primary compatibility candidate)

- Active upstream: https://github.com/ARMSX2/ARMSX2 ; pinned source `fc4ab0b8b4f386617773cf1916e9bc6911dba59b`; GPLv3 metadata.
- Standalone Android ARM64 builds are published; **standalone Android is not equivalent to an Android Libretro .so**.
- Libretro `armsx2_libretro.info` declares a Vulkan 1.1 hardware-context interface, full-path content, `libretro_saves=false`, PS2 BIOS under `system/pcsx2/bios`, and matching resources under `system/pcsx2/resources`. It is marked experimental.
- The published `.github/workflows/linux_build_libretro.yml` builds a **Linux** ARM64 Libretro core, and explicitly uses a 4 KiB page override. This is *not* evidence that a native Android Libretro shared library is buildable or compatible without porting.
- Some newer Libretro documentation describes GLES options, while the pinned source info still states Vulkan-only. Elliot must resolve using the exact compiled source, not an assumed renderer.
- Initial use: feasibility of an actual Android ABI and LibretroDroid Vulkan interface; do not distribute source or binary pending technical and licence review. **PS2 BIOS is user-provided from lawful dumps**, never bundled; protect the memory-card directory.

## Automatic scanner: no false positives

PS2 discs are frequently `.iso`, `.chd`, `.cue`, `.bin`, `.cso` or `.m3u` — **all overlap other platforms**. PS2 `.elf` alone is generic and not sufficient for system detection. Never add these as globally unique Dreamcast, GameCube or PS2 extensions in CARTDECK's `GameSystem.findByUniqueFileExtension` map.

Proper PS2 identification requires:
1. Verified disc container support (ISO9660/UDF, CHD tracks as applicable) and an authentic PS2 `SYSTEM.CNF` boot marker/serial resolved through filesystem metadata, not a filename search against arbitrary bytes.
2. A content-addressed scanner cache for large discs that avoids full CRC re-reading on every rescan and avoids deleting existing games when a SAF provider/permission fails.
3. When ambiguous, explicit per-item user system confirmation that persists, rather than a silently guessed platform.
4. Multi-disc and disc/track companion relationships, import permissions and save/memory-card paths that survive a same-package upgrade and backup.

The companion `media_probe.py` currently refuses PS2 identification on extension alone, deliberately.

## Runtime and acceptance requirements

- Actual native Android ARM64 **LibretroDroid frontend proof** before registering `SystemID.PS2` / `CoreID`: EGL/OpenGL for Play!, Vulkan for ARMSX2 if supported; libretro ABI, frame/audio, pads, analog, L1/L2/R1/R2, Start/Select, pause and clean close.
- Protect emulated memory-card writes, persistent system files and manual save-state semantics. ARMSX2's `libretro_saves=false` means do **not** assume the frontend's generic Libretro SRAM backup captures PS2 saves.
- Correct original 4:3 and variable/widescreen viewport without cropping or hiding CARTDECK controls, adapting the same hardware shell. Verify all on a real phone and document actual FPS/temperature, not estimated compatibility.
- No public support claim, illegal BIOS/game download, app signing/package migration, or changes to the existing 25 supported systems.

**Status:** Android PS2 emulator feasibility substantiated; Play! is preferred for first LibretroDroid interface smoke, ARMSX2 remains the higher-capability path requiring additional Android-Libretro renderer evidence. Neither has been verified to run inside CARTDECK.
