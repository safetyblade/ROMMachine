# P1-C2 — Approved manufacturing-screen implementation

**Owner approval:** 9 November 2026. **Implementation branch:** `feature/cartdeck-p1c2-standard-four-three-ds`.

## Decision

- For conventional single-screen systems: responsive **4:3 manufactured screen opening**, with the existing LibretroDroid renderer containing true 10:9, 3:2, 4:3, 16:9 or variable video inside it. Internal letterboxing on non-4:3 content is expected; it must not be mistaken for a failed video renderer.
- For Nintendo DS: preserve the physically tested **v2101** expanded viewport and stylus behaviour.
- 3DS: conservatively retains expanded dual-screen allocation because this system family is also multi-screen; **not independently device-tested**.
- Landscape: retain the existing overlay/non-overlay layout, with no new renderer, control or save changes.
- Thin the physical bezel from approximately 15dp to **7dp**; restore the full CARTDECK C-slot/wordmark above the standard screen and compact C-slot in a safe footer seat when DS uses the header.
- Reintroduce restrained mechanical/speaker/indicator treatments only on chassis surface outside the aperture.
- Custom camouflage, smiley stamps, personal logos and the future player-facing faceplate editor remain **P1-C3 #75**.

## Renderer boundary

Inspected existing `GLRetroView.viewport`: it is a *setter*, not a getter for the core's live display aspect. The current application does not receive live geometry through its frame-rendered event. Consequently this implementation intentionally uses the approved standard physical aperture instead of fabricating frame geometry. The game itself remains scaled by LibretroDroid in that aperture. The JS/Compose shell measures the actual **allocated screen opening**, not individual nonblack pixels.

## Code boundary

- `GameScreenLayout.kt`: standard 4:3 portrait allocation, dual flexible exception, unchanged landscape.
- `CartdeckViewportPolicy.kt`: simple system-class policy and deterministic fit math.
- `MobileGameScreen.kt`: pass the system profile to the existing layout.
- `BaseGameScreenViewModel.kt`: expose read-only `systemId`, no game data mutation.
- `CartdeckConsoleShell.kt`: 7dp bezel, protected top logo, compact DS footer logo and physical detailing.
- `CartdeckViewportPolicyTest.kt`: system policy, 4:3 geometry, invalid dimensions.

## Acceptance

Hosted build proves Kotlin/resource/test/APK integrity only. Owner and Marcus must still verify real GB/GBC/GBA/PS1/N64/PSP/DS/3DS where supported, orientation, PS1 variable video, DS touch, safe insets, button clearance and existing installed save continuity. 3DS has **NOT TESTED** status; do not claim core support from the layout policy alone.
