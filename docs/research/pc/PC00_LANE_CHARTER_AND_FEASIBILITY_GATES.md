# CARTDECK — PC-00 Integrated PC Emulation Lane
**Controlling owner scope — 17 November 2026.** Owner Jonathan; director Cassia; architecture Daniel; engineering Elliot; UX Sienna; QA Marcus; research Roxy.

## Locked product decisions
- **All approved, legally distributable emulators are integrated in the CARTDECK application.** Players do not download or separately install an emulator to run a supported cartridge. Maintain CI-01 selective native artifact builds, actual APK payload and rollback.
- **Release Settings links to an independent open-source emulator-only repository/source/build package** for all integrated console and PC engine technology. It is **not** a runtime installer, optional engine downloader or separate game-launch app. The release repo and URL do not yet exist: [REL-ENG-01 #307](https://github.com/safetyblade/ROMMachine/issues/307).
- The engine-only repository does **not** negate GPL-covered CARTDECK/Lemuroid/LibretroDroid combined-work corresponding-source requirements. Confirm actual licences/component boundaries and any app host/source release separately; do not represent the repository split as an automatic confidentiality guarantee.
- **Remove the GameNative project entirely from the active PC programme.** No Steam/Epic/GOG/Amazon storefront sign-on, authenticated external games library, proprietary provider download adapter or competing PC frontend.
- **Locally launched modern Windows games remain a feasibility objective:** examine Wine, Box64, FEX, Hangover and suitable Winlator/MiceWine technical components on Android ARM64/Xclipse; source/licence and actual execution proof required. Do not import Winlator/MiceWine's entire app shell.
- **Classic PC first:** DOS, Windows 3.x/95/98, selective XP-era games, lawfully user-provided PC optical images, installers and files. Shandalar / MicroProse Magic: The Gathering (1997) is the first game acceptance path.

## Dedicated PC lane
| ID | Work | State |
| --- | --- | --- |
| [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) | Governance, feasibility and decision record | Controlling |
| [PC-01 #300](https://github.com/safetyblade/ROMMachine/issues/300) | Local Win32/Win64 execution technology, Wine/Box64/FEX | Research |
| [PC-02 #301](https://github.com/safetyblade/ROMMachine/issues/301) | DOSBox Pure, DOSBox-X, classic discs and Windows guest environments | Research |
| [PC-03 #302](https://github.com/safetyblade/ROMMachine/issues/302) | One app, bundled runtime interface, game UI, controls, profiles, Junior, SafeSave and QA | Design |
| [PC-04 #305](https://github.com/safetyblade/ROMMachine/issues/305) | Shandalar first native integration proof | Protected bootstrap |
| [EXP-95 #195](https://github.com/safetyblade/ROMMachine/issues/195) | Original game-specific Shandalar research and trial | Gameplay not verified |
| [REL-ENG-01 #307](https://github.com/safetyblade/ROMMachine/issues/307) | Separate emulator-only source/build release and future Settings link | Release design |

**PC-04 draft source:** [PR #306](https://github.com/safetyblade/ROMMachine/pull/306), original `CoreID.DOSBOX_PURE`, LemuroidCores native AArch64 core, source/provenance verifier and tests. No new Windows runtime or APK, and no Win95 guest or Shandalar gameplay evidence. Preserve unmerged/reversible branch and baseline native engine.

## Engineering study
**DOSBox Pure first:** inspect exact actual pinned native binary, source licence, Android 16 KB ELF load alignment and host support for Windows 95/98 OS guest boot, virtual disk, PC CD mounting, keyboard/mouse and exit. Modern upstream source https://codeberg.org/schelling/dosbox-pure; upstream features do not prove the old binary supports them.

**Fallbacks and later Windows:** DOSBox-X only with viable ARM64 Android host; Wine with Box64, FEX or Hangover when legally and technically compatible, using Winlator/MiceWine as component/graphics references. Device baseline Samsung SM-S721B, Android 17, Xclipse 940, stock Vulkan. Do not assume Turnip/Adreno drivers or universal Windows support.

**Media and saves:** Validate real PC media types and mixed-mode tracks/CDDA; no generic ISO extension-based routing that hijacks PS1/PS2/GameCube/Wii/Xbox. Read-only SAF source, licensed/user-provided Windows/game installer and isolated writable per-game guest VHD/Wine prefix. One cartridge → game menu → keyboard/mouse/controller/TV → verified in-game save, quit and reopen. Per-profile save isolation and Junior restrictions, GEM installed/playable distinctions, SafeSave exports only verified progress; OS VHD and installers are not assumed to be saves-only.

**Release controls:** Full native APK inventory and engine-only source/build artifact inventory from same verified pinned inputs, SHA, notices, GPL/other upstream rights, ARM64/API/16KB and rollback; app/source release compliance independently assessed. Do not bundle Windows media, retail games, assets, BIOS, passwords or DRM bypass.

## Delivery gates and protections
PC-R0 actual existing core/source audit (script authored, not yet executed/accepted) → PC-R1 component licence/native compatibility matrix → PC-R2 integrated host/distribution ADR → PC-R3 complete classic/modern local import + controls/profile/SaveSafe UX → PC-R4 lawful Shandalar real-game evidence and Marcus QA → PC-R5 owner per-route GO/CONDITIONAL/NO-GO.

This roadmap clarification does **not** authorise new production source, a new APK, repository publication, or physical PASS. Current PERF-02 #60, PS3 #297, Wii U #296, X360 #295, GC-01 #262 / PR #294, CI-01 selective native builds, Room/game library/profile saves, signing and rollback are unaffected.
