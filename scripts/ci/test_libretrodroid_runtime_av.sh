#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
git -C "$temporary" init -q
git -C "$temporary" apply --include='libretrodroid/src/main/cpp/runtimeavtiming.h' \
    "$ROOT/scripts/ci/patches/libretrodroid-runtime-av-timing.patch"
c++ -std=c++17 -pthread -Wall -Wextra -Werror \
    -I "$temporary/libretrodroid/src/main/cpp" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_runtime_av_timing_test.cpp" \
    -o "$temporary/runtime-av-test"
"$temporary/runtime-av-test"
