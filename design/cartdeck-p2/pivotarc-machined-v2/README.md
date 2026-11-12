# CARTDECK — PivotArc Machined V2
**Approved visual direction:** 12 November 2026, owner. **Scope:** Point 3 — tactile/manufactured refinement, separate from concurrently running PivotArc A/B Hold Controls and GripArc native implementations. **Status:** DESIGN ASSET SOURCE ONLY.

## Canonical approved artwork
`CARTDECK_PivotArc_Tactile_V2_Concept.svgz` is the **exact approved 1440×920 editable SVG illustration**, losslessly gzip-compressed and committed to preserve source bytes (original SVG SHA-256 `a308876d583e2f1d5b606e95e32c3366c02ca60aa44424b328cc6024b58f0faf`). It can be opened in an SVG-capable editor supporting SVGZ, or extracted using:

```sh
gzip -dc design/cartdeck-p2/pivotarc-machined-v2/CARTDECK_PivotArc_Tactile_V2_Concept.svgz > /tmp/CARTDECK_PivotArc_Tactile_V2_Concept.svg
```

`pivotarc-v2-material-cues.svg` provides separate vector reference shapes for a chamfered cradle, inset D-pad well, button collar, L/R seat and miniature illuminated hold switch. These are MATERIAL CUES, not a replacement for the approved whole-comparison art or safe-fit solver.

`pivotarc-machined-v2-contract.json` carries visual/non-regression guardrails. `test_pivotarc_machined_assets.py` validates exact source extraction/hash and semantic cues (not gameplay).

## Implement on top of current native work, never alongside conflicting edits
The owner has authorised visual V2 direction. This PR **does not implement Android UI** or accept a physical APK. Elliot owns parallel work on #122 PivotArc hold controls and #123/#125 GripArc; do not overwrite or rebase those source files while those PRs are active. Native implementation should begin from the latest accepted working branch only after coordination, preserving the current control geometry.

### Visual refinements
1. Rebuild `CartdeckPivotArcModule` decorative drawing with two-level cradle (outer machined bevel → darker recessed deck), edge shadow, proper physical mounting and protected corner fixings; no flat monochrome panel.
2. Add inset directional *well* behind existing `ControlCross` / `LemuroidCrossForeground`; cross art retains raised depth and press feedback without changing D-pad diagonals or input IDs.
3. Give A and B circular machined seats/collars and preserve raised cap, specular highlight and pressed travel from Standard, while not modifying their approved bounds.
4. Improve L/R shoulder seats/rails to match Standard's elongated raised controls (where current profile supports).
5. Approved A/B hold toggles remain immediately adjacent to their own buttons. Their tiny machined switches and corresponding illuminated face rings reflect actual held state only. Do not turn a decorative ring into a second input source.
6. Keep PivotArc-only compact Start/Select/Menu grouping as implemented by the separate layout task, with individual raised mounts and correct safe-area limits. **Standard Start/Select/Menu remain untouched.**
7. Retain semantic Original/Pink/Green/custom roles across all material layers (underlays, rims, shadows, engravings, highlights, LEDs), low-contrast variants and reduced motion.

### Engineering boundaries
- Maintain existing `PivotArcLayoutProfile` and the actual measured `PivotArcPositionSolver` hit rectangles; if the native branch evolved these, respect its current authoritative geometry. Artwork is pointer-transparent. No touch hit area or routing changes as part of visual V2.
- Maintain 48dp+ effective active targets and no intersection with game screen, gesture edges, utility keys or potential GripArc module. Decorative badge/screw can be suppressed on compact layouts; functional controls cannot be removed.
- No emulation-core replacement, gameplay/control remapping, library indexing, save system or signing change.
- Render native layers from approved vectors/Compose with existing tokens. The 1440×920 illustration is **not** a pixel-perfect gameplay overlay.
- Keep a narrow separately reviewable change and preserve rollback.
- Coordinate merge/rebase only after active #122 and GripArc implementation changes are reconciled.

## QA criteria
Physical-device screenshot compare **Standard vs PivotArc** in the same active theme. Test right/left mirrored PivotArc, Original/Pink/Green/custom, A held/B held/dual hold, non-held state, compact screen, Start/Select/Menu, SNES four-face expansion, D-pad diagonal responsiveness, renderer viewport integrity, and session save/reopen. Test the current APK with V2 material drawing against the previous accepted APK. All source validation is **not** Android runtime acceptance.

**Tracking:** #74 PivotArc display/hardware; #13 tactile controls; #58 PivotArc; #122 hold inputs; #123 GripArc.