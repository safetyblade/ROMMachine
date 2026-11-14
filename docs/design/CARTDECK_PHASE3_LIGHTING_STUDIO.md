# CARTDECK | Lighting Studio Phase 3

**Product owner:** Jonathan Prestwich
**Design:** Sienna Vale
**Engineering:** Elliot Shaw
**Architecture:** Daniel Mercer
**Independent QA:** Marcus Reid
**Track:** [#223](https://github.com/safetyblade/ROMMachine/issues/223)

## Product structure
Phase 3A introduces configurable **static hardware-inspired lighting** in the existing Appearance Studio. Phase 3B (reactive lighting) remains separate pending an authoritative gameplay/input event-source review.

### 3A: implemented in source
- Default **Off** preserves the shipped appearance and does not draw a separate overlay.
- **Static** draws passive LED-like trim strips only in safe, measured manufactured shell regions.
- Player controls: intensity 0–100%, **Follow Appearance Accent** or a selected colour, and independent Screen Surround / Side Rails / Control Wells / Cartridge Shelf Rail zones.
- Real implementation uses current `CartdeckConsoleShell` geometry and `ShelfRail`, not an extra emulator view. Console surface lighting requires measured video bounds and never paints in the video, controller hit boxes or brand seat. Some constrained layouts intentionally hide individual strips.
- On portrait/landscape/tablet the same recipe scales to available physical regions; controller-first fullscreen game hides the shell as previously approved; shelf rail can still show lighting in compatible library views.
- Colour, intensity and zone choices are static. No timers, coroutines, audio reading, screen sampling or gameplay engine hooks.
- High Contrast suppresses decorative lighting while preserving existing text/focus colours. Reduced Motion leaves static mode unchanged; no animation exists in 3A.

### User flow
Appearance → Current preview → My Appearances → **Lighting Studio** → Off or Static → Intensity, Colour, Zones → Apply. Cancel discards the draft; personal profiles can be saved, copied, selected or updated. Existing factory/curated finishes remain selectable and immutable as catalogued; personal variants are player-owned.

### Compatibility
Optional `lighting` JSON under the existing appearance and named-profile fields in `cartdeck/shelf-layout-v1.json` without raising the existing appearance V2 schema or modifying saved shelf sections. All legacy files load default Off. An older APK rewriting appearance metadata may drop optional newer lighting choices, but game inventory, cartridge facing, ROMs, SRAM/saves and controller preferences are unchanged.

### Release gates before merging
1. Source Kotlin lint and unit tests for legacy decode, profiles, copy/reset, user choices, input exclusions, high contrast.
2. Integrated APK build with native engine matrix intact, signed install-over and backup/rollback evidence.
3. Marcus physical QA: Original/Pink/Green/Stealth Acid, mixed spine/front shelf, 393dp portrait, landscape phone, tablet, DS viewport, controller-first library, all four zone choices, per-mode disable, orientation, TalkBack, 48dp actions, high contrast.
4. Daniel verifies graphics drawing uses measured video and native controls, no frame scheduling/battery impact or save data mutation.
5. Sienna checks static visual intensity stays restrained and legible on all shell finish palettes.
No automatic merge or release on source-only PASS.

### 3B: separate design lane (not implemented)
Real reactive lighting requires independently verified input or emulator lifecycle event hooks, bounded update frequency and battery impact, reduced motion, and an explicit OFF/STATIC fallback. A timed breathing effect is not gameplay-reactive and must not be labelled as such. Do not begin until Daniel/Marcus approve a truthful event source.
