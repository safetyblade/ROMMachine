# PERF-PC-01 — implementation checkpoint 2026-11-17

> **Controlling recovery direction (17 Nov 2026):** [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) and the updated [PC-00 charter](PC00_LANE_CHARTER_AND_FEASIBILITY_GATES.md) govern PC-R01–PC-R07. Earlier implementation notes are historical evidence, not permission to modify the shared console host. Resume at PC-R01.

Base master: 4b46aa82dfc1ab15b27a69f1bc9171501b6b2501. Closed unmerged #306 was 98 commits behind, six ahead. Recovered only its four added spike files. No console, shared schema, signing, CI or GC files changed.

## Evidence
- Current master CoreID and GameSystem register DOSBox Pure with .dosz routing and save states disabled.
- Added PcMediaInspector and PcWorkspace to shared Android source, plus JVM regression tests. These are not yet wired to UI or runtime. Optical signatures indicate format, not platform. Explicit PC selection still required.
- Upstream documentation https://docs.libretro.com/library/dosbox_pure/ describes OS installation, guest disk and CD boot; actual shipped binary parity is unknown.
- Native audit scripts recovered, not run against actual binary. Existing script strict mode does not enforce 16KB alignment; do not use its exit code alone as release evidence.
- No test execution, APK or gameplay PASS asserted.

## Remaining engineering
1. Check out actual native artifacts; verify ELF offsets/alignment, exported libretro symbols, source revision and licence; compare current upstream. Pin/build only PC core if needed via CI-01, preserve rollback.
2. Bind Android explicit import through SAF, validate CUE relative references and multi-disc layouts, enforce bounded free-space/copy checks and cleanup. No generic scanner changes. Bind media/guest/prefix metadata to stable cartridge identity and authenticated profile access.
3. Wire per-workspace libretro system/save directory, guest boot/install, disk control, keyboard/mouse, orderly shutdown and resume. Verify synthetic workload then original media on owner device.
4. Implement direct executable launch only after guest execution mechanism verified. Integrate existing GC menu, input, display and shelf contracts without conflicting migrations.
5. Wine alternative: inspect existing Xbox FEX build pattern; independently pin Wine/WoW64 and appropriate translation/graphics components, verify ARM64/16KB/Xclipse and distribution before packaging. No alternate frontend/storefront.
6. Verify CI selective packaging, APK native inventory, signing/install-over and source obligations under #307. No APK candidate currently available.

## Owner acceptance (only after candidate exists)
Install over current CARTDECK; verify existing games and saves. Explicitly import lawful Windows installation media or guest disk and original Shandalar disc. Create isolated guest, install Windows/game, choose executable and create cartridge. Test overworld, duel, deck editor, save, guest shutdown, return to shelf, relaunch and retained progress. Repeat mouse/touch/controller and TV. Record exact failure stage and version. No commercial media or keys submitted to repo. Do not export whole guest as verified game save.

Status: source foundation only; not ready for owner testing. #195 #299 #300 #301 #302 #305 #307 remain open.

## Recovery checkpoint — 17 Nov 2026

- Failed native receipt check: run 44065484302. Failed PC native patch application: run 44066726783, input.cpp and environment.h on pinned LibretroDroid 8835c309.
- Isolation commit f3e6ca2e restored the verified console recipe and retained the PC native experiment under spikes/pc/native-host. Host preflight passed on run 44068037585; app compilation subsequently failed independently on controller IntArray mapping, corrected by fdf070b8. No full APK or device acceptance inferred.
- Later candidate source in PERF-PC-01_EXECUTION_INCREMENT.md replaces the initial checkpoint's source-only inventory: explicit PC import, CARTMOD, guest disks, launch recipe, PC input code and Wine adapter retained. No runnable PC guest or Wine proof.
- PC-R01 independent runtime architecture and laboratory proof is the required next increment. PC-R02–PC-R07 follow the governing roadmap, with independent R06 feasibility permitted in parallel. No shared console host changes or PC dependency on the normal APK. 
