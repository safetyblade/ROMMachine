# CARTDECK P3-GC — GameCube native integration readiness

**Owner direction:** GameCube games should be discovered and presented exactly like the other CARTDECK systems. Same **Arc-Rail cartridge family**, system badge **GC**, existing selected-cover/detail/Open/Resume, and existing game-first adaptive hardware presentation. NO Dolphin app launch or separate game library.

**This branch is preparation only.** No GameCube `GameSystem`, `CoreID`, file scanner entry, binary, BIOS, APK, package ID, signer, save format or public supported-system claim is changed.

Controlled issue: https://github.com/safetyblade/ROMMachine/issues/83

## Verified implementation candidate

- Official Dolphin emulator (Android ARM64 standalone supported): https://github.com/dolphin-emu/dolphin at `41799046d68b2e5ea7cfde91f122320a472b7a61`; GPLv2-or-later as stated by upstream.
- Libretro fork: https://github.com/libretro/dolphin at `32c61f9d0e933331bb7087ba57e96883241b4960`. Its `.github/workflows/build-android-libretro.yml` explicitly configures CMake `LIBRETRO=ON`, `ANDROID_ABI=arm64-v8a` and the Android NDK toolchain. **Upstream build recipe exists; no CARTDECK binary/runtime proof yet.**
- Confirm the fork's maintenance, exact source-level exceptions and any `Sys` directory/data requirements independently of official Dolphin's standalone app. A standalone Android APK is not automatically a valid LibretroDroid core.

## Safety-first system discovery contract

- `GAMECUBE` must be a **new distinct internal SystemID**; existing `NGC` represents **Neo Geo Pocket Color**, NOT GameCube.
- `.gcm` and `.iso` require verified Nintendo disc magic `C2 33 9F 3D` at raw uncompressed byte offset `0x1C`, which this branch probes in read-only test fixtures.
- Wii raw `.iso` signature `5D 1C 9E A3` at `0x18` must not be labelled GameCube; Wii shares Dolphin's engine but is a separate system and control contract.
- `.rvz` is a Dolphin container shared between GameCube and Wii and must be parsed with the proper container library before classification. `.chd`, `.cue`, `.iso` without conclusive signature, and multi-disc `.m3u` remain ambiguous. Filenames or an enclosing `GameCube` folder alone are NOT authoritative.
- Do not infer support for arbitrary PS2/PS1/PSP ISO files from extension matching.
- Preserve current library's Room Game.id, scanned URI, custom/indexed cover precedence, personal shelf ordering and save data. No rescan-prune on parse/permission failure.

## Controlled next technical steps

1. **Daniel**: confirm libretro Dolphin ARM64 `.so` provenance and exact GPLv2-or-later/dependency licence; confirm `Sys` assets, GPU requirements, memory cards and Android 16 KiB page alignment assumptions.
2. **Elliot / Codex**: create a separate *non-shipping* ARM64 core compile attempt pinned to the libretro fork. Check Libretro exported symbols/ELF and memory footprint; DO NOT add a `CoreID` or try to download core from nonexistent LemuroidCores slot.
3. Run an **isolated LibretroDroid graphics context test** on supported hardware for actual video/audio, controller input and clean return. Main risk: GLES/Vulkan/EGL hardware render negotiation, input descriptor compatibility and source-level backend assumptions.
4. Prove GameCube memory-card save after app force-stop/upgrade, genuine analog stick, analog L/R, Z button, controller, audio and 15–20-minute battery/performance/thermal. Verify user-supplied lawful disc image and no copyrighted game/firmware bundled.
5. Only after exact device PASS and legal gate: add a separate scoped integration PR to register GameCube, use collision-safe discovery, badge **GC** in the SAME CARTDECK shelf and ordinary game launch, and test existing GB/GBC/GBA/PS1/NDS and save retention.

**Status:** Ready for isolated ARM64 libretro compatibility investigation, NOT yet playable or approved for integration.
