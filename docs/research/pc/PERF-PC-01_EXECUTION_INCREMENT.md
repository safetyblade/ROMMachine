# PERF-PC-01 — explicit PC Classics execution increment

> **Controlling recovery direction (17 Nov 2026):** [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) and the updated [PC-00 charter](PC00_LANE_CHARTER_AND_FEASIBILITY_GATES.md) govern PC-R01–PC-R07. Earlier implementation notes are historical evidence, not permission to modify the shared console host. Resume at PC-R01.

Based on the approved #299/#301/#302/#305 contract and the existing cleanup branch (#313).
Implementation uses the current bundled DOSBox Pure route rather than importing the Winlator app.
Reference: https://docs.libretro.com/library/dosbox_pure/ (ZIP-contained disc images, guest setup,
core-controlled start menu, save directory); Winlator v11.2.0 remains the separate modern runtime reference.

## Implemented
- Settings PC Classics: explicit read-only SAF selection of game ISO and optional Windows ISO.
- Bounded staged ISO validation, deterministic two-disc DOSZ archive, unique immutable cartridge
  identity, atomic publication, scanner enumeration of the private vault only.
- Normal DOS shelf and bundled libretro player path; managed cartridge system/save directories
  isolated from original DOS and console paths. Single local player only; no account/profile service exists.
- JVM regressions for archive contents, isolation, failures and cancellation.

## Not verified / not delivered
Current pinned core guest boot, internal disc switching, DOSZ optical support, keyboard/mouse,
clean shutdown, Windows installation, Shandalar direct executable launch and physical saves remain unverified.
No new native binary or Wine/Box64 runtime has been imported. No separate frontend/downloader,
commercial media or license bypass. CUE/BIN, VHD and modern Windows imports remain deferred.
The current DOS card appearance/classification is reused, not a finished PC-specific visual design.
Only explicit imports create a PC cartridge; generic ISO scanner rules are untouched.
No APK or source-test PASS until current-head CI completes. Keep release clearance and owner physical gates open.

## Native/reference verification
- Winlator root commit fb66541b93a4eb3ee585a433b4c7b20544d58e40 pins app c2f4ad4534f4637b543a9a3b085e28f50cf6d01c. Its GuestProgramLauncherComponent starts Box64 with guest executable, root filesystem Wine path, library path, and process lifecycle; this is architecture evidence, not a usable Android Wine component within CARTDECK. No unverified Winlator binaries are copied into CARTDECK.
- Strict DOSBox native audit now requires the recorded submodule, ARM64 and 16KB load offset/alignment checks. CI executes synthetic tests and separately collects the actual binary audit. An audit failure is a PC runtime blocker, not evidence of broken console gameplay.
- Included shared DS lint correction identified by #313 CI; the PC branch is based on the cleanup checkpoint, not an older master.

## PC-F4 continuation — physical keyboard and mouse
Build fix #315 shortens the failing message from APK run 44044622365. Added DOSBox-only Activity input dispatch, host keyboard polling and callback registration, relative mouse deltas and button states, and input release on focus/pause. Other cores do not send the PC protocol. The existing final host bridge patch now includes the PC input additions in separate file-diff sections; the host-specific patch and build recipe hashes change without invalidating the shared recipe for all five native backends. The existing native host input suite is now a mandatory pre-publication gate. Kotlin regressions authored. No APK or device acceptance is claimed. Wheel, Unicode, touch keyboard, controller mouse, guest boot and save/reopen remain pending. Wine remains a separate outstanding increment.

## PC-F2/F3 Windows setup continuation
- Fixed invalid connected APK resolver on merged master ef580690; prior build repair #315 APK passed but #316 workflow definition failed before compilation.
- Prepared guest disk import copies a raw 7 MiB–2 GiB sector-aligned IMG to the selected cartridge's system directory, never overwrites; existing archive/progress unchanged.
- New imports can include a guest-only CARTPLAY.BAT generated from an explicitly selected 8.3 Windows executable path with its working directory. Windows Startup copy/shortcut and DOSBox Pure core auto-start selection must be completed in the guest. No host process is used and no Windows OS files are provided. Existing imports retain immutable archive; use normal guest shortcut.
- UI shows disk presence only, not installed/boot verified. Tests authored for command-path validation and guest isolation.
- Reference https://docs.libretro.com/library/dosbox_pure/ and sysfce2/libretro-dosbox-pure dosbox_pure_run.h OS auto-start mechanism; actual bundled revision parity remains to verify. Windows installation, one-click boot and Shandalar physical acceptance remain open.

## PC-W1 execution boundary
Pinned the actual Winlator app gitlink c2f4ad45 (reference targetSdk 28, not suitable for direct drop-in). Implemented explicit PE x86/x64 probe, checksum-verified packaged Box64 and Wine execution command, private per-cartridge Wine prefix, raw argv (no shell interpolation), display/SHM prerequisite and internal Android adapter. Added reference asset audit workflow that inventories and hashes upstream archives but does not distribute or execute them. Wine is NOT selectable or packaged yet: no verified Android translator/wine runtime, full rootfs receipt, X server, audio, process-group shutdown or distribution clearance. The adapter is source groundwork and must not be interpreted as a runnable Wine player.

Windows launcher refinement: replaced the guest BAT with an ANSI Shell Link (MS-SHLLINK, local volume/path and working directory). DOSBox Pure cannot auto-select it as a DOS executable. The generated shortcut still requires user placement in Windows Startup and guest auto-boot selection. No physical Windows shell acceptance claimed. Added structural and archive regression tests.

## HOT-03 navigation integration
Reconciled with master 6c015bea (PR #317). CARTMOD owns PC import and guest management; Settings redirects to the real CARTMOD route. Shelf's normal launch path is unchanged. Managed PC context action takes the existing canonical cartridge ID to its installation page; only linked, indexed cartridges can open guest setup through the same GameInteractor. Setup and disk presence are not readiness. Shelf/CARTMOD product dock replaces only the non-arrange browsing dock; legacy filter tabs, GEM and SafeSave remain absent. No unverified Wine selection or mod placeholder. This is candidate source, not physical/UI acceptance.

## HOT-04/HOT-05 reconciliation
Merged current master 2a0f888a into the PC continuation branch without replacing the approved HOT-04 product dock, shelf section paging, cartridge setup or HOT-05 controller screens. Extended the existing CARTMOD page with the PC wizard, guest management, UUID-targeted installation route and original Return to Shelf action. Retained Settings redirect and current chrome tests. Reformatted the failing ShelfQueryTest fixture without changing test expectations. Reused the repaired connected APK workflow (removes a repeated malformed suffix from the corruption on master, preserving each actual build stage once). CI and physical acceptance pending.

## Superseding native execution direction

The shared host implementation described in PC-F4 is no longer active. The standard APK uses the restored console host and PC-native protocol disabled; the failed experiment remains in spikes/pc/native-host with a dedicated laboratory workflow. Preserve import, guest/workspace, Windows setup, launch preparation and Wine groundwork; prove the runtime independently under PC-R01–PC-R04 before integrating the verified classic runtime under R05, and meet R07 release/console regression gates. Wine R06 remains separate feasibility work. Do not infer Windows boot, one-click launch, gameplay or save success from UI/source availability.
