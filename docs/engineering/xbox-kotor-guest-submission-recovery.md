# KOTOR guest submission recovery — #86 / #143

Baseline: master `a9b6e4b7` includes #187/#188. Native source remains pinned to
`a7cdb5a975e31730d488c47f6f10440b16c1e6f4`; this is an eighth versioned patch,
not an upstream engine replacement.

## Owner evidence

Star Wars: Knights of the Old Republic remains white after waiting and Start.
Samsung SM-S721B / Android 37 / Xclipse 940, engine suffix `29d40fcd30b6`:
COMPLETE_SAMPLES 3/3; white guest image; host serial and guest swap ID both 8.
Kernel entries rise 4 → 128 → 248 while frontend, decoded Kelvin, backend,
Vulkan draw, display-write and rejected-swap counters stay zero; budget remains
999752. Presentation succeeds. The #188 rejected-source guard did not trigger
in that interval. This places the observed lack of new work before frontend
command generation; it does not establish the exact blocked guest operation.

The kernel counter includes synthetic ISR/DPC/APC/callout returns. At this rate,
interrupt returns alone could explain it. The previous interpretation that it
proves the game's main thread is progressing was too broad: some guest callback
execution may continue while the main thread waits. Ordinary exports can also
run through guest fast paths without passing through the sampled `kcall` entry.

## Source correction

`native/dev/nv2a.c:nv2a_write` handled a direct PFIFO CACHE1 DMA_PUT write by
falling through to `memcpy`. It never called `hle_pb_pushed` or `hle_pb_put`.
The decoder in `pbmode.c:decode_thread` sleeps on a kick semaphore; therefore a
direct PUT could leave newly submitted work pending without waking it. This
source defect is independent of the final Vulkan driver or Android surface.

Channel NV_USER writes already used the scout → release-store → decoder-wake
sequence. Route both aliases through one `fifo_dma_write` helper. A PUT notifies
the existing report scout before publishing, release-stores the register, then
wakes the decoder. GET resets the scout after storing the new register. REF
remains an atomic register write without a spurious wake. Direct CACHE1 writes
no longer reach a non-atomic fallthrough store. No forced draw, skipped wait,
timer change or title-specific compatibility guess is introduced.

The owner capture does not include FIFO writes or registers, so **KOTOR's use
of this broken path is not yet demonstrated**. Do not report the title fixed
until the new engine produces actual changing gameplay on the handset.

## Reuse the existing explicit debug probe

Keep the same three pixel pairs, their capture scheduling and one-million-event
budget. Add `fifo_put`, `fifo_get`, decoder-pass and trap/FIFO-closed/flip/no-DMA
stall counts. At each collection append PB mode, active decoder, atomic FIFO
GET/PUT, FIFO access, interrupt, DMA instance and flip registers. Vulkan's
existing `push=0` capability field is not the guest pushbuffer mode; the new
`pb_mode` / `pb_active` fields make that distinction explicit.

On the vCPU, while the probe is armed, count named kernel ordinals, including
synthetic return ordinals. Every 32 entries copy thread-state counts, the first
waiting thread's object count/first guest object address/timeout presence, and
the four most recent entries from the existing normal-export ring. This retains
an export that blocked before the probe began. Export results and caller guest
code addresses are included; arguments, guest strings and paths are not.

The presenter reads atomic copies only. `snapshot_consistent=0` marks an absent
or concurrently updated thread/recent snapshot; do not infer a coherent wait
state from it. Kernel top counts are approximate collection-time totals and
thread states are sampled at a kernel entry, not at the GPU capture instant.
A waiting thread's object address does not identify its object type. Zero
ordinary kernel entries does not prove no guest instructions are executing.

The bounded report buffers expand to retain all three enriched samples. Existing
sample-completion logic and the original numerical pixel infrastructure remain.
No continuous logging, extra UI probe, automatic restart or automatic relaunch
is added. The probe stops at the original limits. Previous engine binaries,
HDD, UDATA/TDATA, saved games, mappings and indexing remain untouched. Core hash
versioning/rollback and patch-source hashes remain; the native manifest adds
`cartdeck.guestSubmission=fifo-put-wakeup-v1`.

## Validation and CI handoff

- 15 targeted hakuX tests pass, including three new compiled-C tests that execute
  the shipped FIFO helper and guest sampler/report code: publish-before-wake,
  both aliases, GET visibility, REF behavior, disabled/bounded sampling,
  callback-only versus ordinary-export counts, pre-probe waits, snapshot flags
  and short-buffer safety.
- Whole `kernel.c`, `nv2a.c` and `pbmode.c` pass host C syntax checks with strict
  warnings. NV2A/PB checks use a declaration-only Android property-header shim;
  the PB check suppresses its pre-existing missing-field-initializer warning.
  These checks do not establish ARM64 ABI/link or Vulkan runtime correctness.
- All eight patches apply cleanly in build order to the pinned source.
- New tests are in Actions source preflight. No full native/APK build was
  launched or polled locally. CI owns the Android native compile/link gate.

## Next handset decision

Use the same KOTOR cartridge and system Vulkan driver. If video changes,
validate meaningful gameplay, audio, controls, clean reopen and persistent HDD
data. Otherwise invoke the existing debug blank-viewport probe once and retain
COMPLETE_SAMPLES plus these new fields:

- `kernel_top`, `recent_exports`, `threads_at_kcall`: identify the actual export
  or synthetic return that repeats, and whether the title threads wait/suspend.
- `fifo_get`, `fifo_put`, `pb_mode`, `pb_active`, decoder stall counts: determine
  whether the guest submitted work, the decoder woke, or a trap/flip/DMA setup
  prevents consumption. Register inequality alone is insufficient across a
  concurrent reset; use the three samples and counters together.
- Existing frontend/backend/draw/display counters and pixels: locate the first
  stage that fails to advance after the corrected submission path.

No attached handset is available here. The exact KOTOR wait, new nonwhite
frames and gameplay acceptance remain hardware gates; #86/#143 stay open.
