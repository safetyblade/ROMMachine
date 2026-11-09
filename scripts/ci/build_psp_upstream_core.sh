#!/usr/bin/env bash
# Build an official, version-matched PPSSPP Libretro candidate and its support assets.
# Keep both artifacts separate from the known-working CARTDECK engine and assets.
set -euo pipefail

SOURCE_URL="https://github.com/hrydgard/ppsspp.git"
SOURCE_SHA="fa50bb1976065c4f8b1b47af227d367fe9771555"  # annotated v1.20.4 target
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the pinned Android NDK}"
out="${1:?Output directory required}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git clone --quiet "$SOURCE_URL" "$work/ppsspp"
git -C "$work/ppsspp" checkout --quiet --detach "$SOURCE_SHA"
git -C "$work/ppsspp" submodule update --init --recursive --jobs 2
test "$(git -C "$work/ppsspp" rev-parse HEAD)" = "$SOURCE_SHA"

(
    cd "$work/ppsspp/libretro/jni"
    "$ANDROID_NDK_HOME/ndk-build" -j2 \
        APP_ABI=arm64-v8a APP_PLATFORM=android-24 APP_CPPFLAGS="-frtti -fexceptions" \
        APP_CFLAGS="-DPNG_ARM_NEON_OPT=0" \
        APP_LDFLAGS="-Wl,-z,max-page-size=16384"
)
core="$work/ppsspp/libretro/libs/arm64-v8a/libretro.so"
test -s "$core"
cp "$core" "$out/libppsspp_modern_libretro_android.so"

# A matching, immutable asset archive is built from the EXACT SAME source commit.
test -s "$work/ppsspp/assets/compat.ini"
python3 - "$work/ppsspp/assets" "$out/ppsspp-modern-assets.zip" <<'PY'
import os
import sys
import zipfile

source, destination = sys.argv[1:3]
with zipfile.ZipFile(destination, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
    for root, directories, filenames in os.walk(source):
        directories.sort()
        for filename in sorted(filenames):
            path = os.path.join(root, filename)
            if os.path.islink(path):
                raise ValueError('Symlinks are forbidden in CARTDECK asset archives')
            archive.write(path, os.path.relpath(path, source))
PY
cp "$work/ppsspp/assets/compat.ini" "$out/compat.ini"
cp "$work/ppsspp/LICENSE.TXT" "$out/PPSSPP_UPSTREAM_LICENSE" 2>/dev/null || true

{
    echo "upstream=$SOURCE_URL"
    echo "commit=$SOURCE_SHA"
    echo "release=v1.20.4"
    echo "core_file=libppsspp_modern_libretro_android.so"
    echo "assets=ppsspp-modern-assets.zip"
    echo "ndk=$("$ANDROID_NDK_HOME/ndk-build" --version 2>/dev/null | head -1 || true)"
} > "$out/provenance.txt"
sha256sum "$out/libppsspp_modern_libretro_android.so" "$out/ppsspp-modern-assets.zip" "$out/compat.ini" > "$out/sha256.txt"
file "$out/libppsspp_modern_libretro_android.so" > "$out/elf-file.txt"
readelf -lW "$out/libppsspp_modern_libretro_android.so" > "$out/elf-program-headers.txt"
readelf -dW "$out/libppsspp_modern_libretro_android.so" > "$out/elf-dependencies.txt"
python3 - "$out/libppsspp_modern_libretro_android.so" <<'PY'
import struct
import sys

data = open(sys.argv[1], 'rb').read()
assert data[:4] == b'\x7fELF' and data[4] == 2 and data[5] == 1, 'Expected little-endian ELF64'
assert struct.unpack_from('<H', data, 18)[0] == 183, 'Expected AArch64'
offset = struct.unpack_from('<Q', data, 32)[0]
size = struct.unpack_from('<H', data, 54)[0]
count = struct.unpack_from('<H', data, 56)[0]
alignments = [struct.unpack_from('<Q', data, offset + n * size + 48)[0]
              for n in range(count)
              if struct.unpack_from('<I', data, offset + n * size)[0] == 1]
assert alignments and all(alignment >= 16384 for alignment in alignments), '16 KB ELF alignment failed'
print('PPSSPP ARM64 and ELF 16 KB alignment PASS', alignments)
PY
