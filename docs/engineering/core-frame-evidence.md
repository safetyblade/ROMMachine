# Libretro core-frame evidence

The previous `GLRetroEvents.FrameRendered` is emitted whenever a frontend step
returns. It remains unchanged for existing callers and cannot prove the core
submitted video. Host `0.14.0-cartdeck3` adds `CoreFrameRendered`: the counter
advances only after a core's non-null, positive-size video-refresh callback
successfully returns through the existing video handler. A null callback is the
Libretro duplicate-frame signal and does not count. Both software pixels and
`RETRO_HW_FRAME_BUFFER_VALID` hardware frames count. The atomic counter resets
at host creation; observation occurs after the normal frontend step completes.
No renderer, core options, timing, state layout or saved data is changed.

This is narrower evidence than correct gameplay: a core can submit black pixels,
and successful callback handling does not prove Android display presentation,
sound, input or saves. Keep phone visual/audio/controller acceptance separate.
The existing host graphics-state, context-validation and input patches remain
present. The Maven version changes to prevent a previously cached host artifact
from silently omitting the added JNI/event contract.

`scripts/ci/test_libretrodroid_core_frame_signal.sh` compiles the real patched
header and checks duplicate/invalid/software/hardware callbacks, thread-safe
counting and reset across games. The normal host contract test suite runs it.
