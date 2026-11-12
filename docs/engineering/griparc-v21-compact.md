# GripArc V2.1 compact combined layout

Status: implementation candidate. Owner acceptance and device QA are pending.

The permanent L now starts at 84% of the original manifest dimensions. Its vertical
arm, bottom arm, and internal spacing scale together; button artwork receives a
further 10% reduction. All four fixed slot targets remain reserved and at least
48dp, regardless of active count. Narrow decks can try 80%, 76%, and 72% scales.
The source manifest, slot identities, mapping logic, and full-L renderer remain intact.

Standard + GripArc now resolves from safe drawing/gesture insets and the existing
viewport. It reserves the dock and all slot targets first, then places a tight upper
shoulder/utility rail, a raised far-side D-pad, and individual face targets. It
prefers the open inside corner for faces, with an upper placement on narrow screens.
Analog targets occupy a separate lower band only for the configured controller;
Dreamcast uses one stick. Face compasses and PadKit input identities are retained.
Validation uses each target plus the two physical L arms, rather than composite
Standard control-group envelopes. Rendering consumes the resolved rectangles.

Resolution tries the saved side/offset and its default position on that side.
The player can explicitly try the opposite side for the session. These are
transient attempts and do not write cartridge settings.
If none fits, Standard controls remain usable and a dialog offers **Use Standard
this session**, which disables accessories until the session ends or the saved
mode/side is explicitly changed. Unsupported controllers receive a distinct
message. Portrait is
required for this combined layout; landscape uses the same explicit fallback.
Standard-only, PivotArc-only, viewport policy, emulator configuration, saved
profiles, ROM handling, and save paths have no behavioral changes.

## Validation and remaining checks

Geometry tests cover PS2, GameCube, Dreamcast, GBA, and SNES on both sides at
393×852dp, 360×740dp and 320×640dp, plus asymmetric cutout/gesture insets.
Viewports derive from each screen opening. Checks cover complete control sets,
safe bounds, collision exclusion, viewport exclusion, fixed slots,
48dp minimum targets, face compass identity, invalid-offset reset, and rejection
of a genuinely short deck. These fixtures are not owner handset measurements.

The geometry suites were also run in an isolated Kotlin/JUnit harness using the
production geometry sources, an Android asset-loader stub, and the GripArc side
enum. The dedicated renderer was compiled against the real Android touch-input
module through a temporary source-set extension. These checks do not establish
that the complete APK builds or that gameplay input works on a device.

The complete APK workflow passed for PR #152's initial joint-deck head
(`43297106398`). Corrections use focused source checks and hand the next integrated
build off to CI. Preserve package ID, signing key, increasing version code, patched
host, and all native engines; do not deliver a stripped replacement.

Before acceptance: require a successful integrated APK build, install-over on the
owner handset, right/left and counts 1–4, simultaneous D-pad/face/stick/shoulder
input, GripArc assigned chords, and save/reopen/cartridge reload. Independent QA
and owner confirmation remain pending. PivotArc + GripArc benefits from the
smaller dock but still uses its existing measured fit pass and needs separate QA.
Very short decks or large system insets may need the Standard session fallback;
48dp targets and the game viewport are never reduced to force a fit.
