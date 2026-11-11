# CARTDECK | GripArc V1 — full-L constant hardware assets
**Design assets:** 12 November 2026 · **Workstream:** #123 / #125 / #34 · **Name:** GripArc (provisional).

This folder is **source artwork and a geometry contract**, not native Kotlin implementation or an emulator input adapter. The owner explicitly requested assets before the implementation prompt.

## Non-negotiable visual invariant
**The complete L-shaped manufactured attachment is identical whether the game activates 1, 2, 3 or 4 buttons.**
The shell NEVER grows, shrinks, shifts, or changes corners as the button count changes. An unused button is **absent** rather than a visible disabled/ghost button, and no hidden slot catches a touch.

| Count | Side arm (vertical) | Bottom arm (horizontal, wrapping inward) |
| --- | --- | --- |
| 1 | G1 | Empty but full L present |
| 2 | G1, G2 | Empty but full L present |
| 3 | G1, G2 | G3 |
| 4 | G1, G2 | G3, G4 |

G1/G2 and G3/G4 each retain the same absolute centers in local module coordinates at **every** count. These do not dynamically centre or spread. Left/right variants mirror X positions as a single unit; functional IDs never swap. Left and right each have a permanent module silhouette.

## Button design
All four GripArc buttons are **elongated L/R shoulder-style keys**, rather than circular A/B face buttons. Source visual: **76 × 38 units**; engineering minimum hit box **48dp**, illustrative envelope **80 × 52 units**. Real Android measurement must scale the entire hardware and targets safely; the SVG viewBox is not physical pixels or guaranteed dp. Raised bezel, pressed travel, small directional glint and restrained semantic-accent edge match CARTDECK/PivotArc tactile hardware. No unapproved hard-coded brand palette in native rendering.

## Files
- `griparc-shell-right.svg` and `griparc-shell-left.svg`: same complete constant L with no action buttons.
- `griparc-button-rest.svg`, `griparc-button-pressed.svg`: elongated L/R-style source assets; labels come from app layout/input data.
- `griparc-{right,left}-{1,2,3,4}.svg`: eight illustration compositions, all reusing identical shell geometry per side and showing only the active button subset. Not screenshots of the native app.
- `griparc-v1-manifest.json`: versioned slot, active-count, mirrored-coordinates, hit-target, palette and behavioural contract.
- `preview.html`: visual asset comparison for both sides and all four counts.
- `test_griparc_assets.py`: SVG/manifest validation ensuring a constant shell, fixed slots and absence of inactive button art.

## Control workflow (locked)
**Library > hold cartridge > Hold Controls** offers exactly:
1. Standard
2. PivotArc
3. Standard + GripArc
4. PivotArc + GripArc

The library selects only the control mode. **All GripArc button-count and key/chord programming must happen in-game** in Controls > GripArc, without returning to the library. Choosing 1, 2, 3 or 4 buttons never moves existing keys. Left/right attachment and optional *whole-module* safe position can be selected in-game and saved for the title. Preserve its profile across app restart, indexing and signed APK updates. Physical pressed-state and illumination follow input state, not just stored preference.

## Implementation guardrails
- Use existing PadKit/game-system button identities. G1–G4 are aliases for **user-configurable** single or simultaneous button combinations; do not invent emulator key IDs or timed macros.
- Use the existing cartridge stable identity approach (shelfGameKey from system+file URI) and a separate versioned local control-profile store; never use Room Game.id as sole durable key. Reindex and game changes must not leak profiles.
- Initial art does not capture pointer events. Native Compose/PadKit handlers use the manifest's unique target geometry, with 48dp+ hit boxes and non-overlapping protection areas. No Android gesture collision or game viewport cropping.
- The complete L housing persists even for G1-only, and users can assign a game both PivotArc **and** GripArc provided native safe-fit rules are met.
- Standard control Start/Select remains in its established position. The independent PivotArc utility layout trial is not altered by these assets.
- Theme roles are conveyed by `data-cd-role` attributes: `chassis, secondary, bezel, button, onButton, onChassis, accent, trim, focus`. Resolve colours through existing CARTDECK appearance tokens (Original/Pink/Green/custom); SVG fallback colours are illustrative only.
- If no safe fit exists, explain the constraint and allow return to the safe layout; never silently obscure gameplay or change slot topology.
- Keep assets on a standalone reviewable design PR. Do **not** merge unrelated emulator, ROM indexing, save, signing or gameplay changes.

## Asset acceptance
Run `python3 design/cartdeck-p2/griparc-v01/test_griparc_assets.py`. Before native implementation: Sienna compares reference art to approved hardware, Cassia accepts actual in-game ergonomic placement, Daniel checks safe-area and input ownership, Marcus tests real Android devices. Asset validator success is **not** gameplay or native touch QA.
