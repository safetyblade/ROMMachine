# CARTDECK SHELF-112 — Native Disc Discovery & Independent QA

Owner observation: after current Codex integration, one ROM each for Dreamcast, GameCube and PS2 does not appear after shelf refresh.

## Source finding and correction
The native emulator build registers `dc,gc,ps2` only in the five-engine engineering APK. The existing detector requires platform-specific disc contents and cannot recognise some Android SAF files if the provider does not supply a usable seekable descriptor. The three native systems deliberately disable generic path/filename matching to prevent misleading classification of shared `.iso` and `.chd`.

A controlled fallback is now available only for registered native engines with **an explicit platform folder**; it never classifies arbitrary root `.iso`/`.chd` files:
- `ROMs/Dreamcast/disc.gdi`, `ROMs/Dreamcast/disc.chd` (GD-ROM CHD), `ROMs/Dreamcast/disc.cdi`, or validated `.m3u` family.
- `ROMs/GameCube/game.gcm`, `.iso`, `.rvz`, `.gcz`, `.ciso` or `.tgc` in an unambiguous GameCube folder.
- `ROMs/PS2/game.iso`, `.chd`, `.cso` or `.isz` (equivalently `PlayStation 2`).
- Supports Android SAF `primary%3AROMs%2FPS2%2Fgame.iso`, standard local path, nested region directories and case-insensitive extensions.
- Existing verified disc signatures take precedence, PS1/PSP serials remain authoritative, Wii magic is a refusal, two different named platform folders are ambiguous and skipped.
- The additional compressed disc extensions above are documented as loadable by their respective upstream Libretro cores, but **have not been verified in this pinned Android build or on a phone**. Discoverable is not the same as playable. Filename/extension alone is never enough for shared containers; the explicit console folder is the user-supplied platform selection.
- Missing GDI tracks and broken M3U playlists are rejected by existing family validation. No modification to file permissions, save files, prior games or shelf arrangements.

Core format references: https://docs.libretro.com/library/flycast/ ; https://docs.libretro.com/library/dolphin/ ; https://docs.libretro.com/library/play/ .

## Marcus Reid — independent QA work package
1. **QA-01 Build identity.** Use `CARTDECK_FIVE_ENGINE_ARM64_DEBUG` from a successful build of the shelf-fix branch. Verify APK registry flag `dc,gc,ps2`, installed package/version increase, 16 KiB ELF alignment, original PS1/PSP engines retained. Baseline CARTDECK 2189 does not contain the three systems.
2. **QA-02 Positive indexing.** Use user-owned/homebrew compatible files in Dreamcast, GameCube, PS2 named folders. SAF storage and internal/local provider. Confirm a fully successful refresh adds one cartridge per platform to Unsorted shelf. Repeat without duplicates. Check case differences and a nested region folder.
3. **QA-03 Collision and completeness.** A generic `ROMs/disc.iso` stays unclassified unless verified by its actual signature; a Wii image stays Wii/unknown, never GC/PS2. A PS1/PSP disc stays PS1/PSP, even in an incorrectly named folder. An incomplete GDI family and invalid M3U never creates a false playable cartridge.
4. **QA-04 Regression/retention.** Game Boy/GBA/PS1/PSP/DS titles remain visible, shelf positions/artwork preserved. Removing a ROM updates only its index record, not player save data; losing SAF permission must fail the refresh without deleting previous records. Ensure no crash on inaccessible files.
5. **QA-05 Gameplay boundary.** Opening and resuming on actual phone is a separate gate. A listed cartridge and CI success do not prove Flycast, Dolphin or Play! runtime compatibility. Capture format, file system/provider, exact build, chipset/Android version and screenshot/logcat for failures; Jonathan performs final gameplay acceptance.

## Remaining diagnostic questions if the owner's ROMs still do not appear
Record the **actual file extensions** (`.gdi/.chd/.iso/.gcm/... `), location relative to named system folder, installed APK version, and refresh result `added/skipped`. Unverified formats and unseekable root-folder ambiguous ISO/CHD images must not be silently re-labelled to another console. Instead add an explicit user-confirmed system-picker design or validated container parser in a separately reviewable follow-up.

Linked issue: #112; native integration #110/#111; parent #60.
