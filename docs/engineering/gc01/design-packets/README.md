# GC-ARC-DESIGN — no-APK controller design workspace

[Design authority issue #377](https://github.com/safetyblade/ROMMachine/issues/377). This is a **standalone design-only branch** with no open pull request. Branch pushes do not trigger the repository's PR / master APK workflows. Use these packets to prepare and approve designs before implementing any console code.

**Non-negotiable order:** GBA full GripArc physical proof → all solo PivotArc → all solo GripArc → Joint LAST (placement-only, artwork and key layouts unchanged).

Each SystemID has its own design packet. Never describe a planned controller as runtime-supported merely because its draft exists.

| Batch | Console packets | Proposed build strategy |
|---|---|---|
| A: Two-button | [GB](GB.md), [GBC](GBC.md), [NES](NES.md), [SMS](SMS.md), [GG](GG.md), [NGP](NGP.md), [NGC](NGC.md), [PC_ENGINE](PC_ENGINE.md) | One *integrated* APK only after the cohort's source, formatting and unit tests pass |
| B: Four/six-button and handheld | [GBA](GBA.md), [SNES](SNES.md), [GENESIS](GENESIS.md), [SEGACD](SEGACD.md), [WS](WS.md), [WSC](WSC.md) | One *integrated* APK only after the cohort's source, formatting and unit tests pass |
| C: Dual-screen / single-analogue | [NDS](NDS.md), [NINTENDO_3DS](NINTENDO_3DS.md), [PSP](PSP.md), [N64](N64.md) | One *integrated* APK only after the cohort's source, formatting and unit tests pass |
| D: Console native / analogue | [PSX](PSX.md), [PLAYSTATION_2](PLAYSTATION_2.md), [PLAYSTATION_3](PLAYSTATION_3.md), [DREAMCAST](DREAMCAST.md), [GAMECUBE](GAMECUBE.md), [XBOX](XBOX.md), [XBOX360](XBOX360.md) | One *integrated* APK only after the cohort's source, formatting and unit tests pass |
| E: Specialist | [FBNEO](FBNEO.md), [MAME2003PLUS](MAME2003PLUS.md), [ATARI2600](ATARI2600.md), [ATARI7800](ATARI7800.md), [WII](WII.md), [DOS](DOS.md), [LYNX](LYNX.md), [WII_U](WII_U.md) | One *integrated* APK only after the cohort's source, formatting and unit tests pass |

**Design gate:** Native ID and face/axis semantics source-verified, housing and relative positions prepared, 48dp+ hitboxes, no overlap/viewport bleed, orientation, saved mappings, and owner design decision. Update [console build tracker](../GC-V2-CONSOLE-BUILD-TRACKER.md) when an actual integration run lands, not for every packet edit.

**Implementation gate:** Combine approved family packets into a single code batch, run formatter `ktlintCheck`, a fast deterministic geometry validator and the GBA/console unit suite **before** downloading any native engine. Only then request one verified integrated APK for the cohort. Reuse that APK for individual owner game/controller tests. Design packets are editable independently and do not require an APK.
