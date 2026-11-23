# Native export adapters — deliberate unimplemented interfaces

**Never label editor JSON as a playable ROM.** The files under \`examples/\` are editable projects only.

## Shared adapter interface (proposal)

\`\`\`
validateCapabilities(project, target) -> {errors[], warnings[], limitations[]}
prepareResources(project, target) -> deterministic resource bundle
build(prepared, pinnedRuntime) -> native console artifact OR typed failure
verify(artifact) -> {format, header, size, checksum, toolchainReceipt}
installToShelf(verified, explicitUserConsent) -> immutable cartridge identity
\`\`\`

Output receipt records source projectId, project revision/hash, target, adapter revision, runtime binary hash, toolchain versions, output digest, save schema and validated capabilities. Only actual reproducible builds may populate these receipt fields.

## GBC adapter candidate

Upstream references: [GB Studio](https://github.com/chrismaltby/gb-studio), [GBDK-2020](https://github.com/gbdk-2020/gbdk-2020). Generate actual GB/GBC ROM compatible with colour hardware where intended. Prove correct CGB flag, mapper/memory banking, save type, title/header/global checksums and behaviour under independent GB/GBC emulators. GB Studio commonly writes \`game.gb\`; verify CARTDECK SystemID detection by header, not suffix alone.

First proof: a 5x5 puzzle triggered by stepping on a goal, with a valid in-game save and reopen; then a 1-room shop.

## GBA adapter candidate

Upstream references: [Butano](https://github.com/GValiente/butano) and [GBA Studio](https://github.com/eoinjordan/GBA-Studio). Generate native \`.gba\` with an approved sprite/tile/VRAM budget and a pinned toolchain; test native boot, controls, deterministic game rules and FLASH/SRAM/EEPROM save semantics. GBA engine needs separately authored runtime and asset bundling; cannot assume a GBC engine binary works on GBA.

First proof: same game rules exported to genuinely running GBA ROM; compare game-state snapshots and detect differences.

## Key risk: data-packing vs source compilation

A precompiled, constrained GBC/GBA interpreter+resource packer is attractive for **offline Android export** but has not been demonstrated in CARTDECK. Not all binary formats permit adding arbitrary data after compilation. Verify linker maps, bank limits, ROM size, patchability, memory alignment and checksum calculation per runtime. If that cannot meet the required rules, use source-code generation plus GBDK/Butano cross compilation on an isolated supported build host. Do not silently require paid clouds or bundle desktop binaries that won't execute on Android ARM64.

## Status

\`GBC.exporter = BLOCKED_NATIVE_RUNTIME_PROOF\`
\`GBA.exporter = BLOCKED_NATIVE_RUNTIME_PROOF\`
\`PSP = OUT_OF_CURRENT_SCOPE\`

Do not call an editor-only demo a complete Maker Beta. The first native output requires an independent build receipt and gameplay/save regression on exact ROM and CARTDECK APK.