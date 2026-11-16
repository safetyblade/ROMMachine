# PC-04 — Shandalar first native PC compatibility spike

Owner authority: first PC game target = MicroProse Magic: The Gathering / Shandalar (1997), a Windows 95-era game. This branch is **bootstrap only**; it does not install games, Windows, third-party frontends or new APK assets.

## Source truth already established

- CARTDECK source registers DOSBOX_PURE in CoreID and SystemID.DOS in GameSystem. The DOS unique cartridge suffix is .dosz; a generic .iso is ambiguous and must not be automatically treated as PC.
- The existing lemuroid-cores git submodule contains an ARM64 DOSBox Pure .so; the exact Git tree evidence and existing submodule pin are in pc04_sources.json.
- Canonical modern DOSBox Pure source: https://codeberg.org/schelling/dosbox-pure. Upstream Win9x/guest-disk/CD features need verification against the bundled binary before changing production.
- Winlator v11.2.0 is the fallback architecture reference for a Wine 32-bit path; do not integrate its entire frontend. Local modern Windows executable research remains in PC-01, without any external storefront or account sign-on.

## Audit from local checkout

Run these commands from the repository root:

    git submodule update --init lemuroid-cores
    python3 spikes/pc/verify_legacy_core.py --strict-binary
    python3 -m unittest discover -s spikes/pc/tests

These are **read-only source/native checks**, not gameplay tests. The script prints machine type, native SHA256, relevant segment alignment and actual gitlink. It does not fetch any executable or touch saves.

## Next executable milestones (engineering increments, not completed yet)

1. PC-04A: verify the actual bundled core and Libretro host supports Windows 95/98 boot, guest disk, ISO/CUE mounting, keyboard/mouse and controlled exit. If impossible, source-build a pinned revised DOSBox Pure independently and preserve existing native library.
2. PC-04B: isolated user-confirmed PC media import/mount (do not reclassify generic ISO or alter other console media scanners).
3. PC-04C: lawful owner-supplied Shandalar + Windows guest installation, overworld, duel, deck editor, in-game save and reliable reopen.
4. PC-04D: if Cemu-like/guest-host approach is unsuitable, test an independently audited Wine + Box64 and stock Xclipse Vulkan setup informed by Winlator. Do not include Winlator APK/frontend as a shortcut.
5. PC-04E: only after working proof, connect PC Classics to CARTDECK shelf/menu/controller/profile/SafeSave by separate acceptance-gated PRs.

## Approved runtime distribution model

All release-approved and legally distributable PC/console emulator engines are **integrated into the CARTDECK APK**. Players do not install or download engine packages separately to play. At release, Settings will have an informational link to a **separate open-source emulator-only repository and its engine source/build artifacts**, defined in REL-ENG-01 #307; this is not runtime provisioning. The separate repository does not itself remove GPL corresponding-source obligations applicable to the app or host.

## Hard boundaries

- No Windows OS, Shandalar game, ISO, licence key, copyrighted assets or third-party APK bundled.
- No DRM circumvention, including automated application of third-party compatibility patches altering protection.
- No Room migration, GameSystem or scanning change, core binary replacement, save root change or menu/input/GC branch modification in this bootstrap.
- Do not merge into mainline or report gameplay working before real Samsung SM-S721B evidence and owner acceptance.

Parent PC-00 #299 | PC Classics #301 | existing Shandalar #195 | active PC-04 #305.
