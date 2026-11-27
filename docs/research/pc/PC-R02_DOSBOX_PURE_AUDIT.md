# PC-R02 — Shipped DOSBox Pure core audit

Programme: PC-00 (#299). Gate: **PC-R02** — audit the actual shipped DOSBox Pure
binary/source and licence, ARM64/16 KB ELF, required libretro APIs,
optical/guest/disk capabilities.
Date: 2026-11-28. Audited against ROMMachine master
`1e2e75c80bc3f30cd873136edee8472e6a37d6ff`.
Machine record: `spikes/pc/pcr02_sources.json`.
Mechanical check: `spikes/pc/verify_pcr02.py` (+ `spikes/pc/tests/test_verify_pcr02.py`).

This audit is read-only against the shipped binary. It makes **no claim of
Win95 or Shandalar gameplay**; guest execution stays with PC-R03/PC-R04.

## Verdicts

| # | Check | Verdict |
|---|-------|---------|
| 1 | Shipped binary identity (pin, path, size, hashes) | **PASS** |
| 2 | Licence identification | **PASS** |
| 2a | Corresponding upstream source revision for the prebuilt | **UNPROVABLE from available sources** |
| 3 | ARM64 ELF + 16 KB page alignment | **PASS** |
| 4 | Required libretro API surface (host contract) | **PASS** |
| 5 | Optical / guest / disk capability present in binary | **PASS (capability present; execution NOT TESTED)** |
| 6 | Binary inclusion in the final assembled APK | **NOT PROVEN — G2 build gate** |

## Evidence

### 1. Identity — PASS

- `.gitmodules` carries a single submodule, `lemuroid-cores`
  (https://github.com/Swordfish90/LemuroidCores). Its gitlink on current
  master is `fee2e824525daa22bcf318f96127fe43fa8a15ad` — identical to the
  audited baseline recorded by PC-04 (#305).
- Binary: `lemuroid-cores/lemuroid_core_dosbox_pure/src/main/jniLibs/arm64-v8a/libdosbox_pure_libretro_android.so`
  — 3,569,624 bytes, git blob SHA `8ada78cfcc87b5f915ede3534f41a53e883a7a9e`.
  Both figures match #305's record, and the blob SHA was **recomputed from
  the downloaded bytes** in this audit, so the analysed file is provably the
  shipped file.
- SHA-256 (newly recorded by this audit):
  `454c1d864353d946317d6bca04948ff8f9fd25cee7fe600f0d5238d42825babe`.
- Host registry agrees: `CoreID.DOSBOX_PURE` names exactly
  `libdosbox_pure_libretro_android.so`, and the DOS `GameSystem` routes to it
  with `uniqueExtensions = listOf("dosz")`.
- Packaging: the core ships as an Android dynamic-feature module whose
  `build.gradle.kts` sets `doNotStrip("*/*/*_libretro_android.so")`.

### 2. Licence — PASS; 2a. Source revision — UNPROVABLE

- Upstream (https://codeberg.org/schelling/dosbox-pure) `LICENSE` is the
  GPLv2 text and its README states DOSBox Pure, as well as original DOSBox,
  is under the **GNU General Public License, version 2 or later**. This
  matches the licence recorded in `pc04_sources.json`
  ("GPL-2.0-or-later, verify corresponding source before redistribution") —
  the identification half of that note is now verified.
- Obligation for bundled distribution: GPLv2 corresponding-source duties
  attach to this binary. The repo's own distribution contract
  (`pc04_sources.json`) already assigns the full corresponding-source review
  to REL-ENG-01 (#307); this audit does not close that review.
- The prebuilt carries **no embedded upstream version or revision string**,
  and LemuroidCores records no upstream commit for it. The exact upstream
  revision the binary was built from therefore cannot be pinned from
  available sources (`canonical_source_revision: UNVERIFIED`, unchanged).

### 3. ARM64 / 16 KB ELF — PASS

- ELF64, little-endian, `ET_DYN`, machine **AArch64 (183)**.
- Both `PT_LOAD` segments have `p_align = 0x4000` (16,384) with congruent
  file offsets/virtual addresses — satisfying the Android 16 KB page-size
  loader requirement.
- `DT_NEEDED` is limited to Android system libraries: `liblog.so`,
  `libc.so`, `libm.so`, `libdl.so`. SONAME: `libretro.so`.

### 4. Libretro API surface — PASS

- The host contract was taken from LibretroDroid source, not from upstream
  claims: `Core::open` resolves 23 entry points with `dlsym` and throws on
  any miss. **All 23 are exported** by the shipped binary; it additionally
  exports the optional `retro_get_region` and `retro_load_game_special`.
- Note: the host's DOS system config sets `statesSupported = false`.
  Save-state serialisation symbols exist in the core; states are a host
  configuration choice, not a core deficiency.

### 5. Optical / guest / disk capability — present; execution NOT TESTED

- The binary contains the DOSBox Pure start menu, the CD-ROM **image**
  interface (`CDROM_Interface_Image`), an IDE ATAPI CD-ROM device, MSCDEX
  support strings and the DOSBox FakeBIOS — the optical/disk capability
  PC-R02 asks about is present in the shipped code.
- Presence is not execution. Win95 guest boot remains **NOT_TESTED**
  (PC-R03/PC-R04) and Shandalar boot remains **NOT_TESTED** (PC-R04). No
  gameplay is claimed anywhere in this audit.

### 6. Final-APK inclusion — NOT PROVEN (G2 gate)

- The source-level packaging path exists (dynamic-feature module +
  `doNotStrip`), but proving the binary lands in the assembled artifact
  requires a G2 candidate APK build and inspection of that artifact. That
  proof is a build gate, not something this source/binary audit can state.

## Mechanical check

`python3 spikes/pc/verify_pcr02.py` re-verifies every PASS above from a
checked-out tree (gitlink pin, byte identity by size/SHA-256/blob SHA, ELF
machine + 16 KB congruence, host symbol completeness, DT_NEEDED allowlist,
registry routing, licence cross-record agreement, capability markers) and
exits non-zero on any drift. Run for this audit against the pinned tree:
**status PASS, 13/13 checks true.** Synthetic-ELF regression tests live in
`spikes/pc/tests/test_verify_pcr02.py` (8 tests, all passing) in the pattern
of the existing PC-04 probe tests.

## What this gate does not decide

- PC-R01 (independent runtime/lab architecture) is still the preceding
  increment; this audit does not integrate anything into a PC runtime.
- Guest execution, install media handling and Shandalar acceptance remain
  PC-R03/PC-R04 with their own proofs, on device, with owner media.
- Owner acceptance of this audit is a separate act; `owner_accepted` stays
  false in the machine record until then.
