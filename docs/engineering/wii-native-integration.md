# Wii engineering implementation

Wii is a separate `wii` library system, gated by `cartdeckNativeSystems`. It uses the exact existing Dolphin modern ARM64 binary and matching Sys archive pinned at `32c61f9d0e933331bb7087ba57e96883241b4960`; no second emulator binary or external application is introduced. The bundle builder must enable `wii` only together with the verified GameCube/Dolphin package.

Content identification reads Nintendo disc magic for ISO, mapped Wii disc headers for unsplit WBFS, both RVZ header SHA-1 values and its disc type, and the existing Nintendo CISO reader. Wii and GameCube remain distinct even in a misleading folder. Unsupported/corrupt files do not acquire Wii identity by filename. WAD and split `.wbf1` are deliberately unsupported by this scanner. Normal discovery retains the existing database identity, custom covers and artwork behavior.

The ordinary CARTDECK launcher loads `DOLPHIN_MODERN` through the existing versioned provisioner, GLES/audio host and gameplay shell. The Wii cartridge badge reads `Wii`. Controller settings offer Dolphin's actual Classic Controller (1025), sideways Remote (513) and Remote + Nunchuk (769) device subclasses. Dedicated PadKit labels follow the upstream input descriptors and retain the existing Start/Select touch positions. Classic has both sticks and A/B/X/Y/L/R/ZL/ZR; sideways has A/B/1/2; Nunchuk has its left stick and C/Z. Right-stick press sends Wii Home. The CARTDECK menu remains a separate frontend button. Hardware gamepads use the same key and analog paths, including the proportional trigger protocol.

Touch pointing uses Libretro's actual pointer device. LibretroDroid maps screen touches through the rendered foreground bounds, excluding letterboxing and the surrounding shell, before Dolphin receives pointer coordinates. Wii pointer settings can instead select right-stick absolute or relative IR. Dolphin's core options retain per-system sensitivity/calibration settings. Shake buttons use Dolphin's motion-emulation inputs. Raw Android accelerometer/gyroscope and physical Wii Remote support are unavailable in this engineering slice, explicitly disclosed on the cartridge detail. Native motion passthrough is not claimed.

Persistent directories are selected by `GameLoader`, independently from core binaries:

- system resources: `<system-directory>/native/wii/engine-versions/<revision-hash>/dolphin-emu/Sys`
- persistent Dolphin User: `<saves-directory>/native/wii/User`
- Wii NAND/title data: `User/Wii` (including title-specific saves)
- virtual SD: Dolphin's configured file under the Wii User tree
- configuration and per-game profiles: `User/Config` and `User/GameSettings`
- snapshot namespace: `dolphin_modern/versions/<selected-version-id>`, with existing per-game state identity

GameCube uses `native/gc`, so its memory cards and User tree are separate. Engine resource upgrades and rollback never delete either User tree. Snapshots remain scoped to the selected immutable native engine version; actual NAND persistence must be verified separately.

Validation: three JVM tests cover Nintendo identity separation, WBFS block mapping/truncation, and RVZ type/checksum rejection. Real Android launch, video, audio, input responsiveness, in-game save/reopen and same-package upgrade persistence are **NOT TESTED** without attached ARM64 hardware and a legal game fixture. Source compilation and ELF checks do not establish gameplay acceptance.
