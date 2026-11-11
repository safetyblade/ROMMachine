# Native emulator integration cycle

Initial base: `8bc160554e83c8596f993d1a28d159db75a3facf`. Final integration includes current master `dadeab3a7d19a58ba6cb52f39141874f709d5705` and Sienna's merged layout changes.
Parent: #60. Workstreams: #56/#61/#79, #93, #82, #83, #84.

The normal scanner, database IDs, Arc-Rail cartridges, game activity, lifecycle and controls are reused. New `dc`, `gc`, and `ps2` registrations are independently included using `-PcartdeckNativeSystems=dc,gc,ps2` in a private engineering APK. The default build continues to advertise the existing systems; native compile success does not promote a system to device-tested support. `ngc` remains Neo Geo Pocket Color. No external emulator Activity is used.

## Build correspondence

| System | Pinned source | Android native result |
| --- | --- | --- |
| PS1 | libretro/pcsx_rearmed `7b0e8f8fadfbce3d170d195b9d9eecaabfa08c8e` | ARM64/API24, ari64+NEON, 16 KB PASS |
| PSP | hrydgard/ppsspp `fa50bb1976065c4f8b1b47af227d367fe9771555` (v1.20.4) | ARM64/API24, 16 KB PASS; matching 189-file assets/compat.ini |
| Dreamcast | flyinghead/flycast `1b7c7d63b73ea6987fb23f9334a7d597401a50f8` | ARM64/API28, GLES3, Vulkan off, 16 KB PASS |
| GameCube | libretro/dolphin `32c61f9d0e933331bb7087ba57e96883241b4960` | ARM64/API28, GLES, 16 KB PASS; matching 2,639-file Sys archive |
| PS2 | jpd002/Play- `1f27232772e484bfb79364b751b84f41f4c87481` + tracked patch | ARM64/API28, GLES3.2, 16 KB PASS |

Use `scripts/ci/build_*_upstream_core.sh` for the individual engines. All outputs are private local build directories, with source/dependency hashes and license notices. PPSSPP links its pinned upstream-provided Android FFmpeg static archives; their hashes and LGPL notice are recorded, rather than claiming to rebuild FFmpeg. GameCube needs CMake >=3.25 (this cycle used 3.31.6). NDK29.0.14206865 was used for the measured builds.

Build the Android host using `scripts/ci/build_libretrodroid_host.sh`. Its base is LibretroDroid0.14.0 commit `8835c3098514390a271e36983957f7bb5f40abf1`, plus the tracked GLES/trigger/header patch. It publishes only to an isolated local Maven directory as `0.14.0-cartdeck1`. It rejects incompatible hardware-context requests, lets Dolphin negotiate down its requested GLES version, and adds proportional L/R trigger motion source4 with full-press digital clicks. Existing source IDs0–3 and digital controls remain intact.

To assemble one engineering APK after independently building the chosen engines:

```bash
export CARTDECK_HOST_MAVEN_REPO="$PWD/work/libretrodroid-host/maven"
export CARTDECK_TEST_VERSION_CODE=2230
export CARTDECK_OUTPUT="$PWD/work/native-apk"
bash scripts/ci/build_native_engine_apk.sh \
  --ps1 work/ps1/build --psp work/psp/build \
  --dc work/dreamcast/output --gc work/gc/package --ps2 work/ps2/output
```

Use a fresh output/bundle directory. Any subset is supported; an unrelated failing core does not prevent building another system. The staging tool verifies ELF architecture/alignment/exports, Android dependencies and manifest hashes before copying. The original PS1/PSP cores remain in the baseline bundled-core module. No experimental binaries are checked into source, uploaded in an ungated workflow, or publicly released by these scripts.

## Runtime and progress boundaries

- PS1/PSP retain opt-in system/title settings and original cores. Modern defaults use supported option keys and JIT/dynarec. Only a validated absent raw PS1 card or absent PSP/SAVEDATA tree is seeded. Neither original nor existing candidate progress is overwritten.
- PSP and Dolphin resources are hash/revision checked before staged activation, with recovery. Dolphin memory cards are outside the replaceable Sys tree.
- Native core versions are immutable and checksummed. Startup failures quarantine the revision; there is no automatic retry loop. Explicit current/previous selection is available from the normal cartridge detail. Dolphin retains a verified Sys tree for each immutable core version, so different-source rollback selects the matching resources. Missing or damaged retained resources fail closed.
- Native save states and previews use the selected binary's version namespace; old snapshots remain intact on upgrade/rollback. Play states stay disabled pending support validation. PS1/PSP states retain separate core IDs.
- Native persistent data is under `saves/native/{dc,gc,ps2}` and `system/native/{dc,gc,ps2}`. Flycast uses per-content VMUs plus persistent `system/native/dc/dc` NVRAM. Play's patch routes its `Play Data Files/vfs/mc0` and `mc1` into the frontend-private PS2 save directory.
- GC GCM/ISO uses disc magic; PS2 ISO uses bounded ISO9660 SYSTEM.CNF/BOOT2 parsing; Dreamcast GDI validates companion families and CHD v5 requires GD-ROM metadata. M3U families reuse the existing merger with BOM/comment handling and verified flat, same-directory references; missing families retain existing library references. Unverified CDI/RVZ/PS2 CHD are not advertised. Wii magic is rejected as GameCube.
- Both analog sticks and digital shoulder controls reuse the existing dual-stick shell. Joypad device1 is explicitly selected. Physical triggers use the host's proportional source4. GC Z is the existing R1 input; no Start/Select positions or cosmetic layouts changed.

## Acceptance and license limits

All five sources and native builds are ready. Normal library/launch source integration is present in the engineering configuration. Compilation, synthetic media tests and file-store tests do **not** establish Android runtime or gameplay success. No physical ARM64 device is attached in this environment; graphics/audio, in-game memory-card roundtrip, pause/restart/resume and the owner's named games require device evidence. No owner acceptance is claimed.

PCSX/Flycast/PPSSPP/LibretroDroid carry GPL obligations; PPSSPP also includes FFmpeg LGPL dependencies. Dolphin's aggregate source is GPLv3-compatible with individual SPDX licenses. Play's principal license is BSD-2-Clause; third-party dependencies have their own notices. Review the actual pinned per-file licenses, resource rights and build notice inventories before distribution. Dolphin's DSP images are the upstream free replacements documented in `docs/DSP/free_dsp_rom`; no proprietary BIOS or game image is included. Jonathan retains final gameplay and distribution decisions.

## GitHub Actions delivery

The main **CARTDECK APK** workflow builds all five pinned engines in independent matrix jobs and builds the patched LibretroDroid host separately. Its final job consumes those six verified inputs, enables DC/GC/PS2, retains the original PS1/PSP fallbacks, and runs the normal source/asset/unit checks before packaging one APK.

The artifact **CARTDECK_FIVE_ENGINE_ARM64_DEBUG** is published only after the APK passes exact staged-byte checks, patched-host identity, compiled console-registry flag, fallback presence, signing and 16 KB alignment. Failed builds retain diagnostics without publishing an APK. The obsolete, separately gated two-core-only workflow has been removed. These are authenticated Actions engineering artifacts, not GitHub Releases or device/gameplay acceptance.
