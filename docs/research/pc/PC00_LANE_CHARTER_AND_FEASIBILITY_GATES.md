# CARTDECK — PC-00 Integrated PC Emulation Lane
**Controlling owner scope — 17 November 2026.** Owner Jonathan; director Cassia; architecture Daniel; engineering Elliot; UX Sienna; QA Marcus; research Roxy.

## Locked product decisions
- **All approved, legally distributable emulators are integrated in the CARTDECK application.** Players do not download or separately install an emulator to run a supported cartridge. Maintain CI-01 selective native artifact builds, actual APK payload and rollback.
- **Release Settings links to an independent open-source emulator-only repository/source/build package** for all integrated console and PC engine technology. It is **not** a runtime installer, optional engine downloader or separate game-launch app. The release repo and URL do not yet exist: [REL-ENG-01 #307](https://github.com/safetyblade/ROMMachine/issues/307).
- The engine-only repository does **not** negate GPL-covered CARTDECK/Lemuroid/LibretroDroid combined-work corresponding-source requirements. Confirm actual licences/component boundaries and any app host/source release separately; do not represent the repository split as an automatic confidentiality guarantee.
- **Remove the GameNative project entirely from the active PC programme.** No Steam/Epic/GOG/Amazon storefront sign-on, authenticated external games library, proprietary provider download adapter or competing PC frontend.
- **Locally launched modern Windows games remain a feasibility objective:** examine Wine, Box64, FEX, Hangover and suitable Winlator/MiceWine technical components on Android ARM64/Xclipse; source/licence and actual execution proof required. Do not import Winlator/MiceWine's entire app shell.
- **Classic PC first:** DOS, Windows 3.x/95/98, selective XP-era games, lawfully user-provided PC optical images, installers and files. Shandalar / MicroProse Magic: The Gathering (1997) is the first game acceptance path.

## Dedicated PC lane
| ID | Work | State |
| --- | --- | --- |
| [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) | Governance, feasibility and decision record | Controlling |
| [PC-01 #300](https://github.com/safetyblade/ROMMachine/issues/300) | Local Win32/Win64 execution technology, Wine/Box64/FEX | Research |
| [PC-02 #301](https://github.com/safetyblade/ROMMachine/issues/301) | DOSBox Pure, DOSBox-X, classic discs and Windows guest environments | Research |
| [PC-03 #302](https://github.com/safetyblade/ROMMachine/issues/302) | One app, bundled runtime interface, game UI, controls, profiles, Junior, SafeSave and QA | Design |
| [PC-04 #305](https://github.com/safetyblade/ROMMachine/issues/305) | Shandalar first native integration proof | Protected bootstrap |
| [EXP-95 #195](https://github.com/safetyblade/ROMMachine/issues/195) | Original game-specific Shandalar research and trial | Gameplay not verified |
| [REL-ENG-01 #307](https://github.com/safetyblade/ROMMachine/issues/307) | Separate emulator-only source/build release and future Settings link | Release design |

**PC-04 draft source:** [PR #306](https://github.com/safetyblade/ROMMachine/pull/306), original `CoreID.DOSBOX_PURE`, LemuroidCores native AArch64 core, source/provenance verifier and tests. No new Windows runtime or APK, and no Win95 guest or Shandalar gameplay evidence. Preserve unmerged/reversible branch and baseline native engine.

## Engineering study
**DOSBox Pure first:** inspect exact actual pinned native binary, source licence, Android 16 KB ELF load alignment and host support for Windows 95/98 OS guest boot, virtual disk, PC CD mounting, keyboard/mouse and exit. Modern upstream source https://codeberg.org/schelling/dosbox-pure; upstream features do not prove the old binary supports them.

**Fallbacks and later Windows:** DOSBox-X only with viable ARM64 Android host; Wine with Box64, FEX or Hangover when legally and technically compatible, using Winlator/MiceWine as component/graphics references. Device baseline Samsung SM-S721B, Android 17, Xclipse 940, stock Vulkan. Do not assume Turnip/Adreno drivers or universal Windows support.

**Media and saves:** Validate real PC media types and mixed-mode tracks/CDDA; no generic ISO extension-based routing that hijacks PS1/PS2/GameCube/Wii/Xbox. Read-only SAF source, licensed/user-provided Windows/game installer and isolated writable per-game guest VHD/Wine prefix. One cartridge → game menu → keyboard/mouse/controller/TV → verified in-game save, quit and reopen. Per-profile save isolation and Junior restrictions, GEM installed/playable distinctions, SafeSave exports only verified progress; OS VHD and installers are not assumed to be saves-only.

**Release controls:** Full native APK inventory and engine-only source/build artifact inventory from same verified pinned inputs, SHA, notices, GPL/other upstream rights, ARM64/API/16KB and rollback; app/source release compliance independently assessed. Do not bundle Windows media, retail games, assets, BIOS, passwords or DRM bypass.

## Delivery gates and protections
PC-R0 actual existing core/source audit (script authored, not yet executed/accepted) → PC-R1 component licence/native compatibility matrix → PC-R2 integrated host/distribution ADR → PC-R3 complete classic/modern local import + controls/profile/SaveSafe UX → PC-R4 lawful Shandalar real-game evidence and Marcus QA → PC-R5 owner per-route GO/CONDITIONAL/NO-GO.

This roadmap clarification does **not** authorise new production source, a new APK, repository publication, or physical PASS. Current PERF-02 #60, PS3 #297, Wii U #296, X360 #295, GC-01 #262 / PR #294, CI-01 selective native builds, Room/game library/profile saves, signing and rollback are unaffected.

## Controlling recovery amendment — 17 Nov 2026 (owner approved)

This supersedes earlier PC host and delivery sequencing instructions wherever they conflict. PERF-PC remains a substream of PERF under #304; this is technical isolation, not an additional organisational lane. One CARTDECK experience and bundled release-cleared engines remain locked.

### Failure record and lessons
- The PC-F4 keyboard/mouse bridge changed the shared LibretroDroid console host patch and recipe. Master APK rejected the outdated immutable host receipt (run [44065484302](https://github.com/safetyblade/ROMMachine/actions/runs/44065484302)).
- Rebuilding that host exposed patch application failures against pinned LibretroDroid `8835c3098514390a271e36983957f7bb5f40abf1`, specifically `input.cpp` and `environment.h` (run [44066726783](https://github.com/safetyblade/ROMMachine/actions/runs/44066726783)). Do not assume upstream DOSBox functionality exists in the actual bundled binary.
- Mixed-lane Kotlin formatting failures and the controller IntArray compilation error were separate issues, not proof of failed PC gameplay. Real whole-repository formatter/check, compile and regression tests are required before merge.
- Isolation commit [f3e6ca2e](https://github.com/safetyblade/ROMMachine/commit/f3e6ca2e9d85ad31ec4bcc9a1d32ed6d018e2664) restored the verified console recipe. PC source experiment is preserved in `spikes/pc/native-host/`, with `.github/workflows/cartdeck-pc-native-lab.yml`; standard APK PC-native input protocol is disabled. Host preflight subsequently passed and reused the console host; full APK/device regression acceptance remains separate. The lab patch remains broken, not a working engine.

### Architecture — non-negotiable
- Shared CARTDECK product layer: Shelf, CARTMOD, cartridge identities, menus, display, profiles, Junior, GEM and SafeSave; independent console and PC execution adapters behind explicit runtime capability boundaries.
- Protect the established console host, its patches/ABI/protocol, engine receipts and native selection, controller fixes, normal library/ISO routing, APK identity/signing and existing saves. PC failures cannot block normal APK assembly or invalidate unrelated console receipts.
- PC-specific build inputs, patches, dependencies, protocol versions, artifact namespaces/receipts and workflows must be independent. No replacing the console AAR, using its Maven coordinate for a PC production artifact, weakening receipt checks, or automatically promoting lab outputs to master. A lab APK must have an isolated install ID/storage and cannot overwrite production data. The current lab is a starting point, not the finished runtime boundary.
- Enable PC input only after the actual selected runtime advertises a verified protocol; unknown capabilities fail closed with a truthful diagnostic. Runtime lifecycle, input and display isolation are required. Independent rollback must preserve cartridge UUIDs, profiles and guest/install data.
- Final integrated distribution is still one CARTDECK APK, with approved PC runtimes bundled, not an external emulator installer/frontend/downloader or storefront. Engine-only source distribution remains #307; licence and corresponding-source duties remain intact.

### Retain, do not recreate
Explicit SAF PC ISO import, deterministic DOSZ media, immutable cartridge identity, CARTMOD/guest disk setup, isolated workspace and Windows launch preparation are candidate source already implemented. Keep the existing PC Kotlin input logic and native experiment in the lab; native protocol disabled on stable APK. Wine PE probe, prefix/argv/checksum adapter and reference audit are groundwork only. Guest boot, optical switching, Windows installation, direct executable launch, real saves, Shandalar and runnable Wine remain unverified. Preserve all existing PC and console data.

### Revised delivery roadmap — PC-R01 through PC-R07
These are delivery increments, distinct from original feasibility gates PC-R0–R5 retained for decision/release review. Do not infer a PASS from source presence. The next authorised PC implementation starts at PC-R01; independent research may progress in parallel without blocking the classic route.

| Increment | Deliverable and exit evidence | Status |
|---|---|---|
| PC-R01 | Independent PC runtime/lab architecture, artifact identity, source pins, isolated install ID/storage and rollback; prove console APK builds without PC outputs; patch apply/build/tests in separate workflow. | NEXT; initial lab exists, boundary incomplete |
| PC-R02 | Audit actual shipped DOSBox Pure binary/source and licence, ARM64/16KB ELF, required libretro APIs, optical/guest/disk capabilities. If inadequate, build a pinned PC-only core or evaluate DOSBox-X. | OPEN |
| PC-R03 | Reproducible synthetic guest execution with CD mounting/switching, virtual disk, keyboard/mouse and release/neutral/focus lifecycle; verify touch/controller and display integration without shared host regression. | OPEN |
| PC-R04 | Persistent Windows 95/98 installation using lawful owner media; real executable launch (not just a generated shortcut or disk presence), save/clean shutdown/reopen, Shandalar overworld/duel/deck editor as the first owner acceptance. | OPEN |
| PC-R05 | Bind verified runtime to retained CARTMOD import, normal Shelf cartridge, menu/display, player/profile/Jr restrictions and SafeSave inventory; avoid unproven save states or exporting whole OS disks as game saves. | OPEN |
| PC-R06 | Independently prove Wine + appropriate Box64/FEX/Hangover/WoW64 translation, Android ARM64/16KB/JIT, stock Xclipse graphics, rootfs/prefix, display/audio/SHM, process lifecycle and local Win32/Win64 execution. Winlator/MiceWine are references only. Do not delay classic proof. | OPEN / parallel research |
| PC-R07 | Promote only verified, licence-cleared PC components via distinct immutable receipts, exact APK inventory, install-over identity/signing, console regression suite (including controller neutrality/release and existing engine launches), save/profile retention and owner acceptance. Keep rollback; standard APK remains buildable when PC is not ready. | BLOCKED pending relevant proofs |

### Resume instruction
Daniel owns the PC-R01 runtime/CI boundary and ADR; Elliot implements a bounded independent lab increment; Marcus validates evidence and console regression gates; Sienna retains CARTMOD and shared UX. Do not restart the older shared-host implementation or commission all increments into master at once. Record exact revisions, tests, artifacts, remaining gaps and rollback; owner performs physical acceptance after candidate delivery. No PC runtime or standard APK readiness claimed by this amendment.
