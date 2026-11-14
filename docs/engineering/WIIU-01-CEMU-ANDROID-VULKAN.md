# WIIU-01 — Cemu Android ARM64 / Vulkan engineering implementation

Tracking: [#233](https://github.com/safetyblade/ROMMachine/issues/233). Source pin: WizzardSK/cemu-libretro at `444c585202302a7f4b319bafb4cf72156a8cac8b`, branch `libretro`, with recursive Git submodules. **Not a shipped or playable Wii U emulator yet.**

## Why a separate lane
The fork claims Android arm64-v8a support, but its `.github/workflows/libretro.yml` explicitly says its Android target has never been built and marks it `continue-on-error: true`. An external Linux ARM64 SO or unknown Android APK cannot substitute. Android Cemu needs Libretro's `RETRO_HW_CONTEXT_VULKAN` renderer and negotiated Vulkan instance/device/extensions/queues. The current CARTDECK LibretroDroid host uses OpenGL ES; a compiled Cemu core cannot render through it without an independently proven Vulkan interface.

## Phase A code in this draft
- `scripts/ci/build_wiiu_cemu_arm64.sh`: checks out exact git revision and submodules, configures NDK 29.0.14206865 and arm64-android vcpkg via upstream CMake, keeps Vulkan enabled and desktop OpenGL disabled, builds actual `cemu_libretro`. Fails red on source, CMake, compiler or linker failure. No binaries on master or default APK.
- `scripts/ci/wiiu_cemu_contract.py`: checks source pin, licence and Vulkan source contract; checks AArch64 ELF64 ET_DYN, >=16 KiB PT_LOAD, required Libretro exports, Android dynamic libraries, build ID and SHA-256. Output JSON hard-codes Vulkan frontend, APK and real gameplay as unverified.
- `scripts/ci/test_wiiu_cemu_contract.py`: offline positive/negative ELF and ABI tests; no permissive stub pass.
- `.github/workflows/cartdeck-wiiu-cemu-arm64.yml`: quick offline tests on pull requests, isolated expensive real Android native build on dedicated branch push/explicit dispatch only. A failure is not allowed to go green. Artifacts contain evidence, not a new unsigned public APK.

The above is *native feasibility engineering*, not SystemID, core installation, game UI, host interoperability or runtime proof. Successful build still needs Daniel's MPLv2/third-party dependency SBOM and distribution review.

## Phase B — actual Vulkan frontend inside CARTDECK
Build a new, isolated in-app Libretro Vulkan host (separate `:wiiu` process). Negotiate `RETRO_ENVIRONMENT_SET_HW_RENDER` with `RETRO_HW_CONTEXT_VULKAN` and `RETRO_ENVIRONMENT_SET_HW_RENDER_CONTEXT_NEGOTIATION_INTERFACE`. Expose a real frontend-owned VkInstance/VkPhysicalDevice/VkDevice/queue and enabled extensions/features to Cemu's `libretro_vulkan.h`, plus render interface callbacks and correct frame presentation. Handle lost/recreated surfaces, background/foreground, shutdown, sync, audio pacing, input poll, pointer/touch coordinates and VFS/seekable content staging. Never change existing Play!, Flycast, Dolphin, PS1, PSP or Xbox renderer while the regular APK is failing.

## Phase C — full cartridge integration
Only after Phase B video/audio/input proof: add a **distinct Wii U platform** (not Wii or Neo Geo Pocket Color / NGC) to the system registry and native detection. Recognise Cemu-documented `.wua`, `.wud`, `.wux`, `.rpx`, `.elf` and `title.tmd` with positive format/folder identity. Do not treat every `.iso` as Wii U. Preserve existing cartridge IDs, artwork, renames, shelf organisation and instal-over. Keep app-private user-owned `Cemu/keys.txt`, optional fonts and `Cemu/mlc01` NAND, shader caches, updates and DLC separate from Dolphin Wii data. No key/font/ROM downloads or silent save migration.

Cemu Libretro reports `retro_serialize_size = 0`: **no native save states**. CARTDECK Resume must mean reopen that game and its persisted in-game Wii U NAND save, not rewind/reload a RAM snapshot.

## Phase D — real acceptance
Owner handset Samsung SM-S721B Vulkan-capability query, actual Wii U test fixture/title and >50% battery; verified gameplay beyond first frame, sound, buttons + analog, Wii U GamePad touch, two screen modes, game save/reload, 15-minute stable session, crash recovery and install-over survival. Then Marcus independent QA and Daniel licence release gate. A game that requires unsupported GamePad/motion features is not declared playable by compile success.

**Stage output:** Code/configuration for A in isolated draft. B–D are deliberately *not claimed complete*.
