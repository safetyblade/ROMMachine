# PC-FOLDER — Direct Windows EXE from a local game folder

Owner goal (23 November 2026): CARTMOD selects an installed/portable game folder, chooses its EXE, creates a Shelf cartridge, and starts the actual Windows game inside CARTDECK on Android. No Windows ISO, manual Windows desktop, external emulator, store or GameNative. Shandalar is the final end-to-end target.

## Baseline and evidence
- PR #352 merged to master 19 November, merge 510811f4. This follow-up draft starts from master 545e429f. No production host, Room or save migration.
- Canonical PC cartridges, DOSBox Pure, private profile and per-cartridge game workspaces, separate opt-in Wine host, shared menu and source-pinned ARM64 runtime scripts are present.
- Legacy single-EXE import loses nearby DLLs/assets; the new folder path seals ALL selected folder files and subdirectories with per-file SHA and size receipts, preserves working directory and immutable cartridge UUID, and stages a writable game folder atomically.
- The last consolidated normal APK 44497559426 succeeded. Native Android Wine 44497559458 and ARM Linux Box64+Wine 44497559415 FAILED. Independent Linux x86_64 Wine PE32/PE64 execution and file/registry persistence passed. A successful APK is not equivalent to a Windows game running on Android.
- SAF selected folder is copied immediately, read-only; no permanent access permission required. Prevent traversal, Windows reserved names, collisions, unsupported file count/depth, imports beyond limits, and publication of incomplete files. No original files modified.
- Prototype scope: PE32 executable, max 4096 files, 12 folder levels, max 512 MiB per file and 2 GiB total. Folder import is exposed only when the Wine native integration build is enabled. This is not shipped or approved Wine gameplay.

## Final implementation gates, in order

| Gate | Required engineering work | Passing result |
|---|---|---|
| F1 — folder and Shelf | Source tests; SAF folder browse, EXE selection, full resources, stable identity, relative CWD, upgrade and no overwrite; standard app regressions | Exact-head unit/source/standard APK pass and independent owner physical import |
| F2 — Android Wine | Fix Wine 10 ARM64 compiler defect in wineandroid.drv/dllmain.c pointer-to-UINT64 assignment, obsolete jcenter Gradle path and combined ARM Linux ntdll loader; produce signed source-verified Android package and active 32-bit WoW64/graphics driver | Native source audit and actual verified on-device PE32 host start, no console host change |
| F3 — small real game | Run official NetHackW.exe from complete folder using the SAME integrated Shelf activity; test Win32 GDI windows, keyboard/touch/mouse, working directory, registry, save and reopen | Device log and owner test, not a synthetic fixture only |
| F4 — larger game | OpenTTD win32 ZIP with official open-source OpenGFX2 baseset; graphics path, data/DLLs, new game, touch controls, audio, save and reopen | Device gameplay, sound and persistent game progress |
| F5 — installer and profile | Support already-installed portable folders; optional in-folder setup EXE; extract installed target and remember choice; persistent prefix, 32-bit requirements, authenticated profiles/Junior restrictions, SafeSave policy | Game installation/reopen and separate saves without Windows ISO |
| F6 — Shandalar | Owner-supplied lawful installed files; identify game EXE and CWD, legacy dependencies/DirectDraw and possibly 16-bit setup constraints | Enter world, complete duel, save campaign/deck, restart and install-over |
| F7 — release | Full connected regression/Room/saves/identity/engine receipts/Android ABI/16KB/signing/rollback plus license distribution review | Owner accepts exact APK and only then deliberate release/merge |

## Two verified-source test game candidates (no bundled binaries)

1. NetHack 3.6.7 Win32 graphical. Official source:
https://nethack.org/download/3.6.7/nethack-367-win-x86-tty-curses.zip
SHA256: 63de6c0122288031ef2edc050e99c3b3f7aba8b9483a45baa03e01dde1c1c6fb
Extract the complete ZIP, select the folder, choose NetHackW.exe, not the console NetHack.exe. The official developer instructions explicitly require keeping all folder contents. Test menu, movement, configuration, save, relaunch, and registry/prefix.

2. OpenTTD 15.3 official Win32 portable ZIP:
https://cdn.openttd.org/openttd-releases/15.3/openttd-15.3-windows-win32.zip
SHA256: 3f092edc8f381c3d2d3a59458703899da6f876345b3850a3c76c0dffe68f0e74
Extract the entire ZIP, choose openttd.exe; OpenTTD needs an official open-source OpenGFX2 base graphics set in its baseset directory. Source: https://www.openttd.org/downloads/openttd-releases/latest . Do not substitute non-free Transport Tycoon Deluxe data. Test assets, graphics, mouse, audio, game creation, save/reopen.

Official archives are NOT committed, uploaded, or packaged into the app. Manual fixture workflow downloads only from those authoritative sources, validates official SHA, PE32 architecture and folder structure, publishes JSON evidence only; actual device compatibility is UNTESTED.

Final game: MicroProse Magic: The Gathering (1997), Shandalar, from lawful owner files only, not third-party reimplementations. No game assets in repository or bundled APK.

## Engineering continuation / acceptance instructions

Do not stop at a manifest, source receipt or standalone CPU emulator. Fix real R06C Android Wine compilation and ntdll loader; produce exact-head signed integrated build. Use original bundled PE32 GDI/keyboard/mouse/audio/save probe for first device gate, then run NetHack and OpenTTD folder games and Shandalar. Keep all native PC code capability-gated, existing console host and save identity untouched. If engine fails, capture exact library or JNI symbol and fail visibly instead of pretending to launch.

For every game record: exact version code/commit/signing, Android model and ABI, selected executable relative path, source digest, first frame, controls, audio, saved changes, shutdown, second start, install-over and rollback. No inferred passes. Coordinate owner test in #299 and #305. Do not auto-merge or close until approved.
