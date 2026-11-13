# Cartridge Setup test gate repair

Refs #155. No cartridge-front design change.

Latest inspected master run: 43369823362, commit 84e409751b25f3ac3f6957e2ba71f2ecb60d3ddb,
failed. Its retained CartridgeExperienceTest XML reports 22 tests, seven failures:
six absent submenu controls after Setup navigation, and one ComposeTimeoutException
in WindowCapture_androidKt.forceRedraw's OnDraw/PixelCopy handshake. The prior
completed #160 run also passed native/host preparation and failed these tests.
There is no XML evidence of picker permission exceptions or missing cover assets.

The scoped harness correction explicitly advances virtual sheet animation time,
asserts the native Setup sheet is displayed, scrolls category targets into view
and checks they are displayed before pointer injection. It waits again after
category changes, when sheet anchors and content height can change. Existing
artwork callback, cancellation/reset, identity, narrow viewport and single-engine
assertions are retained. These stronger visibility checks also expose an actual
UI placement failure if transition synchronization is insufficient.

The screenshot test draws the actual laid-out activity and native sheet dialog
decor views on Robolectric's native Canvas. It avoids the unsupported real-window
redraw handshake, still writes real front/Setup PNGs, and asserts both a visible
dialog and varied rendered pixels instead of accepting a blank image. It does
not invent screenshots or declare visual acceptance from synthetic fixtures.

The focused suite must run before acceptance. Workspace downloads remain blocked
by nativeruntime-dist-compat:1.0.16 redirecting to gcp.mirrors.maven.org (CONNECT
403); environment settings must permit that official host. A dependency failure
before test execution is not a pass for this repair. CI must verify all 22 tests,
including all seven previously failing paths, and retain front/Setup PNGs.
Jonathan/Marcus still verify Setup, cover change/reset/cancel and accessible Open
on a phone, including landscape and large text. Native work proceeds independently.
