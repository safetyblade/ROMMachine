#!/usr/bin/env bash
# Exercise the real patched Input.cpp on the build host; no Android runtime claim.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SOURCE=${LIBRETRODROID_SOURCE_DIR:-"$ROOT/work/libretrodroid-host/source"}
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME for official Android keycodes}"
WORK=${HOST_INPUT_TEST_DIR:-"$ROOT/work/libretrodroid-input-test"}
mkdir -p "$WORK/include/android"
# Input.cpp only uses the two stable key-action values from android/input.h.
printf '#pragma once\n#define AKEY_EVENT_ACTION_DOWN 0\n#define AKEY_EVENT_ACTION_UP 1\n' > "$WORK/include/android/input.h"
ln -sf "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/android/keycodes.h" "$WORK/include/android/keycodes.h"
CPP="$SOURCE/libretrodroid/src/main/cpp"
"${CXX:-c++}" -std=c++17 -Wall -Wextra \
    -I"$WORK/include" -I"$CPP" -I"$CPP/libretro/libretro-common/include" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_input_test.cpp" "$CPP/input.cpp" \
    -o "$WORK/input-test"
"$WORK/input-test"
echo 'LibretroDroid input contract: PASS'

# VideoLayout only uses debug logging; Android logging is unnecessary on the
# native test host. Compile the actual geometry and rectangle implementations.
printf '#pragma once\n' > "$WORK/include/android/log.h"
"${CXX:-c++}" -std=c++17 -Wall -Wextra \
    -I"$WORK/include" -I"$CPP" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_pointer_test.cpp" \
    "$CPP/videolayout.cpp" "$CPP/utils/rect.cpp" \
    -o "$WORK/pointer-test"
"$WORK/pointer-test"

LIBRETRODROID_SOURCE_DIR="$SOURCE" bash "$ROOT/scripts/ci/test_libretrodroid_gl_state.sh"

LIBRETRODROID_SOURCE_DIR="$SOURCE" bash "$ROOT/scripts/ci/test_libretrodroid_graphics_error.sh"

LIBRETRODROID_SOURCE_DIR="$SOURCE" bash "$ROOT/scripts/ci/test_libretrodroid_core_frame_signal.sh"
