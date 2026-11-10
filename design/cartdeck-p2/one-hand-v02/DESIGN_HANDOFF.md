# CARTDECK | Sienna Vale — One Hand Mode v0.2
**Issue #58 • Design only • 10 November 2026**

## Approved direction
`Layout → One Hand Mode → Left / Right`. The one selected layout is a **global app preference** and applies to **every game**, so switching titles does not unexpectedly switch layout or prompt for per-game setup. The controls available within that active layout are system-aware, e.g. GBA includes L/R, SNES four face buttons, PS1 extra shoulders and analog, DS dual screens/mic/screen function. The physical feasibility of expanded PS1 and DS one-hand gameplay is still a design/QA gate; no unsupported functionality is promised as complete.

## Updated hardware arrangement
**All frequent controls inside ONE ThumbDock.** The approved chamfered D-pad + A/B + L/R shoulders for applicable systems occupy the same left or right thumb-side material island. Expanded ABXY and additional buttons later adapt the same concept. Neither A/B nor shoulders sit across the phone or in a detached opposite-side group.

**Utilities outside ThumbDock.** Select, Start and Menu are on a protected opposite-side utility rail for deliberate access, never in the Android bottom gesture region. This is for an **opt-in One Hand mode only**. The current Standard-mode Start and Select placement and touch bounds are explicitly frozen by #74 and stay unchanged.

**Screen first.** Use the existing measured viewport and correct game aspect. Do not shrink, mirror, crop, stretch or overpaint pixels. Left/right flips control artwork/positions, not the game or canonical brand. All hit targets need actual ≥48dp bounds; the preview reference coordinates are *not Android dp*.

## Global layout architecture
V1 state: `layout=standard|one_hand`, `hand=left|right`, persisted once. Standard mode is reversible immediately and does not lose a game state. Do not create per-cartridge, genre or other auto-switching behaviour in v1.

Later, Racing / Fighting / Sports join the same **Layout** page. Long-press a cartridge in the shelf/catalogue to open `Game Layout` and optionally choose a title-specific loadout. Future opt-in genre rules may suggest one. Precedence when explicitly enabled: **per-game manual override > enabled genre rule > global selected layout > visible fallback**. Genre rules default off. Never replace the global default silently.

## Source/engineering boundaries
Reuse P1-C2 hardware assets, Original/Pink/Green semantic colours, PadKit controls and one LibretroDroid AndroidView. No emulator, ROM, save, library identity, Android package or standard control geometry changes as part of design. The v0.2 source drawings show **illustrative 393×852 reference positions**, not tested ergonomics.

Required validation after UI engineering authorisation: true left/right one-thumb reach; GB/GBC/GBA turn-based flow; SNES four-face chords; PS1 analog+shoulder combinations; DS stylus and two screens; accidental Start/Select/Menu avoidance; 48dp hit targets, Android gestures, orientation, game changes, preference persistence, Standard-mode regression and retained progress.
