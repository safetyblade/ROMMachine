# EMU-RECOVERY-01 implementation ledger

Accepted source/rollback checkpoint: `b81400035d30502a09442225c71a27816d4737f2`.
Recovery branch: `engineering/emu-recovery-20261117`. No master merge authorised.
Protected independent GC lane: #294, `24f0ee27aad90e3dc406d1a3698b281626ecc2f1`.
Only CI-01 files were recovered from that revision; no GC runtime, menu, controller,
Display, library, save root, schema, package ID or signing changes were imported.

## #297 PS3-PKG-01 — distribution BLOCKED

Owner screenshot version is unknown. The issue references artifact **14197824895**
from run **43805557009**, source `f6d3fd66b372a514cebc23fa28e9569f27c4b66c`.
Its successful build log explicitly records **include_ps3=false**, versionCode
**90747016**, package `dev.prestwich.cartdeck.debug`, APK SHA256
`9669c7e0eeae5c64c7b8f7c353efb7ba9a063e92b5cb28a05d0494198ee7f97e`.
This is NOT the separate Xenia report's 90747632. PS3 source integration does not
make that PS3-excluded artifact PS3-capable. Crysis 3 compatibility is untested.

No licensing clearance is recorded in #198/#297 or the reviewed source history.
Required correction before PS3-inclusive APK: owner distribution decision covering
GPL compatibility/corresponding source obligations, then an explicitly recorded
`distribution_licensing_cleared=true`. Firmware/games/RAP must remain user-supplied.
Source-only engine work remains preserved; normal non-PS3 APKs may continue.
The combined consumer now rejects include_ps3 without clearance before staging;
the PS3 source workflow's combined APK job only runs after that recorded control.
PS3-inclusive builds must also pass their actual ELF/dependency/checksum inventory.

CI-01 recovery is limited to independent producers, immutable receipts, resolver,
source retention and APK consumption. No old GC/CI branch was merged. Unchanged
native components must be reused; missing/mismatched components stop assembly and
identify an individual build. No all-engine fallback is authorised by this lane.

## #296 WIIU-CRASH-02 — shipped provenance first; physical FAIL remains open

The referenced 43805557009 candidate staged Cemu SHA256
`15980240cc76f97bf270c31d1dfcf39a7cc9f9b40acc4d2c75fe30134ca669d5`,
Build ID `3d9c4d31dbe5a94db49c4044c553ebac262bdeb1`.
It predates #292; it is neither old b956dab nor the local corrected 5ccae build.
A different checksum alone does NOT establish patch presence. Exact source receipt
and binary inspection remain required. No new owner tombstone or installed Cemu
hash is supplied for the current continuing crash; do not invent its signature.

## #295 X360-PERF-01 — owner baseline, not benchmark acceptance

Owner build 90747632, source `779680ade075d2d2a4c589c6a8d57a8e26c116c0`,
SHA256 `25143bdbd51871179272ebcb58af47df116af440233f254d330182776bdc259e`,
Build ID `1fc50886db14b97c1a109e712c9094896bad6fd1`.
FIFA Street 3 gameplay is owner-observed with stutter/visual defects. Present/JIT
counters are not unique correct images, FPS, simulation speed or fidelity proof.
Device measurements and physical acceptance remain PENDING; no owner wait required.

## Delivery and rollback

All three issues remain OPEN. Each increment must add its source/native/test/CI
receipts here. No data clear, reinstall, save conversion or identity change.
Keep old engine versions and their matching symbols. A new candidate is not master
acceptance and must not displace the owner-tested APK until explicitly accepted.

### Increment 2: confirmed stale candidate / patch attestation

Downloaded exact CI-01 Cemu artifact **14197560121**, source run **43801396773**,
producer revision `d61303e268360d35774955b8283cde73336f87eb`. Its core hash and
Build ID match the 43805557009 staging log. Its source-patches.json/native receipt
contain ONLY the USB/CMake manifest repair: **the #292 atomic64 repair is absent**.
The candidate is conclusively pre-fix even though its checksum differs from b956dab.
The owner's subsequently crashing installed Wii U APK is not uniquely identified;
no fresh fault mechanism is inferred without its exact report.

Correction: staging/native evidence now requires the actual pinned atomic64 CPP
hash plus current helper hash. This rejects the real 15980240 artifact, missing,
duplicated or modified proofs. Current CI-01 Cemu identity includes the changed
Cemu verifier, invalidating Cemu only; other engine recipes remain unchanged.
Actual loaded-core SHA256 is computed once off the UI thread before load and
reported with full Build ID/source revision. No per-frame hashing or ROM paths.

The existing corrected local Cemu binary remains SHA256
`5ccae18918cd9f0986ca6ab8c906351081a6320fbede1690cabb82bfbe4b65ae`, Build ID
`bb0f95b6e539ca4c808fe67674bb0c1d2e6d7447`. Its compiled CPP/helper match this
repair; no new speculative CPU/GPU fix or forced all-engine rebuild was made.
Three artifact-attestation regression tests PASS; actual alignment/CAS/endian and
concurrency execution at offsets 0–7 passes ASan/UBSan. Fresh selective Cemu receipt,
combined APK and loaded-runtime retest remain required. NAND/save roots unchanged.

Physical procedure: install over existing package; copy engine report before play;
confirm the corrected full SHA/Build ID; cold boot Bayonetta 2, play 15 minutes,
save through the game, Return to Shelf, reopen and load the save. Repeat Sonic's
previously working segment and a controller/menu/rotation cycle. Capture exact
native trace if it fails. **Physical acceptance PENDING, continuing crash OPEN.**

### Increment 3: Xenia buffered performance candidate

The pinned runtime already owns asynchronous pipeline creation, AAudio callbacks,
Vulkan completion timelines and its surface lifecycle. Those paths were studied
against the #257/#260/#263 lessons and were NOT replaced with Cemu code.

Implemented native 512-sample bounded windows for guest-swap/host-present intervals,
ARM64 JIT assembly calls, shader translation calls, driver pipeline creation,
swapchain acquire, present calls, present queue-lock waits, presentation fence
waits and CPU submission duration. Sampling is allocation-free; percentile sorting
occurs only on report requests. Reports distinguish CPU wall time from GPU execution.
Added skipped-draw and actual AAudio concealment-frame counters, guest-sequence
repeat counts, process RSS/native heap and Android thermal status. Host presents
and guest swap identities remain explicitly NOT unique correctly rendered images;
GPU execution/unique completed FPS/guest simulation speed are UNAVAILABLE until
reliably measured. Window percentiles can include pauses; capture uninterrupted runs.

Targeted correction: two unconditional per-skipped-draw XELOGI formatting/logcat
paths now increment buffered counters instead. Async pipeline decisions, missing
geometry behavior, shaders, blending/depth, texture data, renderer ownership and
stock Samsung driver selection are unchanged. This removes avoidable hot-path
logging; it does NOT establish that reported graphical corruption is repaired.

Verification so far: all pinned source replacement hooks applied to real upstream
files; bounded percentile/overwrite/concurrency/report-buffer native test PASS;
four existing native Xenia contract tests PASS. Synthetic Linux-host record-only
cost was 17.21–23.70 ns/event across two runs (not Android overhead, frame time,
FPS or a game performance comparison). Exact-source ARM64 rebuild is in progress;
no replacement engine is accepted without that build and its matching symbols.
No firmware, game or proprietary licence data is used by these tests.

Selective recovery workflow builds ONLY cemu and xenia, then reuses eight unchanged
CI-01 components. Missing/mismatched artifacts fail; no all-engine fallback. The
recovery candidate uses the same complete APK/signing/native inventory/unit gates
as the normal pipeline. PS3 is explicitly excluded until recorded distribution
clearance. No master merge or GC-01 feature merge occurs.

Owner/Marcus physical procedure after delivery: preserve the old Xenia engine,
profile and saves; use stock Vulkan and identical FIFA Street 3 segment/controller/
orientation. Capture Advanced reports at boot, match entry, 60 seconds and 15 minutes,
plus an identical video segment. Repeat without clearing caches for warm-cache
comparison; cache state is UNKNOWN unless independently controlled. Record frame
interval p50/p95/p99, JIT/pipeline totals/deltas, acquire/fence/queue/present times,
audio concealment, RSS and thermal status; compare menus, geometry/textures/depth,
save/reopen and Return to Shelf. Do not derive unique-image FPS from presents.
No same-device before/after, visuals, thermals or sustained speed are yet measured.
**Gameplay improvement/fidelity acceptance PENDING, not PASS.**
