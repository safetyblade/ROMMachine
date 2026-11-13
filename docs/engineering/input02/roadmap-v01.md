# INPUT-02 — Execution roadmap and release-gate contract
**Version:** 0.1 · **13 November 2026** · **Owner direction: function-first** · **Tracking:** [#172](https://github.com/safetyblade/ROMMachine/issues/172)

## Final user experience
CARTDECK should feel like a modern controller regardless of the old console:
- platformers: **Jump → physical South**, translated to original game's jump key;
- kart/racing: **Accelerate → Right Trigger**, **Brake → Left Trigger**, **Steering → Left Stick**, translated to original game's A/B/Cross/Y/analog trigger as verified;
- football/soccer: primary **Pass/Kick → South**, remaining Shoot/Through Ball/Cross according to verified game's actual controls;
- shooter: **Fire → RT** and **Aim → LT** where verified; fighting games need their own conventions;
- TV: controller-driven fullscreen hides all visible touch buttons, with a guaranteed menu path; remote alone is for navigation, not automatically a usable gamepad.

## Pre-existing building blocks
- `InputDeviceManager`: enabled device flow, per-device saved mappings and gamepad port routing.
- `LemuroidInputDeviceGamePad`: Android face-code A/B, X/Y -> RetroPad positional conversions. These are potentially deliberate; **preserve and verify**.
- `GameViewModelInput`: native button/axis/trigger pathways and menu shortcuts.
- `NativeControlInventory` and `ControllerConfigs`: selected console native controls, special labels and controller variants.
- `GripArcProfileStore`: per-cartridge mapping and chord ownership; **separate from INPUT-02 action profiles**.

## Critical path (no forced calendar order)
| Stage | Issue | Immediate deliverable | Required input | Exit evidence |
| --- | --- | --- | --- | --- |
| A — native truth & evidence | [#176](https://github.com/safetyblade/ROMMachine/issues/176) | Reviewed 16-source seed; console+genre schema; title/version match and exact original → core key map | Research draft #175, native controller configs | Evidence and mapped-key tests; owner design approval |
| B — controller input foundation | [#177](https://github.com/safetyblade/ROMMachine/issues/177) | Physical gamepad position/axis/trigger normalization, per-device identity and ports, no hold regressions | Existing Android detection/RetroPad mapping | Xbox/Switch/PS-style fixtures + real capability evidence |
| C — gameplay action routing | [#178](https://github.com/safetyblade/ROMMachine/issues/178) | Feature-gated versioned profile resolver, RT digital/analog, safe revert, exact original input | A+B stable interfaces | Source→output automated tests + verified representative gameplay |
| D — accessible settings | [#179](https://github.com/safetyblade/ROMMachine/issues/179) | Authentic / Modern Function / Custom, action preview, map editor, source confidence | C profile API; Sienna UX; TV #166 | Save/reopen, one-click revert and controller/remote usable UI |
| E — supported game catalogue | [#180](https://github.com/safetyblade/ROMMachine/issues/180) | Additional manual/device-verified game profiles and honest coverage report | A evidence schema; may run parallel | No false-positive title/region/genre matches |
| F — independent QA / release | [#181](https://github.com/safetyblade/ROMMachine/issues/181) | End-to-end real console+gamepad matrix, APK/artifact, rollback evidence, owner acceptance | Integrated B-E + existing input/TV streams | Marcus and owner device PASS; gates all green |

**Parallelism:** A and B can run together; E catalogue work after A schema is stable. D mockups can run alongside C but cannot claim persistence/menu behaviour until wired. F device sign-off is a release gate, not a blocker against early source tests.

## Verified title vs genre heuristic
1. Existing per-game Custom profile wins.
2. Modern Function **opt-in** + exact verified title/version/region/core profile → automatic.
3. Modern Function opt-in + **confirmed genre** + sufficiently reviewed console/genre pattern → present one-tap **Suggested**; user may accept and save. No silent guessed mappings.
4. No known title/genre → **Console Authentic**.
5. Actual in-game manual settings overrides / controller variant changes require re-evaluation of the currently selected profile; the app cannot inspect arbitrary gameplay context.

## Technical non-negotiables
- Every mapping is **physical input position → original game control output**. Original console label ≠ Android key ≠ RetroPad A/B ≠ user action name. All bridges require fixtures.
- Distinguish digital RT→original face key (threshold/hysteresis and hold/release) vs genuine analog trigger or pressure support. Do not promise analog if selected core/game only supplies digital.
- If A is accelerated by RT and also available on a face button, preserve combined press ownership; a key held by 2 sources stays down until both release.
- Avoid duplicate game input from trigger axis+digital event; handle loss of focus, controller removal, menu, profile switch, game exit. No latent held keys.
- Special controller modes (Wii pointer, GameCube Z/C, Dreamcast triggers, N64 C buttons, Xbox Black/White, PSP single analog, DS portrait stylus) remain available; if unsupported, don't apply a misleading template.
- Existing default and custom controller bindings, GripArc, saved cartridges, emulation, VMU, ROM/library and install-over state remain intact.
- Controller-specific prompts reflect Xbox A, Switch B, PlayStation Cross for the **same physical South**; UI must show the *original game action* as the reason for remap.
- Preserve a fallback/feature flag and easy Authenticated/Modern/Custom transition without emulator restart.

## Blockers and resolution path
| Risk / unknown | Proof needed | Workstream |
| --- | --- | --- |
| `Game` has no reliable genre/action data | Curated classification with provenance or user-supplied genre; no unknown auto | A/E |
| Original console buttons can mean different actions by game | Correct per-title override and reviewed genre suggestions | A/C/E |
| Per-core native code differences (Dreamcast A/B, GameCube, PSP, Wii) | Native output bridge fixtures per selected core/controller | A/C |
| Same-model device remapping target ambiguity | Match by real device identity, preserve descriptor-scoped prefs | B |
| Right-stick and trigger axis differences | Device capability matrix, 0-1 analog calibration, digital fallbacks | B/C |
| One original key has multiple physical owners | Ref-counted or equivalent press arbitration | C |
| Original button needed by menus too | Honest key-level mapping and preserved authentic fallback | C/D |
| TV fullscreen hides touch overlay | Controller menu/remote invocation, display-stream acceptance | D/F + #162/#166 |
| PS3 legacy Bluetooth may not pair on target Android | Report unsupported hardware fact, do not claim software mapping fixes pairing | B/F |
| Unverified manual examples | Re-review evidence and gameplay test; downgrade confidence as needed | A/E/F |

## Acceptance checkpoints
**G0 Baseline protected:** exact current working source hash, APK, active core routes and saved per-device mapping export/snapshot; no regression to #122/#135/#162.
**G1 Input truth:** representative native key translations and title/genre evidence approved; unknown fallback tested.
**G2 Correctness:** repeatable pure/unit tests show SNES B→Jump South in Mario World and SNES B→Accelerate RT in Mario Kart; A→RT in approved GBA kart; PS Cross→RT in approved racing; Dreamcast RT naturally analog; no double-release.
**G3 Integrated UX:** cartridge modes and controller glyphs correct; gamepad/remote in-game menu; profiles persist, Custom overrides and revert with no save impact.
**G4 Device QA:** actual Xbox-style, Switch Pro and supported PS-style pads, minimum representative platformer/racer/football gameplay, two controllers, hotplug, touchscreen fallback and full ARM64 APK install-over; Marcus+owner sign-off.
**Release:** optional Modern Function enabled only for known profiles, with clear confidence and rollback. No blanket global A→RT remapping. Individual emulator/game playability follows its independent engine acceptance.

## Completion evidence rules
Every issue needs a single concise completion comment with: source commit + PR; tests passed/failed/not tested; observed gameplay/core output; affected consoles; APK artifact where applicable; known gaps; owner/QA gate. Close parent #172 only after #176–181 requirements and G0–G4 are met, not after a design-only PR merge.
