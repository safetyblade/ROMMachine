> **CONTROLLING OWNER HANDOFF — 22 NOV 2026:** [GC01 authoritative A–H implementation and prior UI reuse](gc01/GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md) supersedes conflicting historical feature-scope/design direction. **Implement, do not rescope**. SafeSave must expose real per-console in-game saves/state/VMU/card counts and Export All with exceptions; controllers need live two-press **reciprocal SWAP** and actual native host testing; Junior uses a simple PIN; Wii+3DS motion UI belongs to GC with native sensor bridges in PERF; GEM needs **only a simple exact-ID Roxy/team CSV/JSON enrichment interface wired to existing offline recommendation logic, not a new database-platform project**. Existing 25-screen Sienna UX and merged controller/nav/shelf UI remain the source foundation. This is a design/integration instruction, not release/QA approval. The historical status matrix below is preserved and must be refreshed with current source/physical evidence.

# CARTDECK GC integration and physical acceptance

Draft integration candidate for #262. The baseline is master
`5d7e31abef1e5018ea01c6f271ea8889670b0f9c`. Continue on #362. Keep the
separate PERF candidate #367 intact. Its successful APK run
44973008175 and artifact 14734563067 are historical baseline evidence.
Xbox 360 engineering run 44973008246 failed on that commit.

## Feature inventory

| Checkpoint | Integrated source | Remaining acceptance and scope |
| --- | --- | --- |
| GC-01 baseline | #362 landscape Off default and opt-in surround. Two targeted #367 SafeSave changes retain PSP group identity and run the Room reopen test on an I/O worker. | Exact candidate Kotlin, Room, native and APK gates. |
| GC-02 SafeSave | Retained #363 PSP snapshot store and cartridge UUID lookup. PSP capture and restore now reject symbolic links and verify indexed hashes and file counts before replacement. This checkpoint retains the separate SafeSave v1 snapshot index and exports its schema. Main Room stays at v13. | Resolve profile and database ownership, connect verified engine adapters and UI, implement archives, transfer and recovery, prove save reopening and install-over. Existing PSP code is a partial slice. |
| GC-03 GEM | Bundled release catalogue, guarded identity linking, personal ratings store and deterministic GemFinder reused. Added catalogue browse, title/console/genre/mechanics search, incremental results, links for resolved installed cartridges, and portable personal scores for stable catalogue release IDs. | Profile integration, release upgrade continuity, catalogue gaps, editorial DeckFit coverage and discovery preferences. Verify the 88,246 records across 30 systems in the APK. |
| GC-04 TV and controller | Mobile/DeX gamepad confirm translation, stick navigation, cartridge pickup focus containment and shelf focus restoration. Native TV uses a focused cartridge dialog before launch. Existing controller mapping and game menu remain in use. | Native TV device journey, pointer and assigned-port acceptance across guest hosts, reconnect and physical regression. |
| GC-05 SIGNATURE and remaining GC | Original startup clip, audio attributes and focus, saved startup gain/mute, safe final crop, controller/remote skip and reduced-motion fallback. | Verify device playback and framing. Complete remaining shelf, GripArc, Modern Function, display, branding and Junior features against #262. |
| GC-06 release gate | Draft PR uses the existing test and APK workflow. | Exact-source successful workflow, signed ARM64 identity and install-over, Marcus evidence and Jonathan approval. |

## Marcus physical QA matrix

Record APK artifact, exact Git SHA, package, version code, signer SHA-256,
device model, Android version, controllers and connection types with every
result. Capture steps, observed result and evidence file for each run. Use
a disposable test profile/device for destructive recovery fixtures and
lawfully supplied titles. Keep backups of any existing test progress.

| Case | Procedure and expected result | Status |
| --- | --- | --- |
| Install and upgrade | Fresh install on a spare device. Then install over the previous approved build on a populated device, using the same signer. Verify cartridge IDs, ROMs, saves, configurations, appearance and offline launch. | NOT TESTED |
| PSP | Use real PPSSPP Modern progress. Exit the engine, capture complete SAVEDATA, change progress, restore, relaunch and load the captured checkpoint. | NOT TESTED |
| Shared cards | Use cards with two titles. Roundtrip entire PS1 and both PS2 engine card formats, then Dreamcast VMU. Compare whole-file hashes and reopen both titles. | NOT TESTED |
| Remaining engines | Use one verified save and reopen fixture per adapter. Record unsupported, excluded and locked cases separately. | NOT TESTED |
| SafeSave recovery | Export game, player, everyone, card and selected versions to a document provider. Import preview, skip, new slot when supported, replace after backup and copy to another player. Exercise corruption, interruption, permission revocation and low storage. Verify that existing progress remains usable. | NOT TESTED |
| Profiles and Junior | Set up Manager, Standard and Junior. Save distinct progress in the same game. Verify settings, visibility and authentication, including blocked deep links, recent games and resume. | NOT TESTED |
| Offline GEM | Enable airplane mode. Browse and page catalogue results, match editions, open a verified installed cartridge, edit and clear both ratings, reopen and confirm persistence. Check For You reasons and wildcard preferences. | NOT TESTED |
| DeX gamepad | From a wired or wireless DeX display, use gamepad only. Move between sections and cards, confirm, play, open universal menu and exit. Verify the original section/card focus and no shelf input during details, menu or game. | NOT TESTED |
| TV remote | Repeat the shelf journey with D-pad, OK and Back. Check overscan, focus visibility, single activations and modal dismissal. | NOT TESTED |
| Two controllers | On a supported guest engine, assign P1/P2, map and save, swap, reconnect, confirm P1 owns menus, then verify both players work in game. | NOT TESTED |
| Startup speaker, Bluetooth and HDMI | Cold-start per route, listen to the full clip, inspect final frame, then test volume buttons, Startup Sound, mute, Skip, app background and a real audio interruption. Check for overlap at game launch. | NOT TESTED |
| Responsive controls | Check portrait, compact landscape, tablet and TV. Exercise Standard, PivotArc and GripArc, all native controls, analogue diagonals, simultaneous input and DS/3DS touch exceptions. | NOT TESTED |

## Jonathan's device sequence

1. Record the previous approved APK, test ROM library and visible game save
   checkpoints. Install the candidate over that APK without clearing data.
   Confirm package signer continuity, selected appearance and mapped controls.
2. Cold-start the handset. Check the C-slot and wordmark in portrait and
   landscape, listen through speaker, Bluetooth and HDMI, and test Skip,
   rotation and backgrounding. Set Startup Sound to 25%, mute, relaunch,
   unmute and confirm it returns to 25%.
3. On the DeX setup, set the pointer aside. Choose a collection, move focus
   to a cartridge, press A, select Open, play, open the universal menu
   and exit to the same cartridge. Repeat with a TV remote on Android TV.
4. Verify saved progress in a known PSP game, then capture, edit progress,
   restore and reopen. Repeat with a multi-title card/VMU. Run Manager and
   Junior isolation and Grown-Ups authentication once the routes are ready.
5. In airplane mode browse GEM, change and clear ratings, reopen and inspect
   recommendations. Check compact landscape and tablet controls using
   known games. Record each result independently of Marcus.
6. Review reports and the exact commit before giving explicit merge approval.
