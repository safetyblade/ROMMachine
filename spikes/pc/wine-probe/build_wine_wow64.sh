#!/usr/bin/env bash
# Build source-pinned x86_64 Wine with the new WoW64 architecture on Linux.
# This is NOT a Bionic runtime or a distributable CARTDECK component.
set -euo pipefail
work=$(realpath "$1")
rev=b073859675060c9211fcbccfd90e4e87520dc2c2
mkdir -p "$work"
test ! -e "$work/source"
git clone --depth 1 --branch wine-10.0 https://github.com/wine-mirror/wine.git "$work/source"
test "$(git -C "$work/source" rev-parse HEAD)" = "$rev"
mkdir -p "$work/build" "$work/stage" "$work/evidence"
cd "$work/build"
"$work/source/configure" --prefix=/opt/cartdeck-wine --enable-archs=i386,x86_64 --disable-tests 2>&1 | tee "$work/configure.log"
grep -q 'HAVE_X11' include/config.h
make -j4 > "$work/build.log" 2>&1 || { tail -80 "$work/build.log"; exit 1; }
make install DESTDIR="$work/stage" > "$work/install.log" 2>&1 || { tail -80 "$work/install.log"; exit 1; }
prefix="$work/stage/opt/cartdeck-wine"
test -x "$prefix/bin/wine"
test -x "$prefix/bin/wineserver"
find "$prefix" -name 'wow64.dll' -print -quit | grep -q .
find "$prefix" -path '*/i386-windows/ntdll.dll' -print -quit | grep -q .
find "$prefix" -path '*/x86_64-windows/ntdll.dll' -print -quit | grep -q .
mkdir -p "$work/evidence"
cp "$work/source/COPYING.LIB" "$work/evidence/"
dpkg-query -W -f='${Package}:${Architecture}\t${Version}\n' > "$work/evidence/build-packages.tsv"
git -C "$work/source" archive --format=tar.gz --output="$work/evidence/wine-source.tar.gz" "$rev"
tar -C "$prefix" -czf "$work/evidence/wine-wow64-linux-x86_64.tar.gz" .
(
    cd "$work/evidence"
    printf 'revision=%s\nsource=https://github.com/wine-mirror/wine.git\nconfigure=--enable-archs=i386,x86_64 --disable-tests\nandroid_execution=NOT_TESTED\nproduction_eligible=false\n' "$rev" > provenance.txt
    sha256sum wine-source.tar.gz wine-wow64-linux-x86_64.tar.gz > sha256.txt
)
