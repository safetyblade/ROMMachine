# GA-R01 engineering status

Baseline is the verified install-over commit `7c4aefa4` (build `91714637`),
descended from master `1e2e75c8`. Design authority is immutable commit
`5476f12400b4a6d0f0288aaee9a7472424afeeba`. Its manifest contains 33 systems,
53 configurations and 106 configuration/orientation combinations.

## Current coverage

Standalone runtime and production renderer coverage is 20 of 33 systems and
24 of 53 configurations (48 orientation fixtures). Handset and APK acceptance
remain open.

F01 adds measured cap growth and a shared production hardware renderer.
Landscape reuses the production CARTDECK surround and main brand. The
standalone landscape aperture reserves space for the measured central utility
rail and respects the system bottom inset.
Both orientations have dedicated coordinates for the standalone runtime entries
below. The solver rejects layouts when the native targets, programmable targets
and measured gameplay viewport cannot coexist. Current geometry and production render fixtures
cover the listed digital and analogue modes. The Kotlin checks are not a handset result.

| System | Configuration | State |
| --- | --- | --- |
| GBA | base | Standalone runtime and dedicated preview source |
| GB | base | Standalone runtime and dedicated preview source |
| GBC | base | Standalone runtime and dedicated preview source |
| NES | base | Standalone runtime and dedicated preview source |
| SMS | base | Standalone runtime and dedicated preview source |
| GG | base | Standalone runtime and dedicated preview source |
| NGP | base | Standalone runtime and dedicated preview source |
| NGC | base | Standalone runtime and dedicated preview source |
| PC_ENGINE | digital2 | Standalone runtime and dedicated preview source |
| PC_ENGINE | digital6 | Six-button native support and standalone layout pending |
| SNES | base | Standalone runtime and dedicated preview source |
| GENESIS | digital3 | Standalone runtime and dedicated preview source |
| GENESIS | digital6 | Standalone runtime and dedicated preview source |
| SEGACD | digital3 | Standalone runtime and dedicated preview source |
| SEGACD | digital6 | Standalone runtime and dedicated preview source |
| WS | guest_horizontal | Pending |
| WS | guest_vertical | Pending |
| WSC | guest_horizontal | Pending |
| WSC | guest_vertical | Pending |
| NDS | base | Pending |
| NINTENDO_3DS | base | Standalone runtime and preview; touchscreen and guest input await handset verification |
| PSP | dpad_primary | Standalone runtime and dedicated preview source |
| PSP | analogue_primary | Standalone runtime and dedicated preview source |
| N64 | base | Pending |
| PSX | digital | Standalone runtime and dedicated preview source |
| PSX | dualshock | Standalone runtime and preview; conditional L3/R3 guest dispatch remains unverified |
| PLAYSTATION_2 | base | Pending |
| PLAYSTATION_3 | base | Pending |
| DREAMCAST | base | Standalone runtime and dedicated preview source |
| GAMECUBE | base | Standalone runtime and dedicated preview source |
| XBOX | base | Pending |
| XBOX360 | base | Pending |
| FBNEO | digital2 | Pending |
| FBNEO | digital3 | Pending |
| FBNEO | digital4 | Pending |
| FBNEO | digital6 | Pending |
| MAME2003PLUS | digital2 | Pending |
| MAME2003PLUS | digital3 | Pending |
| MAME2003PLUS | digital4 | Pending |
| MAME2003PLUS | digital6 | Pending |
| ATARI2600 | base | Standalone runtime and dedicated preview source |
| ATARI7800 | base | Standalone runtime and dedicated preview source |
| LYNX | base | Standalone runtime and dedicated preview source |
| WII | sideways | Pending |
| WII | classic | Pending |
| WII | nunchuk_stick | Pending |
| WII | nunchuk_touch | Pending |
| WII | gamecube | Pending |
| WII_U | gamepad | Standalone layout pending; shared GamePad touch fix in source |
| WII_U | pro | Standalone layout pending; shared GamePad touch fix in source |
| DOS | gamepad | Pending |
| DOS | keyboard_action | Pending |
| DOS | mouse | Pending |

## Shared implementation and remaining work

- Existing cartridge chord lists remain the persisted native identities.
  Typed Hold, Turbo and game-menu assignments extend the same profile store.
  Factory assignments apply only to an unconfigured cartridge or explicit reset.
  GripARC selection supports cartridge overrides and a global default for the
  implemented systems. Per-native-mode programming needs further work. Hold
  uses the existing shared latch and pressed-state rendering. The PSX and 3DS
  factory presets use that same latch and active state is cleared when inputs
  are neutralized.
- Cemu commit `444c585202302a7f4b319bafb4cf72156a8cac8b`
  computes a full-size DRC viewport in TV-only single-display mode.
  The patched core guards touch against `LibretroDRC_ShouldRenderScreen(true)`,
  then uses its existing DRC subrectangle mapping. The Android host tracks
  a fresh finger contact and the native host gates the applied controller.
  The matching Cemu core and Vulkan host passed CI-01 run `45980505728`;
  gameplay and consolidated APK verification remain open. GamePad and Pro GripARC layouts are pending.
- Wii Nunchuk Aim & Fire and shared IR routing are pending. Pointer ownership
  must come from a fresh, valid contact on the rendered image or real IR-stick
  engagement before the firing owner is acquired. Existing independent Nunchuk
  movement and manual fire are retained.
- Automatic, guided and individual physical-controller mapping from the baseline
  remain in place. Native-host typed action dispatch, physical programmable
  assignments, remaining host and controller modes, game verification and the
  consolidated APK are pending.
- Conditional specialist controls need native dispatch and fit evidence before
  activation. Joint is outside this implementation.
