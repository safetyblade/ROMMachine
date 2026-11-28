# CARTMOD Maker — the path from project data to a made, launched, exported ROM

**Status: research findings, 28 Nov 2026.** Companion to `spikes/cartmod-maker/README.md`
("Engineering sequence after PC gate") and `spikes/cartmod-maker/EXPORT_ADAPTER_CONTRACT.md`.
Builds on the maker foundation in PR #392 (Kotlin model of the v1 project contract +
validator + deterministic preview stepper, fenced NOT WIRED). Facts are labelled
**VERIFIED** (repo/plan evidence) or **REASONED** (established platform knowledge applied
to this design). Gates #212 / #304 stand throughout.

## The goal, in the plan's own sequence

**VERIFIED** — the spike README's engineering sequence after the PC gate:

1. "Author a limited data-driven native GBC runtime against GBDK/GB Studio references; compile once through a verified desktop/CI toolchain. Boot and save it in an independent emulator."
2. "Implement a safe GBC packer that injects validated project-data resources into the proven runtime and produces a **real** ROM with correct header, memory banking, title and checksums. Compare Python/Kotlin preview and native behaviour."
3. "Repeat with a separately built GBA runtime using Butano and its target-specific memory/graphics/saving constraints."
4. "Write the Kotlin/Jetpack Compose Android editor and on-device offline persistence; connect its data contract and preview first, native build adapters only after validation."
5. "Bring signed/hashed outputs into CARTDECK as new, stable, user-approved cartridges without changing existing originals or SaveBindings. Confirm actual install-over and device QA."

**VERIFIED** — both exporters are currently `BLOCKED_NATIVE_RUNTIME_PROOF`
(EXPORT_ADAPTER_CONTRACT.md). Step 1's app-side half landed in PR #392; its native half
(the runtime itself) is the current blocker for everything downstream.

## The pipeline: project data → ROM

**Project JSON (v1 contract) → packer → precompiled runtime image → `.gb`/`.gbc` (later `.gba`).**

### Why the ROM is packed, not compiled, on device

**REASONED, and stated in the plan itself:** GBDK-2020 and devkitPro/Butano are desktop
C toolchains. There are no Android/ARM64 builds of them to bundle, and the export
contract explicitly forbids the shortcut: "Do not silently require paid clouds or bundle
desktop binaries that won't execute on Android ARM64." So compilation happens **once**,
on a verified desktop/CI host (step 1), producing a fixed native runtime that interprets
project data. Everything the user "makes" on device is data. The on-device component is
a **packer**: it takes the validated project, serialises it into the runtime's resource
format, writes it into reserved space inside the runtime image, fixes the header, and
recomputes checksums. The export contract names this the key open risk — "data-packing
vs source compilation … has not been demonstrated in CARTDECK" — and sets the fallback:
if a runtime cannot meet the rules by packing, fall back to source generation + cross
compilation on an isolated build host (which breaks offline on-device export, so it is
the fallback, not the plan).

### What the GBC packer must patch

**REASONED** (standard GB/GBC header layout; the runtime's linker map defines the exact
offsets, which is why step 1 must reserve the data region deliberately):

- **Project data region** — a bank (or banks) reserved at link time, left as known
  padding in the runtime image. The packer overwrites it with the serialised project
  resource bundle and **must reject any project whose bundle exceeds the reserved
  space** (bounded validation already exists in the v1 contract; the packer adds the
  byte budget).
- **Title** at `0x134–0x142` (ASCII, from the project's `title`; the v1 contract caps
  titles at 64 chars, the header allows at most 15 bytes in CGB mode — the packer
  truncates or rejects, it does not overflow).
- **CGB flag** at `0x143` (`0x80` = CGB-enhanced, `0xC0` = CGB-only) — fixed by the
  runtime's target, set by the packer to match.
- **Cartridge type `0x147`, ROM size `0x148`, RAM size `0x149`** — these describe the
  runtime's mapper/banking and save hardware. They are properties of the compiled
  runtime, not of the project; the packer copies them from the runtime's provenance
  record and never lets project data change them.
- **Header checksum** at `0x14D`: `x = 0; for i in 0x134..0x14C: x = x − rom[i] − 1`
  (mod 256). Recomputed after any header byte changes.
- **Global checksum** at `0x14E–0x14F` (big-endian): sum of every byte in the ROM
  except those two. Recomputed last, after data injection.

### The header logo — a decision point, not a detail

**REASONED:** bytes `0x104–0x133` hold a fixed logo bitmap that real Game Boy hardware
checks during boot; a ROM without the correct bytes does not boot on hardware. GBDK's
toolchain (rgbfix) writes these bytes as part of producing a valid ROM, so the step-1
runtime will contain them if built the standard way. Many emulators tolerate a wrong
logo; hardware does not. Whether CARTDECK-distributed maker ROMs carry those bytes is
a legal-technical call (the bytes are a Nintendo trademark) that belongs to the owner
at the step-1 gate — this research flags it and takes no position.

### GBA differences

**VERIFIED:** the plan gives GBA its own runtime and its own step (step 3, Butano) and
warns "cannot assume a GBC engine binary works on GBA" (EXPORT_ADAPTER_CONTRACT.md).
**REASONED:** the packer *concept* transfers, the details do not:

- Header: entry branch at `0x00`, logo at `0x04–0x9F` (same hardware-boot decision
  point), title at `0xA0–0xAB` (12 chars), game code at `0xAC–0xAF`, fixed byte `0xB2`
  = `0x96`, and a **header complement check** at `0xBD` computed over `0xA0–0xBC` —
  there is no GBC-style global checksum to recompute.
- No mapper/banking: a flat ROM image (≤ 32 MB), so the data region is simpler to
  reserve — but save hardware (SRAM / Flash / EEPROM) is a property baked into the
  Butano runtime, and the packer cannot change it after the fact.
- There is **no data-injection precedent for GBA in the plan yet**: step 3 says
  "repeat with a separately built GBA runtime", i.e. the GBA packer is designed after
  the GBC packer is proven, not in parallel.

### Path call: GB/GBC first, GBA second

**VERIFIED + REASONED:** the plan sequences GBC first, names its first proofs (a 5×5
goal puzzle with a working save, then a 1-room shop — both already exist as v1
templates, Pocket Puzzle and Pocket Shop), and its references (GBDK-2020, GB Studio)
are the mature data-driven path. GBA inherits the proven packer pattern afterwards.
**GBC is the shorter path; GBA is a second runtime, not a port.**

## LAUNCH: the made ROM re-enters the existing app

**VERIFIED:** CARTDECK already scans a library, detects GB/GBC systems **by header, not
suffix alone** (EXPORT_ADAPTER_CONTRACT.md: "verify CARTDECK SystemID detection by
header, not suffix alone"), and boots them through its existing emulator stack. The
maker adds **no emulator work**:

1. The packer writes the finished `.gb`/`.gbc` to an app-managed staging location (or
   directly into a user-chosen library folder).
2. The existing library scan picks it up; header detection registers it as GB/GBC.
3. README step 5 governs identity: outputs arrive as **new, stable, user-approved
   cartridges** — a re-pack of the same project must not silently overwrite or rebind
   an existing cartridge or its saves (SafeSave bindings stay with the original
   cartridge identity).
4. Boot and save then run through the ordinary cartridge path — which is also the
   acceptance test: preview behaviour (Python/Kotlin stepper) and native behaviour
   must match on the same project (README step 2).

## EXPORT: the ROM leaves the app

**REASONED from in-app precedent:** the codebase already moves user files out through
Android SAF and the share sheet — SafeSave's export flow and the support diagnostic
package export (file-provider backed share / save-as) are the established patterns;
the maker export reuses them rather than inventing a third mechanism.

An export consists of:

- **The ROM file** (`.gb`/`.gbc`, later `.gba`) — the deliverable, via SAF
  save-as (user picks the destination) and/or share.
- **Optionally the project JSON** (`.cartproj.json`) alongside it, so the game can be
  re-imported and edited later. A ROM alone is playable but not re-editable; the pair
  is the complete export.
- The build receipt the export contract specifies (source project id + revision/hash,
  target, runtime binary hash, output digest) belongs with the cartridge record inside
  the app; only reproducible builds may populate it — the packer must therefore be
  deterministic (same project + same runtime → byte-identical ROM), which the header /
  checksum steps above already require.

## Ordered remaining steps, with gates

| # | Step | Gate |
|---|------|------|
| 1 | Finish step 1's native half: author the data-driven GBC runtime (GBDK), compile once on a verified desktop/CI toolchain, boot + save in an independent emulator | Toolchain gate; logo decision (owner) |
| 2 | Canonical project-JSON serialiser on top of PR #392's model (shared input for packer + editor), round-trip tested against the validator | Lands with/after #392 — see note below |
| 3 | GBC packer: data injection at linker-reserved offsets, header fix-ups, header + global checksum recomputation, size-budget rejection | Step 1 proven |
| 4 | Behaviour proof: preview stepper vs native ROM on Pocket Puzzle + Pocket Shop; save/reopen on the produced ROM | Emulator proof, then device QA |
| 5 | Editor + offline persistence (README step 4), connected to the contract and preview first | After packer validation |
| 6 | Launch integration: staged output → library scan → user-approved cartridge, existing saves untouched (README step 5) | Owner gate (#212) |
| 7 | Export via SAF/share (ROM + project JSON) | Steps 3 + 6 |
| 8 | GBA runtime via Butano + GBA packer (README step 3), same proof pattern | After GBC path proven end-to-end |

## Note on the serialiser (step 2) — deliberately not coded in this pass

The canonical serialiser must serialise **PR #392's model** — and #392 is an open,
unmerged draft. Basing a serialiser PR on master (which does not contain the model)
would mean duplicating `MakerProject` and its types into a second, divergent model
that someone later has to reconcile. The serialiser therefore lands either stacked on
#392's branch or immediately after #392 merges, as a small PR: `CartmodMakerJson`
(encode/decode) + a round-trip test (decode ∘ encode = identity on the templates;
validator accepts every decoded project). Estimated one short run once #392's state
is settled.
