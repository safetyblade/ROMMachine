# Wii / original Xbox independent QA

This record distinguishes source inspection and host tests from Android gameplay acceptance.

## Environment and acceptance

The managed executor has no attached Android device (`adb devices -l` is empty), no Android emulator and no `/dev/kvm`. Real Wii/Xbox game frames, game audio, physical/touch controller response, Android foreground/background recovery, clean game exit, authentic Wii NAND saves, authentic Xbox HDD saves and same-package APK upgrade save preservation are **NOT TESTED**. A signed ARM64 APK and passing host tests do not establish these outcomes. Marcus QA must remain **acceptance pending** until a recorded physical-device run supplies the evidence required by issues #85/#86.

Record each device run with APK SHA-256, signer, versionCode, model, CPU/GPU and driver, Android version/page size, engine revision, legitimate fixture identity/hash, controller profile, outcome and logs. Verify in-game saves after fully killing the app and after an in-place APK upgrade. Do not substitute a save state for NAND/HDD persistence.

## Independent hakuX host tests

Selected hakuX source: `a7cdb5a975e31730d488c47f6f10440b16c1e6f4`.

The upstream `tests/hle` suite passed **15/15** on this Linux x86-64 executor. It covers XBE/image parsing, resource layout, ring/arena/page tracking, push buffers, texture swizzle, shader translation, APU HRTF math, cheat parsing and Vulkan compatibility mappings. This is pure host logic coverage only.

The unmodified upstream strict test build first failed on `HlePbSink.run_map` missing-field initializer warnings and absent Linux `libm` linkage for `test_apu_hrtf`. It passed after these configuration-only portability overrides (no emulator source was changed):

```sh
cmake -S "$HAKUX_SOURCE/tests/hle" -B "$TEST_OUTPUT" -G Ninja \
  -DNC_DEPS="$HAKUX_DEPENDENCIES" \
  -DCMAKE_C_FLAGS=-Wno-error=missing-field-initializers \
  -DCMAKE_C_STANDARD_LIBRARIES=-lm
cmake --build "$TEST_OUTPUT" -j2
ctest --test-dir "$TEST_OUTPUT" --output-on-failure
```

Source/dependency deviations and licence obligations are tracked in `wii-xbox-source-obligations.md`. Android build/package evidence and the independent final code review must be appended after the combined implementation is available.

## Review findings sent to implementation

These findings were identified while the implementation was still being assembled. They are not acceptance failures until the final commit is reviewed; final verification must record whether each is closed:

- Xbox's empty Libretro core list must bypass core-selection/download code. The supported-games guide must detect the hakuX/SDL3 backend separately rather than falsely showing an ARM64 device as unsupported.
- SDL's Java activity can return early after a library-load failure. The adapter must not call JNI unconditionally afterward, or a readable setup failure becomes `UnsatisfiedLinkError`.
- Xbox must return the existing game-result contract to CARTDECK. A plain `startActivity` bypasses `handleGameFinish`, leaves save-sync/cache work cancelled and fails to update Continue/last-played metadata. Forward that result through an internal XBE relaunch.
- The native process must actually own the game lock before accessing HDD data. The inherited `GameProcessLock.acquire` silently accepts a failed `tryLock`, so a separate ownership check or throwing acquisition is needed to prevent concurrent runs after a launch race.
- The native ABI verifier must require every JNI entry point used by the adapter, not just a subset of pad/pause calls.

Read-only source review confirms the upstream Xbox EEPROM/console identity is stored under the configured HDD (`eeprom.bin`), so a complete HDD snapshot includes identity as well as UDATA/TDATA. Actual game-save persistence remains NOT TESTED.
