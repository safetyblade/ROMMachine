#!/usr/bin/env bash
# ARM64 Bionic Wine + PE WoW64 translator. Builds components, not another Android app.
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
work=$(realpath "$1")
ndk="${ANDROID_HOME:?}/ndk/29.0.14206865"
toolchain="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin"
wine_rev=b073859675060c9211fcbccfd90e4e87520dc2c2
box_rev=2f130fab1d6e1a4ee8a71dc60cfdfcc839ad192a
ft_rev=42608f77f20749dd6ddc9e0536788eaad70ea4b5
mkdir -p "$work"/{logs,source-evidence,delivery}
fetch() {
    git clone --depth 1 --branch "$2" "$1" "$3"
    test "$(git -C "$3" rev-parse HEAD)" = "$4"
}
fetch https://github.com/wine-mirror/wine.git wine-10.0 "$work/wine" "$wine_rev"
fetch https://github.com/ptitSeb/box64.git v0.4.4 "$work/box64" "$box_rev"
fetch https://github.com/freetype/freetype.git VER-2-13-3 "$work/freetype" "$ft_rev"
curl -fL --retry 3 -o "$work/mingw.tar.xz" https://github.com/mstorsjo/llvm-mingw/releases/download/20261117/llvm-mingw-20261117-ucrt-ubuntu-22.04-x86_64.tar.xz
echo "e7a15a49a2ea95e9f3d8a9d0cfabe5052a4b10fb4777cc54de5a8c27b64bb701  $work/mingw.tar.xz" | sha256sum -c -
mkdir "$work/mingw"
tar -xJf "$work/mingw.tar.xz" --strip-components=1 -C "$work/mingw"
export PATH="$work/mingw/bin:$toolchain:$PATH"
# Build native host tools required by the cross configure.
mkdir "$work/host-tools"
(
    cd "$work/host-tools"
    "$work/wine/configure" --disable-tests --without-x --without-wayland --without-freetype --without-mingw > "$work/logs/host-configure.log" 2>&1
    make -j4 __tooldeps__ > "$work/logs/host-tools.log" 2>&1
)
python3 "$root/spikes/pc/wine-android/patch_native_wine.py" "$work/wine" "$work/delivery/java"
cmake -S "$work/freetype" -B "$work/ft-build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
    -DFT_DISABLE_ZLIB=TRUE -DFT_DISABLE_BZIP2=TRUE -DFT_DISABLE_PNG=TRUE \
    -DFT_DISABLE_HARFBUZZ=TRUE -DFT_DISABLE_BROTLI=TRUE \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
    -DCMAKE_INSTALL_PREFIX="$work/ft-install"
cmake --build "$work/ft-build" --parallel 4 > "$work/logs/freetype-build.log" 2>&1
cmake --install "$work/ft-build"
mkdir "$work/android-build"
(
    cd "$work/android-build"
    export CC="$toolchain/aarch64-linux-android28-clang"
    export CXX="$toolchain/aarch64-linux-android28-clang++"
    export AR="$toolchain/llvm-ar" RANLIB="$toolchain/llvm-ranlib" STRIP="$toolchain/llvm-strip"
    export aarch64_CC=aarch64-w64-mingw32-clang i386_CC=i686-w64-mingw32-clang
    export LDFLAGS="-Wl,-z,max-page-size=16384"
    export PKG_CONFIG_LIBDIR="$work/ft-install/lib/pkgconfig"
    export FREETYPE_CFLAGS="-I$work/ft-install/include/freetype2" FREETYPE_LIBS="-L$work/ft-install/lib -lfreetype"
    "$work/wine/configure" --host=aarch64-linux-android --build=x86_64-pc-linux-gnu \
        --with-wine-tools="$work/host-tools" --prefix=/opt/cartdeck-wine \
        --enable-archs=aarch64,i386 --disable-tests --without-x --without-wayland \
        > "$work/logs/android-configure.log" 2>&1 || { tail -80 "$work/logs/android-configure.log"; exit 1; }
    make -j4 > "$work/logs/android-build.log" 2>&1 || { tail -80 "$work/logs/android-build.log"; exit 1; }
    make install DESTDIR="$work/install" > "$work/logs/android-install.log" 2>&1 || { tail -80 "$work/logs/android-install.log"; exit 1; }
)
# Box64's built-in WoW64 PE target uses native ARM64 Wine, not the Linux Box64 executable.
# A Linux ARM-target top-level configure supplies the source lists; PE build remains cross-compiled.
cmake -S "$work/box64" -B "$work/box-build" -G Ninja \
    -DARM64=ON -DARM_DYNAREC=ON -DWOW64=ON \
    -DCMAKE_BUILD_TYPE=RelWithDebInfo -DNOLOADADDR=ON
cmake --build "$work/box-build" --target wowbox64 --parallel 3 > "$work/logs/wowbox64-build.log" 2>&1 || { tail -80 "$work/logs/wowbox64-build.log"; exit 1; }
mapfile -t dlls < <(find "$work/box-build" -type f -name wowbox64.dll)
test "${#dlls[@]}" = 1
runtime="$work/install/opt/cartdeck-wine"
test -f "$runtime/arm64-v8a/lib/wine/aarch64-unix/ntdll.so"
install -m 644 "${dlls[0]}" "$runtime/arm64-v8a/lib/wine/aarch64-windows/wowbox64.dll"
mkdir -p "$runtime/arm64-v8a/lib"
cp -L "$work/ft-install/lib/libfreetype.so.6" "$runtime/arm64-v8a/lib/libfreetype.so.6"
i686-w64-mingw32-clang -O2 "$root/spikes/pc/wine-probe/guest.c" -o "$work/delivery/probe32.exe" -lgdi32 -luser32 -ladvapi32
python3 "$root/spikes/pc/wine-android/package_runtime.py" "$runtime" "$work/delivery" "$toolchain/llvm-readelf"
for spec in "wine:$wine_rev" "box64:$box_rev" "freetype:$ft_rev"; do
    name="${spec%:*}"; rev="${spec#*:}"
    git -C "$work/$name" archive --format=tar.gz --output="$work/source-evidence/$name-source.tar.gz" "$rev"
    git -C "$work/$name" diff --binary > "$work/source-evidence/$name-applied.patch"
done
cp "$work/wine/COPYING.LIB" "$work/source-evidence/WINE-COPYING.LIB"
cp "$work/box64/LICENSE" "$work/source-evidence/BOX64-LICENSE"
cp "$work/freetype/docs/FTL.TXT" "$work/source-evidence/FREETYPE-LICENSE"
cp "$work/logs/android-configure.log" "$work/source-evidence/"
cp "$root/spikes/pc/wine-android/"*.py "$root/spikes/pc/wine-android/build_native_wine.sh" "$work/source-evidence/"
(
    cd "$work/source-evidence"
    sha256sum * > SHA256SUMS
)
printf 'wine=%s\nbox64=%s\nfreetype=%s\nndk=29.0.14206865\nandroid_execution=NOT_TESTED\nproduction_eligible=false\n' "$wine_rev" "$box_rev" "$ft_rev" > "$work/delivery/provenance.txt"
