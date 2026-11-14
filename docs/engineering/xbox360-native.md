# Xbox 360 native integration — #197

Owner's implementation authorization supersedes the issue's earlier research-only scope.

Selected XenDroid `779680ade075d2d2a4c589c6a8d57a8e26c116c0`: native Android
ARM64 Xenia CPU backend, Vulkan surface, AAudio/OpenSLES and existing Android
input driver. Compared Xenia Libretro `4ed46ea90071a8c168cc980876094a2a0387eb49`:
it requires retro_hw_render_interface_vulkan and a Vulkan-negotiating frontend.
The existing GLES LibretroDroid host is deliberately unchanged. One native
architecture is retained; no separately installed emulator or upstream frontend.

The adapter builds only emulator-core, retains the base JNI ABI and registers
only four frontend-neutral native methods. Stock Vulkan is forced after config
load, not an Adreno/Turnip driver. Unimplemented guest UI providers use upstream
headless defaults; content installation/deletion is not exposed. The original
Xbox native host and graphics are untouched.

Build: scripts/ci/build_xbox360_native.py. Root source is pinned; recursive gitlinks
are checked at their recorded commits. Upstream SHA-pinned Gradle wrapper and
NDK29/CMake3.30.3 are used. Package verification requires ARM64/16KB ELF, resolved
native dependencies, JNI entry and ELF build ID. Manifests hash packaged bytes;
unstripped symbols, source/adapter provenance, licenses and corresponding source
are retained. Native engine artifact is engineering evidence, not APK acceptance.

Provisioning reuses XboxEngineStore's verified immutable packages, active/previous
pointers and explicit rollback, parametrized with backwards-compatible hakuX
defaults. Xbox360Engine uses native-engines/xbox360 and native/xbox360/content,
cache, config, logs and staging. Creating data directories never clears or
migrates content. Original Xbox HDD and installed engine stay independent.

Initial native diagnostics distinguish HOST_INIT, subsystem setup, successful
content launch and JIT **compilation**. They do not turn compilation, startup or
presentation into gameplay acceptance; guest pixels, frame/audio/input and
persistent save acceptance require runtime evidence. Engine load and API gates
are checked by the app before starting the isolated Activity.

Local validation: retained JNI Java compiles against Android35; the adapter was
applied to the actual pinned checkout and registration/stage markers verified;
C++17 compiled progress test passes. Local native build fails fast at missing
NDK29.0.14206865 (not a guest/GPU failure). Independent engineering CI provisions
the NDK and shader compilers after Kotlin preflight. No native/Actions/APK wait.
A complete Android native link and Xclipse runtime are unverified gates.

## Cartridge/runtime continuation

The stacked runtime change adds permanent `xbox360` identity without changing enum ordinals or any existing engine. Discovery remains available when the engine is absent; eligibility then explains that the verified native package is not installed. Engine selection never promotes startup, a JIT count or presentation into successful-gameplay fallback evidence.

ISO recognition uses bounded XDVDFS directory traversal and a verified `default.xex` signature. The original Xbox XBE probe runs first, and existing indexed cartridges retain their original records through the library's existing-record refresh path. Explicit Xbox 360 folder context is a fallback, not extension-only ISO detection. XEX, STFS/SVOD XCP and ZArchive footer checks match the pinned backend; M3U is not an executable engine format. An explicitly filed M3U cartridge is retained with a compatibility explanation. SAF XEX imports stage companion directories; SVOD stages its sibling `.data` directory. Staging is disposable, atomically copies each file and never writes back to original media.

`Xbox360Activity` hosts the same CARTDECK shell, PadKit layouts, appearance and shared menu in `:xbox360`; it does not launch XenDroid's application. Resume keeps the active guest. Advanced contains copyable local reports and explicit next-launch rollback; save-state, disc-change and fast-forward actions are absent. The native adapter requests the existing event loop to quit, destroys the guest, and reports shutdown completion; the Android host bounds the wait before isolated-process termination. No automatic relaunch occurs. Native crashes retain the last local report and existing launch journal.

The pad adapter preserves the pinned 24-entry ABI, signed axes, Back/Start, stick clicks, bumpers and analogue triggers. A targeted upstream correction passes trigger values instead of forcing 255 and synchronizes GetState reads with OnKey writes. Guide opens CARTDECK's menu (the backend has no guest Guide binding). Standard + GripArc uses the existing dock-first solver/accessory and separate slot input aggregation. Unsupported accessory geometries, including currently undefined Xbox 360 PivotArc geometry, show a session fallback while retaining settings; no existing console geometry is changed.

Diagnostics record actual selected Vulkan vendor/device/driver/API, missing `independentBlend`, device/present results, JIT compilation, guest swaps and host presents separately. Guest-image validity and gameplay remain explicitly UNVERIFIED. A new JVM contract project compiles the real import/staging/storage/pad/report code and retained Java JNI ABI against Android 35 without resolving the GLES frontend: `./gradlew -p scripts/ci/xbox360-tests test`. It does not replace the full Android integration gate.

Local evidence: targeted Kotlin formatting and the standalone JVM contracts pass. Full app compilation was attempted but dependency resolution returns HTTP 403 for the existing JitPack LibretroDroid 0.14.0 artifact. The local environment lacks pinned NDK 29.0.14206865, so there is no local native-binary, Vulkan, audio or gameplay success claim. The independent native Actions job provisions that toolchain. Full app compilation, native build/link, APK identity/signature/install-over, Xclipse ARM64 JIT/page behavior, video/audio/input, in-game saving across reopen/update/rollback and interruption acceptance remain gates. Do not merge or promote a device build on these source checks alone.
