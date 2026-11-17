# PERF-PC-01 — implementation checkpoint 2026-11-17

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
