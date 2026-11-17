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
