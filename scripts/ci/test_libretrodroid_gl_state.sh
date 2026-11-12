#!/usr/bin/env bash
# Exercise the actual state guard and vertex upload against a deterministic fake GL driver.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SOURCE=${LIBRETRODROID_SOURCE_DIR:-"$ROOT/work/libretrodroid-host/source"}
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME for official GLES/EGL declarations}"
WORK=${HOST_GL_TEST_DIR:-"$ROOT/work/libretrodroid-gl-test"}
mkdir -p "$WORK/include"
for directory in GLES3 EGL KHR; do
    cp -R "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/$directory" "$WORK/include/"
done
"${CXX:-c++}" -std=c++17 -Wall -Wextra -Werror \
    -I"$WORK/include" -I"$SOURCE/libretrodroid/src/main/cpp" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_gl_state_test.cpp" -o "$WORK/gl-state-test"
"$WORK/gl-state-test"
echo 'LibretroDroid GLES2/GLES3 state handoff: PASS'
