#!/usr/bin/env bash
# Build a private-test Play! Android core; does not register/promote PS2 support.
set -euo pipefail
source_url=https://github.com/jpd002/Play-.git
source_sha=1f27232772e484bfb79364b751b84f41f4c87481
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK 29.0.14206865}"
out="${1:?Usage: build_ps2_upstream_core.sh OUTPUT_DIRECTORY}"
root="$(cd "$(dirname "$0")/../.." && pwd)"
patches=(
    "$root/scripts/ci/patches/play-android-private-storage-gles.patch"
    "$root/scripts/ci/patches/play-java-vm-host-bridge.patch"
)
mkdir -p "$out"
out="$(cd "$out" && pwd)"
# Retain source/cache on request for incremental local builds.
work="${PLAY_BUILD_WORK_DIR:-$(mktemp -d)}"
mkdir -p "$work"
work="$(cd "$work" && pwd)"
if [[ -z "${PLAY_BUILD_WORK_DIR:-}" ]]; then trap 'rm -rf "$work"' EXIT; fi
src="$work/play"
if [[ ! -d "$src/.git" ]]; then git clone --no-checkout "$source_url" "$src"; fi
test "$(sed -n 's/^Pkg.Revision *= *//p' "$ANDROID_NDK_HOME/source.properties")" = 29.0.14206865
git -C "$src" checkout --detach "$source_sha"
test "$(git -C "$src" rev-parse HEAD)" = "$source_sha"
git -C "$src" submodule update --init --recursive --depth 1 --jobs 2
for patch_file in "${patches[@]}"; do
    if git -C "$src" apply --reverse --check "$patch_file" 2>/dev/null; then
        : # Already patched incremental source tree.
    else
        git -C "$src" apply --check "$patch_file"
        git -C "$src" apply "$patch_file"
    fi
done
# Do not build a tree containing untracked source patches without recording them.
git -C "$src" diff --binary > "$out/source.patch"
sha256sum "${patches[@]}" "$out/source.patch" > "$out/patch-sha256.txt"
build="$work/build"
# Add debug information without replacing Android toolchain or Release flags.
printf 'add_compile_options(-g)\n' > "$out/cartdeck-symbols.cmake"
sha256sum "$out/cartdeck-symbols.cmake" >> "$out/patch-sha256.txt"
cmake -S "$src" -B "$build" -G Ninja \
    -DBUILD_LIBRETRO_CORE=ON -DBUILD_PLAY=OFF -DBUILD_TESTS=OFF \
    -DCMAKE_BUILD_TYPE=Release -DANDROID_ABI=arm64-v8a \
    -DCMAKE_PROJECT_INCLUDE="$out/cartdeck-symbols.cmake" \
    -DANDROID_NDK="$ANDROID_NDK_HOME" -DANDROID_PLATFORM=android-28 \
    -DANDROID_STL=c++_static \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"
cmake --build "$build" --target play_libretro --parallel "${NATIVE_JOBS:-2}"
core="$build/Source/ui_libretro/play_libretro_android.so"
test -s "$core"
toolbin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
# Keep the unstripped build output for debugging; verify the exact packaged copy.
"$toolbin/llvm-strip" --strip-unneeded -o "$out/libplay_modern_libretro_android.so" "$core"
python3 "$root/scripts/ci/native_symbols.py" --readelf "$toolbin/llvm-readelf" retain \
    --unstripped "$core" --packaged "$out/libplay_modern_libretro_android.so" --output "$out/symbols"
core="$out/libplay_modern_libretro_android.so"
"$toolbin/llvm-readelf" -h -lW "$core" > "$out/elf.txt"
grep -q 'Machine:.*AArch64' "$out/elf.txt"
python3 - "$out/elf.txt" <<'PY'
import pathlib, sys
loads = [int(line.split()[-1], 16) for line in pathlib.Path(sys.argv[1]).read_text().splitlines() if line.strip().startswith('LOAD ')]
assert loads and min(loads) >= 16384, f'16 KiB ELF alignment failed: {loads}'
PY
"$toolbin/llvm-nm" -D --defined-only "$core" | awk '{print $NF}' > "$out/symbols.txt"
for symbol in retro_init retro_deinit retro_load_game retro_run retro_set_environment retro_get_system_av_info retro_serialize retro_unserialize cartdeck_set_java_vm; do
    grep -Fxq "$symbol" "$out/symbols.txt"
done
"$toolbin/llvm-readelf" -dW "$core" > "$out/dependencies.txt"
# This remains private output until distribution approval, never a public CI artifact.
cp "$src/License.txt" "$out/PLAY_LICENSE.txt"
git -C "$src" submodule status --recursive > "$out/submodules.txt"
python3 - "$src" "$out" <<'PY_LICENSE'
import hashlib, pathlib, sys
src, out = map(pathlib.Path, sys.argv[1:])
notices = []
for dependency in ['CodeGen', 'Framework', 'Nuanceur', 'libchdr', 'Dependencies']:
    for path in (src / 'deps' / dependency).rglob('*'):
        if path.is_file() and '.git' not in path.parts and path.name.lower().startswith(('license', 'copying', 'copyright')):
            notices.append((path.relative_to(src).as_posix(), hashlib.sha256(path.read_bytes()).hexdigest()))
(out / 'third-party-notice-index.txt').write_text(
    'Source notice inventory for owner review; not a distribution clearance.\n' +
    ''.join(f'{digest}  {path}\n' for path, digest in sorted(notices)))
PY_LICENSE
{
    echo "upstream=$source_url"
    echo "commit=$source_sha"
    echo "ndk=29.0.14206865"
    echo "android_api=28"
    echo "core_file=libplay_modern_libretro_android.so"
    echo "renderer=GLES 3.2 required; runtime not proven"
    echo "persistence=frontend save directory/Play Data Files/vfs/mc0 and mc1"
    echo "status=NATIVE BUILD PASS only; not ANDROID RUNTIME PASS"
} > "$out/provenance.txt"
sha256sum "$out/libplay_modern_libretro_android.so" > "$out/sha256.txt"
{
    echo "revision=$source_sha"
    echo "core.sha256=$(sha256sum "$out/libplay_modern_libretro_android.so" | awk '{print $1}')"
} > "$out/play-modern.properties"
