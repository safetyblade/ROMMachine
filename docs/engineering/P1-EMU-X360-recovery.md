# P1-EMU-X360 packaging continuation

Baseline: a3407cee11fbb8e78002200103f1b08aa2a4663d.
Status: INCOMPLETE ENGINEERING; Samsung gameplay unverified.

Recovered/retained #219 and its native-engine, cartridge-runtime and verified-APK
branches. The baseline already contains the isolated Activity, native adapter,
24-entry controller ABI, persistent virtual storage, diagnostics, verified
versioned engine store and rollback. No old UI/database tree was merged.

The new lane now selects the existing canonical opt-in combined ARM64 APK
job. That proven pipeline includes the five existing engines, Wii, original
Xbox and the real source-built Xenia package, preserving current gaming paths.
It runs full app lint/JVM checks and verifies package identity, signing,
version, engine bytes and dependency closure. The independent native workflow
retains engine evidence only. Standard distribution remains gated; no X360-only
APK is installed over the player's working Wii/Xbox application.
The stager now permits a genuine X360-only package instead of requiring an
unrelated legacy engine argument. No native binary is substituted.

JNI verification now requires an actual defined, exported function; an undefined
JNI_OnLoad import or a similarly named symbol cannot satisfy packaging. Tests
exercise undefined/local/hidden/data symbols. The existing native source still
forces the stock Vulkan loader; no Adreno driver is selected for Xclipse.

Local targeted Xbox 360 JVM contracts and native diagnostic tests passed.
Native source/dependency checkout and full compilation, complete APK and Room
checks remain required. No installable APK, SHA256, sound, guest frame or
performance success is claimed until those checks actually finish.

Physical #248 must eventually verify SM-S721B stock-driver feature checks,
independentBlend failures, JIT versus guest swaps versus visible images, audio,
all controls, persistent in-game save/reopen, menu return and rollback. Save
State/Load State remain unavailable. Original Xbox hakuX data is unchanged.
