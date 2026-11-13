# GripArc — owner / Marcus acceptance matrix (issue #135)

**Physical status: NOT TESTED in this session. Keep #135 open.** Record handset,
Android version, width × height in dp, gesture/cutout insets, APK SHA/version,
source commit and CI run. Use controller/engine paths and legal test content;
no particular commercial title is required. Owner handset first, then narrower
classes where available. Mark each case PASS / FAIL / NOT TESTED and attach a
screenshot or short input trace for failures.

| Path | Required combinations | Acceptance |
| --- | --- | --- |
| Standard + GripArc | PS2, GameCube, Dreamcast, GBA, SNES; left/right × G count 1/2/3/4 | Full L; fixed G slots; no inactive ghosts; D-pad, faces, shoulders and only the native analogs work together; no viewport obstruction |
| PivotArc + GripArc | GB/GBC, GBA, SNES; left/right × G count 1/2/3/4 | One notched island + full L; Hold A/B indicators and programmable chords work simultaneously; future X/Y reserve produces no inactive buttons |
| Hold / chord ownership | Hold B + D-pad + ordinary B + G chord sharing B; Hold A/B + two overlapping G chords; physical + touch + GripArc | Releasing one owner retains other owners; final release sends one UP; no stuck key after menu/background/fallback |
| Safe fit and themes | Both modes; Original/Pink/Green/custom; actual gestures/cutouts | At least 48dp targets; no clipping, hidden controls or touch overlap; housing and inputs align; viewport size/position unchanged |
| Session fallback | Short safe deck / large insets; active holds/chords before loss of fit | Clear explanation; Standard button AND back-dismiss provide working Standard inputs; both accessory owners release; rotation does not silently restore accessories during that session |
| Side choice | Force preferred-side failure, then explicitly choose the other side | No silent side switch; opposite-side action is explicit and session-only; saved side remains unchanged after reopening |
| Cartridge persistence | Save counts/mappings/side for two cartridges; reopen, reload, Resume and Restart | Each cartridge restores its own saved mode/count/chords/side; session overrides do not persist |
| Install-over continuity | Install verified newer APK over existing owner build without uninstall | Same package/certificate; newer version; library, saves, save states, VMU and cartridge settings retained |
| Standalone regressions | Standard-only and PivotArc-only before/after, same handset | Existing layout, viewport, mappings, holds, engines and save behavior unchanged |

## V2.2 refinement checks

The compact runtime contract is now `griparc-v22-manifest.json`, not a scaled V1
assembly. For each matrix case, verify the smaller full L, italic GripArc mark,
fixed slots at counts 1 and 4, and illuminated explicit Hold A/B legends in the
joint mode. Compare each combined native controller to its standalone version:
all symbols, face compass, shoulder/trigger/stick roles, utility buttons, wells,
highlights and pressed states must remain identifiable. Include GameCube Z,
C-Stick and programmable half-trigger codes, Sony digital/DualShock variants,
Dreamcast's single stick/native trigger pair, and cartridge switching. Unmapped
profiles must explicitly retain Standard instead of showing a partial controller.
**V2.2 physical fit, gameplay, themes and install-over remain NOT TESTED here.**

## Source fixtures and engineering gates

The tests derive the portrait viewport from the production opening: 22dp side
margins, 84dp top, and 4:3 aspect. The fixtures conservatively reserve the whole opening, including any top-only
cutout padding; production continues to use its measured viewport.
Insets define the safe rectangle independently;
they never shrink the game viewport to force a controller fit.

| Screen dp | Viewport (left, top → right, bottom) | Safe area including 8dp edge gap |
| --- | --- | --- |
| 320 × 640 | 22,84 → 298,291 | 8,32 → 312,608 |
| 360 × 740 | 22,84 → 338,321 | 8,32 → 352,708 |
| 393 × 852 | 22,84 → 371,345.75 | 8,32 → 385,820 |
| 393 × 852, asymmetric insets + 32dp top cutout | 22,84 → 371,345.75 | 24,40 → 377,812 |

Every reported fit is checked against real target bounds, complete dock material,
all four slots, visual sizes and painted island-outline extents. Owner-class
fixtures must fit the supported paths; smaller/shorter fixtures may reject and
exercise the explicit Standard session fallback. Fixture safety is not physical
acceptance of every phone in that width class.

The source gate includes geometry, ownership/fallback, standalone and Standard
combined regressions plus targeted Kotlin formatting. The existing complete APK
workflow remains the integration gate. A scoped dependent job marks **PR #152**
ready for engineering review only after that workflow succeeds and verifies the
PR head has not changed. No polling or auto-merge is used. Ready for engineering
review does not close this acceptance matrix or issue #135.
