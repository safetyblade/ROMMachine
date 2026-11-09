# Native emulator laboratory — single CARTDECK ARM64 debug APK

This branch collects the **independently reviewable** DC-01/DC-02 (PRs #95/#101), GC-01 (#98), PS2-01 (#99) and shared media source (#104) into **one existing CARTDECK application** and one CARTDECK APK pipeline. It does not supplant or merge those individual PRs; it is a separate **debug-only integration exercise**.

## Debug runtime routes

- `com.swordfish.lemuroid.app.debug.DreamcastRuntimeProbeActivity` — pinned Flycast, versioned A/B core fallback, local lawful GDI/CHD.
- `com.swordfish.lemuroid.app.debug.GameCubeRuntimeProbeActivity` — local Dolphin ARM64 Libretro build, validated GameCube GCM/ISO.
- `com.swordfish.lemuroid.app.debug.PlayStation2RuntimeProbeActivity` — local Play! ARM64 Libretro build, lawful PS2 user media.

All three are declared in the same **debug** AndroidManifest, with no second application, no package ID/signature change, no supported-system UI claims, and no core ROMs/BIOS shipped. Each uses the existing `GLRetroView` native LibretroDroid frontend and isolated app-private debug save locations. `NextGenDiscIdentity` provides content-aware signatures but is not wired to production scanner until gameplay works.

## CI proof vs device acceptance

The existing CARTDECK APK workflow compiles source and executes unit tests. Native Flycast, Dolphin and Play! core workflows verify reproducible ARM64 ELF output but intentionally do not package their binaries. The debug-only runner scripts require a connected Android device, a locally compiled/licence-cleared core, and lawful game media. **A successful APK, compiled libretro core or frame event is NOT a playable result.**

**Still required:** device logs showing actual context negotiation, moving pixels, audio and all controls, VMU/memory-card save roundtrip, 15–20-minute thermal/performance behaviour and in-place update. On graphics-context failure capture the exact `SET_HW_RENDER` context and implement engine-specific bridge instead of changing existing players. Daniel licensing, Marcus independent QA, and Cassia acceptance must precede public support/packaging. No changes to existing GB/GBC/GBA, PS1, NDS saves, shell or Start/Select positions.
