> **GC C/E BLOCKER #135 / PIV-HH-01 CONSOLIDATED 22 NOV:** [Unifying GripArc V2.2 repairs and all **12** handheld PivotArc systems](gc01/GC01_PIVOTARC_GRIPARC_UNIFIED_ENGINEERING_ACCEPTANCE_2026-11-22.md). `SystemID.NGC` = Neo Geo Pocket Color is a distinct 12th handheld source identity. Current **Standard+GripArc** source/input partly works but owner found **oversized full L and hidden console buttons**; **PivotArc+GripArc physical FAIL**. Full source-native input/48dp/visual fit and proper G1–G4 + A/B holds, left/right, console switching, same-head APK and Marcus device evidence remain acceptance requirements; no source fixture or docs can mark #135 PASS.

> **PIV-HH-01 — HANDHELD PIVOTARC DESIGN ADDITION (22 NOV):** [New console-authentic PivotArc layouts for GB, GBC, GBA, GG, LYNX, NGP, WS, WSC, PSP, NDS and 3DS](../design/gc01/GC01_PIVOTARC_HANDHELD_ONE_HAND_LAYOUTS_2026-11-22.md) (see repository-root-relative path docs/design/gc01/) add a **GC C/E design and future engineering QA gate**. Existing GB/GBC/GBA/SNES PivotArc and Hold A/B are preserved; **no new fixed H1–H3**. Optional Standard-only Hold Tray is separate. Evaluate one-thumb game reach, source-original buttons, WonderSwan orientation, Lynx Option/Pause, PSP analog, NDS/3DS stylus/motion, 48dp/no cropping, exact guest down/up and safe fallback; physical PASS still pending. This is documentation until Kotlin and owner/Marcus tests exist.

> **HHOLD-01 — FINAL OWNER CORRECTION (22 NOV 2026):** The prior proposal to add **three H1–H3 buttons into PivotArc** was **withdrawn**. Preserve existing PivotArc **Hold A/B** and GripArc G1–G4. [Handheld Hold Controls design](../design/gc01/GC01_HANDHELD_UNIVERSAL_HOLD_CONTROLS_DESIGN_2026-11-22.md) covers an **optional 1–3 slot Standard handheld touch tray** for GB/GBC/GBA/GG/LYNX/NGP/WS/WSC/PSP/NDS/3DS, console-accurate source mapping, Settings ownership and no overlapping native controls. QA proof includes GBA B-held+D-pad, PSP, DS/3DS stylus, per-player/per-game persistence and gamepad/touch release. This is design scope, not implemented functionality or physical QA.

> **LATEST OWNER INPUT / QA REFINEMENT — 22 NOV 2026:** See [authoritative GC implementation handoff](gc01/GC01_AUTHORITATIVE_OWNER_IMPLEMENTATION_HANDOFF_2026-11-22.md). **Reciprocal RT↔X analogue↔digital swaps are warning-only (not blocked)**; test a real graduated on-screen analogue touch trigger and honest guest-axis response. **Wii gamepad-first**, option for genuine virtual IR/right-stick pointer + phone motion, mixed controls. **3DS independent virtual tilt stick** and real phone sensors; no claim of guest support from UI alone. Junior PIN plus parent-held recovery code at setup. All original GC A–H, GEM offline recommender, SafeSave real per-console counts and Export All must remain tested; physical status not inferred from docs/CI.

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

## Current A–H implementation evidence

After #362 merged, the continuation is draft #368 against master. SafeSave visual acceptance is **FAIL pending owner review** ([owner correction](https://github.com/safetyblade/ROMMachine/issues/290#issuecomment-7078323576)). The Save Centre uses the shipped SafeSave wordmark, primary backup and restore actions, counts from managed inventory, and expandable console groups. Verified unmatched imports can be retained in Unassigned Backups for later review. The Android TV shelf opens this same Save Centre through its SafeSave tile. Explicit assignment and a full verified-backup index remain open.

| Work | Source status | Remaining acceptance |
| --- | --- | --- |
| A, native menus | Existing shared menu retained | Menu ownership, reopen and recovery journeys. Physical QA NOT TESTED. |
| B, display | Existing viewport and opt-in landscape surround retained | Confirm aspect, safe zones, DS/3DS dual screens and tablet/TV settings. Physical QA NOT TESTED. |
| C, controllers | Existing mapping draft supports a reciprocal two-button preview, explicit confirmation and Undo. Native settings capture two physical presses and warn about digital trigger substitution. | Confirm routed guest actions and P1/P2 on each host, disconnect/reconnect, and full D-pad TV and DeX journeys. Physical QA NOT TESTED. |
| D, Wii and 3DS | Existing attachment and controller inventory retained | Guest IR and motion require source-backed dispatch and physical evidence. Physical QA NOT TESTED. |
| E, PivotArc and GripArc | Existing handheld inventories and source mappings retained; narrower GripArc painted arms share the solver footprint while retaining all four 48dp targets. PivotArc adds source layouts for Game Gear, Lynx, Neo Geo Pocket and Neo Geo Pocket Color using existing guest input codes. The optional Standard Hold Tray uses existing input ownership and measured clearance for GB, GBC, GBA, GG, Lynx, NGP, NGC and PSP. | WonderSwan, WonderSwan Color, PSP, DS and 3DS PivotArc layouts, four remaining Standard Hold Tray inventories, and joint fit for all twelve handhelds, 48dp targets, original guest buttons, orientation and held-input release. Physical QA NOT TESTED. |
| F, Modern Function | Existing research and custom mapping retained | Verified title and edition action mapping and actual gameplay. Physical QA NOT TESTED. |
| G, Xbox 360 continuity | Existing engine and player paths retained | Native ownership and save-only storage adapter, install-over and reopen. Physical QA NOT TESTED. |
| H, display/performance boundaries | Existing native provenance and engine pins retained | Respective runtime evidence and physical checks. PERF remains in its own lane. |
| GEM | The bundled overlay carries 35 exact-release contributions, including five independently authored gameplay-fact records sourced to publisher pages. Four proposed input holds remain staged. The 91 reviewed touch scores and 14 existing input holds are retained. Public provisional touch scores use half-points under the latest owner decision. Personal scores remain integers. | Full profile semantics, remaining licensed coverage and editorial confidence. Physical QA NOT TESTED. |
| SafeSave and Junior | Existing SafeSave surface displays app-managed bound inventory and provides SAF export, including whole recognized PS1 cards, verification, compatible same-player restore and interrupted-restore rollback. The import preview resolves local targets and shows which existing files will be backed up before replacement. | Native adapter coverage, immutable player session authorization, Junior playroom, multi-owner exports and transfers. Physical QA NOT TESTED. |
| SIGNATURE and TV | Existing startup clip, sound preferences and controller navigation retained. | Device audio routing and interruptions, logo framing and TV focus/launch/return. Physical QA NOT TESTED. |

### Source coverage for the Save Centre

Managed RAM means an existing UUID-bound save file, when present. Managed state
coverage includes occupied manual slots, autosaves and available sidecars under
a recognized configured core namespace. The restore checks binding, cartridge,
player and compatibility before activation. State restores presently require the
same CARTDECK build. An undocumented native save path remains outside this
inventory.

| Registered family or system | Game saves | Manual / auto states | Whole card / VMU | Restore |
| --- | --- | --- | --- | --- |
| GB, GBC, GBA | Managed RAM | Managed / managed | — | Managed format only |
| NES, SNES, Master System, Mega Drive, Game Gear, Sega CD | Managed RAM | Managed / managed | — | Managed format only |
| Atari 2600/7800, Lynx, PC Engine, Neo Geo Pocket/Color, WonderSwan/Color, N64 | Managed RAM | Managed / managed where emitted | — | Managed format only |
| FBNeo, MAME 2003 Plus | Managed RAM | Managed / managed where emitted | Native NVRAM excluded | Managed format only |
| DS | Managed RAM | Managed / managed | — | Managed format only. External conversions excluded |
| 3DS | Managed RAM where emitted | State creation unavailable in configured core | Native Citra storage excluded | Managed format only |
| PS1 | Recognized raw 128 KiB PCSX card | Managed / managed | Whole raw PCSX card | Recognized card and managed states |
| PSP | Managed RAM where emitted. PSP savedata snapshot backend separate | Managed / managed for recognized namespaces | — | Managed format only in Save Centre |
| Dreamcast | Managed RAM where emitted | Managed / managed for recognized namespaces | Native VMU excluded | Managed format only |
| GameCube | Managed RAM where emitted | Managed / managed for recognized namespaces | Native Dolphin cards excluded | Managed format only |
| Wii | Managed RAM where emitted | Managed / managed for recognized namespaces | Native mixed NAND excluded | Managed format only |
| PS2 | Managed RAM where emitted | Managed / managed for recognized namespaces | Native Play! and ARMSX2 cards excluded | Managed format only |
| PS3 | Guest user savedata outside this inventory | Engine states outside this inventory | — | Unavailable |
| Xbox | Mixed guest HDD excluded | Engine states outside this inventory | — | Unavailable |
| Xbox 360 | Native XUID-owned progress excluded pending a bounded adapter | Engine states outside this inventory | — | Unavailable |
| Wii U | Mixed cartridge NAND excluded | Engine states outside this inventory | — | Unavailable |
| DOS / PC | Managed RAM where emitted. Persistent guest disk excluded | DOSBox Pure declares state creation unavailable in this configuration | — | Managed format only |

For each row with managed support, use an owned title. Save in-game, write two
manual slots and an autosave where available, close the engine, export the
cartridge, change each save, restore after the backup confirmation, then reopen
the game and load each slot. For PS1, use a full card holding more than one
game and compare the entire image. Repeat after an install-over. Record game,
engine, exact APK SHA, Android version, device and individual result. All rows
are currently **NOT TESTED** on physical hardware.

### Sequential GC checkpoints

| Source | Scope | Verified evidence |
| --- | --- | --- |
| `dc7799cd` | Reconciled draft #368 with merged GC and added the branded Save Centre and five factual GEM records. | Catalogue and Room passed. The APK job stopped on HTTP 429 fetching Robolectric SDKs. [Workflow](https://github.com/safetyblade/ROMMachine/actions/runs/45016962589) |
| `98615c02` | Retained unmatched verified archives, reduced GripArc's painted footprint, and added a contribution preview with duplicate protection. | 747 local Android tests passed. The APK job still stopped on HTTP 429. [Workflow](https://github.com/safetyblade/ROMMachine/actions/runs/45059572339) |
| `f6b9b2fb` | Added Game Gear, Lynx and both Neo Geo Pocket PivotArc layouts and used a checksum-verified Maven Central mirror in CI. | 748 CI tests, UI gate and signed ARM64 build passed. [APK](https://github.com/safetyblade/ROMMachine/actions/runs/45060169330/artifacts/14769303114) |
| `f46c9792` | Enabled the optional measured Standard Hold Tray on eight supported handheld systems and added compact SafeSave console summaries and top-level access to Unassigned Backups. | 750 CI tests, UI gate and signed ARM64 build passed. The artifact source tree, checksum and signing certificate were verified. [APK](https://github.com/safetyblade/ROMMachine/actions/runs/45061354741/artifacts/14770495096) |
