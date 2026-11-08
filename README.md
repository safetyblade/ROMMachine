# CARTDECK

**CARTDECK by prestwich.dev** is a native Android retro-gaming project built around an original virtual-console experience: **cartridge shelf → boot → play → suspend → resume**.

The app is under active development. The Android implementation currently builds on the existing Lemuroid/LibretroDroid code and emulator-core infrastructure. Source availability does **not** establish that every advertised console or feature has passed CARTDECK device QA. **There is no authorised public CARTDECK release yet.**

## Product principles

- A distinctive original C-slot identity, cartridge-inspired browsing and optional virtual handheld shell.
- Simple, offline-first game importing and resume; no integrated ROM catalogue.
- Preserve reliable battery-backed game saves, save states, controller support and input latency above visual effects.
- A clean default device presentation with optional floating controls and full-screen gameplay.
- Phone/tablet first; retain a controller-first Android TV pathway.

### Current engineering gate

**P0: GB / GBC / GBA playable debug APK and independent save-persistence verification.**

The existing package ID and test signing are intentionally preserved during P0 for in-place upgrade/save testing. Replacing them with a permanent CARTDECK identity requires a separate migration and QA gate. Unverified features should not be presented as released.

## Building

This project uses the existing Gradle wrapper and a recursive core submodule.

```sh
git clone --recurse-submodules https://github.com/safetyblade/ROMMachine.git
cd ROMMachine
./gradlew :lemuroid-app:assembleFreeBundleDebug
```

Actual build requirements, Android SDK/NDK compatibility and packaging evidence are recorded in `docs/P0_BASELINE.md` and `docs/PHONE_TESTING.md`. The module and application namespaces retain historical names until migration is approved. See `.github/workflows/cartdeck-standalone-debug.yml` for the single standalone ARM64 debug APK workflow. See `docs/engineering/CI_SINGLE_APK.md` for validation and failure-review details.

## Brand sources

Approved original CARTDECK vector artwork and colour editions are in `design/cartdeck-v03/`. The Android launcher C-slot and TV lockup vector resources are located in `lemuroid-app/src/main/res/`. Original/Pink are the approved directions; Mono/Green remain demonstration/reference editions unless separately approved.

The UI/icon integration is deliberately separate from emulator, core, ROM scanning and save code. Review `docs/CARTDECK_BRAND_MIGRATION.md` before further package/branding changes.

## Source lineage and licences

CARTDECK is developed from [Lemuroid](https://github.com/Swordfish90/Lemuroid) by Swordfish90, based on [LibretroDroid](https://github.com/Swordfish90/LibretroDroid), which builds on the [Libretro](https://www.libretro.com/) emulator ecosystem and multiple independent emulation cores. Additional original Lemuroid attribution, source notices and the GPL text remain in this repository. See `COPYING`, `THIRD_PARTY_NOTICES.md`, and the CARTDECK Drive Source & Licensing Register for core-by-core due diligence.

Rebranding does not change or eliminate upstream licence obligations. No commercial copyrighted ROMs, proprietary console firmware, or unlicensed manufacturer artwork are included or authorised by this project.

## Controlled delivery records

[Project folder (Google Drive)](https://drive.google.com/drive/folders/1O4Bc1p9IAEkiSWCPuauhtDEirGYX0r_1) · [Project foundation](docs/PROJECT_FOUNDATION.md) · [P0 baseline](docs/P0_BASELINE.md) · [Phone upgrade protocol](docs/PHONE_TESTING.md).

**Product Owner:** Jonathan Prestwich · **Project Director:** Cassia Rowan · **Studio Governance:** Helena Ward.
