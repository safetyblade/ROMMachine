# Xbox guest publication recovery — #86 / #143

Baseline inspected: ROMMachine master a0f3a1ee (after PS2 #182), hakuX
`a7cdb5a975e31730d488c47f6f10440b16c1e6f4` with the six existing CARTDECK patches.
Dreamcast and PS2 engineering is outside this change.

Owner evidence already establishes white pixels in the 640×480 HLE display
image, upstream of the successful Android Vulkan presentation. No replacement
pixel capture, driver override, Android layer change, restart, or HDD migration
is introduced here.

## Verified source defect and correction

`vk/back.c:cmd_swap_present` called `hle_disp_begin_write` before checking the
source surface, successful `hle_surf_ensure`, or a color aspect. `display.c`
marks that slot pending immediately; a late flip can select it, and timeline
completion makes it ready even when the conditional copy never ran. Therefore
an absent/failed/depth source could publish stale or unwritten display memory.

Validate source existence, allocation, color aspect and nonzero dimensions
before reserving a slot. On rejection, retain the previous display image and
submit the command's timeline value so guest fences still complete. No invented
frame is published. A valid source follows the original gamma/blit path.
This source defect is demonstrated by compiled guard tests; **its occurrence on
the owner's title is not established** and the patch is not a claimed fix for
all-white game rendering.

## Existing probe, additional bounded stage evidence

The old `serial` is `g_ps.serial`, incremented when the host acquires a changed
display frame, not an instruction counter. Retain it for compatibility and add
`guest_swap_id` from `HleDispFrame.swap_id`. Unchanged serial alone cannot prove
that guest execution stopped.

An explicit existing debug pixel-probe request arms counters once per process.
They stop after three native sample collections or one million total events,
whichever comes first. Ordinary launches leave the counters disabled. No paths,
guest memory, screenshots or new periodic logs are collected. Values accompany
the existing samples, labelled `stages_at_collect` (collection time, not GPU
capture time):

| Field | Evidence |
| --- | --- |
| kernel_calls | Guest entered kernel HLE `kcall`; positive deltas establish some guest execution. Zero does not exclude a guest compute loop or blocked execution. |
| pb_method | Kelvin methods delivered by the NV2A pushbuffer decoder; zero is also possible in D3D hook mode. |
| fe_cmd / fe_draw | Commands/draws generated for the native renderer. |
| back_cmd / back_draw_dispatch | Backend receives commands / reaches draw dispatch with a usable pass. Dispatch is not proof of a Vulkan draw. |
| vk_draw | Actual draw commands recorded in draw.c and the batch flush path; recording does not prove GPU completion or meaningful pixels. |
| display_write | Valid source and available display slot reached the copy/gamma recording path. |
| swap_reject | Missing, failed, noncolor or zero-sized source rejected before publication. |
| budget_left | If zero, stopped counters must not be interpreted as stalled guest execution. |

Core boot/failure, XBE/kernel frontier, FEX faults, pipeline/shader failures and
existing native statistics remain the relevant accompanying logs. Do not infer
successful XBE boot or instruction progress from wall-clock guest cycles.

## Validation and remaining hardware gate

12 targeted Python/C tests pass, including exact changed native guard compilation
with `-Wall -Wextra -Werror`, invalid-source/fence behavior, default-disabled and
exhausted probe budgets, existing pixel summary and native package contracts.
All seven patches apply cleanly in build order to the pinned revision. The new
test is in Actions source preflight. No full native build or APK was awaited.
The complete ARM64 engine link and Vulkan execution remain CI/device gates.

Native patch hashes enter source-manifest.json; the manifest also identifies
`cartdeck.guestPublication=validated-source-v1`. Existing engine version identity
uses revision plus core SHA, preserving the previous installed version and rollback.
HDD, UDATA/TDATA, saves, mappings and indexing source are untouched.

Required owner evidence: run the same title with the system Vulkan driver and
new engine hash, obtain one COMPLETE_SAMPLES 3/3 report with stage counters and
guest_swap_id, and retain nearby FEX/HLE/pipeline fault logs. Compare counter
deltas while budget remains. A rising swap_reject isolates invalid source
publication; frontend draws without backend commands isolates consumption;
backend dispatch without vk_draw directs investigation to pipeline/geometry;
recorded draws and new all-white display images directs investigation to render
target content/shader/texture state. The existing pixel probe does not sample
intermediate render targets, so nonwhite render-target writes cannot yet be
asserted. Audio, controls, changing meaningful gameplay, clean reopen and HDD
persistence still require owner acceptance. Issues must remain open.
