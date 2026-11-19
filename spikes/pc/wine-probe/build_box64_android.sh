#!/usr/bin/env bash
# Independent Android Bionic candidate. Nothing here is staged into CARTDECK production.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
work=${PC_WINE_WORK:?Set fresh PC_WINE_WORK}
sdk=${ANDROID_HOME:?Android SDK required}
ndk="$sdk/ndk/29.0.14206865"
rev=2f130fab1d6e1a4ee8a71dc60cfdfcc839ad192a
mkdir -p "$work"
test ! -e "$work/source"
git clone --no-checkout https://github.com/ptitSeb/box64.git "$work/source"
git -C "$work/source" checkout --detach "$rev"
test "$(git -C "$work/source" rev-parse HEAD)" = "$rev"
cmake -S "$work/source" -B "$work/build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
    -DANDROID=ON -DARM64=ON -DARM_DYNAREC=ON -DNOLOADADDR=ON \
    -DCMAKE_BUILD_TYPE=RelWithDebInfo -DCMAKE_EXE_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
    -DNO_LIB_INSTALL=ON
cmake --build "$work/build" --target box64 --parallel 3
mkdir -p "$work/evidence"
cp "$work/build/box64" "$work/evidence/libcartdeck_box64.so"
cp "$work/source/LICENSE" "$work/evidence/BOX64-LICENSE"
cp "$work/build/CMakeCache.txt" "$work/evidence/CMakeCache.txt"
readelf="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
"$readelf" -h -lW -d "$work/evidence/libcartdeck_box64.so" > "$work/evidence/elf.txt"
python3 "$root/spikes/pc/wine-probe/verify_translator.py" "$work/evidence/elf.txt"
git -C "$work/source" archive --format=tar.gz --output="$work/evidence/box64-source.tar.gz" "$rev"
{
    printf 'source=https://github.com/ptitSeb/box64\nrevision=%s\n' "$rev"
    printf 'abi=arm64-v8a\napi=28\nndk=29.0.14206865\nandroid_execution=NOT_TESTED\nwine_execution=NOT_TESTED\nproduction_eligible=false\n'
    sha256sum "$work/evidence/libcartdeck_box64.so" "$work/evidence/box64-source.tar.gz" "$0"
} > "$work/evidence/provenance.txt"
