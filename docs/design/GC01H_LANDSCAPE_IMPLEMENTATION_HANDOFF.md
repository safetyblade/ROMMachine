# CODEX / Elliot — GC-01H Landscape Aspect & CARTDECK Surround implementation handoff

**Status:** DESIGN COMPLETE, SOURCE IMPLEMENTATION NOT STARTED, NO APK/HARDWARE PASS. This file is the implementation-ready companion to [landscape design V1](GC01H_LANDSCAPE_ASPECT_SURROUND_V1.md) and the editable SVG/tokens at `design/cartdeck-v03/surround/`. Track [#284](https://github.com/safetyblade/ROMMachine/issues/284), [#60](https://github.com/safetyblade/ROMMachine/issues/60), [#262](https://github.com/safetyblade/ROMMachine/issues/262).

**Scheduled position:** After CI-01, GC-01C checkpoint, D/E and GEM prerequisites where relevant, as part of H/PERF-02. Do not start on the active build branch. Confirm fresh master/PR heads immediately before execution. Use safe connector-sized commits and one draft implementation PR.

## Design authority (no further design loop)
- Aspect mode **AUTO on every game session launch**. The WIDESCREEN choice is session-only and must require real verified widescreen support + native acknowledgement. Source pixel aspect and full video are preserved; never unilaterally stretch/crop.
- Surround **STANDARD** default (stronger 7–10dp rim and restrained accent). Optional **ORNAMENTAL** (26/18/12dp responsive premium manufacturable side-band) with appearance-aligned C-slot/chamfer/rail detailing and compact fallback. Appearance can persist independently of aspect mode.
- Use current Original/Pink/Green and My Appearances/Stealth Acid semantic palettes; all ornament must remain outside video/control/brand regions; no interactive duplicate input owner.
- Native engine support, warning cleanup and licence requirements remain truthful. The backend display status, not a viewport assumption, governs Widescreen availability.
- Decorative reference is symbolic artwork, not screenshots or renderer evidence.

## Required source increments

### H-DISPLAY-01: truthful aspect capabilities
Read `NativeViewportPolicy.kt`, `NativeRendererAudit.kt`, `XboxGameControls.kt`, all integrated hosts, engine options and existing Libretro core settings.

Implement a small tested resolver producing:
```text
requestedAspect: AUTO | WIDESCREEN
effectiveAspect: verified pixel-aspect-corrected display ratio (nullable)
nativeWidescreenCapability: VERIFIED | UNSUPPORTED | UNKNOWN
apply: REQUESTED | ACKNOWLEDGED | REJECTED | TIMED_OUT | NOT_SUPPORTED
fallback: AUTO
evidence: engine revision, title/region/revision where known, origin of aspect
```
Do not persist `WIDESCREEN` across session start. Widescreen must not simply overwrite the video aperture width without changing the renderer's actual output. Preserve 4:3 fallback but mark lack of source measurement. Unit tests for boot/reset, supported/unsupported, false 16:9 report, dynamic native dimensions, failed native response and late callbacks.

### H-DESIGN-02: two tokenised, noninteractive surrounds
Refactor existing `CartdeckMaximisedFrame.kt` and where needed `CartdeckConsoleShell.kt` to use one common measured `videoRect`, exclusion rules, semantic colours and `surroundStyle`. Implement Standard and Ornamental with targets/constraints from `surround-v1.design-tokens.json`.

Geometry is testable pure code: safeDrawing and TV overscan minus controller/brand exclusions -> valid full-source-video rectangle -> border/gutter ornaments only outside. When room is tight, Ornament gets smaller; only the user's explicitly chosen Ornamental style may make <=5% symmetric size tradeoff to allow a meaningful frame. Never draw a stroke onto source video. Keep 1px inward guard from `NativeViewportPolicy.inwardPixels`. No second Surface, GL frame, renderer recreation or input intercept for decoration.

Visual comparisons across phone 19.5:9, phone 16:9, small 360dp landscape, tablet, TV with 32dp overscan, 4:3 and true 16:9 guest frames, control-deck and controller-only; Original/Pink/Green/Stealth/Custom. No portrait default relayout.

### H-UX-03: Display route, preview and appearance persistence
Reuse CARTDECK `GameMenuPanel` / existing native host adapters and TV DPAD navigation. Add **Display > Aspect** with Auto and capability-gated Widescreen. Add **Display > Surround** Standard / Ornamental. Add **Appearance > Gameplay Surround** with live schematic previews; apply through existing `ShelfAppearance` and named profile flow. Persist only `surroundStyle` via backward-compatible `ShelfStore` appearance extension with V1/V2 reads; do not alter Room or saves. Distinguish global visual preference from in-game session Auto aspect.

On failure to apply Widescreen, revert cleanly to Auto and show one clear message; retain running game without fake command success. No 16:9 stretch hidden in UI. Tests for menu focus, 200% text, TV remote, session resets, saved Ornamental across install-over, unknown stored future appearance, invalid custom colours and reset-to-Standard fallback.

### H-PS2-04: ARMSX2 native evidence and warning cleanup (issue #284)
Trace exact pinned PS2 video aspect/native widescreen capability and actual `patches.zip` runtime lookup. The outer APK ZIP verification is not proof native code can open the installed resource. Correct faulty resource paths, validate source-owned unpacked ZIP even when a marker exists, and transactionally self-repair corrupt source-owned resources without touching BIOS, cards, save directories or ROM. Avoid raw warnings/toasts on a healthy ROM boot. Genuine BIOS/resource/fatal/save failures stay visible as accurate CARTDECK recovery; internal logs remain in opt-in diagnostics.

Do not add a misleading synthetic widescreen mode or silently enable game patch/hack flags. Preserve Play! fallback and separate PS2 cards. Tests for resource clean install, corrupt ZIP, install-over, missing optional resource, real failures, zero unnecessary warnings on healthy boot. No native recompilation solely for shell cosmetics.

### H-QA-05: exact-SHA integration and rollback
Run visual snapshot/geometry/unit/Compose tests plus connected APK CI using verified CI-01 native artifacts. Verify all approved console hosts retain menus and saved game roots; no display regression DS dual-screen, Wii U stylus pointer, native Xbox/Xenia, Cemu Vulkan, ARMSX2. Verify Widescreen is disabled when not truly supported; do not fabricate a video/pixel acceptance from a surface size.

Provide links to actual workflow run, APK artifact + SHA256 and version/signature identity when built and permitted. Compare measured screenshots with the vector studies (not pixel-match of fictitious game content), list exact deviations and record all cases as PASS/FAIL/NOT RUN. Jonathan/Marcus do actual device trials at the agreed H checkpoint, respecting current programme testing decisions.

## File-level acceptance and rollback

- Current snapshot anchors: `CartdeckMaximisedFrame.kt` with 6dp trim and `NativeViewportPolicy.presentationAspect(Host.ARMSX2)=4f/3f` are the explicit design improvements; do not edit source until the planned H stage.
- For legacy appearance records, `surroundStyle` absent -> Standard. A newer unknown format is preserved without destructive rewrite. A user switching Ornamental -> Standard must restore the original screen geometry.
- Preserve original `ShelfStore` atomic updates; `GameMenuContract`/pause-gate routing, DS/Cemu input, TV/remote and the real native renderer lifecycle.
- Never move to actual game-specific widescreen settings without proving the backend can apply and undo them.
- No changes to ROM contents, user-owned BIOS, engine HDD/NAND/memory cards, UUID/SaveBinding, Xbox profiles or APK identity.
- Rollback is to the previous source/SHA and Default Standard+Auto presentation; preserve user appearance additions across install-over.

## Design signoff ledger

- Product requirements: **owner-approved in conversation, 16 Nov 2026** (Auto each launch; Widescreen option; stronger Standard; Ornamental premium option).
- Source-backed design/spec/tokens/visual references: **prepared, design package only**.
- Sienna/Daniel separate human signoff: **not independently recorded**.
- Engineering build/QA and hardware acceptance: **not yet performed**.

**Execution stop condition:** One tested, reviewable H display/performance implementation PR with working reversible aspect and surround behaviour and no PS2 false warnings. Do not commence another styling redesign or claim source/hardware PASS without evidence.
