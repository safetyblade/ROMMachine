# XB-01–03 — Original Xbox source execution and physical gate
Base: consolidated CARTDECK master; only original Xbox hakuX renderer (not Xenia).
Source pin: `rfandango/hakuX@a7cdb5a975e31730d488c47f6f10440b16c1e6f4`.

## XB-01 — implemented baseline
The existing Advanced-only on-demand native probe records up to three asynchronous captures with identical 64-pixel sample counts in guest and Vulkan swapchain. The existing presentation code already uses linear + clamp-to-edge. The normal SDL3/Vulkan render path, shader compilation, JIT, input, audio and virtual HDD remain unchanged.

## XB-02 — implemented bounded edge diagnosis
The new pinned-source transform replaces the old 8×8 interior sampling coordinates with 4 sides × 2 rings × 8 points per image. Outer ring samples the exact boundary; inner ring samples one pixel inside. The same normalized positions are measured from the guest and swapchain stage, with stage dimensions included in the existing report. This is sparse sampling (not complete edge coverage). Differences cannot conclusively establish causality without scene-matched screenshots, format/channel and driver validation.
Abort if source anchors drift, preserve the original on-demand cap of 3 frames and no extra GPU queue wait. Native source receipt includes the transform SHA. No always-on instrumentation.

## XB-03 — source optimisation status
Retain the existing successful-write-only asynchronous shader-pipeline cache persistence transform already merged in master (`hakux_pipeline_cache_commit.py`). This prevents an unsuccessful cache write being marked saved; it is not proof of faster FPS. No arbitrary shader, FEX JIT, Vulkan sampler or frame-skipping change is justified before on-device measurements. Only pursue a source-level optimisation after a measured CPU/GPU wait or resource problem is identified.

## First hardware run
1. Same original Xbox game, title SHA, device, stock Vulkan driver and fixed scene; screenshot and capture a copyable Advanced GPU/pixel report before and after reproduction.
2. Note exact visible edge (left/top/right/bottom), whether at the guest image edge or display glass, game viewport dimensions, aspect ratio, appearance (continuous stripe versus transient bleed), and whether UI/menus also show it.
3. Compare 1× and normal presentation, FXAA/SGSR only when actually available; don't invent an integer scaling mode. Hold driver, graphics accuracy, clock, save identity and game scene fixed for matched trials.
4. Capture cold and warm cache, 3 repeats where practical, 15–20 min sustained stability, geometry, texture, controls, sound, exit/reopen and saves.
5. Run existing `scripts/ci/xbox_perf_benchmark.py` only with comparable confirmed graphics-integrity reports. Host present counts are **not** real guest FPS. Do not promote a performance improvement without a matched baseline and zero visual regression.

### Stop conditions
If the 1px fringe is present in the guest source, inspect NV2A render target and scissor. If absent from source and newly present at output, inspect the final-presentation UV/texel-centre math, FXAA/SGSR, colour transforms and extent. No global crop, opaque border hacks or generic overscan default until cause verified.

XB-01 and XB-02 are source/CI candidates. XB-03 has a retained cache reliability improvement and a measurement gate, but no measured speedup or further speculative native speed patch. Owner physical PASS remains open.
