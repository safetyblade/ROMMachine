#!/usr/bin/env bash
# Build a second, isolated ARM64 PCSX ReARMed Libretro core for CARTDECK.
# This does not replace the approved native binary or promote a default.
set -euo pipefail

SOURCE_URL="https://github.com/libretro/pcsx_rearmed.git"
SOURCE_SHA="7b0e8f8fadfbce3d170d195b9d9eecaabfa08c8e"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to a pinned Android NDK}"
out="${1:?Output directory required}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
git clone --quiet "$SOURCE_URL" "$work/pcsx_rearmed"
git -C "$work/pcsx_rearmed" checkout --quiet --detach "$SOURCE_SHA"
git -C "$work/pcsx_rearmed" submodule update --init --recursive --jobs 2
test "$(git -C "$work/pcsx_rearmed" rev-parse HEAD)" = "$SOURCE_SHA"

toolchain="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
test -x "$toolchain/aarch64-linux-android24-clang"
export CC="$toolchain/aarch64-linux-android24-clang"
export CXX="$toolchain/aarch64-linux-android24-clang++"
export AR="$toolchain/llvm-ar"
export AS="$CC"
export LD="$toolchain/ld.lld"
export LDFLAGS="-Wl,-z,max-page-size=16384"

make -C "$work/pcsx_rearmed" -f Makefile.libretro \
    platform=unix ARCH=arm64 DYNAREC=ari64 BUILTIN_GPU=neon LIBPTHREAD= \
    -j2
core="$work/pcsx_rearmed/pcsx_rearmed_libretro.so"
test -s "$core"
cp "$core" "$out/libpcsx_rearmed_modern_libretro_android.so"
cp "$work/pcsx_rearmed/COPYING" "$out/PCSX_UPSTREAM_COPYING" 2>/dev/null || true
{
    echo "upstream=$SOURCE_URL"
    echo "commit=$SOURCE_SHA"
    echo "core_file=libpcsx_rearmed_modern_libretro_android.so"
    echo "ndk=$("$ANDROID_NDK_HOME/ndk-build" --version 2>/dev/null | head -1 || true)"
    echo "toolchain=$("$CC" --version | head -1)"
} > "$out/provenance.txt"
sha256sum "$out/libpcsx_rearmed_modern_libretro_android.so" > "$out/sha256.txt"
file "$out/libpcsx_rearmed_modern_libretro_android.so" > "$out/elf-file.txt"
readelf -lW "$out/libpcsx_rearmed_modern_libretro_android.so" > "$out/elf-program-headers.txt"
readelf -dW "$out/libpcsx_rearmed_modern_libretro_android.so" > "$out/elf-dependencies.txt"
python3 - "$out/libpcsx_rearmed_modern_libretro_android.so" <<'PY'
import struct
import sys

data = open(sys.argv[1], 'rb').read()
assert data[:4] == b'\x7fELF' and data[4] == 2 and data[5] == 1, 'Expected little-endian ELF64'
assert struct.unpack_from('<H', data, 18)[0] == 183, 'Expected AArch64'
phoff = struct.unpack_from('<Q', data, 32)[0]
phentsize = struct.unpack_from('<H', data, 54)[0]
phnum = struct.unpack_from('<H', data, 56)[0]
loads = []
for index in range(phnum):
    offset = phoff + index * phentsize
    if struct.unpack_from('<I', data, offset)[0] == 1:
        loads.append(struct.unpack_from('<Q', data, offset + 48)[0])
assert loads and all(align >= 16384 for align in loads), 'ELF PT_LOAD is not 16 KB aligned'
print('ARM64 and 16 KB ELF PT_LOAD alignment: PASS', loads)
PY
