# INPUT-02 — CARTDECK Modern Function Controls
**State:** planning and research; no action remapping implemented
**Owner:** Jonathan Prestwich
**Controlling roadmap:** [#172](https://github.com/safetyblade/ROMMachine/issues/172)
**Draft review:** [#175](https://github.com/safetyblade/ROMMachine/pull/175)

This is an **ACTION-FIRST** controller system, not only a controller-brand/face-button-letter translator.

When enabled, CARTDECK should understand a verified game's original action input and route the familiar modern physical position to that native output: e.g. **right trigger → held GBA A for Mario Kart Super Circuit Acceleration**. Different games on the same console may use the same original key for radically different functions. Console-only global A/B swaps are not an adequate solution.

## Documents
- [Console / genre action matrix (source research)](console-genre-action-matrix-v01.md): native controls and candidate modern placements.
- [Research JSON data](console-genre-action-matrix-v0.1.json): **research only**, never dynamically read as production mapping without verification/versioning.
- [Delivery roadmap](roadmap-v01.md): implementation lanes, dependencies, release gates and known blockers.
- [QA acceptance matrix](qa-acceptance-matrix-v01.md): actual controller, gameplay, console, safety and TV checks.
- [Engineering handoff](engineering-handoff-v01.md): concise implementation slices for Elliot/Codex.

## Tracked issues
| Stage | Issue | State |
| --- | --- | --- |
| Source/native mapping review | [#176](https://github.com/safetyblade/ROMMachine/issues/176) | OPEN — no approved runtime profiles |
| Physical controller normalizer | [#177](https://github.com/safetyblade/ROMMachine/issues/177) | OPEN — no new normalizer released |
| Automatic action profile resolver | [#178](https://github.com/safetyblade/ROMMachine/issues/178) | OPEN — no runtime remap |
| Controls UI and learning | [#179](https://github.com/safetyblade/ROMMachine/issues/179) | OPEN — no production UI |
| Console/genre catalogue expansion | [#180](https://github.com/safetyblade/ROMMachine/issues/180) | OPEN — seed only |
| Independent release QA | [#181](https://github.com/safetyblade/ROMMachine/issues/181) | OPEN — no device acceptance |

**Integration boundaries:** [INPUT-01 #122](https://github.com/safetyblade/ROMMachine/issues/122) held-key/input ownership, [GripArc #135](https://github.com/safetyblade/ROMMachine/issues/135) per-cartridge chords, [DISPLAY-01 #162](https://github.com/safetyblade/ROMMachine/issues/162) gamepad hiding touch in fullscreen, [TV remote #166](https://github.com/safetyblade/ROMMachine/issues/166). Preserve all working emulators, ROM scanning, saves and per-device mappings.

## Current evidence
The 16 game examples / 44 action rows in this draft are sourced research examples and **must pass independent source/key-code validation before activation**. A cited original game manual does not demonstrate Android core runtime equivalence or real device gameplay.

The app's `Game` entity and LibretroDB metadata layer currently have no reliable genre/control-action fields, so auto-selection requires a separately curated, identified catalogue. **No hidden filename-only classification.**
