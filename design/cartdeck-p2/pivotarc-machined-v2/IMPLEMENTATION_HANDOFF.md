# CARTDECK | Elliot Shaw — PivotArc Machined V2 Native Implementation Handoff
**STATUS:** PREPARED, DESIGN APPROVED, native code **not** implemented in this asset PR.
**Owner:** Jonathan Prestwich · **Director:** Cassia Rowan · **Visual:** Sienna Vale · **Engineering:** Elliot Shaw · **Architecture:** Daniel Mercer · **QA:** Marcus Reid
**Related:** #74 / #13 / #58, active PivotArc Hold Controls #122; concurrent GripArc #123/#125.

## Authorised outcome when implementation starts
Apply owner-approved *PivotArc Machined V2* tactile surface treatment so PivotArc matches the perceived depth of Standard CARTDECK. Deliver a separate, focused native Compose implementation with verified Android QA APK. This is a **cosmetic/tactile feedback** package, not a game-input or emulation change.

## Pre-implementation integration gate
1. Inspect newest master and source/PR heads for PivotArc A/B Hold Controls and GripArc. Do not start on a stale PivotArc source snapshot.
2. Use `CARTDECK_PivotArc_Tactile_V2_Concept.svgz` as the approved visual comparison. It decompresses to the exact owner-approved source vector, SHA-256 `a308876d583e2f1d5b606e95e32c3366c02ca60aa44424b328cc6024b58f0faf`.
3. Use `pivotarc-v2-material-cues.svg` for detachable material details only. It's a set of visual references, not tappable native SVG hotspots.
4. Keep code on an **independent visual-only branch**; reconcile and merge after both the hold-control implementation's functional geometry and the current master branch state have been inspected.
5. Document the real version, commit and APK used for comparison; avoid status claims on unfinished parallel code.

## Implementation order
**A. Measured cradle:** In `CartdeckPivotArcModule.kt`, replace flat plate effect with a dual-depth manufactured assembly: exterior bevel, inner recess, contact shadow, edge reflection and guarded physical fixings. Derive from the measured union of actual active PivotArc controls, excluding utilities and gameplay viewport. Existing PivotArcWordmark remains correctly branded and optional if space is insufficient.

**B. D-pad:** Create a recessed directional well beneath the existing D-pad foreground. The existing `ControlCross` continues to own all pointer and diagonal input. The well must not affect the touch rect or create dead zones. Keep the center pivot, chamfer and raised visual travel.

**C. A/B face controls:** Add or improve independent machined collars and raised face caps with shadow, thin specular highlight and pressed travel. The A/B labels remain clear under all edition themes. Continue using real pad pressed state.

**D. L/R:** Add equally manufactured shoulder-key mounting seats, respecting L/R visual orientation and actual hit bounds. Do not relocate L or R.

**E. Hold toggles:** Integrate V2's miniature switch housing into the **approved nearby-A and nearby-B toggles** created under the separate Hold Controls project. Accent illumination on toggle and corresponding A/B ring is driven solely by actual held state. Do not reimplement latch semantics. Do not change the HOLD A/B touch regions or their control-input ownership.

**F. Utility keys:** Use small raised hardware wells for the PivotArc Start/Select/Menu cluster *as it actually exists when implementation starts*. Do not use illustrative poster pixel coordinates. Standard Start/Select/Menu positions and appearance are not changed.

**G. Theme support:** Preserve Original, Pink, Green, custom named palettes, high-contrast and reduced-motion support. Use `CartdeckEditionTokens` and `LemuroidPadTheme` semantic roles for chassis, secondary, bezel, button, trim, onButton, onChassis, focus and accent. Materials must remain layered even when named custom colours are similar.

## Hard constraints
- No changes to emulator core, rendering resolution/viewport, ROM scanning, file permissions, package ID, signing, saves, save states, firmware or library.
- No changes to PadKit virtual key IDs or controller input event handlers.
- Decorative layers must be pointer-transparent. Existing touch bounds must remain 48dp+ and safe.
- The module must accommodate future X/Y/L2/R2 and left/right mirrored states without clipping/overlap.
- Keep gameplay viewport visible and Android gesture zones clear.
- PivotArc may be combined with GripArc; new material does not extend outside the safe measured envelope.
- Do not import poster as a fullscreen bitmap or stretch it over controls. Compose/vectorise individual material cues.
- Avoid global changes to `LemuroidButtonForeground.kt` that would unintentionally alter Standard. Scope visual additions to PivotArc if necessary.

## Verification
**Automated:** asset SHA validation, geometry no-crossing invariants, Compose screenshot/golden if available, theme/selected-state snapshots, structural/non-pointer art tests and normal Android compile/lint. Checks must be run in CI on the **actual integration branch**, not just design PR.

**Real Android device / Marcus:** Compare exact same game/screen under Standard vs PivotArc. Test right/left mirroring, A/B unheld/held/dual held, all three presets plus one low-contrast named custom style, D-pad diagonals, Start/Select/Menu, shoulder use, compact-screen fit and settings/saves/regression. Verify performance and responsiveness unchanged. Verify GripArc coexistence where supported. Separate findings: PASS/FAIL/NOT TESTED.

**Owner acceptance:** A native working APK and side-by-side screenshots are required; a vector illustration is not proof of gameplay implementation.

## Deliverables
Focused GitHub PR, source commit, lint/unit/CI results, signed install-over compatible ARM64 debug APK workflow/artifact link, screenshot evidence, known differences vs owner-approved source, rollback notes, and Marcus QA handoff. No unrelated emulator/core or layout changes.

**Stop condition:** If active hold-control or GripArc development creates conflicting geometry, preserve their accepted functional work and present a precise conflict with a safe cosmetic fallback; do not silently move controls or block gameplay.
