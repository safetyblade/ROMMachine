# CARTDECK PS2-NEXT-A — ARMSX2 native integration feasibility

Status: **implementation in isolated engineering branch, device gameplay NOT VERIFIED**.
Owner authorization: 14 November 2026. Tracking: [#218](https://github.com/safetyblade/ROMMachine/issues/218), [#222](https://github.com/safetyblade/ROMMachine/issues/222). The production engine remains **pinned Play!** until a replacement survives the complete device and release gates.

## Source and exact build inputs

- Upstream: [ARMSX2/ARMSX2](https://github.com/ARMSX2/ARMSX2), pinned commit **7a999782377e61fb96eca65e476b5ec0d182058e**. The source is fetched into a new, isolated working directory. No moving branch/tag is accepted.
- Android: **NDK 29.0.14206865**, Android ARM64 target, pinned upstream Gradle wrapper (9.4.1).
- Native source boundary: upstream platforms/android/app/src/main/cpp/CMakeLists.txt, target emucore_4k or emucore_16k. Source code for Android JNI lives in platforms/android/app/src/main/cpp/native-lib.cpp.
- Stage 1 build command (does NOT package a CARTDECK APK): python3 scripts/ci/ps2_next_armsx2.py /private/clean/work --pages 4k.
- Stage 2 page-size proof: python3 scripts/ci/ps2_next_armsx2.py /private/new/work --pages both.
- Separate workflow: .github/workflows/cartdeck-ps2-next-armsx2.yml. All outputs remain private; the workflow uploads only machine-readable build evidence, not unreviewed engine binaries.

There is no hard dependency on the public upstream ARMSX2 standalone Android APK. Its package ID, visual shell, settings UI and game library are NOT reused. Its separately advertised Linux ARM64 Libretro library is NOT Android-compatible evidence.

## Executable quality gate

The Python builder checks the exact Git revision and submodules, an unchanged checked-out source tree, GPLv3 and third-party notice inventory, and that a native output actually exists. It verifies an ELF64 little-endian AArch64 ET_DYN library with **at least 16 KiB PT_LOAD alignment** and Android dynamic dependencies. It rejects desktop glibc, libstdc++ and proprietary Discord linkage. It checks six required real Android JNI exports: initialize, runVMThread, onNativeSurfaceChanged, onNativeSurfaceDestroyed, setPadButton and shutdown. Each native variant gets a file SHA-256, GNU build ID, ABI record and dependency inventory. No reported build may substitute a placeholder or prebuilt Linux library.

Unit tests deliberately exercise missing JNI symbols, wrong architecture, wrong ELF type, insufficient page alignment, prohibited dependencies, missing source notices, and unsafe build-directory overwrite. A build failure is RED, not silently skipped.

**Important:** A 16 KiB-aligned ELF is not equivalent to building the core with the correct 4 KiB/16 KiB emulator host page model. The separate emucore_4k and emucore_16k builds are explicitly recorded and must be selected according to the actual device's page size, as upstream NativeApp does.

## Native integration boundary — phase B contract

The upstream Java application calls into a native C++ ABI that is *not* a generic Libretro implementation:

| Host responsibility | Source-verified upstream API | CARTDECK integration requirement |
|---|---|---|
| Initialise | NativeApp.initialize(path, biosFolder, apiVer) | App-private versioned ARMSX2 data root and user-supplied BIOS; no Play! reuse |
| Graphics | onNativeSurfaceChanged(Surface, width, height); onNativeSurfaceDestroyed() | Isolated PS2 game process and SurfaceView with correct Vulkan/GLES lifecycle on Xclipse 940 |
| VM boot | runVMThread(String path) | Copy/seekable-stage user-selected SAF image to private staging and audit actual VM startup |
| Controls | setPadButton(index, range, pressed), setPadButtonForPort | Map CARTDECK GripArc and gamepad state to PS2 digital/analog inputs; preserve handheld/landscape/TV |
| Lifecycle | pause(), shutdown() | Explicit stop/cleanup; never auto-relaunch after SIGSEGV |
| Save data | PCSX2/ARMSX2 VM config and memory-card folders | Private ARMSX2 cards per persistent cartridge identity; no Play! card migration |

The upstream JNI is bound to the Java class name kr.co.iefriends.pcsx2.NativeApp. Phase B must provide a small, legally compliant package-compatible bridge **or** refactor the upstream native JNI symbol mapping in a maintained patch. It must NOT copy the entire standalone app, nor imply the six entry points alone cover all callbacks. Inventory every Java callback and native dependency from source before adding the bridge. The existing CARTDECK NativeEngineRegistry and GameLauncher remain unchanged in this feasibility PR.

The source-backed Android core includes SDL3, Oboe, shaderc, ADRENOTOOLS, EGL and Vulkan integration, and requires a native build with dependencies and corresponding-source obligations resolved. Audit the actual ELF dependencies and app-specific JNI bridges before runtime embedding; a working compiled core alone does not establish a stable host.

## Ownership, saves and rollback

Keep CARTDECK system PS2, Game/cartridgeId identity, custom artwork, name, collection, favourites, existing Play! save directory and all current console launchers intact.

Phase B engine-selection plan:
- New source-built engine is an opt-in per-cartridge alternative to Play!, not an unconditional new default.
- Separate files such as private native-engines/ps2/armsx2/<upstream-revision> for engine versions and private saves/ps2/armsx2/<cartridgeId> for memory cards; never delete or silently convert Play! cards.
- User-supplied legitimate BIOS from a neutral file picker; never ship or fetch BIOS, ROMs or commercial assets.
- Renderer path chosen from actual device capabilities, not only advertised GLES. Xclipse 940 ANGLE/GLES and Vulkan must each pass independent context-lifecycle tests.
- Crash stages distinguish first video callback from actual game/audio/input/save acceptance. A native signal 11 remains a failure without a captured tombstone; no silent retry loops.
- Only promote default after measured Soulcalibur III physical gameplay, two sticks/audio, 15-minute thermal test, save/reload, independent QA, legal source-notice approval and revert/install-over verification.

## Diagnostics carried forward

Owner report: CARTDECK 1.17.0-DEBUG (90568914), Play! SHA 1daff2c0780be34749120247506c1e9739d0bdd4a3ec01ff81545f988681629e, source 1f27232772e484bfb79364b751b84f41f4c87481, host 0.14.0-cartdeck3, Samsung SM-S721B API 37, Xclipse 940 through ANGLE. Native process exit Android reason=2 / status=11 (SIGSEGV) after CORE_FRAME, no matching tombstone. This **does not** prove which native library or graphics driver caused the fault, nor does it prove gameplay on the proposed replacement.

## Release boundary

Do not make this new workflow a required check for regular CARTDECK APK, Stage #145, Xbox360, Wii, Dreamcast or PS1/PSP releases. Do not bundle ARMSX2, create a callable PS2 activity, or alter the default PS2 configuration until native build evidence, JNI dependency analysis and Phase B ownership are independently approved.
