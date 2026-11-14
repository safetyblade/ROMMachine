# CARTDECK — Phase 3B-B Controller Activity Lighting

**Scope:** #242 (child of #227); Sienna visual design; Daniel architecture; Elliot engineering; Marcus independent QA. Owner-authorised 14 November 2026.

## Source-verified input ownership
- `GameViewModelInput.initializeGamePadKeysFlow` maps physical keys, handles menu/quick-load/quick-save/fast-forward shortcuts and PivotArc capture, then forwards gameplay buttons to `GLRetroView.sendKeyEvent(action, mappedCode, port)`.
- `GameViewModelInput.initializeVirtualGamePadMotionsFlow` forwards only existing axis-to-digital button transitions; it does **not** reflect continuous analog position.
- `GameViewModelTouchControls.handleVirtualInputButton` passes PadKit button events through PivotArc routing and forwards to `GLRetroView.sendKeyEvent(action, id)`. Its menu handler stays unchanged.
- The new observer is called **after** the already accepted existing send, with no return value influencing dispatch. No new lifecycle/Activity/SDL input collector and no handler retry.
- Dedicated native SDL/PS2/WiiU/Xbox paths that bypass these GLRetroView handlers are **not covered** by this first implementation. Never present them as tested without appropriate native event parity evidence.

## Player experience
Lighting Studio now has:
1. Off — no decoration.
2. Static — fixed existing shell/shelf accents.
3. Game feedback — existing #229 rumble-originated source.
4. **Controller activity** — a short brightness accent when a mapped touch, Bluetooth gamepad or existing axis-derived digital button goes down and is forwarded to the emulator.

The source is explicitly **player input**, not game hits, health changes, achievements or detected effects. The appearance preview remains static; a genuine gameplay input session is required for activity.

Existing colour, 0–100 intensity, four illumination zones, personal profiles, Apply/Cancel, high-contrast and reduced-motion controls remain. The shelf rail is static while browsing. In controller-fullscreen the shell remains hidden and no new gameplay overlay is introduced.

## Event reducer and limits
`CartdeckMappedInputActivityGate` holds only a bounded temporary set of **currently pressed** (source, mapped button) pairs to avoid repeated activity on held keys. It ignores menu/mode/back and unforwarded inputs; it processes ACTION_UP only for release bookkeeping. It never saves or logs device IDs or key histories. Each input source has independent held state. Changing mode, pausing, exiting, or losing the shell resets it.

`CartdeckReactiveLightingSession` is reused with a separate game-session state flow for controller activity. Only a new button-down edge sends a non-blocking conflated strength sample of 0.75. Existing hard limits: at most eight effective strength state updates per second, return to static baseline after 300 ms of silence, and finite 180 ms Compose brightness transition. No polling while idle. Lighting adds at most 35% of baseline intensity (bounded to 100%) on the same validated Phase3A manufactured shell LED trim rectangles. No game pixels, control hitboxes or brand seat are painted.

## Non-regression requirements
**No changes to** emulator cores, frame timing, save binding, ROM contents, Room schema, controller remapping or mappings, physical gamepad shortcuts, native analog triggers, PivotArc held-input functions, haptic preferences, input arrival order, PadKit dispatch or native controller routes.

## Test matrix
- Input gate: disabled Off/Static; new press counted; held/repeated DOWN ignored; UP re-arms; independent physical sources and touch; excluded menu/mode/back; invalid actions; capacity bound.
- Reducer: disabled mode silent, input sample, idle return, 125 ms throttle, pause/exit reset.
- Model: old shelf JSON defaults Off; prior three lighting modes valid; Controller Activity survives personal profile save/copy/reopen; no shelf/ROM identity changes.
- Renderer: same bounded safe strips, unchanged static baseline, 35% cap, high contrast/reduced motion static.
- Integrated Kotlin lint, JVM tests, unchanged Room schema, ARM64 APK pipeline and owner-approved install-over/rollback.
- Marcus: Samsung actual PadKit and physical Bluetooth controller; hold, repeat, menu, quick save/load, auto-remapping, gamepad disconnect, rotate, pause/resume; simultaneous touch and controller; no-rumble game; real game and save continuity. Native Xbox/PS2/WiiU lighting remains unclaimed pending distinct host QA.

**Stage gate:** code review + passing CI + Daniel source invariants + Sienna visual check + Marcus independent device evidence + owner merge/release decision. An open PR is not a hardware pass.
