#!/usr/bin/env bash
# Only used when the passed native artifacts have expired. No placeholder fallback.
set -euo pipefail
sdk="${ANDROID_HOME:?Android SDK required}"
manager="$sdk/cmdline-tools/latest/bin/sdkmanager"
"$manager" --sdk_root="$sdk" --install 'ndk;29.0.14206865' 'cmake;3.31.6' 'cmake;3.30.3' 'cmake;3.22.1'
export ANDROID_NDK_HOME="$sdk/ndk/29.0.14206865"
export PATH="$sdk/cmake/3.31.6/bin:$PATH"
sudo apt-get update -qq
sudo apt-get install -y -qq glslang-tools spirv-tools ninja-build nasm autoconf automake libtool pkg-config
rustup target add aarch64-linux-android
export NATIVE_BUILD_JOBS=2 NATIVE_JOBS=2
for engine in ps1 psp dc gc ps2; do
  case "$engine" in
    ps1) builder=build_ps1_upstream_core.sh ;;
    psp) builder=build_psp_upstream_core.sh ;;
    dc) builder=build_dreamcast_upstream_core.sh ;;
    gc) builder=build_gamecube_upstream_core.sh ;;
    ps2) builder=build_ps2_upstream_core.sh ;;
  esac
  bash "scripts/ci/$builder" "work/connected/legacy/native-engine-$engine"
done
python3 scripts/ci/build_hakux_native.py work/connected/hakux
python3 scripts/ci/build_xbox360_native.py work/connected/xenia
python3 scripts/ci/ps2_next_armsx2.py work/connected/armsx2 --pages both
CARTDECK_WIIU_BUILD_VULKAN_HOST=true bash scripts/ci/build_wiiu_cemu_arm64.sh work/connected/cemu
export HOST_BUILD_DIR="$RUNNER_TEMP/connected-libretrodroid-source"
export HOST_MAVEN_REPO="$GITHUB_WORKSPACE/work/connected/host/maven"
export LIBRETRODROID_SOURCE_DIR="$HOST_BUILD_DIR/source"
export BUILD_JOBS=2
bash scripts/ci/build_libretrodroid_host.sh
