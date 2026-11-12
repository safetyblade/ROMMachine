#!/usr/bin/env bash
# Source-level event semantics; this does not certify Android presentation/gameplay.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SOURCE=${LIBRETRODROID_SOURCE_DIR:-"$ROOT/work/libretrodroid-host/source"}
WORK=${HOST_FRAME_TEST_DIR:-"$ROOT/work/libretrodroid-frame-test"}
mkdir -p "$WORK"
"${CXX:-c++}" -std=c++17 -Wall -Wextra -pthread \
    -I"$SOURCE/libretrodroid/src/main/cpp" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_core_frame_signal_test.cpp" \
    -o "$WORK/core-frame-test"
"$WORK/core-frame-test"
echo 'LibretroDroid core-frame callback contract: PASS'
