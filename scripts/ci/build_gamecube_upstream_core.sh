#!/usr/bin/env bash
# Private Android ARM64 build; does not activate or publicly distribute Dolphin.
set -euo pipefail
readonly source_url=https://github.com/libretro/dolphin.git
readonly source_sha=32c61f9d0e933331bb7087ba57e96883241b4960
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK r29}"
out="${1:?Output directory required}"
mkdir -p "$out"
out="$(cd "$out" && pwd)"
workspace="${DOLPHIN_BUILD_WORKSPACE:-$(mktemp -d)}"
mkdir -p "$workspace"
workspace="$(cd "$workspace" && pwd)"
if [[ -z "${DOLPHIN_BUILD_WORKSPACE:-}" ]]; then trap 'rm -rf "$workspace"' EXIT; fi
source_dir="$workspace/source"
if [[ ! -d "$source_dir/.git" ]]; then
    git clone --filter=blob:none --no-checkout "$source_url" "$source_dir"
fi
git -C "$source_dir" checkout --detach "$source_sha"
test "$(git -C "$source_dir" rev-parse HEAD)" = "$source_sha"
# Never silently build a locally patched engine under upstream provenance.
test -z "$(git -C "$source_dir" status --porcelain --untracked-files=no --ignore-submodules=all)"
git -C "$source_dir" submodule update --init --recursive --depth 1 --jobs 2
! git -C "$source_dir" submodule status --recursive | grep -Eq '^[-+U]'
cmake -S "$source_dir" -B "$workspace/build" -G Ninja \
    -DLIBRETRO=ON -DCMAKE_BUILD_TYPE=Release \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
    -DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384 \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake"
cmake --build "$workspace/build" --parallel "${NATIVE_BUILD_JOBS:-2}"
mapfile -t candidates < <(find "$workspace/build" -type f -name '*dolphin*libretro*.so')
[[ ${#candidates[@]} == 1 ]] || { echo 'Expected exactly one Dolphin core' >&2; exit 1; }
core="${candidates[0]}"
toolbin="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin"
python3 "$(dirname "$0")/verify_gamecube_core.py" "$core" "$toolbin/llvm-readelf"
# Stage only after verification; caller decides whether/where to package privately.
cp "$core" "$out/libdolphin_modern_libretro_android.so"
"$toolbin/llvm-strip" --strip-unneeded "$out/libdolphin_modern_libretro_android.so"
python3 "$(dirname "$0")/verify_gamecube_core.py" "$out/libdolphin_modern_libretro_android.so" "$toolbin/llvm-readelf"
mkdir -p "$out/system/dolphin-emu/Sys"
cp -a "$source_dir/Data/Sys/." "$out/system/dolphin-emu/Sys/"
cp "$source_dir/COPYING" "$out/DOLPHIN_COPYING"
{
    echo "upstream=$source_url"
    echo "commit=$source_sha"
    echo 'abi=arm64-v8a'
    echo 'android_api=28'
    echo 'system_resource_path=dolphin-emu/Sys'
    echo 'save_directory_contract=<system-specific persistent saves>/User'
    cat "$ANDROID_NDK_HOME/source.properties"
    git -C "$source_dir" submodule status --recursive
} > "$out/provenance.txt"
(cd "$out" && find system -type f -print0 | sort -z | xargs -0 sha256sum > assets-sha256.txt)
(cd "$out" && sha256sum libdolphin_modern_libretro_android.so > sha256.txt)
"$toolbin/llvm-readelf" -dW "$core" > "$out/elf-dependencies.txt"
python3 - "$out" "$source_sha" <<'PYZIP'
import hashlib, pathlib, sys, zipfile
out = pathlib.Path(sys.argv[1])
archive = out / 'dolphin-modern-assets.zip'
root = out / 'system/dolphin-emu/Sys'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for path in sorted(root.rglob('*')):
        if path.is_file():
            info = zipfile.ZipInfo(path.relative_to(root).as_posix(), (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, path.read_bytes())
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
(out / 'dolphin-modern.properties').write_text(
    f'upstream.revision={sys.argv[2]}\n'
    f'core.sha256={sha(out / "libdolphin_modern_libretro_android.so")}\n'
    f'assets.sha256={sha(archive)}\n')
PYZIP
echo "NATIVE BUILD PASS: $out; Android runtime/gameplay remain unverified"
