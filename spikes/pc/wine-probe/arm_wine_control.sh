#!/usr/bin/env bash
# An actual ARM64 Linux host, not Android and not a simulated success.
set -euo pipefail
test "$(uname -m)" = aarch64
work=$(realpath "$1")
wine=$(realpath "$2")
fixture=$(realpath "$3")
rev=2f130fab1d6e1a4ee8a71dc60cfdfcc839ad192a
mkdir -p "$work"
git clone --no-checkout https://github.com/ptitSeb/box64.git "$work/box64"
git -C "$work/box64" checkout --detach "$rev"
test "$(git -C "$work/box64" rev-parse HEAD)" = "$rev"
# Linux uses Box64's standard fixed-address layout; Android's PIE build remains separate.
cmake -S "$work/box64" -B "$work/build" -G Ninja -DARM64=ON -DARM_DYNAREC=ON -DCMAKE_BUILD_TYPE=RelWithDebInfo
cmake --build "$work/build" --target box64 --parallel 3
runner="$work/build/box64"
export PATH="$work/build:$wine/bin:$PATH"
unix="$wine/lib/wine/x86_64-unix"
test -s "$unix/ntdll.so"
test -x "$wine/bin/wine"
test -x "$wine/bin/wineserver"
{
    file "$wine/bin/wine" "$unix/ntdll.so" "$wine/bin/wineserver"
    readelf -h -d -V "$unix/ntdll.so"
    readelf -d "$wine/bin/wine"
} > "$work/loader-elf.log"
export WINEDLLPATH="$wine/lib/wine"
export BOX64_PATH="$wine/bin" BOX64_LD_LIBRARY_PATH="$unix:$wine/lib"
# ntdll is a guest Unix ELF, never a host ARM library; preserve the exact Wine bytes.
export BOX64_EMULATED_LIBS=ntdll.so
export BOX64_LOG=2 BOX64_DLSYM_ERROR=1
export WINELOADER="$wine/bin/wine" WINESERVER="$wine/bin/wineserver"
export WINEDEBUG=-all WINEARCH=win64 BOX64_DYNAREC=1
# Preserve the loader trace on failure instead of suppressing the reason for dlopen failure.
timeout 45 "$runner" "$wine/bin/wine" --version > "$work/loader-version.log" 2>&1 || { tail -120 "$work/loader-version.log"; exit 1; }
for bits in 32 64; do
    export WINEPREFIX="$work/prefix-$bits"
    game="$work/game$bits"
    mkdir "$game"
    cp "$fixture/probe$bits.exe" "$game/"
    timeout 180 "$runner" "$wine/bin/wine" wineboot -u > "$work/boot-$bits.log" 2>&1 || { tail -120 "$work/boot-$bits.log"; exit 1; }
    for n in 1 2; do
        (
            cd "$game"
            timeout 120 "$runner" "$wine/bin/wine" "./probe$bits.exe"
        ) > "$work/run-$bits-$n.log" 2>&1 || { tail -120 "$work/run-$bits-$n.log"; exit 1; }
        grep -F "PC_R06_WINDOWS_OK bits=$bits counter=$n" "$work/run-$bits-$n.log"
        test "$(cat "$game/progress.txt")" = "$n"
        timeout 60 "$runner" "$wine/bin/wineserver" -k
        timeout 60 "$runner" "$wine/bin/wineserver" -w
    done
done
printf 'linux_arm64_box64_wow64=PASS\nwin32=PASS\nwin64=PASS\nregistry_file_gdi=PASS\nandroid=NOT_TESTED\n' > "$work/result.txt"
