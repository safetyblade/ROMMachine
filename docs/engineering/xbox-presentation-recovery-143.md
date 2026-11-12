# Xbox guest-presentation recovery (#143)

## Demonstrated source defect

Pinned hakuX 1.3 source `a7cdb5a975e31730d488c47f6f10440b16c1e6f4`
sets `g_shown` in `native/app/present.c::nc_present_frame` immediately after
`hle_present_frame` acquires a guest image. `hle_present_end` submits and presents
that image afterwards. Therefore the previous JNI `nativeGetFrameShown` could
report success before `vkQueuePresentKHR`, including when presentation fails.
This is a demonstrated false-success defect, **not proof of the phone white
screen's underlying GPU or guest-execution cause**.

The presentation patch makes that JNI probe read a latch set only after Vulkan
accepts a present with an acquired guest image (`VK_SUCCESS` or
`VK_SUBOPTIMAL_KHR`, following successful submission). Surface creation, an empty
compositor frame and guest-image acquisition alone cannot set it. The latch is
process-local; existing single-run FEX/relaunch behavior resets it for each game.

## Executable recovery path

- The existing native process, SDL surface, controller bridge, version store,
  disc FD, HDD and rollback are retained. No other console host is changed.
- Only a selected package declaring `cartdeck.presentation=vk-present-guest-v1`
  can record presentation success in the existing engine registry. The selected
  engine identity is recorded after the actual installed version is selected.
- Older rollback packages can retain their acquired-frame behavior without being
  falsely certified as guest presentation or killed for lacking the new JNI probes.
- A 90-second foreground startup budget exposes a recoverable failure instead of
  an indefinite blank surface. Pausing/backgrounding/menu time is excluded, and
  long device-sleep gaps are bounded. An unexpectedly stopped core is reported
  immediately. Existing intentional quit/relaunch handling remains separate.
- The failure screen offers a copyable report, return to shelf and a retained
  previous engine when available. No automatic retry loop is added. Runtime
  outcomes are recorded through #153's registry; no parallel engine manager exists.

The existing pause menu also offers an on-demand copy button. It records a manual
snapshot without marking an engine failed, including when accepted presentation
still looks white.

The report snapshots the actual Vulkan physical device name/vendor/device/driver
and API version, capability floor/tier/format flags and active quirk mask. It also
records the most recent queue-submit and queue-present `VkResult`, native boot
state, failure category and whether a guest present was accepted. Before those
stages are reached it reports unavailable/pending values, not invented success.
Known source URI/title, disc FD and app-storage paths are removed from the native
failure message. Proprietary content is never bundled or uploaded.

## Xclipse and acceptance boundary

The owner's SM-S721B/Xclipse 940 finding remains relevant. The pinned engine
already contains experimental non-Adreno compatibility paths; this change reports
their actual capability/quirk decisions rather than forcing Adreno-only drivers
or speculative universal rendering settings. It does not replace the GPU backend.

Even an accepted Vulkan guest present does not establish meaningful pixels,
audible sound, working controls or save persistence. If the phone still shows
white with `guest_present_accepted=1`, investigate guest GPU drawing and SDL/UI
composition using that report; do not label it a first-present timeout. If it is
zero, use the boot/capability/submit/present evidence to isolate the failing stage.

Phone acceptance remains required: lawful ISO/XISO through the existing scanner,
actual visible guest output, audio/pad input, clean exit and repeat reopen with
HDD save data preserved; verify explicit rollback and install-over. A matching
upstream-host comparison can distinguish an integration defect from upstream
Xclipse/game compatibility. No new phone gameplay result is claimed here.

## Source validation and CI handoff

Changed native translation units are checked with the retained Android ARM64
compiler flags using `-fsyntax-only`, without rebuilding the engine. Their bytes
are matched to the complete patch stack applied to the pinned source. JVM tests
cover foreground timeout, accepted presentation, stopped core, legacy rollback
and pause budget. ABI fixture tests reject packages missing presentation/GPU JNI.

The existing builder records all patch hashes in provenance and corresponding
source, emits the presentation contract property, and verifies the new ABI. PR
#150's source preflight and subsequent native/APK verification remain authoritative.
Full builds execute independently; #143 stays open for runtime acceptance.
