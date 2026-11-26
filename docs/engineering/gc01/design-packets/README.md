# GC-ARC-DESIGN — no-APK controller design workspace

[Design authority issue #377](https://github.com/safetyblade/ROMMachine/issues/377). This is a **standalone design-only branch** with no open pull request. Branch pushes do not trigger the repository's PR / master APK workflows. Use these packets to prepare and approve designs before implementing any console code.

**Non-negotiable order:** GBA full GripArc physical proof → all solo PivotArc → all solo GripArc → Joint LAST (placement-only, artwork and key layouts unchanged).

**Universal PivotArc design authority (owner, 26 Nov 2026):** Every PivotArc aims for one-handed use, including stick-based and home consoles. Use GBA as the digital reference and PSP as the stick reference (PSP hold/reach and placement review pending). Keep primary gameplay inputs close in a thumb island, individual face-button Hold as a defining feature, secondary controls reachable but separated, full native input identity, handedness and safe hit targets. Do not duplicate a wide two-hand controller's geometry or silently omit keys. Where a game genuinely requires independent simultaneous controls, disclose the limit; do not imply universal one-hand playability. GB-PA-01 and GBC-PA-01 are owner layout locked; physical and exact-head engineering evidence remain pending.

**Multi-button refinement (owner, 26 Nov 2026):** Standard PC Engine approved; previous six-button geometry rejected for thumb interference. Pull forward 3/4/6-button families for game-action analysis and PSP-inspired radial layouts. Holds are fundamental but permanent Hold count in complex profiles is determined by sustained gameplay need and ergonomic fit; preserve existing approved two-button Hold A/B equivalents. Do not assume every face key needs a permanent Hold. See [shared design study](../GC-ARC-MULTIBUTTON-STUDY.md).

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
