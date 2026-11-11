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
if [[ -n "${CARTDECK_PSP_SOURCE:-}" ]]; then
    source="$(cd "$CARTDECK_PSP_SOURCE" && pwd)"
else
    source="$work/ppsspp"
    git init --quiet "$source"
    git -C "$source" remote add origin "$SOURCE_URL"
    git -C "$source" fetch --quiet --depth 1 origin "$SOURCE_SHA"
    git -C "$source" checkout --quiet --detach FETCH_HEAD
    git -C "$source" submodule update --init --recursive --depth 1 --jobs 2
fi
test "$(git -C "$source" rev-parse HEAD)" = "$SOURCE_SHA"
git -C "$source" diff --exit-code --quiet
git -C "$source" submodule foreach --recursive 'git diff --exit-code --quiet'
if git -C "$source" submodule status --recursive | grep -qE '^[-+U]'; then
    echo 'PPSSPP submodules must match the pinned source revision' >&2
    exit 1
fi

(
    cd "$source/libretro/jni"
    "$ANDROID_NDK_HOME/ndk-build" -j2 \
        NDK_TOOLCHAIN_VERSION=clang APP_ABI=arm64-v8a APP_PLATFORM=android-24 APP_CPPFLAGS="-frtti -fexceptions" \
        APP_CFLAGS="-DPNG_ARM_NEON_OPT=0" \
        APP_LDFLAGS="-Wl,-z,max-page-size=16384"
)
core="$source/libretro/libs/arm64-v8a/libretro.so"
test -s "$core"
cp "$core" "$out/libppsspp_modern_libretro_android.so"
"$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-debug "$out/libppsspp_modern_libretro_android.so"

# A matching, immutable asset archive is built from the EXACT SAME source commit.
test -s "$source/assets/compat.ini"
python3 - "$source/assets" "$out/ppsspp-modern-assets.zip" <<'PY'
import os
import sys
import zipfile

source, destination = sys.argv[1:3]
with zipfile.ZipFile(destination, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
    for root, directories, filenames in os.walk(source):
        directories[:] = sorted(name for name in directories if name != ".git")
        for filename in sorted(filenames):
            if filename == ".git":
                continue
            path = os.path.join(root, filename)
            if os.path.islink(path):
                raise ValueError('Symlinks are forbidden in CARTDECK asset archives')
            entry = zipfile.ZipInfo(os.path.relpath(path, source), (1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o100644 << 16
            with open(path, 'rb') as asset:
                archive.writestr(entry, asset.read())
PY
cp "$source/assets/compat.ini" "$out/compat.ini"
python3 - "$out" "$SOURCE_SHA" <<'PYMANIFEST'
import hashlib
import pathlib
import sys
root = pathlib.Path(sys.argv[1])
def digest(name):
    return hashlib.sha256((root / name).read_bytes()).hexdigest()
(root / 'ppsspp-modern.properties').write_text(
    'revision=' + sys.argv[2] + '\nandroid_api=24\n' +
    'core.sha256=' + digest('libppsspp_modern_libretro_android.so') + '\n' +
    'assets.sha256=' + digest('ppsspp-modern-assets.zip') + '\n' +
    'compat.sha256=' + digest('compat.ini') + '\n')
PYMANIFEST
cp "$source/LICENSE.TXT" "$out/PPSSPP_UPSTREAM_LICENSE" 2>/dev/null || true

{
    echo "upstream=$SOURCE_URL"
    echo "commit=$SOURCE_SHA"
    echo "release=v1.20.4"
    echo "core_file=libppsspp_modern_libretro_android.so"
    echo "assets=ppsspp-modern-assets.zip"
    echo "ffmpeg_dependency=pinned upstream Android static archives; not rebuilt by this command"
    echo "ndk_revision=$(sed -n 's/^Pkg.Revision = //p' "$ANDROID_NDK_HOME/source.properties")"
    echo "ndk=$("$ANDROID_NDK_HOME/ndk-build" --version 2>/dev/null | head -1 || true)"
} > "$out/provenance.txt"
git -C "$source" submodule status --recursive > "$out/submodule-revisions.txt"
(cd "$source/ffmpeg/android/arm64/lib" && sha256sum ./*.a) > "$out/ffmpeg-static-dependencies.sha256"
cp "$source/ffmpeg/COPYING.LGPLv2.1" "$out/FFMPEG_LGPLv2.1_LICENSE"
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
