# EMU-RECOVERY-02 implementation ledger

Baseline: accepted master `4b46aa82dfc1ab15b27a69f1bc9171501b6b2501`, including
landed #298 and GC #294. Branch `engineering/emu-recovery-02-20261117`.
No save, NAND, identity, schema, signing, profile, controller mapping or shared
menu contract changes. PS3 distribution gate unchanged. No master merge.

## Wii U independent increment

Owner APK90804540 / integration3977c0c7, Cemu SHA256
`18911ab654df005c7142ff67b3b34e43c1adfe4733c5bba5cc97d09b58a2c48c`,
Build ID `ceed2b7dd2291941a10ce38eb5d99339ed751896`, upstream444c5852.
Exact producer14265749150 / run43947615252 retained locally and in Actions;
its output hashes and recorded atomic64 helper/source proof reverified. The
same core is deliberately retained. This is not evidence of another SIGBUS.

Confirmed source defects:
- AAudio error -899 causes audioBatch to request guest shutdown. Callback now
  marks the disconnection only; VM/control-loop service closes/reopens safely,
  including while paused. Close/reset occurs after callbacks quiesce. Counters
  and historical error remain; bounded one-second retries if output unavailable.
- shutdown_device_idle marker was never cleared after vkDeviceWaitIdle returned,
  and even an empty destructor set it again. It now brackets only a real wait,
  retaining completed duration/result separately. Old marker age is not proof
  of a current Vulkan stall. Required Vulkan synchronization remains intact.
- Surface loss wrote the menu pause flag independently of Kotlin's cached pause
  gate. Surface validity is now independent; only frame-boundary VM control
  acknowledges pause/resume. Stop is terminal/idempotent. Park uses a command
  condition variable, with a timer only for audio-recovery servicing. No teardown
  is added to normal pause. Android surface replacement remains VM-owned.
- Final report now retains native terminal state; Copy report refreshes it.
  Advanced reports include requested/acknowledged lifecycle sequence, surface
  generation/state and completed device wait, plus audio recovery counters.

Tests: AudioTest ASan/UBSan (100 disconnect/reconnect cycles, callback never closes,
FIFO fresh after reopen, persistent counters, failed reopen bounded, stop cannot
resurrect); SessionControlTest ASan/UBSan (100 pause/resume cycles, surface-loss
resume ordering, terminal commands); existing timing/negotiation/core-lifetime
regressions and real Android ARM64 host compile PASS locally. Atomic64 attestation
3 tests PASS, CI01 remaining-engine contracts7 PASS. CI APK gates pending.

Frame-call diagnostics now record video/audio callback wall-time deltas associated
with the longest retro_run. These are inclusive CPU wall times, not GPU execution
or a diagnosis of the owner's 4.43s call. Battle visual artefact root cause remains
unconfirmed without a reproducible scene/trace; no shader/format/depth hacks made.

Selective CI verifies complete old receipt outputs then pins core hash/Build ID,
upstream and current atomic repair; rebuilds only frontend host; issues a new
combined receipt. Unchanged core/other engine recipes remain unchanged. A missing
or different approved core fails closed rather than silently compiling all cores.

Physical acceptance PENDING: 10 menu cycles in cutscene and battle, same in-memory
progress/audio/controls, 15-minute play, save/normal quit/reopen, Sonic regression,
rotation/surface replacement/controller hotplug and audio route disconnect/reconnect.
Rollback: exact APK90804540 artifact14268751754 and symbols14268936742 retained,
plus latest GC APK90829006 artifact14296792285. Do not uninstall or clear data.

## Xbox 360 independent increment

Progressive FIFA Street3 deformation is owner-observed. Latest retest's binary
identity is unknown; never substitute Wii U diagnostics or the older25143bdb core.
Current accepted instrumentation and stock Samsung Vulkan remain preserved.
Sustained resource-coherency/lifetime investigation in progress. No physical
rendering correctness or frame-rate acceptance asserted.

### Xenia sustained constants verification — no speculative engine change

Initial full-buffer test found stale values outside a sparse shader's read map.
Review of Shader::AnalyzeUcode showed dynamic-addressed shaders already receive
all256 bits in that map. The initial test therefore did not establish stale
**consumed** constants or a rendering defect. The proposed full-map override is
withdrawn; do not ship it or count it as a confirmed geometry repair. An earlier
intermediate native build may contain that rejected override; its receipt cannot
match the final restored recipe. The final engine reuses the accepted source and
verified binary, preserving pacing and instrumentation. This correction supersedes
initial #295 commentary about a confirmed interpreter-cache defect.

Retained regression compiles actual pinned map-selection and bulk-write method
bodies against minimal types. It passes10000 animation-like updates across all256
registers in dynamic read maps, static sparse reads, unused writes and transitions
under ASan/UBSan. This is sustained source verification, not gameplay simulation
or a binary runtime substitute. No graphics correctness acceptance inferred.

Audited existing upload pool capacity bounds/flush-before-retire and fence reclaim,
constant UBO invalidation, descriptor retirement at frame_completed_, atom-aligned
noncoherent flush/invalidate, shared-memory host/device aliases and barriers.
No confirmed FIFA geometry root cause with available evidence; exact latest binary,
scene/time-to-onset and GPU capture remain missing. No blanket synchronization,
shader, driver, resolution, frame skipping or frames-in-flight changes applied.
Existing timing windows, skipped-draw counter, RSS/thermal/audio metrics retained.
Physical comparison: identical match, multiple players,15-minute play, restart and
rematch, cold/warm cache, Advanced reports/video at onset, save/reopen. Do not infer
correct geometry/FPS from host presents. Stock Vulkan baseline.

## Verified delivery — 17 November 2026 (Sydney)

[PR309](https://github.com/safetyblade/ROMMachine/pull/309) remains unmerged.
[Run44030747663](https://github.com/safetyblade/ROMMachine/actions/runs/44030747663)
PASS; tested branch source `36edad7549bf474deae8810b3cfa60123a03b5e8`, actual
PR integration checkout `3d627b811b1a644754d3ef76cfdebdcd1b5a73d8`.
The delivery-record follow-up changes documentation only; it is not the APK source.

[APK artifact14305587283](https://github.com/safetyblade/ROMMachine/actions/runs/44030747663/artifacts/14305587283),
versionCode **90839697**, versionName1.17.0-DEBUG, package
`dev.prestwich.cartdeck.debug`, **196960215 bytes**.
APK SHA256 `89caa4f57375b243667dddf13079c800a302cbcb7012d90f38ea94be05af2e21`.
Signing certificate SHA256
`ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8`
matches the accepted GC upgrade baseline; versionCode increases. This proves
package/signing upgrade compatibility, not a performed Android installation.

Only the final changed Wii U host was compiled for this candidate:
SHA256 `88c2292b83c86929105b8bd9ebe66cc1f2f9dc5d118574e825764e63a379ee67`,
BuildID `c4a3323e54ad588363516cd5b2e4347c4ce6083b`.
Its core remains18911ab6 / BuildIDceed2b7d above. Producer14305201980 was downloaded
and independently receipt/hash/ELF/atomic64-verified locally. The new host preserves
all11 JNI entry points. Xenia remains accepted SHA256
`6881bb62ae01cf522935f5d2b768412cae5b2d71306e2f4368358f07edbd4937`,
BuildID `fafc8f7f9c449d26f55c4c4c87a2fa4b5d9850bf`, source779680ad.
Final producer explicitly reused artifact14267658103, now retained as14305775578;
no rejected interpreter override is packaged. Final combined symbols14305397885.
Full native inventories/notices/source receipts accompany the APK/native artifacts.
Machine-readable exact delivery facts: [DELIVERY.json](DELIVERY.json).

CI: formatting, all receipt/source gates, new sustained constants regression,
existing Wii U tests, atomic64 sanitizer test, host lifecycle/audio tests, native
ELF/JNI verification, combined inventory/upgrade APK verification PASS. Downloaded
unit/UI artifact14305632300 independently sums **546 tests,0 failures/errors/skips**
across128 XML suites. All42 packaged native libraries are ARM64 and pass16KB ELF
alignment. No physical16KB-device or gameplay claim.

The first intermediate host run lacked pinned CMake; provisioning is corrected.
Initial broad legacy workflow filters also launched unrelated experimental builds;
recovery native jobs are now excluded there (source tests retained). Intermediate
speculative Xenia work was withdrawn; the obsolete run was superseded. Final
producer receipts demonstrate unchanged-engine reuse, not unnecessary compilation.

### Remaining owner acceptance (both issues OPEN)

1. Install90839697 over the existing signed debug build; keep app data/ROMs/saves.
2. Bayonetta2: ten menu pause/resume cycles in cutscene and opening battle; verify
   same progress, audio and controls; play15 minutes, save, quit/reopen/load save.
3. Repeat Sonic; rotate/recreate surface, hotplug controller and disconnect/reconnect
   audio route. Check advanced session/pause acknowledgements and audio recoveries.
4. FIFA Street3: capture exact Xenia SHA/BuildID, identical match and time-to-onset,
   multiple characters, sustained15-minute segment, restart/rematch, warm/cold cache
   comparison where controlled. Capture video/screenshot and Advanced reports at
   boot/match/onset/end; compare frame-time/queue/audio/RSS/thermal deltas.
5. Rendering artefacts remain UNRESOLVED. Supply matching scene/trace after testing;
   do not call more presents or passing source tests rendering acceptance.

GC/source/save baseline remains protected; no schema, package identity, signing,
controller mapping, shared-menu/presentation or storage changes. Preserve previous
APK/symbol artifacts and existing engine rollback; never uninstall/clear data to
force downgrade. PS3 remains excluded with unchanged distribution controls.
