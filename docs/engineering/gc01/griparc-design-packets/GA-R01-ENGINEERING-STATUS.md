# GA-R01 engineering status

Baseline is the verified install-over commit `7c4aefa4` (build `91714637`),
descended from master `1e2e75c8`. Design authority is immutable commit
`5476f12400b4a6d0f0288aaee9a7472424afeeba`. Its manifest contains 33 systems,
53 configurations and 106 configuration/orientation combinations.

## Current coverage

The consolidated starting tree recorded 20 of 33 systems and 24 of 53
configurations. Commit `77fe3415` passed 173 focused Android tests and the
53-entry manifest audit. Its 40 registered configurations produced 80
production renders. The current source candidate registers 45 configurations
across all 33 systems. Its full validation is tracked separately from that
earlier result.

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
| PC_ENGINE | digital6 | Six-button startup option and port-one descriptors from the pinned ARM64 core. Both orientations in source |
| SNES | base | Standalone runtime and dedicated preview source |
| GENESIS | digital3 | Standalone runtime and dedicated preview source |
| GENESIS | digital6 | Standalone runtime and dedicated preview source |
| SEGACD | digital3 | Standalone runtime and dedicated preview source |
| SEGACD | digital6 | Standalone runtime and dedicated preview source |
| WS | guest_horizontal | Mode-aware dual cursors and logical A/B assignments in both handset orientations |
| WS | guest_vertical | Mode-aware dual cursors and logical A/B assignments in both handset orientations |
| WSC | guest_horizontal | Mode-aware dual cursors and logical A/B assignments in both handset orientations |
| WSC | guest_vertical | Mode-aware dual cursors and logical A/B assignments in both handset orientations |
| NDS | base | Both orientations in source. Mic and lid keep their core-specific routes in free upper-control space |
| NINTENDO_3DS | base | Standalone runtime and preview; touchscreen and guest input await handset verification |
| PSP | dpad_primary | Standalone runtime and dedicated preview source |
| PSP | analogue_primary | Standalone runtime and dedicated preview source |
| N64 | base | Independent left analogue, D-pad and discrete C directions in source |
| PSX | digital | Standalone runtime and dedicated preview source |
| PSX | dualshock | Standalone runtime and preview; conditional L3/R3 guest dispatch remains unverified |
| PLAYSTATION_2 | base | ARMSX2 host layout and typed input in source; Play! and guest validation open |
| PLAYSTATION_3 | base | Native host layout and typed input in source; requires the separate protected PS3 payload |
| DREAMCAST | base | Standalone runtime and dedicated preview source |
| GAMECUBE | base | Standalone runtime and dedicated preview source |
| XBOX | base | Hakux native host layout and typed input in source |
| XBOX360 | base | Xenia native host layout and typed input in source |
| FBNEO | digital2 | Pending verified title-specific compact button inventory, mode registration and dispatch |
| FBNEO | digital3 | Pending verified title-specific compact button inventory, mode registration and dispatch |
| FBNEO | digital4 | Existing backend and numbered seat mapping reused in source |
| FBNEO | digital6 | Existing backend and numbered seat mapping reused in source |
| MAME2003PLUS | digital2 | Pending verified title-specific compact button inventory, mode registration and dispatch |
| MAME2003PLUS | digital3 | Pending verified title-specific compact button inventory, mode registration and dispatch |
| MAME2003PLUS | digital4 | Existing backend and numbered seat mapping reused in source |
| MAME2003PLUS | digital6 | Existing backend and numbered seat mapping reused in source |
| ATARI2600 | base | Standalone runtime and dedicated preview source |
| ATARI7800 | base | Standalone runtime and dedicated preview source |
| LYNX | base | Standalone runtime and dedicated preview source |
| WII | sideways | Both orientations in source with existing native Sideways Remote route |
| WII | classic | Both orientations in source with existing native Classic route |
| WII | nunchuk_stick | Pending a verified fresh IR-stick engagement and independent fire ownership |
| WII | nunchuk_touch | Pending native viewport contact validity and independent fire ownership |
| WII | gamecube | Both orientations in source with the Dolphin GameCube route for compatible titles |
| WII_U | gamepad | Native layout and typed dispatch in source; verify applied screen and controller on handset |
| WII_U | pro | Native layout and typed dispatch in source; verify applied screen and controller on handset |
| DOS | gamepad | DOS_AUTO layout and keyboard access in source; default slots unassigned while guest bindings are unknown |
| DOS | keyboard_action | Pinned LibretroDroid host lacks verified direct guest keyboard or mouse dispatch for this mode |
| DOS | mouse | Pinned LibretroDroid host lacks verified direct guest keyboard or mouse dispatch for this mode |

## Shared implementation and remaining work

- Existing cartridge chord lists remain the persisted native identities.
  Typed Hold, Turbo and game-menu assignments extend the same profile store.
  Factory assignments apply only to an unconfigured cartridge or explicit reset.
  GripARC selection supports cartridge overrides and a global default for the
  implemented systems. Standalone assignments can be stored per native mode,
  with the existing cartridge JSON as the fallback. Layout selection alone
  does not write assignments. Legacy Joint continues using its own defaults
  and momentary routing. Hold uses the shared latch and pressed-state rendering.
  The active state is cleared when inputs are neutralized.
- Cemu commit `444c585202302a7f4b319bafb4cf72156a8cac8b`
  computes a full-size DRC viewport in TV-only single-display mode.
  The patched core guards touch against `LibretroDRC_ShouldRenderScreen(true)`,
  then uses its existing DRC subrectangle mapping. The Android host tracks
  a fresh finger contact and the native host gates the applied controller.
  Cemu artifact `15189417240` passed verification locally on `3982bd19`
  and again in connected run `46061721197`. The run staged the runtime,
  built the starting-tree ARM64 APK and passed 880 Android tests, including
  both SafeSave low-storage boundary tests. No native rebuild was needed.
  This is packaging evidence for the starting tree. The current layout
  candidate requires its own connected verification.
- Wii Nunchuk Aim & Fire and shared IR routing are pending. Pointer ownership
  must come from a fresh, valid contact on the rendered image or real IR-stick
  engagement before the firing owner is acquired. Existing independent Nunchuk
  movement and manual fire are retained.
- Automatic, guided and individual physical-controller mapping from the baseline
  remain in place. Physical programmable assignments, the remaining native
  modes, guest tests and the current-tree connected APK remain open.
- Conditional specialist controls need native dispatch and fit evidence before
  activation. Joint is outside this implementation.
