# ROM library folders and shelf refresh

Select the **parent ROMS folder** in Settings, then use **Refresh shelf** after adding or removing files. CARTDECK recursively scans its subfolders. Keep one console per named subfolder; game-specific subfolders are allowed. Existing cartridges retain their database identity, custom name/cover, favourites and play history when their document URI stays the same. Refresh does not delete save files or custom artwork.

Example:

```text
ROMS/
  gba/                    # .gba
  gbc/                    # .gbc
  gb/                     # .gb
  nds/                    # .nds
  n64/                    # .n64/.z64/.v64
  psx/
    Game/
      Game.cue
      Game.bin
  psp/                    # .iso/.cso/.pbp supported by the configured core
  Dreamcast/
    Game/
      Game.gdi
      track01.bin
      track02.raw
      track03.bin
    Another Game.chd
  GameCube/               # .gcm/.iso; Dolphin also accepts .rvz/.gcz/.ciso/.tgc
  PS2/                    # .iso; Play! also advertises .chd/.cso/.isz
  WiiU/                   # .wua/.wud/.wux; Cemu .rpx/.elf/.tmd in explicit Wii U folder
    Game/
      code/
        game.rpx
  Xbox360/                # verified .iso, .xex, .xcp, .zar; keep Game/default.xex with companions
```

Wii U and Xbox 360 have permanent catalogue/shelf identities even if their emulator packages are absent. **Findable does not mean playable.** Current Wii U/Cemu Vulkan implementation is a private engineering test; opening a Wii U cartridge in the normal app remains blocked until hardware QA. Xbox 360 requires a packaged, provenance-verified Xenia engine for launch. Other native systems retain their build-specific registration gates.

Discovery follows this order:

1. Positive content signatures identify a disc. A real PS1/PSP signature overrides a misleading folder name. Wii content is not assigned to GameCube.
2. Unambiguous native suffixes identify GDI/CDI as Dreamcast and GCM as GameCube when that system is enabled. GDI references must resolve to their companion tracks.
3. Wii U: console-specific `.wua/.wud/.wux` extensions can identify the platform; common `.rpx/.elf/.tmd` formats only classify inside a clear `WiiU`, `Wii U` or `Nintendo Wii U` folder. Wii discs never become Wii U from a misleading directory.
4. Xbox 360: existing verified XDVDFS/XEX/STFS/ZAR signatures take precedence. Folder aliases `Xbox360`, `Xbox 360` and `Microsoft Xbox 360` provide a conservative fallback when Android SAF cannot seek the content. Shared `.iso` outside a clear folder stays unclassified; Xbox/XBE discs remain original Xbox.
5. Shared or compressed native disc formats use an exact console folder name when the storage provider cannot expose a readable content signature. Supported aliases include `dc`/`Dreamcast`/`Sega Dreamcast`, `gc`/`GameCube`/`Nintendo GameCube`, and `ps2`/`PlayStation 2`/`PS2 ROM`/`PS2 ROMS`/`PS2 Games`. Case, spaces, underscores and hyphens are normalised. Do not nest one console folder inside another.
6. Existing systems retain their metadata database, serial/checksum, unique-extension and configured folder rules. Use their database system IDs as folder names for ambiguous formats.

An arbitrary `.iso` in the parent folder cannot safely identify its console. Put ambiguous images in the correct console folder. Do not rename an image's extension to claim a different format: discovery does not convert it, and engine support still depends on the actual container.

Keep CUE/BIN and GDI/track families together. Multi-disc M3U playlists must reference existing files in that directory. Do not rename companion files without updating their descriptor. Each valid family becomes one cartridge.

Refresh reports added, removed and skipped entries. “Skipped” includes non-game files, incomplete disc families and unidentified formats; it does not mean the emulator has tested compatibility. Unavailable folders or provider read failures fail the refresh and preserve the previous library rather than treating the folder as empty. Re-select the parent ROMS folder if Android has revoked access. Empty or truncated disc headers must not crash a scan.

Moving a file to a different URI is a remove/add operation. Keep a backup before reorganising an established library: a new document URI can produce a new cartridge identity even when the game content is unchanged. Same-document display-name changes preserve the existing ID and player metadata.

When an engine requires a local copy, CARTDECK validates the source size and reported modification time before reusing its disposable cache. Replacing a ROM or ZIP at the same URI with a same-sized file therefore refreshes its staged bytes when the provider reports a new timestamp. Providers that omit size or modification time are copied again on Open rather than trusting a potentially stale cache. Interrupted copies retain the previous complete cache, and no refresh or staging operation replaces saved-game data.

## GEM-02/03 master catalogue refresh

Wii U and Xbox 360 are two distinct platform IDs (`wiiu` and `xbox360`). The GEM Master Game Catalogue currently includes a separate Xbox 360 source seed (1,235 research records), but no Wii U title records. The Console Coverage and Expansion Plan tabs now queue Wii U source discovery for a future licensed metadata import, without touching user GemRatings, DeckFit scores or any game rows. The next catalogue refresh must preserve the existing Xbox 360 seed and include the Wii U coverage queue; title IDs and cross-platform versions must be reconciled by platform and source identity, never title text alone. A Wii U title discoverable in GEM-02 is not verified to run on the Samsung.
