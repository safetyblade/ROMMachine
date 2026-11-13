# CARTDECK INPUT-02 — Console + genre action translation matrix v0.1
**13 November 2026** · [Issue #172](https://github.com/safetyblade/ROMMachine/issues/172) · **Research/design only; NO gameplay binding changes**

## Decision
The original console does **not** have one default meaning for a button. It has a default *controller layout*, while game genre and individual game assign the *functions*. We can derive a **console × genre** heuristic and then override it for a **verified title/edition**. When a player selects **Modern Function**, known titles apply their audited profile automatically. A confirmed genre heuristic may offer an immediate suggested mapping for an unknown title, but must never claim it is verified.

The canonical **play action** routes from the modern physical control to the *correct original native game key* (e.g., GBA racer right trigger → GBA A). The existing console/core adapters translate that game key into RetroPad/PadKit keycodes; the table must **not** treat a native GameCube A, GBA A or Dreamcast A as interchangeable.

## 1. Source-backed exemplar matrix — race/drive
| Original console | Verified exemplar | Original Accelerate | Original Brake | Modern Accelerate | Modern Brake | Keep special controls |
|---|---|---|---|---|---|---|
| SNES | Super Mario Kart | **B** | **Y** | RT → SNES B | LT → SNES Y | L/R hop or drift; A item |
| N64 | Mario Kart 64 | **A** | **B** | RT → N64 A | LT → N64 B | R drift; Z item; A+B spin-turn |
| GBA | Mario Kart: Super Circuit | **A** | **B** | RT → GBA A | LT → GBA B | R hop/drift; L item |
| NDS | Mario Kart DS | **A** | **B** | RT → DS A | LT → DS B | R drift; L/X item; DS portrait-only |
| GameCube | Mario Kart: Double Dash!! | **A** | **B** | RT → GC A | LT → GC B | L/R drift; X/Y item; Z switch |
| PS1 | R4: Ridge Racer Type 4 | **Cross (×)** | **Square (□)** | RT → PS Cross | LT → PS Square | Existing L/R shifting must remain reachable |
| PS2 | Gran Turismo 3 A-spec | **Cross (×)** | **Square (□)** | RT → PS Cross | LT → PS Square | DualShock2 pressure behavior needs core-specific proof |
| PSP | Ridge Racer | **Cross (×)** | **Square (□)** | RT → PSP Cross | LT → PSP Square | R nitrous, L view remain |
| Dreamcast | Crazy Taxi | **R trigger** | **L trigger** | RT → DC R trigger | LT → DC L trigger | Already natural; A reverse gear, B drive gear |
| Original Xbox | Forza Motorsport | **R trigger** | **L trigger** | RT → Xbox R trigger | LT → Xbox L trigger | Already natural; A handbrake |

**Interpretation:** Nintendo-era kart racers often used a face button for throttle, while some Dreamcast/Xbox driving games already used triggers. The **same modern RT/LT racing feel** is achieved by *different original output keys*. That is the desired product behaviour.

## 2. Source-backed platform / sport matrix
| Console | Exemplar | Verified original action | Proposed modern position | Retained functionality |
|---|---|---|---|---|
| NES | Super Mario Bros. | A = jump, B = run/fire | South → NES A; West → NES B | Jump height is hold-duration-sensitive |
| SNES | Super Mario World | B = jump, Y = run | South → SNES B; West → SNES Y | A spin jump remains separate |
| Genesis | Sonic the Hedgehog | A/B/C = jump | South → Genesis B (one valid jump key) | Remaining jump keys stay available |
| N64 | Super Mario 64 | A = jump | South → N64 A | B interaction/attack remains available |
| Dreamcast | Sonic Adventure | A = jump | South → Dreamcast A | B/X/Y character interactions retained |
| PS2 | FIFA Soccer 2005 | Cross = ground pass; Circle = shoot; Square = lob; Triangle = through ball | South → Cross; East → Circle; West → Square; North → Triangle | In defense and menus the **same native keys change function**; map original key, not inferred screen state |

**Important counterexample:** On SNES, B is **jump** in Super Mario World, but **accelerate** in Super Mario Kart. A single `SNES B -> RIGHT_TRIGGER` system-wide rule is therefore WRONG.

## 3. Console family × genre fallback candidates (NOT title-verified)
| Console family | Platforming Jump → South | Racing Accelerate → RT | Racing Brake → LT | Confidence/automaticity |
|---|---|---|---|---|
| NES | A | Unknown | Unknown | Jump: known title exemplar; do not apply to all NES games |
| SNES | B | B (kart example) | Y (kart example) | For racing **suggest** B/Y if genre is known; no cross-title auto without verification |
| Genesis / Mega Drive | A/B/C in Sonic | Unknown | Unknown | Need per-title action selection |
| GB/GBC | Unknown | Unknown | Unknown | No verified baseline; Authentic |
| GBA | Unknown | A (Mario Kart Super Circuit) | B (Mario Kart Super Circuit) | Genre-confirmed **kart** candidate only |
| Nintendo 64 | A (Mario 64) | A (Mario Kart 64) | B (Mario Kart 64) | Genre-confirmed **kart** candidate only |
| Nintendo DS | Unknown | A (Mario Kart DS) | B (Mario Kart DS) | Portrait DS only, as specified in DISPLAY-01 |
| GameCube | Unknown | A (Double Dash) | B (Double Dash) | Genre-confirmed kart candidate only |
| PS1 | Unknown | Cross (Ridge Racer R4) | Square | Racing candidate, title verification first |
| PS2 | Unknown | Cross (Gran Turismo 3) | Square | Racing candidate; preserve native pressure limits |
| PSP | Unknown | Cross (Ridge Racer) | Square | Racing candidate, title verification first |
| Dreamcast | A (Sonic Adventure) | R trigger (Crazy Taxi) | L trigger | Arcade driving candidate; GameCube/PlayStation analog rules do not transfer |
| Original Xbox | Unknown | RT (Forza) | LT | Already modern for exemplar; verify per game |
| Wii | Unknown | Unknown | Unknown | Depends on selected Wii Remote / Nunchuk / Classic / GC mode; no auto-application |
| Arcade / SMS / Neo Geo / Atari / PC Engine / other | Unknown | Unknown | Unknown | Keep Authentic until representative games are reviewed |

**Meaning of "automatic":** once the player chooses **Modern Function**, a known cartridge/title/edition with validated native keys gets mapped automatically without manually remapping each button. For unverified titles, a recognised **genre** can provide a clearly identified **suggested template**, with one-tap Apply/Undo. Console alone is insufficient.

## 4. Runtime resolver rule (planned)
```
physical controller (any brand) -> normalized position
  -> selected cartridge + native console / active core-controller config
  -> [verified title profile, else reviewed console + confirmed genre suggestion]
  -> original console input (not a printed modern button label)
  -> existing RetroPad / core adapter and input ownership gate
```
- **Modern profile** overrides only named original actions; preserve all other original controls. Do not silently erase an emulator key or system-specific input.
- A physical **RT** can emit an original **digital A/B/Cross** during hold; release reliably on LT/RT falling below a calibrated threshold, controller disconnect, game pause/exit and profile switch. Use hysteresis to avoid chatter.
- If a source has genuine analog throttle, retain proportional 0..1; if native game input is digital, do **not** claim analog pressure. DualShock2 pressure-sensitive face inputs require specific core support before describing as analog.
- Duplicate modern inputs and remaining native face controls require a **per-output owner/ref count**, so releasing one held source never prematurely releases another.
- Menu/TV escape actions stay separate, gamepads are per-port, saved device remaps and per-game GripArc remain unaffected.
- A game's menu may reuse the same original input as gameplay (e.g. A to confirm, A to accelerate). Do not infer game state. Retain normal face-button confirmation and optionally allow RT duplicate native A in gameplay. In-game menus may therefore also accept RT unless a proven contextual mode is available.
- Presets are additive, versioned, per cartridge + console/core/controller type. No ROM database migration, save loss, engine changes, or overwrite of user-defined controller mappings.

## 5. Data and evidence requirements
Source `Game` currently has `title, systemId, fileUri, developer` etc but **no genre or control-action fields**. Existing Libretro DB metadata conversion doesn't supply genre. A classification/enrichment step is needed before applying console × genre suggestions; **never guess genre from console or title keywords alone** as authoritative.
- Use stable cartridge identity and verified title aliases/regions/versions.
- Store `source URL`, action, original button, analog/digital semantics, review status and user override.
- `title profile` evidence = manual/verified gameplay. `genre heuristic` evidence = multiple reviewed titles and still clearly marked as estimated.
- The companion JSON `console-genre-action-matrix.v0.1.json` is **research seed data**, not an executable mapping file. The runtime must not read/apply it as-is.

### Source documents for the verified examples
- nesMario: https://manuals.plus/m/ab1bea73e4818cd21d57680baea3d746921ea179d3cee338ab26f9ef8e7cace7
- snesWorld: https://www.world-of-nintendo.com/manuals/super_nes/super_mario_world.shtml
- snesKart: https://www.world-of-nintendo.com/manuals/super_nes/super_mario_kart.shtml
- mdSonic: https://www.gamesdatabase.org/Media/SYSTEM/Sega_Genesis/Manual/formated/Sonic_The_Hedgehog_-_1991_-_Sega.pdf
- n64Kart: https://www.nintendo.com/eu/media/downloads/games_8/emanuals/nintendo_8/Manual_Nintendo64_MarioKart64_EN.pdf
- n64Mario: https://www.nintendo.com/eu/media/downloads/games_8/emanuals/nintendo_8/Manual_Nintendo64_SuperMario64_EN.pdf
- gbaKart: https://www.world-of-nintendo.com/manuals/game_boy_advance/mario_kart_super_circuit.shtml
- ndsKart: https://www.nintendo.com/eu/media/downloads/games_8/emanuals/nintendo_ds_21/Manual_NintendoDS_MarioKartDS_EN.pdf
- gcKart: https://www.gamesdatabase.org/Media/SYSTEM/Nintendo_GameCube/Manual/formated/Mario_Kart-_Double_Dash_-_Nintendo.pdf
- ps1Ridge: https://secure.cdn.us.playstation.com/manuals/classic/games/r4-ridge-racer-type-4-manual-en.pdf
- ps2GT3: https://db.hfsplay.fr/files/2019/09/15/Gran_Turismo_3_-_Manual_-_PS2_POJPPqZ.pdf
- pspRidge: https://manuals.plus/m/51bfb33ecef95c62155b02e5e0f129ae0f26c1f5d6e6b76d13c59c29f92fc855
- dcTaxi: https://gamefaqs.gamespot.com/dreamcast/196990-crazy-taxi/faqs/6845
- dcSonic: https://www.manualslib.com/manual/318256/Sega-Sonic-Adventure.html?page=2
- xboxForza: https://www.gamesdatabase.org/Media/SYSTEM/Microsoft_Xbox/Manual/formated/Forza_Motorsport_-_Microsoft_Game_Studios.pdf
- ps2Fifa: https://www.psxdatacenter.com/games/U/F/SLUS-01585.html

## 6. Next engineering gates
**INPUT-02A** audit extra titles per platform/genre and confirm mappings in actual in-game default settings; establish consistent versioned profile schema; tests cover title resolution and unknown fallback.
**INPUT-02B** build per-title/confirmed genre resolver behind a user-enabled **Modern Function** preference; map digital/analog triggers with ownership and disconnect safety; no game replay state inference.
**INPUT-02C** add a cartridge **Controls → Modern Function / Console Authentic / Custom** selector and an in-game preview 'RT → original GBA A (Accelerate)' with revert.
**INPUT-02D** test representative games on owner hardware, including Super Mario World and Super Mario Kart sharing SNES B but requiring different modern positions. Test the same player controller across Switch Pro/Xbox/PS3-style devices (where Android supports them). Keep Wii remotes, two-screen DS and unsupported cores explicit.
