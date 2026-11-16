# CARTDECK Junior / SafeSave — Source-Level Engine Persistence Audit
**Audit:** 2026-11-16 · Daniel Mercer · GitHub master source review only. **No APK/physical gameplay or complete-save capture verified.**
**Repository source observed:** `master`, Room **v13** (`RetrogradeDatabase.kt`). PR #206 (collision-safe binding) merged 2026-11-14. Issue #145 remains open; no assumption of full scope acceptance.

## 1. Confirmed source-level contracts
| Component | Evidence on current master | Finding | Design consequence |
| --- | --- | --- | --- |
| Database | `retrograde-app-shared/.../library/db/RetrogradeDatabase.kt`, `dao/Migrations.kt` | Room **13**; v12→13 introduces `retired_cartridges`, 11→12 supplies stable UUID/legacy aliases | Next migration is additive from current schema, choose actual next unused version at branch cut; preserve retired identity ledger |
| Cartridge saves | `.../saves/CartridgeSaveBinding.kt` | UUID-scoped SRAM/states/previews, copy-verify-activate; explicitly excludes native NAND/HDD/cards and PSP directories | New per-player ownership layer MUST wrap, not rewrite or double-bind existing cartridge binding; preserve unresolved legacy ownership |
| SRAM | `.../saves/SavesManager.kt` | Normal/isolated engine SRAM; modern PCSX can seed raw card; global legacy aliases still possible | Stable player namespace + legacy default adoption needed |
| Save states | `.../saves/StatesManager.kt` | Manual and auto states by core namespace, metadata sidecars | Preserve state version/core compatibility; transfer as matched bundle |
| Shared paths | `.../storage/DirectoriesManager.kt` | Internal `system`, external app-scoped `saves`, `states`, `state-previews`; internal `cartdeck/save-bindings` | Snapshot both internal/external app scoped roots, not just external savedata; don't include ROMs |
| Engine launcher | `lemuroid-app/.../shared/game/GameLauncher.kt` | Multiple independent launch branches: Wii U, PS3, ARMSX2, Xbox, Xenia, common Libretro | Authorization MUST run before **any** native path and be revalidated in host, not rely on a UI filter |
| Common engine loader | `.../lib/game/GameLoader.kt` | Native engines can own `system` and `saves` directories; PPSSPP migrated/snapshotted separately | Need engine adapter discovery/flush/snapshot; global storage ownership cannot be inferred from cartridge filename |
| Original save sync | `lemuroid-app-ext-play/.../savesync/SaveSyncManagerImpl.kt`, `SaveSyncWork.kt` | Play flavor has Google Drive appDataFolder 2-way `saves`, `states`, previews syncing, including optional periodic worker. Free flavor unsupported | **High-risk compatibility:** legacy sync may conflict with per-profile SafeSave or restored files. Explicit compatibility/migration lock; never enable both writers on same profile paths; preserve prior cloud state |

## 2. Engine/save coverage matrix
**Status definitions:** SOURCE-PATH = concrete source-backed path or format; ADAPTER-DESIGN = discovery contract specified but not built; DEVICE-UNVERIFIED = no physical test in this audit. *None* of the below are SafeSave-export PASS.

| Platform/family | Source evidence / likely artifacts | Complexity and required adapter | Present readiness |
| --- | --- | --- | --- |
| GB/GBC/GBA and other existing Libretro SRAM systems | `SavesManager`, `StatesManager`, `CartridgeSaveBinding` | Cartridge-scoped save RAM and core-scoped states; unknown cartridge save type varies by core; preserve legacy aliases | SOURCE-PATH / DEVICE-UNVERIFIED |
| PS1 PCSX | `PlayStationMemoryCard.kt` raw 128 KiB 'MC' card check; `SavesManager` PCSX modern seed | Raw card may be represented as SRAM; do not export twice; capture both card and states with engine/version | SOURCE-PATH / DEVICE-UNVERIFIED |
| DS | `GameLoader` DesmumeMigrationHandler; `MelonDsSavesMigrator.kt`; `CartridgeSaveBinding` DS read guard | `.sav/.dsv` conversions and per-core states; no silent conversion during import | SOURCE-PATH / DEVICE-UNVERIFIED |
| PSP | `GameLoader.kt` `system/PPSSPP` + `system/PSP`, `PSPSavedataSeed.kt`; `PPSSPP` modern separated | PSP SAVEDATA full trees, resources vs owned saves; preserve native directory layout | SOURCE-PATH / DEVICE-UNVERIFIED |
| Dreamcast | `GameLoader.kt` isolated Flycast modern; native/system directory | VMU/card and game saves need detailed native location and flush audit; cannot infer global SRAM equivalence | ADAPTER-DESIGN / DEVICE-UNVERIFIED |
| GameCube/Wii | `GameLoader.kt` Dolphin native system + saves + provisioned assets | Memory card images, Wii save containers, Wii system NAND may be mixed; separate saves from cache/resources/system software | ADAPTER-DESIGN / DEVICE-UNVERIFIED |
| PS2 ARMSX2 | `Armsx2Storage.kt`: `native/ps2/armsx2/cartridges/<cartridgeUUID>/memcards`, separate `bios` and `resources` | Per-cartridge, not per-player; must add profile layer without moving BIOS/resources or breaking engine; include complete cards | SOURCE-PATH / DEVICE-UNVERIFIED |
| PS2 Play! | `GameLoader.kt` `native/system` + `saves/native` ; `Armsx2Storage.kt` explicitly separates from Play! cards | Discover actual Play! memory-card/storage path; cannot assume ARMSX2 cards equivalent | ADAPTER-DESIGN / DEVICE-UNVERIFIED |
| PS3 | `Ps3Storage.kt`: per-engine `native/ps3/{armsx3,rpcs3-libretro}`, `guest/dev_hdd0/home/<8-digit user>`, `profiles.properties` | Native PS3 user may differ from CARTDECK player; map one-to-one or isolated clone after validation; exclude firmware, games and trophies unless separately classified; inspect existing in-app save import/export methods | SOURCE-PATH / DEVICE-UNVERIFIED |
| Xbox original | `XboxStorage.kt`: shared `native/xbox/hdd`, revision-change full-HDD zip backup | Existing HDD can contain non-save files, do not call existing full HDD archive 'saves-only'; engine might not be multi-user-isolated yet | SOURCE-PATH / DEVICE-UNVERIFIED |
| Xbox 360 | `Xbox360Profiles.kt`: persistent Xenia offline XUID, Account file and `content/<XUID>` | CARTDECK local player UUID distinct from native XUID; preserve selection for legacy default; avoid sharing native XUID/content across players, never upload native account secrets as saves | SOURCE-PATH / DEVICE-UNVERIFIED |
| Wii U | `WiiuStorage.kt`: `native/wiiu/cartridges/<UUID>` NAND; media staging separate | Per-cartridge NAND not per-player; save-only extraction required, preserve cartridge identity; current registry may list unapproved engine runtime | SOURCE-PATH / DEVICE-UNVERIFIED |
| Other registered native/emulated systems | NativeEngineRegistry, individual core path contracts | Audit every registered core for actual savedata path, version and compatibility | NOT INVENTORIED / DEVICE-UNVERIFIED |

## 3. Critical audit findings
1. **Room 13 supersedes earlier draft saying v12.** Never implement migration on stale schema. Record complete current master SHA at start of branch and verify 11→12→13→next as installed, with schema export JSON and legacy test fixture.
2. **Existing Google Drive sync is real in the Play flavor.** Must not silently reuse its old 2-way folder sync against new profile-owned data. Preserve legacy sync for default profile until migration choice; stop concurrent writers during upgrade; do not delete remote data. Require explicit migration/consent for future SafeSave Vault.
3. **PS3 and Xenia already implement native user profiles.** A CARTDECK profile must map to an engine-native profile and remain isolated; don't create competing accounts or replace guest user data on upgrade.
4. **PS2 ARMSX2 and WiiU use per-cartridge stores, not per-player.** Add adapter namespace or verified mapping before allowing two players to access same cartridge's storage.
5. **Xbox raw HDD contains potentially mixed content.** Its existing engine-revision backup is not automatically eligible for saves-only export/cloud.
6. **No local code path proves an all-engine export or import.** Initial SafeSave implementation must include a coverage report and refuse the phrase 'Everything backed up' where engines are not audited.
7. **Native engine availability != successful gameplay.** Never use console registration alone as a save-support claim.

## 4. Mandatory implementation discovery (before writing each engine adapter)
For each active engine/core record: `systemId`; `coreId`; `engineVersion`; paths of game saves, card/VMU images, user account metadata, statestates and sidecars; game-scope versus bank-scope; whether writes flush on pause/exit; required quiesce; ownership model; test fixture; import version contract; export exclusions; max artifact size; whether Play flavor legacy Google Drive touches same paths; rollback path; ownership reviewer and Marcus device evidence.
Do **not** mark an engine adapter READY until its synthetic roundtrip and corresponding device reopen are verified. If storage cannot isolate two users, block secondary profile launch (with a clear message) rather than allowing shared progress.
