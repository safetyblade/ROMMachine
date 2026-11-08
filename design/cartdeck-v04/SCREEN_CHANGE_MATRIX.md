# CARTDECK P1 — All screen changes and state transitions

| # | Screen | Current / verified source | Target behaviour | Asset / component | Gate |
|---|---|---|---|---|---|
| 01 | Launch/splash | Android launcher/Manifest | C-slot identity, no manufacturer imitation or artificial wait | v03 brand + slot pulse | P1-D |
| 02 | Empty library | HomeScreen, HomeViewModel | Device-ready hero; Add Cartridges via existing SAF picker; notification prompt contextual | brand, shelf | P1-A |
| 03 | Indexing/import | HomeScreen / library scan monitor | Compact progress, cancellation/permission denied and retry, don't block existing library | insert icon | P1-A |
| 04 | Library/bookshelf | HomeScreen, GamesScreen, FavoritesScreen | Sideways tightly arranged book spines; browse by recent/system/collections; title text vertical & legible | book-spine SVG | P1-A2 |
| 05 | Spine selection/focus | game item Composables | Selection highlight, ownership of Back, talkback title/system, long-press context; **no auto-launch** | shelf spine focus | P1-A2 |
| 06 | Cover detail | new selected-game route / dialog | Selected spine rotates/slides into front cover; cover, metadata, last played, Open/Resume and Edit Cover | cover-case/cartridge-edge | P1-A2 |
| 07 | Cover editor | existing CustomCoverManager / update route | User local image or original fallback, crop/position and reset, storage permission control; separate from ROM metadata | cover-placeholder, edit icon | P1-A2 |
| 08 | Systems/Favourites/Search | existing Compose destinations | Retain routing, consistent cards, selected game leads to cover detail not straight game | icons, spines | P1-A2 |
| 09 | Game insert + boot | current GameInteractor launch | Nonblocking slot/wake/C-slot; skippable, reduced motion and mute; game loads immediately when ready | boot slot + v03 logo | P1-D |
| 10 | GB/GBC gameplay | MobileGameScreen + GameScreenLayout + PadKit | CARTDECK default portrait chassis + same screen frame; native 10:9 image (when core reports it), proper D-pad/A/B | shared shell + v03 D-pad | P1-B |
| 11 | GBA gameplay | same | Same shell, native 3:2 game image, L/R controls, no unused black dead space where avoidable | shared shell + shoulders | P1-B |
| 12 | PS1 gameplay | PSX.kt / PSXDualShock.kt | **Same CARTDECK shell/view port**, image contained at core aspect, four-face+L1/L2/R1/R2 and optional two analog; no separate console identity | universal shell + generic controls | P1-B2 |
| 13 | Other systems | NES/SNES/Genesis/DS/PSP/etc | Same visual family, adapt required buttons/6-face/stylus/multi-display; system config drives input, never edition | generic control library | P1-B2 |
| 14 | Gameplay landscape/tablet | GameScreenLayout.kt | Same brand geometry adaptive to display bounds, no clipped game video, safe insets | landscape shell | P1-B |
| 15 | Fullscreen gameplay | MobileGameScreen | No shell, correct aspect game, controller available; reliable Pause/Back | none required | P1-C |
| 16 | Floating gameplay | MobileGameScreen + TouchControllerSettingsManager | Game-first, adjustable translucent controls & reset; all mandatory keys reachable | control library | P1-C |
| 17 | Pause/menu & saves | GameMenuHomeScreen | Resume/Save State/Load State/Controls/Display/Exit, in-game SRAM distinct from manual state | pause/display glyphs | P1-C/D |
| 18 | Display & edition picker | SettingsScreen / new preference route | Console Shell default; Original/Pink selector; personal colour editor for shell/bezel/button/accent with live preview and contrast checks; Green/Mono preset prototypes remain unapproved; accessibility preferences take priority | v03 editions + v04 catalog + appearance-editor.json | P1-C |
| 19 | Cartridge/storage/backup | Settings / SAF / save sync | Keep existing import, save and permission mechanics; add future export only in separately approved feature | shelf icons | Future |
| 20 | Error/recovery | scan/core/game crash views | CARTDECK error style with actions, never cosmetic failure destroys launch/save data | same tokens | P1-D |
| 21 | Android TV | existing TV activities | Controller-first fullscreen/TV library, no virtual touch, system frame optional | same brand / layout rules | P1-D |
| 22 | Accessibility | every view | 48dp+ targets, TalkBack names, focus, high contrast, motion/mute, text scaling, predictive Back | focus tokens | Every gate |

## Spine → front cover → play contract
- Never launch immediately on shelf-spine single tap. Tap opens cover details; Open/Resume invokes **the existing** `GameInteractor.onGamePlay(game)`.
- Back returns to same shelf position. Cover editor returns to same selected cartridge. Long press may retain context menu.
- Selected record and user artwork are independent: `Game.id` and system ID unchanged. No save migration, ROM relocation, API/Core override.
- Fallback cover is entirely CARTDECK original graphics and runtime title text; user-supplied copyrighted art is local personal data, not bundled assets.
- Editions tint shelf, spine and cover hardware without rewriting the player's artwork or the game's identity.

## Design authority
UI must match approved v0.2 Original/Pink identity, corrected D-pad from PR #10, and v0.4 shared asset catalog. The generated concept posters are direction studies and not literal button input implementations.
