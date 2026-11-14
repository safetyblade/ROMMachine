# PS2-NEXT-C1 — Full JNI source audit, bounded initial repair

Owner-approved PS2-NEXT: issue #232 (parent #218). Upstream ARMSX2 pinned revision `7a999782377e61fb96eca65e476b5ec0d182058e`.

## Verified discrepancy
At pinned ARMSX2 revision, `platforms/android/app/src/main/cpp/native-lib.cpp` calls `HTTPDownloaderAndroid::BindFromJNI(env)` during `NativeApp.initialize()`, whether or not CARTDECK uses cover/achievement networking. The called source `common/HTTPDownloaderAndroid.cpp` performs `FindClass("kr/co/iefriends/pcsx2/HttpClient")`, binds its static `doRequest(String,String,byte[],String,int)` method with return `HttpClient.Response`, and reads `Response.statusCode:int`, `contentType:String`, `data:byte[]`. Previously CARTDECK's debug-only bridge omitted **both** HttpClient and Response classes.

Upstream source clears pending exceptions and returns `false` when the class is missing, so this is a **known missing optional JNI binding**, **not evidence that it caused the owner's SIGSEGV**. Without a bridge it leaves the HTTP downloader unavailable. Do not claim this alone makes PS2 playable.

## Isolated C1 repair
- Introduce `lemuroid-app/src/debug/java/kr/co/iefriends/pcsx2/HttpClient.java` with the exact descriptor and Response fields required by pinned JNI.
- Deliberately return `statusCode=-1` with empty data; the private debug probe must not start unknown background HTTP downloads. This is a **fail-closed transport placeholder**, not production HTTP, achievements or covers.
- Add `scripts/ci/test_ps2_next_jni_contract.py` verifying the nine static callbacks bound by `native-lib.cpp`, the HTTP class/field descriptor, BiosInfo bridge and no accidental network activation. Pin evidence to this exact source revision.
- Keep everything in `src/debug` and a separate workflow. Do not change default PS2 Play!, regular APK, emulated PS2 saves, user data, game detection or any other console.

## Still required before playing
- Native source-build success with all matching 4K/16K C++/Rust libs, Android dependency closure and GPLv3 notices (separate original workflow, which the pull-request contract job did not execute).
- Complete audit of any **other** JNI `FindClass` or callbacks outside `native-lib.cpp` (this patch only covers its nine callbacks and HTTPDownloaderAndroid). Convert optional/no-op callbacks into real bounded implementations where needed by gameplay; do not confuse ABI method existence with behavioural compatibility.
- Make complete test APK, user-provided BIOS/resources and lawful ISO, verify Android native surface, audio, gamepad and memory-card persistence on Samsung SM-S721B. A returned VM or first frame is **not** a gameplay pass.
- Follow gated normal Open/Resume and reversible Play! engine opt-in only with Marcus/Daniel/owner acceptance.

This change is small and crash-prevention-minded. It does **not** establish complete ARMSX2 Android app integration.
