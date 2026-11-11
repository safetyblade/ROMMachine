#!/usr/bin/env bash
# Private Android ARM64 Flycast candidate. Never downloads firmware or game data.
set -euo pipefail
source_url=https://github.com/flyinghead/flycast.git
source_sha=1b7c7d63b73ea6987fb23f9334a7d597401a50f8
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to an Android NDK}"
out="${1:?Output directory required}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
# Retaining a build tree allows interrupted native builds to resume locally.
build_root="${FLYCAST_BUILD_ROOT:-$(mktemp -d)}"
if [[ -z "${FLYCAST_BUILD_ROOT:-}" ]]; then trap 'rm -rf "$build_root"' EXIT; fi
mkdir -p "$build_root"
build_root="$(cd "$build_root" && pwd)"
src="$build_root/upstream"
if [[ ! -d "$src/.git" ]]; then git clone --no-checkout "$source_url" "$src"; fi
git -C "$src" checkout --detach "$source_sha"
test "$(git -C "$src" rev-parse HEAD)" = "$source_sha"
git -C "$src" diff --quiet --ignore-submodules=all
git -C "$src" diff --cached --quiet --ignore-submodules=all
# Android libretro GLES does not use desktop GUI, SDL, Freetype, Vulkan, or Oboe.
# Avoid fetching unused platform SDKs, while retaining exact upstream gitlinks.
git -C "$src" submodule update --init --recursive --depth 1 --jobs 2 -- \
    core/deps/libchdr core/deps/rcheevos core/deps/asio core/deps/tinygettext
for dependency in libchdr rcheevos asio tinygettext; do
    if [[ "$dependency" == libchdr ]]; then
        # Upstream zlib CMake renames its tracked source header on configure.
        # Accept only that known generated rename, still checking the original bytes.
        header=deps/zlib-1.3.1/zconf.h
        actual="$src/core/deps/libchdr/$header"
        if [[ ! -f "$actual" ]]; then actual="$actual.included"; fi
        git -C "$src/core/deps/libchdr" show "HEAD:$header" | cmp - "$actual"
        git -C "$src/core/deps/libchdr" diff --quiet -- . ":(exclude)$header"
    else
        git -C "$src/core/deps/$dependency" diff --quiet
    fi
    git -C "$src/core/deps/$dependency" diff --cached --quiet
done
cmake -S "$src" -B "$build_root/build" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release -DLIBRETRO=ON \
    -DUSE_OPENMP=OFF -DUSE_VULKAN=OFF -DUSE_GLES=ON \
    -DUSE_HOST_LIBZIP=OFF -DUSE_LUA=OFF \
    -DANDROID_PLATFORM=android-28 -DANDROID_ABI=arm64-v8a \
    -DANDROID_STL=c++_static -DANDROID_WEAK_API_DEFS=ON \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"
cmake --build "$build_root/build" --target flycast_libretro --parallel "${NATIVE_BUILD_JOBS:-2}"
core="$build_root/build/flycast_libretro.so"
test -s "$core"
toolbin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
"$toolbin/llvm-readelf" -h "$core" | grep -q AArch64
"$toolbin/llvm-readelf" -lW "$core" > "$out/elf-program-headers.txt"
python3 - "$out/elf-program-headers.txt" <<'PY'
import pathlib, sys
loads = [int(line.split()[-1], 16) for line in pathlib.Path(sys.argv[1]).read_text().splitlines() if line.strip().startswith('LOAD ')]
assert loads and min(loads) >= 0x4000, 'ELF PT_LOAD alignment below 16 KiB'
print('ARM64 ELF PT_LOAD >=16 KiB: PASS', loads)
PY
"$toolbin/llvm-nm" -D --defined-only "$core" > "$out/elf-exports.txt"
for symbol in retro_init retro_deinit retro_load_game retro_unload_game retro_run retro_set_environment retro_set_video_refresh retro_set_audio_sample_batch retro_set_input_poll retro_set_input_state retro_get_system_av_info retro_serialize retro_unserialize; do
    grep -Eq "[[:space:]]${symbol}$" "$out/elf-exports.txt" || { echo "Missing export $symbol" >&2; exit 1; }
done
cp "$core" "$out/libflycast_libretro_android.so"
cp "$src/LICENSE" "$out/FLYCAST_LICENSE"
"$toolbin/llvm-readelf" -dW "$core" > "$out/elf-dependencies.txt"
git -C "$src" submodule status --recursive -- core/deps/libchdr core/deps/rcheevos core/deps/asio core/deps/tinygettext > "$out/source-submodules.txt"
{
    echo "upstream=$source_url"
    echo "commit=$source_sha"
    echo "core_file=libflycast_libretro_android.so"
    echo 'abi=arm64-v8a'
    echo 'graphics=opengles3;vulkan=off'
    echo "ndk=$(sed -n 's/Pkg.Revision = //p' "$ANDROID_NDK_HOME/source.properties")"
    echo 'runtime_status=NOT_DEVICE_TESTED'
} > "$out/provenance.txt"
(cd "$out" && sha256sum libflycast_libretro_android.so > sha256.txt)
{
    echo "revision=$source_sha"
    echo "core.file=libflycast_libretro_android.so"
    echo "core.sha256=$(cut -d ' ' -f1 "$out/sha256.txt")"
    echo 'core.abi=arm64-v8a'
    echo 'core.graphics=opengles3'
} > "$out/flycast.properties"
printf '%s\n' 'Native build and static validation passed. Android gameplay and distribution approval remain separate gates.'
