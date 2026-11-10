# CARTDECK | Sienna V09 — unified manufactured shell + PivotArc

**Design approval:** 10 November 2026, owner-approved Sienna concept sheet across Original, Green and Pink.
**Scope:** All CARTDECK native gameplay views, every preset edition and every named custom colour profile; PivotArc is a conditional extension to the manufactured console.
**Parent:** #58 / P2-C1; source PR #107; this design evolves the common P1-C2 hardware foundation without merging unrelated historical design branches.

## Locked owner decisions

1. Preserve the CARTDECK C-slot and full wordmark; strengthen the upper half with a low-profile cartridge crown, a shallow bay slot, three vent cuts per side and a single central status accent. Avoid any element crossing the real game viewport.
2. Original, Green and Pink preserve **exactly the same geometry and engraved detail**. Named custom colour themes are equally full-featured: shells, bevels, grilles, indicator slots, seats, lip and shadows cannot disappear or become flat one-colour silhouettes.
3. PivotArc retains the owner-approved Candidate C **D / L / B / A / R** control relationships and independent hand mirror. A peripheral **mounted control cradle** with four low-profile fixings, machined edges and selective accent light distinguishes it from Standard. It lives underneath existing PadKit targets; never a new touch layer.
4. The approved **PivotArc** italic wordmark (capital P and A) is a recognisable product sub-brand. Present it as a premium identification badge on the module when clear space permits, and in Settings → Layout. The CARTDECK mark remains primary and larger. The native text treatment is the first vector-safe interpretation, not a declaration that the complete artwork in the concept image is embedded as a bitmap.
5. Neon hierarchy:
   - Primary: cartridge crown and short top-corner bars
   - Secondary: real viewport bezel lip and functional status indicator
   - PivotArc: two restrained module perimeter light segments
   - Control seats: minimal accent ring under selected action keys; avoid bright uniform glow on every edge
   All neon follows accent semantic tokens and is not hard-coded teal across coloured editions.
6. Do **not** enlarge a fixed five-button module only. Preserve space for **four additional common functional inputs**, initially X/Y and L2/R2, with conditional console mappings. Future analog / C / six-button controls must use the same data-driven geometry mechanism. A game is unsupported until **all** required inputs are functionally mapped and verified.
7. On compact phones prioritize actual game pixels and safe 48dp+ touch targets. A module badge/corner screw/line is expendable; functional controls are not. If required controls cannot fit without unsafe intersections, show the existing compatibility fallback. Never silently omit a required button.
8. Keep Standard Mode Start/Select positions and hit bounds untouched. No change to renderer, saves, Android identity, ROM library or cores.

## Native implementation boundaries

- Shared `CartdeckConsoleShell`: cartridge crown and measured accent elements only when there is sufficient *actual* top space; the old C-slot, shell silhouette, viewport hole and decoration remain.
- Shared `CartdeckHardwareSeats`: factory asset families for seats, rails, speakers and indicators; selected custom colours applied with **luminance-preserving colour blending**, rather than a flat colour replacement; conditional module overlay only for valid PivotArc geometry.
- `CartdeckPivotArcModule`: derives physical plate from the measured set of all active gameplay control rectangles, excluding Start/Select/Menu. Decorative structure has no pointer modifiers. The plate is absent when it would collide with the viewport/utility rail. Additional four buttons influence its extent automatically.
- `PivotArcLayoutProfile` + `PivotArcControlGroup`: existing conditional controls; prepare X/Y/L2/R2 IDs but do not claim unsupported systems to be working. Preserve Candidate C centres, absolute safe-area solver and handedness.
- `PivotArcWordmark`: one reusable composable for module/settings; adapt stroke/colour to edition and custom palette with readable contrast.

## Acceptance matrix

| Case | Requirement |
|---|---|
| Standard 2-button GB/GBC/GBA | Restored top crown, full branding, C2 grilles/indicator/seats and existing Start/Select input locations |
| Standard 4-button SNES/PS1 | Same full manufactured shell, no change to control mapping/size semantics |
| Dual analog / PSP / PS2 extensions | New shell artwork must not occupy existing PadKit input bounds |
| Nintendo DS | Preserve both game screens; upper crown may hide when real viewport leaves insufficient headroom; compact C-slot fallback remains |
| PivotArc GBA / SNES | D/L/B/A/R relationship and existing native mappings; module attached, scaled keys, branded and fully detailed |
| Four extra controls | X/Y/L2/R2 rectangles included in plate geometry and safe-area test; modules activate only with working console adapters |
| Original / Green / Pink | Full light/grille/engraving detail and contrast on every preset |
| Custom palette | Change chassis/secondary/bezel/button/accent separately; bevels, vents, shadows, indicator and seam textures must stay legible |
| Appearance switching | Custom profile → factory → named custom never deletes source material effects or changes game pixels |
| One-hand testing | Left/right mirror, no hidden touches, 5 minute single-hand play, no spurious buttons, persistence and Standard fallback |

## Checkpoint and evidence rules

- Visual mock-ups are accepted as direction, not literal Android resource files. All Android art must be drawn from semantic colours, existing controlled vectors and safe geometry; no screenshot used as the functional control surface.
- CI success proves Kotlin tests and APK assembly, not owner-approved visual appearance or physical-input ergonomics.
- Compare actual phone screenshots of Standard GBA, Standard SNES, DS and PivotArc GBA, on all 3 presets **and a custom high-contrast theme**.
- Keep this as an independent review branch based on the current PivotArc branch. No PR merge until independent Marcus QA and owner gameplay/visual inspection.
