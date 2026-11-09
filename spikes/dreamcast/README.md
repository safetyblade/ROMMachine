# CARTDECK — Dreamcast/Flycast integration spike (P3-DC)

Status: **experimental research branch only; no app integration or release clearance**.
Parent: [P3-X #81](https://github.com/safetyblade/ROMMachine/issues/81).
Execution: [Dreamcast #82](https://github.com/safetyblade/ROMMachine/issues/82).
Protected mainline: P1-C2 adaptive viewport [#74](https://github.com/safetyblade/ROMMachine/issues/74).

## Product requirement

A successfully accepted Dreamcast game should join the **same CARTDECK Arc-Rail shelf**, with a
**DC** system badge, cover/details, the ordinary **Open/Resume** action, and the same branded
gameplay shell. Players should select their ROM folder once and have Dreamcast titles
appear during the normal scan. There must be no second app, separate game library,
manual core selection, side-loaded firmware download or new launch mechanism.

## Why a lab spike comes first

CARTDECK currently registers 25 systems in `GameSystem.kt`; **Dreamcast is not registered**.
Its `CoreID.kt` has no Flycast and the free core updater fetches only named binary modules
from `S​​wordfish90/LemuroidCores` tag 1.17.0. Adding `CoreID.FLYCAST` and `SystemID.DREAMCAST`
today would cause download failures and/or imply unsupported games are playable.

The upstream [Flycast repository](https://github.com/flyinghead/flycast) supplies a
Libretro build for Android ARM64 using NDK + CMake. This branch pins
`1b7c7d63b73ea6987fb23f9334a7d597401a50f8` and attempts an **isolated core-only**
build via [the experiment workflow](../../.github/workflows/cartdeck-dreamcast-spike.yml).
The binary stays in the temporary CI workspace: no APK, core binary, copyrighted
game, firmware or source-license changes are committed or distributed.

LibretroDroid documents support for GL-based cores but does not list Flycast as
a tested core. Actual `retro_hw_render` context negotiation, GLES/Vulkan behaviour,
audio, inputs, VMU and Android device runtime **remain unproven**.

### Licensing

Flycast's repository metadata labels it **GPL-2.0** and LibretroDroid is **GPL-3.0**.
However, inspected current Flycast core files, including `shell/libretro/libretro.cpp`
and `core/hw/pvr/pvr.cpp`, explicitly allow **GPL version 2 or later**.
That supports a possible GPLv3-compliant combination but does not settle licensing
for the entire binary: dependency sources, their exact licence exceptions and
the combined distribution/linkage model must be reviewed by Daniel,
with legal advice if needed. **Do not publish an integrated binary pending clearance.**
Avoid the deprecated `libretro/flycast` fork; use the maintained
`flyinghead/flycast` source.

## Disc discovery safety contract

The existing library has system-neutral `.chd`, `.cue`, `.iso`, `.bin` and `.m3u`
files already used by PS1, PSP and Sega CD. **Never classify these as Dreamcast from
the extension alone.** The repo's `GameSystem.findByUniqueFileExtension` uses a map
that silently overwrites duplicate extensions; carelessly adding Dreamcast would
regress other systems.

`disc_probe.py` and `test_disc_probe.py` are read-only **format feasibility**
tests, not a runtime scanner. They allow:
- Dreamcast-specific `.gdi` descriptor as an unambiguous *candidate*.
- Strict GDI local track manifest validation without extracting/copying any files.
- Ambiguous CHD, ISO, CUE, BIN and M3U classifications until actual disc metadata or
  explicit, stable user system association can be established.
- Refusal of absolute/traversal GDI companion paths.

Before wiring to production scanner, add companion file existence/URI validation,
path/content identity, duplicate-suppression and user-facing unsupported-core errors.

## Required implementation sequence

1. **Stage 0 — source / legal / binary proof** (this draft PR): run descriptor tests,
   compile pinned Android ARM64 Flycast as a separate binary, inspect ABI/hash, review
   licence compatibility. Neither success alone nor an upstream Android Flycast APK
   proves CARTDECK gameplay.
2. **Stage 1 — frontend proof**: Daniel and Elliot test Flycast's GL hardware-context
   negotiation in an isolated Android/LibretroDroid harness. Obtain legal homebrew
   test content; establish visible frame, audio and ordinary controller operation.
   Preserve mainline APK, package ID, signing and user data.
3. **Stage 2 — save and media proof**: Dreamcast `dc/vmu_save_*.bin` and
   `dc_nvmem.bin` persist after close/reopen and update; test .gdi companion tracks,
   .chd magic/path metadata and .m3u multi-disc without false matches against other
   existing cores. Report per-device frame pacing and thermal measurements.
4. **Stage 3 — product integration** (new, separately reviewed PR): only after
   technical/legal approval, add properly gated Dreamcast system/core registration,
   native core provision, scanner association, DC labels, appropriate control mappings
   and game-first 4:3/variable viewport. Reuse the SAME CARTDECK bookshelf, covers,
   Open/Resume, gameplay activity and saves infrastructure.
5. **Stage 4 — independent device acceptance**: Marcus checks exact APK build/hash,
   Dreamcast load/audio/ABXY/analog/triggers, VMU persistence and in-place update
   and verifies existing GB/GBC/GBA/PS1 and DS were unaffected. Cassia/owner approve
   presentation. Only then can the supported-games guide advertise Dreamcast.

**No change to the 25 existing configured systems, ROMs, saves, build signer or
normal CARTDECK APK in this research PR.**
