# Complete bounded Xbox pixel report

Refs #86, #143. Builds on merged #168 and #169; not a rendering fix.

Source-proven diagnostic races: Android's PixelCopy result was copied immediately,
regardless of whether the native three-sample sequence had finished. A PixelCopy
callback arriving after its three-second timeout could overwrite the timeout and
copy a second report. An in-flight diagnostic could also overwrite the saved
native failure report after the engine stopped.

The explicit debug action now joins both terminal observations before copying
once. Native evidence is the existing bounded CPU report from #168; polling that
text schedules no GPU work. Three guest/swap pairs plus successful SurfaceView
capture produce COMPLETE_SAMPLES. Unsupported/error paths produce INCOMPLETE_SAMPLES.
A six-second diagnostic budget produces CAPTURE_TIMEOUT with the actual available
pair count; no result classifies white pixels as gameplay, engine success or an
engine failure. The frozen native snapshot used for completion is included in the
same copied report. Manual Copy engine/GPU report remains available immediately.

Surface completion is first-result-wins. Late callbacks recycle the bitmap but do
not overwrite the report. Pause, exit and engine failure cancel automatic report
publication. A buffer still owned by PixelCopy is recycled only after its callback,
not at timeout. The existing one-request/three-native-pair/one-SurfaceView bounds,
debug-only feature gate, numerical-only export, native graphics/quirks, preferences,
HDD/UDATA/TDATA, rollback and controller state remain intact.

Five pure tests cover native-first and surface-first completion, slow native work,
late callbacks, unavailable transfer, deadline, cancellation and single publication.
Android production/test compilation passes. Root ktlintCheck passes in a local
integration with #170; that separate display-format fix is not included here.
These checks are separate from handset acceptance. The unrelated Display-01 formatting fix stays in #170 and cartridge
experience runtime failures stay in #155; no CI gate is removed.

## First meaningful owner run

Once a verified debug APK contains #168/#169 and this follow-up, open a lawful Xbox
cartridge until the viewport is white. In the CARTDECK menu choose Probe blank
viewport (debug), then keep the resumed game foreground until the numeric report
is copied (up to six seconds). Retain capture_status, native_pairs, each serial's
guest/swap color/alpha/hash, gamma/format data and SurfaceView sample/geometry.
Incomplete/timeout evidence must not be interpreted as a blank-frame success.

Guest-white evidence directs the next patch to guest execution/NV2A/HLE; varied
guest but white final swap points to Vulkan composition; varied final swap but
white Android samples points to SDL/surface/compositor. Varied SurfaceView output
with a white visible viewport requires Compose/layer inspection. Pixel hashes
across transformed/scaled frames are not expected to match bitwise. A targeted
rendering correction still requires the resulting device evidence, followed by
changing graphics, audio, input and persistent HDD save/reopen acceptance. No
Xclipse driver substitution or additional Vulkan-present test is introduced.
