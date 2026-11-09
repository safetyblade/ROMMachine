# GC-01 Dolphin Android experimental runtime

This is **executable implementation infrastructure**, not user-facing GameCube support. Issue #83.

- Pinned Libretro Dolphin: `32c61f9d0e933331bb7087ba57e96883241b4960`. CI clones and **compiles** Android ARM64, validates libretro exports and 16 KiB PT_LOAD alignment and prints binary SHA-256. The binary is not uploaded or shipped.
- Debug-only `GameCubeRuntimeProbeActivity` launches a local Dolphin library via CARTDECK's real `GLRetroView`, local disc and isolated persistent `files/gamecube-lab/saves` path. Raw GCM/ISO requires the authenticated GameCube disc signature at offset 0x1C. Wii and generic ISOs are excluded.
- Private physical phone driver: `bash scripts/ci/gc01_device_probe.sh DEBUG_APK DOLPHIN_LIBRETRO_SO LAWFUL_GAME_FOLDER DISC_FILE DEBUG_PACKAGE`.
- On-device actual moving pixels, audio, right analog, analog triggers, Dolphin Sys assets, memory card, safe background/exit, 16 KiB device and thermal tests remain **NOT TESTED**. The first frame callback does **not** prove playable emulation.

Do not add SystemID, core downloader, Settings supported systems or signed/public binary before Marcus validates normal-game flows and Daniel clears distribution; this PR preserves current player saves/UI.