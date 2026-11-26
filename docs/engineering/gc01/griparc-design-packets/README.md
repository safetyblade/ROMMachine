# GripARC — full-controller power-user design programme
Owner direction 27 Nov 2026 | issue #379 | design branch only
Reference PR #378 at 6f68b9c3e4f81e3cabc8e5cc283ce7dea29a5b1d, still open at setup; no PivotARC full physical acceptance inferred. Rebase/transfer approved design packets to the verified engineering baseline when ready. Do not modify active PivotARC PR or runtime in this lane.

## Superseding product decisions
- Standard remains conventional native controller.
- PivotARC is one-hand/ease-of-play; preserve its existing approved art and geometry.
- Standalone GripARC is a full native performance/power-user controller. Reuse the proven full-body chassis direction, but design the entire native and programmable layout per system/mode. No arbitrary scattered G buttons, no mandatory 2x2 bank, no stretch of Standard positions. Give each console/mode a useful default preset; place additional programmable controls for reach/use with native inputs. Visible labels reflect current function, not G1–G4; internal stable slot IDs can remain. Four slots as the initial candidate based on existing storage; not a new universal count mandate. Owner can refine count/preset per profile.
- PivotARC + GripARC is a distinct later hardware assembly: a separate contained L-shaped programmable module that attaches around the original immutable PivotARC. Supersedes prior tracker statements about mounting PivotARC on standalone full-body GripARC or reusing its exact hardware. Share mapping/preset architecture, not geometry. Do not duplicate native controls; fixed original PivotARC shape, position/relative geometry, cap styling, wordmark and mappings. Rigid positioning of original module only if owner-authorized fit requires it, no redesign. Joint last; no implementation or preview claim.
- Standalone controller selection naming should eventually expose GripARC as a complete mode, while maintaining existing persisted STANDARD_GRIPARC compatibility; no migration now.

## Existing source and capability boundary
GripArcProfiles.kt blob 37ca5d1a15bca7352db8821623f78229d7828888 at reference head: four persisted slots, each up to four simultaneous distinct Android key codes; per-cartridge UUID plus legacy URI fallback, count 1–4. Current generic default prefers L1/R1/A/B and catalog is incomplete and system-level. This does not prove Turbo, toggle Hold, timed macros or every native backend. Design presets may propose these as explicitly gated new capability; no misleading labels or silent substitution. Native input catalog must become actual controller-mode/host-aware, retaining existing custom mapping and safe release/ownership. No user configuration reset. Reassignment updates short visible label, accessible full description; factory reset explicit. Simultaneous chords only as currently represented, no timed behavior inferred; optional turbo or other semantics require separate typed action contract and lifecycle verification. Keep diagnostics Advanced.

## Workflow
GBA first (prior full-body chassis proof PASS, spacing FAIL); then remaining 32 using same one-at-a-time approvals as PivotARC, group only if same geometry and mode distinctions are documented. Each packet includes native control inventory/mapping reference; power-user purpose and default preset functions; whole-pad sketch and coordinates; programmable seat placement and labels; actual input capability gates; styling; both-hand layout and fit; title tests; owner layout lock; source/CI/visual/device status independently. Use original GBA/PSP PivotARC manufactured detail as common product quality, but distinctive GripARC identity. Full-body design may use generous real area with no unnecessary gaps. Effective independent 48dp touch targets and full analogue travel, actual viewport safety, no duplicated input ownership, proper release on edit/switch/pause/menu and lifecycle, preserve saves and cartridge preferences. No implementation until design batch complete and owner authority; one consolidated engineering batch, formatting first, compile/unit/visual checks, realistic handset evidence, consolidated APK. Physical acceptance only after owner tests. Joint separate last.
## Register
| Batch | Systems | Owner design approval |
|---|---|---|
| A — Classic retro | GB, GBC, NES, SMS, GG, NGP, NGC, PC_ENGINE | 0/8 |
| B — Handheld / multi-button | GBA, SNES, GENESIS, SEGACD, WS, WSC | 0/6 |
| C — Touch / single analogue | NDS, NINTENDO_3DS, PSP, N64 | 0/4 |
| D — Modern consoles | PSX, PLAYSTATION_2, PLAYSTATION_3, DREAMCAST, GAMECUBE, XBOX, XBOX360 | 0/7 |
| E — Specialist | FBNEO, MAME2003PLUS, ATARI2600, ATARI7800, LYNX, WII, WII_U, DOS | 0/8 |
Total 0/33; GBA candidate ready for review. Existing PivotARC register remains historical; this charter supersedes outdated GripARC/JOIN geometry decisions only.
