#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SOURCE=${LIBRETRODROID_SOURCE_DIR:-"$ROOT/work/libretrodroid-host/source"}
WORK=${HOST_INPUT_TEST_DIR:-"$ROOT/work/libretrodroid-input-test"}
mkdir -p "$WORK"
"${CXX:-c++}" -std=c++17 -Wall -Wextra -Werror \
    -I"$SOURCE/libretrodroid/src/main/cpp" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_graphics_error_test.cpp" \
    -o "$WORK/graphics-error-test"
"$WORK/graphics-error-test"
echo 'LibretroDroid graphics failure classification: PASS'
python3 - "$SOURCE/libretrodroid/src/main/cpp/libretrodroidjni.cpp" "$WORK/jni_surface_boundary.inc" <<'PY'
import pathlib
import sys
source = pathlib.Path(sys.argv[1]).read_text()
start = source.index('JNIEXPORT void JNICALL Java_com_swordfish_libretrodroid_LibretroDroid_onSurfaceCreated(')
opening = source.index('{', start)
depth = 1
end = opening + 1
while depth:
    if source[end] == '{':
        depth += 1
    elif source[end] == '}':
        depth -= 1
    end += 1
pathlib.Path(sys.argv[2]).write_text(source[start:end] + '\n')
PY
CPP="$SOURCE/libretrodroid/src/main/cpp"
"${CXX:-c++}" -std=c++17 -Wall -Wextra -Werror -Wno-unused-parameter \
    -include stdexcept -I"$CPP" -I"$WORK" \
    "$ROOT/scripts/ci/fixtures/libretrodroid_surface_error_test.cpp" \
    "$CPP/utils/libretrodroidexception.cpp" "$CPP/errorcodes.cpp" \
    -o "$WORK/surface-error-test"
"$WORK/surface-error-test"
echo 'LibretroDroid actual JNI surface exception boundary: PASS'
