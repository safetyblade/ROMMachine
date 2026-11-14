#!/usr/bin/env bash
# WIIU-01: source-built Android ARM64 Cemu Libretro ABI feasibility ONLY.
# This cannot produce a CARTDECK APK or claim gameplay.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with the pinned NDK}"
NDK_VERSION=29.0.14206865
NDK="$ANDROID_HOME/ndk/$NDK_VERSION"
SOURCE_SHA=444c585202302a7f4b319bafb4cf72156a8cac8b
WORK="${1:?Specify a clean output directory}"
[[ ! -e "$WORK" ]] || { echo "Output directory already exists; refusing stale binary" >&2; exit 1; }
mkdir -p "$WORK"
WORK="$(cd "$WORK" && pwd)"
[[ -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" ]] || {
  echo "Android NDK $NDK_VERSION unavailable" >&2; exit 1;
}
git clone --no-checkout https://github.com/WizzardSK/cemu-libretro.git "$WORK/source"
git -C "$WORK/source" checkout --detach "$SOURCE_SHA"
git -C "$WORK/source" submodule update --init --recursive
python3 - "$ROOT" "$WORK/source" <<'PY'
import pathlib
import sys
sys.path.insert(0, str(pathlib.Path(sys.argv[1]) / "scripts/ci"))
from wiiu_cemu_contract import check_source
check_source(sys.argv[2])
print("Pinned Cemu source and recursive submodules: PASS")
PY
export VCPKG_FORCE_SYSTEM_BINARIES=1
export VCPKG_MAX_CONCURRENCY=4
# Upstream CI bootstraps the pinned vcpkg submodule before CMake manifests.
# Without this step a fresh, clean Android runner has no executable vcpkg.
test -f "$WORK/source/dependencies/vcpkg/bootstrap-vcpkg.sh"
bash "$WORK/source/dependencies/vcpkg/bootstrap-vcpkg.sh"
test -x "$WORK/source/dependencies/vcpkg/vcpkg"
cmake -S "$WORK/source" -B "$WORK/build" -G Ninja \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static \
  -DVCPKG_TARGET_TRIPLET=arm64-android -DVCPKG_HOST_TRIPLET=x64-linux \
  -DENABLE_LIBRETRO=ON -DENABLE_OPENGL=OFF -DENABLE_VULKAN=ON \
  -DENABLE_LIBUSB=OFF -DENABLE_BLUEZ=OFF -DENABLE_WAYLAND=OFF \
  -DENABLE_FERAL_GAMEMODE=OFF -DENABLE_DISCORD_RPC=OFF \
  -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384"
cmake --build "$WORK/build" --target cemu_libretro -- -j 4
CORE="$WORK/source/bin/cemu_libretro.so"
[[ -s "$CORE" ]] || { echo "Cemu build ended without an actual core" >&2; exit 1; }
cp "$CORE" "$WORK/cemu_libretro_android_arm64.so"
python3 "$ROOT/scripts/ci/wiiu_cemu_contract.py" \
  --source "$WORK/source" \
  --binary "$WORK/cemu_libretro_android_arm64.so" \
  --readelf "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" \
  --report "$WORK/native-evidence.json"
echo "Native ELF proof available at $WORK/native-evidence.json"
echo "STOP: Vulkan CARTDECK frontend, APK, and Wii U gameplay remain unverified."
