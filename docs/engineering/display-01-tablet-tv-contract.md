# DISPLAY-01 — CARTDECK large-screen UI and gameplay contract
**Date:** 13 November 2026 · **Owner:** Jonathan Prestwich · **Workstream:** Tablet/TV/landscape presentation.
**Tracking:** [#162](https://github.com/safetyblade/ROMMachine/issues/162)
**This PR stage:** Device/display policy and acceptance design **only**. No finished tablet/TV gameplay UI or Fullscreen Fill renderer is claimed.

## Target experience
**Phone portrait:** Existing physical CARTDECK handset shell and touch controls unchanged.
**Tablet portrait:** Cartridge shelf with wider multi-column rows, proper information hierarchy, usable split detail/menus where expanded, touch controls in game.
**Tablet landscape:** Game first; native-size source pixels **Fill** the display by default with an optional touch overlay when no gamepad is connected. In-game menu offers **Native aspect** without restarting.
**Android TV / explicit TV mode:** Focusable CARTDECK Leanback cartridge rows, remote-first settings and game menus, full-screen rendered game with ZERO virtual controls/bezel. Physical game controller required for titles a 5-key remote cannot play. User can open CARTDECK Game Menu using the TV remote and connected controller to change Display **Fill / Native aspect**. TV mode should be accessible as explicit user choice on tablets/external displays, not silently activated by width alone.
**DS/3DS:** Ineligible in TV and landscape gameplay; unavailable detail with portrait guidance rather than silent omission. Existing portrait DS stylus and save formats remain functional. Do not globally disable DS.

## Screen options: two real, different rendering results
- **Fill screen (TV/landscape default)**: Display actual frame edge-to-edge, **no source content cropped**, which may stretch classic 4:3 frames to 16:9. Tell the user this can change proportions. Do not just set `Modifier.fillMaxSize()` and claim it worked.
- **Native aspect**: Actual core aspect-preserving framebuffer with pillarbox/letterbox; no content lost and no invented native widescreen for non-widescreen games.
- Optional future `Zoom/Crop` is NOT in scope.
- Source-native resolution, widescreen core options and shader performance remain separate from the presentation fit mode. Never relaunch, reload ROM or reset saves merely to toggle.

## CURRENT source findings — reuse these seams
- `app/tv/main/MainTVActivity.kt`, `tv/home/TVHomeFragment.kt`, `tv/shared/GamePresenter.kt`, `tv/game/TVGameScreen.kt`, `tv/gamemenu/TVGameMenuFragment.kt`: existing Leanback TV navigation and TVGameActivity; brand/focus/menus rather than replacing TV subsystem.
- `mobile/feature/shelf/ShelfBoard.kt`: already scales cartridge columns with `maxWidth`; tablet card widths, metadata, navigation and settings need responsive rules, not merely more tiny columns.
- `mobile/feature/gamemenu/GameMenuActivity.kt`: current game menu is a max 400dp sheet; tablet can use a larger focused two-pane menu. Keep phone unchanged.
- `mobile/feature/game/GameScreenLayout.kt`: landscape allocates a large Android game view. `tv/game/TVGameScreen.kt` already fills its view. **Neither controls native LibretroDroid aspect-fit.**
- Upstream LibretroDroid `VideoLayout::updateForegroundVertices()` calculates `scaleX` and `scaleY` from the reported game aspect and host screen aspect; this always fits/letterboxes. To achieve **actual Fill**, implement an opt-in native host `presentationMode` policy with no aspect correction when `FILL`, and keep the current logic when `NATIVE`. Wire an explicit API through the patched local LibretroDroid host, JNI and `GLRetroView` with a runtime update, use supported render threads. Integrate with the existing `scripts/ci/build_libretrodroid_host.sh` and patch series. Do NOT change the core's gameplay aspect variable; do not distort touch coordinates without updating pointer transforms.
- `BaseGameActivity` handles key/shortcut input, `TVGameMenuActivity/Fragment` provide Leanback preferences and game menu. Add **Display** entry and safe remote-only invocation, not a dead preference that does nothing.

## Controlled implementation lanes (Codex / Elliot)
1. **DISPLAY-A:** use `CartdeckScreenPolicy` device/dual-screen eligibility and UI dimensions in actual routing. Add explicit TV-mode preference. Keep TV feature detection separate from tablet size. Prevent ineligible dual-screen launch from TV/game launcher and landscape *before* loading the core. Display sensible UI. No save changes.
2. **DISPLAY-B:** responsive tablet library and menu UI. Tablet medium 600–839dp; expanded 840dp+ available window. Adapt grid/card sizes without ruining shelf spine art, preserve sorting/favourites and game actions. Tablet landscape library/menu focus and keyboard navigation.
3. **DISPLAY-C:** reskin/reorganise existing Leanback TV UI with CARTDECK tokens (Original, Pink, Green, custom). Make focus visible at ten-foot range, D-pad/OK/Back work on every item, no touch needed. Provide menu invocation via dedicated remote Menu key, controller MENU/shortcut, and a documented/verified path for basic remote D-pad/OK/Back only (e.g. deliberate long-press OK); don't steal gameplay short-press OK or remote system Home. Back linear navigation and safe exit. Controller absence warning for games needing buttons beyond remote.
4. **DISPLAY-D:** add `Fill` vs `Native` to actual renderer. Start Fill on TV/landscape except DS; select Native from in-game TV menu and analogous phone/tablet menu; re-render immediately without game relaunch or lost save. Persist presentation choice per cartridge/display context or per device; no global forced changes to portrait user settings. Verify host and fallback for engines with nonstandard renderer.
5. **DISPLAY-E:** full integrated ARM64 CI, aspect render pixel tests, controller/remote focus tests, DS guard/portrait preservation, tablet + 1080p/4K actual-device QA, install-over saves.

## Critical support and regressions
- No on-screen **TV** controls even with a controller disconnected; do not synthesize fake presses. Warn about missing controller and keep menus accessible.
- Tablet/landscape touch-only devices must remain playable; optional overlay may be shown unless real controller connected and user disabled it.
- Tests for orientation, tablet split-screen, external HDMI, TV system insets, controller disconnect/reconnect, Android key-repeat, physical Back/Menu/Guide, focused menu restore, game restart.
- Multi-frame aspect tests must compare visible actual gameplay pixels at 4:3 and 16:9, not just the measured AndroidView rectangle.
- No accidental break to Samsung-style phone portrait UI, PivotArc/GripArc #135, working GameCube/Dreamcast controller mappings, DS portrait stylus and saves, Steam/other core inputs, Xbox host.
- Build/output CI cannot mark completion without a tested paired `Fill`/ `Native` rendering change and a TV remote menu path.

## Acceptance and source of truth
See `display-01-acceptance.md`. Only merge UI controls when their corresponding actions work. Policy module is implemented and unit tested in this design PR but **not yet connected** to game routing or renderer.
