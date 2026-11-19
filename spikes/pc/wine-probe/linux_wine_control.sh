#!/usr/bin/env bash
# Linux control only. Real Windows PE/GDI/registry/file tests, no Windows OS image.
set -euo pipefail
out=$(realpath "$1")
mkdir -p "$out"/{fixture,prefixes,game32,game64}
here=$(cd "$(dirname "$0")" && pwd)
i686-w64-mingw32-gcc -O2 -Wall -Wextra "$here/guest.c" -o "$out/fixture/probe32.exe" -lgdi32 -luser32 -ladvapi32
x86_64-w64-mingw32-gcc -O2 -Wall -Wextra "$here/guest.c" -o "$out/fixture/probe64.exe" -lgdi32 -luser32 -ladvapi32
export WINEDEBUG=-all
wine --version > "$out/wine-version.txt"
dpkg-query -W -f='${Package}:${Architecture}\t${Version}\n' 'wine*' 'libwine*' > "$out/package-versions.txt"
for bits in 32 64; do
    export WINEARCH=win64 WINEPREFIX="$out/prefixes/$bits"
    game="$out/game$bits"
    cp "$out/fixture/probe$bits.exe" "$game/"
    timeout 180 wineboot -u > "$out/boot$bits.log" 2>&1
    for n in 1 2; do
        (
            cd "$game"
            timeout 120 wine "./probe$bits.exe"
        ) > "$out/run-$bits-$n.log" 2>&1
        grep -F "PC_R06_WINDOWS_OK bits=$bits counter=$n" "$out/run-$bits-$n.log"
        test "$(cat "$game/progress.txt")" = "$n"
        timeout 60 wineserver -k
        timeout 60 wineserver -w
    done
done
test "$(cat "$out/game32/progress.txt")" = 2
test "$(cat "$out/game64/progress.txt")" = 2
# Prefixes include only synthetic data, never publish the actual Wine prefix or runtime binaries.
sha256sum "$out/fixture/"*.exe > "$out/fixture-sha256.txt"
printf 'linux_x86_64_wine=PASS\nwin32=PASS\nwin64=PASS\ngdi_registry_file_persistence=PASS\nandroid=NOT_TESTED\nbox64_combined=NOT_TESTED\n' > "$out/result.txt"
