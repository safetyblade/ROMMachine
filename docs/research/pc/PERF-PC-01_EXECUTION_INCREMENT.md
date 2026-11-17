# PERF-PC-01 — explicit PC Classics execution increment

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
