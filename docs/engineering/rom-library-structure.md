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
```

The native systems must be included and enabled in the installed engineering APK. A normal build without these engines does not enable their folders.

Discovery follows this order:

1. Positive content signatures identify a disc. A real PS1/PSP signature overrides a misleading folder name. Wii content is not assigned to GameCube.
2. Unambiguous native suffixes identify GDI/CDI as Dreamcast and GCM as GameCube when that system is enabled. GDI references must resolve to their companion tracks.
3. Shared or compressed native disc formats use an exact console folder name when the storage provider cannot expose a readable content signature. Supported aliases include `dc`/`Dreamcast`/`Sega Dreamcast`, `gc`/`GameCube`/`Nintendo GameCube`, and `ps2`/`PlayStation 2`/`PS2 ROM`/`PS2 ROMS`/`PS2 Games`. Case, spaces, underscores and hyphens are normalised. Do not nest one console folder inside another.
4. Existing systems retain their metadata database, serial/checksum, unique-extension and configured folder rules. Use their database system IDs as folder names for ambiguous formats.

An arbitrary `.iso` in the parent folder cannot safely identify its console. Put ambiguous images in the correct console folder. Do not rename an image's extension to claim a different format: discovery does not convert it, and engine support still depends on the actual container.

Keep CUE/BIN and GDI/track families together. Multi-disc M3U playlists must reference existing files in that directory. Do not rename companion files without updating their descriptor. Each valid family becomes one cartridge.

Refresh reports added, removed and skipped entries. “Skipped” includes non-game files, incomplete disc families and unidentified formats; it does not mean the emulator has tested compatibility. Unavailable folders or provider read failures fail the refresh and preserve the previous library rather than treating the folder as empty. Re-select the parent ROMS folder if Android has revoked access. Empty or truncated disc headers must not crash a scan.

Moving a file to a different URI is a remove/add operation. Keep a backup before reorganising an established library: a new document URI can produce a new cartridge identity even when the game content is unchanged. Same-document display-name changes preserve the existing ID and player metadata.
