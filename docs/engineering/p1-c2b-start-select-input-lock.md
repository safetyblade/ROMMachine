# CARTDECK P1-C2B — PadKit input lock / baseline

Issue #74. Baseline master at P1-C2B branch creation: `cfd72033c944cfffde79a363de782694ea43fa05` (merged approved asset PR #80).

## Immutable functional positions
- `lemuroid-touchinput/src/main/java/com/swordfish/touchinput/radial/layouts/shared/Controls.kt`: `SecondaryButtonSelect(position)` uses `Modifier.radialPosition(120f - 30f * position)`; `SecondaryButtonStart(position)` uses `Modifier.radialPosition(60f + 30f * position)`.
- GBA `GBALeft` / `GBARight`: `SecondaryButtonSelect(position = 2)` and `SecondaryButtonStart(position = 2)`.
- NDS `MelonDSLeft` / `MelonDSRight`: exactly the same `position = 2`.
- PS1 DualShock `PSXDualShockLeft` / `PSXDualShockRight`: exactly the same `position = 2`.
- No modification of these source lines or of PadKit IDs `KEYCODE_BUTTON_SELECT`, `KEYCODE_BUTTON_START`, size, padding, radial rotation or touch listener is authorised.
- DS left microphone `KEYCODE_BUTTON_L2`, screen/layout `KEYCODE_BUTTON_THUMBL`, Menu `KEYCODE_BUTTON_MODE` remain as existing. PS1 analog key configuration and DS stylus mapping remain untouched.

## Instrumentation and QA
P1-C2B-1 adds passive whole left/right PadKit group bounds collection solely for decorative seam exclusion. It is *not* a before/after Start/Select per-button measurement and cannot be passed off as such. P1-C2B-2 may add passive per-input `onGloballyPositioned` reporting, without input position or mapping changes.

**Actual owner-device gate at C11:** record Start/Select screen-root bounds on the current installed pre-C2B APK and integrated APK using the same screen dimensions/orientation/touch scale/margins/rotation, for standard A/B, ABXY, DS and PS1 analog. Any movement introduced by C2B is a defect. Do not assert this gate from source inspection alone.

## Unchanged policies
`CartdeckViewportPolicy.kt` and `GameScreenLayout.kt` are unchanged; single `AndroidView` Libretro renderer, built-in DS expanded viewport, PadKit controller composables, emulator inputs, game saves and application identity are untouched.

The reference `design/cartdeck-p1c2/hardware/layout-contract.json` is illustrative geometry only. Native screen surround is 7dp *outside the measured renderer*. Canonical C-slot/wordmark remain separate decorative art from `design/cartdeck-v03/brand`.
