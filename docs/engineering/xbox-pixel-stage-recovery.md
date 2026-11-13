# Xbox white viewport: bounded pixel-stage evidence

Refs #86, #143. PARTIAL: no visible Xbox gameplay or rendering repair claimed.

Owner evidence already proves an accepted guest Vulkan present, successful
submit/present results and no native failure on Xclipse 940. The viewport is still
white. Rechecking those success flags cannot identify where pixels become white.

An explicit development-build pause-menu action now requests three asynchronous
8x8 sample pairs from the acquired guest display and the final swapchain image,
spaced over 30 presenter iterations. Each reports RGB range, near-white/black
sample counts, alpha range, numerical FNV fingerprint, guest/swap dimensions,
swap format and gamma-ramp range/change count. No image, raw pixel dump or game
memory is written or copied to the report.

Copies are part of the existing presenter command buffer. Collection uses its
already-required successful timeline wait before mapped host-coherent memory is
read or freed. No new blocking GPU wait or timeout is introduced. Probe buffers
are bounded to three 512-byte logical allocations; incomplete GPU work retains
its allocation until process teardown rather than freeing a resource still in
flight. Unsupported transfer-source usage/format and allocation/submit failure
are reported; they do not substitute a different renderer or native driver.

Only an explicit debug action enables transfer-source swapchain usage and sample
copies; the default presenter path is unchanged. The upstream fresh_clear and
present_cpu_wait workarounds remain intact. JNI/provenance feature detection hides
the action for retained older engines. Controls, HDD/UDATA/TDATA, engine rollback,
package identity and regular launch behaviour are unchanged.

After resuming the same guest, Android PixelCopy samples the actual SDL SurfaceView
once into a bounded 32x32 in-memory bitmap. It exports only the same numerical
summary and the surface/overlay size, visibility, alpha, Z and opacity properties.
The bitmap is recycled after callback; a bounded callback timeout reports missing
evidence, without cancelling/freeing a native copy still in flight. Pause or exit
before the observation prevents a PixelCopy request. The normal Copy engine/GPU report includes all
available stages and remains available for previous engines.

## Interpreting evidence

- White guest samples point before the presenter (guest rendering/NV2A/HLE frame
  production). A white swapchain alone does not establish that guest output was white.
- Varied guest samples but white swapchain samples focus the investigation on the
  presenter, image sampling, gamma or format path. Check gamma evidence before
  assuming a GPU workaround is wrong.
- Varied final swapchain samples but white SurfaceView samples focus on Android
  presentation, Surface lifecycle or driver/compositor handoff.
- Varied SurfaceView samples with an owner-visible white viewport require overlay
  and composition inspection, guided by visibility/opacity/Z/bounds. PixelCopy of
  a SurfaceView does not claim to capture the final multi-layer Android compositor.

Samples, hashes, frames and coordinates across stages are not bitwise comparable:
scaling, rotation, gamma, pixel packing and capture timing differ. White samples
are evidence at sampled positions, not proof every pixel or every game is blank.

## Acceptance and remaining blocker

Pure Kotlin and exact C helper tests discriminate white, black, varied and alpha
samples. The patch applies to the pinned hakuX revision plus the merged patch stack.
Android production/test source compilation and root Kotlin lint pass; native CI compilation remains a separate check;
no full native rebuild is run locally. The workspace has no attached Xclipse phone.

On the owner phone with the matching new engine installed, open a lawful Xbox
fixture until the white viewport persists, open the pause menu and choose Probe
blank viewport (debug). The same guest resumes. After the report is copied, retain
its serials, all three pixel stages, gamma/format values and surface geometry.
If GPU work stalls or transfer support is absent, retain that failure result.
Do not reset HDD or toggle quirks. Use the demonstrated white-producing stage to
implement the next native correction, then verify visible graphics, audio, controls,
HDD saves, exit/reopen and install-over. Issue #86/#143 stays open until phone
acceptance; numeric sampling is not a gameplay PASS.
