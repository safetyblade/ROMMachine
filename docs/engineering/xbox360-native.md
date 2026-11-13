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
