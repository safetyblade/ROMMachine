# PERF-XB execution and QA packet — Stages 0 through 4

Owner: Jonathan. Engineering: Elliot. QA: Marcus. Architecture/provenance: Daniel.
Controlling issues: #60 (programme), #295 (X360 geometry), #86 (Xbox), #323 (360 digital separate).
This packet accompanies PR #367 and is **not** physical handset acceptance.

## Build and promotion
1. **S0 / baseline recovery:** repair SafeSave mapping: Room `SafeSaveSnapshotEntity.groupName` must reach `SafeSaveSnapshotRecord.groupName`. All Kotlin, Room, JVM, native receipt and connected APK checks must be green on the **same SHA**. Install APK over existing signature without clearing content, database or save files. Preserve previous Xenia and hakuX engine rollback.
2. **S1 / truthful observability:** X360 has native buffered event windows and guest sequence progress, never image-proven unique game frames. Original Xbox has a bounded report-time process CPU/PSS snapshot and native hakuX Vulkan/probe data. Both require the exact engine SHA, driver and title/scene IDs; all figures must retain their actual units and evidence type. Run locally saved benchmark reports through `python3 scripts/ci/xbox_perf_benchmark.py BASE.json CANDIDATE.json`. This rejects mismatched title SHA, scene, driver, device, settings and cache state, and does not declare FPS from presents.
3. **S2 / geometry correctness:** FIFA Street 3 (2008, Xbox 360 title ID 4541083B) is the owner's **progressive deformation regression**, NOT the same game as FIFA Street (2012) in external fork release notes. Compare NORMAL, SKIP_OFF and ORDERED_UPLOADS as independent one-launch options with consistent game/camera and warm/cold cache records. Log onset, compare photos/frames at 0,5,10,15,20min. Reproduce root cause before promoting global renderer policy. The existing XGFX page-validity tests and deferred pipeline counters are evidence tools, not proof that malformed graphics are fixed.
4. **S3 / X360 efficiency candidate:** the pinned XenDroid shader translation path spins on `yield()` and publishes translation before validity/PostTranslation. `shader_translation_wait.py` ports the upstream C++20 acquire/release + wait/notify pattern, including failure publication, through exact source anchors. The patch is bounded to native shader lifetime/CPU waiting. Native compile, real Xclipse gameplay, corrected geometry, sustained thermals and source/licence checks remain required. Do not use fake interpolation/frame generation, global frame skip, unproven driver overrides or a silent accuracy downgrade.
5. **S4 / original Xbox:** retain current hakuX pin, SDL3, FEX, native NV2A Vulkan and original Xbox FATX virtual HDD. Diagnose on-device CPU/PSS/native Vulkan/report and FEX/renderer conditions before implementing any bottleneck-specific source change. Candidate independent forks (BoxForever and hakuX-Advanced) need full source/ABI/license comparison and Xclipse evidence. No blind wholesale fork upgrade. **No source-level Xbox GPU/JIT speed change is claimed in PR #367.**

## Representative matrix
| Console | Game | Class | Target and caution |
| --- | --- | --- | --- |
| X360 | Forza Horizon (2012) | sustained streaming / thermal | original 30fps target, Android unverified |
| X360 | Gears of War 3 | heavy GPU/EDRAM/shader | original 30fps target, Android unverified |
| X360 | Virtua Tennis 3 | higher native-rate latency | original 60fps target, Android unverified |
| X360 | Sonic Unleashed | motion and dynamic scenes | original 30fps target, Android unverified |
| X360 | FIFA Street 3 | deformation correctness | 15-20min owner reproduction; native 60fps support not promised on Android |
| Xbox | Halo 2 | combat, fog and shaders | original game's intended rate, validate title |
| Xbox | Forza Motorsport | track/texture streaming | no 60/120fps mod in baseline |
| Xbox | Burnout 3 | fast input/audio/motion | no modified guest clock |
| Xbox | Fable: The Lost Chapters | character and texture integrity | renderer regression |
| Both | known-good legal homebrew / simple control | emulator startup, timing sanity | must actually boot first |

Missing games must be marked NOT TESTED, not silently replaced by synthetic claimed numbers. Each title used must be user-owned or appropriately licensed. No copyrighted games or firmware bundled.

## Controlled device protocol
- Same Samsung SM-S721B / stock Xclipse driver, power and thermal conditions; native 1x scale, exact app signer/version and native engine SHA/BuildID.
- Three 2-3 minute fixed scene runs per build, cold/warm caches separated, plus 15-20 min sustained matched scenario. Use A-B-B-A where possible; retain actual run variance.
- Report guest command progress, nonduplicated guest sequence evidence, host-presentation interval, p50/p95/p99 CPU-monotonic windows, shader/pipeline wait, audio underrun counts, total RSS/PSS and thermal status where available; label missing GPU execution/unique correct frames explicitly UNAVAILABLE.
- No performance PASS while skinning, geometry, depth, character appearance, audio, menu or save state is regressed. Live physical screenshots, saves/reopen, controller input and return to shelf are separate QA gates.
- Preserve save/user/profile/Room/ROM identity across install-over and selective engine rollback. Original Xbox HDD and X360 guest content are isolated.
- Each stage must present source diff + native library receipt + same-head APK artifact + reproducible measured result; owner and Marcus acceptance before merge. Do not close #295 merely because a synthetic patch test passes.

## Software acceptance status at authoring
S0: compile fix committed but latest full APK result pending.
S1: native X360 guest progress indicators, Xbox host snapshot and strict Python comparator implemented; handset baseline pending.
S2: existing XGFX tools retained; root-cause deformation and phone resolution **NOT YET DEMONSTRATED**.
S3: Xendroid+ shader translation wait/publication candidate implemented in pinned native builder; actual binary rebuild/phone metrics pending.
S4: original Xbox on-demand host telemetry implemented; bottleneck-specific native optimisation awaits measured evidence.


## S5/S6 selective implementation — 23 November 2026 (owner-directed)

**Actual code in this same draft PR:** stages S5a and S6a are bounded native
performance improvements. They do NOT claim complete upstream fork migration,
new device FPS, physical QA or graphics correction.

### S5a — Original Xbox hakuX pipeline cache commit reliability

The pinned hakuX code already has on-disk per-title Vulkan caches, SPIR-V
reuse and asynchronous pipeline prebuild. Do NOT recreate them or blindly
switch the native core. `scripts/ci/hakux_pipeline_cache_commit.py` now
patches the *exact pinned native source* so `g_pc.saved` advances only after
GLib successfully commits the pipeline cache blob (temp-file + rename). A
failed background write remains dirty and retryable at the next timed or
shutdown flush; the snapshot stamp prevents an in-flight shader compile from
being incorrectly marked persisted. No shader quality, cache keys, SPIR-V
version, renderer mode or FEX guest timing changes.

The native build recipe applies it after the existing pinned patches,
records its SHA in `source-manifest.json`, bundles its source in the
GPL corresponding-source archive, and labels the resulting native receipt
`cartdeck.pipelineCacheCommit=successful-atomic-write-v1`.
Fail-closed anchor tests do NOT replace actual ARM64 build or game reopen proof.

### S6a — Xbox 360 host Vulkan instrumentation idle overhead

The native Xenia diagnostics already activate only through Advanced. However,
five hot-path host instrumentation sites sampled the high-resolution clock
even when capture was disabled. `capture_start()` now skips those clock
reads unless capture is on, and `capture_finish()` keeps bounded timer
samples only for enabled sessions. The shader wait/publish patch, renderer,
buffers, frame policy, guest clocks, pipeline cache and CPU scheduler are
unchanged. Portable C++ test verifies off=0 and on=real samples; no assumed
FPS or frame-time gain without device A/B.

### Promotion and remaining work

1. Offline Python/C++ source gates including strict hakuX source anchors.
2. Build pinned hakuX ARM64 + pinned modified Xenia ARM64 with actual receipt,
   all 16KiB ABI/native build checks, no Gradle network timeout hidden as
   source error.
3. Build the same-head signed install-over APK, preserve native Xbox HDD,
   Xbox 360 guest saves and per-game bindings.
4. Compare A-B-B-A physical runs on Samsung SM-S721B Xclipse, and Adreno if
   available, with cold/warm pipeline caches, 15–20 minute visuals, p95/p99
   host timing, CPU/PSS/thermals, audio, input, save/reopen and rollback.
5. Separate future source diffs required for **S5b** GPU surface/descriptor
   reuse and FEX efficiency, and **S6b** Xenia Edge scheduler/I/O/audio/vertex
   work. These are not implemented by S5a/S6a. Port only evidence-backed
   individual changes, not wholesale forks or Adreno-specific driver hacks.
6. FIFA Street 3 #295 remains DEFERRED. A cache or host pacing correction
   never proves that progressive geometry corruption is repaired.

No master merge/issue closure/release without Jonathan approval.
