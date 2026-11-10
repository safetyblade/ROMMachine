# CARTDECK — Console layout activation and shell audit
**Date:** 10 November 2026
**Owner decision:** Sienna V09/PivotArc device appearance visually accepted; PRs #106, #107 and #108 merged to master.
**Scope:** actual installed emulator interface. This audit is about touch control and manufactured shell presentation, NOT an assertion that every potential core is bundled or works in gameplay.

## Shared hardware active across all registered libretro gameplay systems

- `MobileGameScreen` supplies one global `CartdeckConsoleShell` and `CartdeckHardwareSeats` path regardless of console. The visual shell is not a set of old duplicated layouts.
- All 25 `GameSystem.all()` entries have registered controller configurations, routed via `TouchControllerID.getConfig` to existing PadKit families.
- Standard controller families: GB/GBC, GBA, NES, SNES, Mega Drive 3/6, Sega CD, SMS, Game Gear, N64, PS1 Standard and DualShock, PSP, arcade 4/6, DS MelonDS/DeSmuME, Nintendo 3DS, Atari 2600/7800, Lynx, PC Engine, Neo Geo Pocket/Color, WonderSwan/Color and DOS.
- Sienna V09 upper cartridge crown, shell/chamfers, protection for game pixels, edition colours and custom-colour luminance-preserving details are shared by all registered console layouts wherever measured headroom allows. DS/3DS flexible viewport may deliberately use compact branding instead of overlays.
- Standard remains the unaltered, immediate fallback when a console does not have a complete PivotArc system adapter.
- No new controller core is authorized by this visual pass; emulator provisioning, BIOS and ROM paths are outside scope.

## Activation corrections in this pass

1. Nintendo 3DS now uses the same hardware dual-screen flag as Nintendo DS (the viewport already classified both as dual).
2. Nintendo 64's C-direction secondary control now has its own measured hardware seat ID so it does not overwrite the primary D-pad's recorded rect. It uses existing compact-chamfered CARTDECK housing artwork.
3. Mega Drive six-button C/Z and N64 auxiliary Z buttons now report their own measured rectangles for individual machined face seats. Existing input event IDs and touches are unchanged.
4. Automated coverage tests check every registered system has Standard controller configurations; real gameplay remains an independent acceptance gate.

## Explicit non-claims

- `SystemID.NGC` is **Neo Geo Pocket Color**, not Nintendo GameCube. This repository does **not** register GameCube as a playable `SystemID` via the GameSystem/PadKit path.
- Dreamcast and PlayStation 2 also do not yet appear in this GameSystem/PadKit console registry. A decorative design is not a substitute for a native emulator integration and complete input mapping.
- PivotArc is presently enabled as a full native control profile only for GB/GBC, GBA and SNES. Its four optional X/Y/L2/R2 geometric positions are foundation work, **not** verified expanded-console support.
- PSP/PS1 and 3DS controller mapping availability does not mean modern emulator/BIOS compatibility or gameplay performance has been independently verified.
- A 95+ unit-test / CI pass is code verification, not a five-minute actual device acceptance.

## QA matrix

| Group | Default controller in code | Sienna shared hardware | Remaining QA |
|---|---|---|---|
| GB/GBC/GBA, NES, SNES | Registered PadKit | Shared, Original/Pink/Green/custom | Live gameplay, 4:3 and peripheral sizing |
| PS1 Standard/DualShock, PSP | Registered PadKit with shoulders/analogs | Shared, multi-control seats | Check shoulders, sticks, logo/viewport and performance |
| N64 | Registered PadKit incl C-direction/Z | Shared with distinct C-direction + Z seats | Confirm no decorative overlaps or duplicate active controls |
| Mega Drive/Sega CD, arcade | Registered 3/6 or 4/6 button variants | Shared, auxiliary wells | Visually check X/Y/C/Z placement and no touches stolen |
| DS/3DS | Registered dual-screen touch layouts | Flexible dual-screen chrome | Check stylus, mic, lower screen and bezel no-bleed |
| Older families / DOS | Registered PadKit | Shared | Portrait/landscape/rotated sources and optional inputs |
| PivotArc GB/GBC/GBA/SNES | Opt-in Candidate C | Dedicated branded module | Device comfort, left/right mirrors and settings persistence |
| Dreamcast, GameCube, PS2 | Not registered as active PadKit systems | Design foundation only | Native core, ROM scan, functional mapping and QA needed |

**Merge rule:** do not re-enable abandoned experimental visual layouts or mark unfinished consoles playable. Extend the common shell and current conditional profile architecture only. Preserve Standard Start/Select coordinates and all player data.
