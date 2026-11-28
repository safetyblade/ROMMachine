# CARTMOD Riivolution — libretro boot-glue inspection

**Date:** 28 November 2026
**Scope:** Does the Dolphin core CARTDECK actually ships — `libretro/dolphin`
at the pinned commit in `scripts/ci/build_gamecube_upstream_core.sh` —
apply Riivolution patches when the libretro frontend boots a Wii disc?
**Pinned source inspected:** `libretro/dolphin` @
`32c61f9d0e933331bb7087ba57e96883241b4960` (sparse checkout of
`Source/Core/DolphinLibretro`, `Source/Core/Core`, `Source/Core/DiscIO`).

## Verdict: (b) — parser and patcher exist in DiscIO, but the libretro boot path never invokes them

The machinery is all present in the core half of the tree. The frontend
half never touches it.

### Evidence

1. **The parser/patcher exist upstream, as previously listed.**
   `Source/Core/DiscIO/RiivolutionParser.h` declares
   `DiscIO::Riivolution::ParseFile(const std::string& filename)` (line 222)
   returning a `Disc`, and `Disc::GeneratePatches(const std::string& game_id)`
   (line 200) producing `std::vector<DiscIO::Riivolution::Patch>`.

2. **The core boot consumes patches from exactly one place.**
   `BootParameters` carries the patches:
   `Source/Core/Core/Boot/Boot.h` line 152 —
   `std::vector<DiscIO::Riivolution::Patch> riivolution_patches;`
   They are applied by `CBoot` during title boot
   (`Source/Core/Core/Boot/Boot.cpp` lines 706 and 709, including
   `DiscIO::Riivolution::ApplyGeneralMemoryPatches`) and
   `BootManager` reacts to them (`Source/Core/Core/BootManager.cpp`
   line 137: fast disc speed is forced when patches are present).

3. **The libretro frontend never sets them.**
   A case-insensitive search for `iivolution` across the whole of
   `Source/Core/DolphinLibretro/` returns **zero hits** — no core option,
   no preset loading, no parser call. The frontend's disc boot
   (`Source/Core/DolphinLibretro/Boot.cpp`) builds its parameters with
   `BootParameters::GenerateFromFile(normalized_game_paths)` (line 778)
   and hands them straight to `BootManager::BootCore` (line 807);
   `GenerateFromFile` does not populate `riivolution_patches`, so the
   vector is empty on every libretro boot and the patcher is dead code
   in this build.

### The exact insertion point for a future slice

Upstream already ships the helper a frontend is meant to use:
`AddRiivolutionPatches(BootParameters*, std::vector<DiscIO::Riivolution::Patch>)`,
declared at `Source/Core/Core/Boot/Boot.h` line 233 and defined at
`Boot.cpp` line 769 — it merges the patches into the disc FST
(lines 782–788) and assigns `boot_params->riivolution_patches`.

A future Riivolution slice therefore needs, in
`Source/Core/DolphinLibretro/Boot.cpp`, between `GenerateFromFile`
(line 778) and `BootCore` (line 807):

1. `DiscIO::Riivolution::ParseFile(<staged XML path>)` for the mod the
   user selected for this disc;
2. `disc.GeneratePatches(game_id)` for the booted disc's game id;
3. `AddRiivolutionPatches(boot_params.get(), patches)`.

That is a change to the **pinned upstream core**, not to CARTDECK app
code — it forks the engine build (`build_gamecube_upstream_core.sh`
currently refuses to build a locally patched tree under upstream
provenance), so it needs its own gate, provenance story, and device
proof. Per #212 the Riivolution slice stays gated "after PERF native
proof" + device; this note and the accompanying app-side scanner/stager
scaffold are the start of the lane, not the slice itself.

## What the app side can already do

Standalone Dolphin resolves Riivolution content from
`User/Load/Riivolution/` (XML configs plus the replacement files they
reference). The CARTMOD scaffold landed with this note
(`feature/cartmod/riivolution/`) scans a mod folder in that layout,
extracts game id / patch ids / file references, computes the
`Load/Riivolution/` staging layout against a Dolphin user directory,
and emits a preset descriptor in the shape of standalone Dolphin's
saveable `.json` presets — so when the core slice is approved, the app
already knows how to recognise and stage a mod.
