#!/usr/bin/env bash
# Only used when the passed native artifacts have expired. No placeholder fallback.
set -euo pipefail
# CI-01C: optional per-component build. --all is deliberate legacy recovery only.
selected="${1:---all}"
case "$selected" in
  --all|ps1|psp|dc|gc|ps2|hakux|xenia|armsx2|cemu|libretrodroid) ;;
  *) echo "Unknown native component: $selected" >&2; exit 2 ;;
esac
if [[ "$selected" == '--all' && "${CARTDECK_FULL_NATIVE_REBUILD_AUTHORIZED:-false}" != true ]]; then
  echo 'Full native rebuild requires CARTDECK_FULL_NATIVE_REBUILD_AUTHORIZED=true' >&2
  exit 2
fi
selected_for_build() { [[ "$selected" == '--all' || "$selected" == "$1" ]]; }
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
  if selected_for_build "$engine"; then
    bash "scripts/ci/$builder" "work/connected/legacy/native-engine-$engine"
  fi
done
if selected_for_build hakux; then python3 scripts/ci/build_hakux_native.py work/connected/hakux; fi
if selected_for_build xenia; then python3 scripts/ci/build_xbox360_native.py work/connected/xenia; fi
if selected_for_build armsx2; then python3 scripts/ci/ps2_next_armsx2.py work/connected/armsx2 --pages both; fi
if selected_for_build cemu; then CARTDECK_WIIU_BUILD_VULKAN_HOST=true bash scripts/ci/build_wiiu_cemu_arm64.sh work/connected/cemu; fi
export HOST_BUILD_DIR="$RUNNER_TEMP/connected-libretrodroid-source"
export HOST_MAVEN_REPO="$GITHUB_WORKSPACE/work/connected/host/maven"
export LIBRETRODROID_SOURCE_DIR="$HOST_BUILD_DIR/source"
export BUILD_JOBS=2
if selected_for_build libretrodroid; then bash scripts/ci/build_libretrodroid_host.sh; fi
