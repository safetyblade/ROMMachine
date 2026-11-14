#!/usr/bin/env bash
# WIIU-01: source-built Android ARM64 Cemu Libretro ABI feasibility ONLY.
# This cannot produce a CARTDECK APK or claim gameplay.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with the pinned NDK}"
NDK_VERSION=29.0.14206865
NDK="$ANDROID_HOME/ndk/$NDK_VERSION"
# vcpkg's Android triplet runs its own compiler probe. It does not inherit the
# outer CMAKE_TOOLCHAIN_FILE's NDK discovery.
export ANDROID_NDK_HOME="$NDK"
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
  -DCMAKE_TOOLCHAIN_FILE="$WORK/source/dependencies/vcpkg/scripts/buildsystems/vcpkg.cmake" \
  -DVCPKG_CHAINLOAD_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DCEMU_TARGET_ANDROID=ON \
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
if [[ "${CARTDECK_WIIU_BUILD_VULKAN_HOST:-false}" == "true" ]]; then
  cmake -S "$ROOT/spikes/wiiu/vulkan-host" -B "$WORK/host-build" -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
    -DCEMU_LIBRETRO_HEADERS="$WORK/source/src/libretro"
  cmake --build "$WORK/host-build" --target cartdeckwiiuvulkanhost -- -j 4
  HOST="$WORK/host-build/libcartdeckwiiuvulkanhost.so"
  test -s "$HOST"
  "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" -h -lW "$HOST" > "$WORK/vulkan-host-elf.txt"
  python3 - "$HOST" "$WORK/vulkan-host-elf.txt" "$WORK/native-evidence.json" "$ROOT" <<'PY'
import hashlib
import json
import pathlib
import re
import sys
sys.path.insert(0, str(pathlib.Path(sys.argv[4]) / 'scripts/ci'))
from verify_gamecube_core import verify_android_elf
so = pathlib.Path(sys.argv[1])
readelf_output = pathlib.Path(sys.argv[2]).read_text()
verify_android_elf(so.read_bytes(), '')
assert "AArch64" in readelf_output
proof_path = pathlib.Path(sys.argv[3])
proof = json.loads(proof_path.read_text())
proof['host_sha256'] = hashlib.sha256(so.read_bytes()).hexdigest()
proof_path.write_text(json.dumps(proof, indent=2, sort_keys=True) + '\n')
print("Android ARM64 Wii U Vulkan presenter ELF compiled; real frame QA still missing")
PY
  cp "$HOST" "$WORK/cartdeck_wiiu_vulkan_host_arm64.so"
fi
echo "STOP: Vulkan CARTDECK frontend APK and Wii U gameplay remain unverified."
