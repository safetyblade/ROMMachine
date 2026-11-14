# CARTDECK | Phase 3B-A — Core Feedback Lighting

**Design:** Sienna Vale · **Architecture review:** Daniel Mercer · **Engineering:** Elliot Shaw · **Independent QA:** Marcus Reid
**Owner:** Jonathan Prestwich · [Phase 3B issue #227](https://github.com/safetyblade/ROMMachine/issues/227)
**Integration:** Draft PR, separate from Phase 3B-B controller activity and any native-engine enhancements.

## Verified event source
`GameViewModelRetroGameView` already collects `GLRetroView.getRumbleEvents()` through `RumbleManager.collectAndProcessRumbleEvents()` while the activity is RESUMED. The existing host sets `GLRetroViewData.rumbleEventsEnabled = settingsManager.enableRumble()`.

The lighting implementation is a **non-blocking observer of the existing rumble collector**. It does NOT change `rumbleEventsEnabled`, user vibration settings, hardware haptic dispatch, input mappings, or emulator cores. The callback runs inside the existing event `onEach` before the unchanged vibrator call. If haptics are disabled, the game/core has no feedback, or the emulator doesn't expose a rumble request, lighting stays at its selected Static baseline.

**Do not claim collisions, achievements, scores, music beat, actual game state, or audio response.** Rumble is a request from the game's core, not an explanation of what the gameplay event meant.

## Reducer and lifecycle
`CartdeckReactiveLightingSession` is owned by the game viewmodel and uses a **single conflated channel**; the rumble thread calls `trySend` without starting coroutines, blocking, or awaiting UI work. The reducer combines normalized strong/weak strengths using the same basic weights as the rumble manager, clamps malformed/NaN strengths, emits a maximum of eight effective feedback signal updates per second, and resets to zero after 300 ms of rumble silence. It waits suspended while idle. **No permanent timer or background service.**

The Compose screen enables the session only while the gameplay screen is resumed, game is loaded, reduced motion and high contrast are off, and a physical shell is actually visible. An explicit disable resets the reducer state. The game observer disables it on pause/destroy; viewmodel cleanup also disables it. Changing profiles or leaving game closes the feedback path.

A finite 180 ms Compose brightness transition is triggered by actual session state changes and returns to static baseline. Static mode schedules no reactive frames.

## Lighting recipe and rendering
The additive saved mode `feedback` extends the existing V2 appearance JSON `lighting.mode`; `off` and `static` have unchanged semantics and older appearances default Off. Existing zones, colour, intensity, personal appearances and high-contrast settings persist. No Room schema or save-game fields change.

At `feedbackStrength=0`, Game Feedback looks identical to Static. Actual event intensity adds at most `min(base * 0.35, 100 - base)` multiplied by normalized feedback; this never raises 0% intensity, never exceeds 100%, and never introduces hue flashes. **Only** the existing `CartdeckConsoleShell` safe trim rectangles can react. No video/PadKit/brand pixels, controller focus, scroll, bookshelf or fullscreen gamepad overlays are changed. The shelf rail remains static while browsing.

In Appearance → Lighting Studio, player chooses Off, Static or Game feedback. The UI explicitly states that feedback requires both core/game rumble and the existing rumble preference enabled; otherwise it behaves as Static. Controller Activity is not exposed yet.

## QA gates — NOT ACCEPTED UNTIL VERIFIED
1. Kotlin lint, source contracts, Android unit tests, Room schema and full native-engine-integrated APK CI.
2. Test with a title/core that generates real rumble and with a no-rumble title; verify the latter remains static. Turn rumble off and prove lighting does not secretly re-enable physical vibration or host rumble.
3. Physical controller and touch PadKit input parity, GRIP/hold/menu shortcuts, motion triggers, and same save progress. Fullscreen gamepad must stay clear of shell decoration.
4. Portrait/landscape/tablet/DS and Original/Pink/Green/Stealth Acid; high contrast and reduced motion must suppress reactive transitions. No overlap with measured gameplay aperture and control touches.
5. Confirm bounded updates, 300 ms idle return and acceptable battery/frame impact on the owner Samsung. Sienna visual, Daniel architecture, Marcus independent QA, owner release gate before merging.
6. Restore previous APK without wiping app data and document optional new appearance mode's downgrade behaviour. Prior APKs may reject a newer lighting mode on strict validation; export/backup personal appearance JSON before deliberate downgrade.

## Out of scope
Phase 3B-B mapped controller activity, independent audio capture, video-frame analysis, damage/score detection, per-game event parsing, new core interfaces, and any changes to actual vibration preferences.
