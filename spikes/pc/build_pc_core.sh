#!/usr/bin/env bash
# Preserved upstream preview6, not an assertion that it is the latest release.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK=${PC_CORE_BUILD_DIR:?Use a fresh PC-specific build directory}
REVISION=a4a0bab7f8931433588f2fcad9045c85b277373d
NDK="${ANDROID_HOME:?}/ndk/29.0.14206865"
mkdir -p "$WORK"
test ! -e "$WORK/source"
git clone --no-checkout https://github.com/schellingb/dosbox-pure.git "$WORK/source"
git -C "$WORK/source" checkout --detach "$REVISION"
git -C "$WORK/source" submodule update --init --recursive
test "$(git -C "$WORK/source" rev-parse HEAD)" = "$REVISION"
test -z "$(git -C "$WORK/source" status --porcelain)"
"$NDK/ndk-build" -C "$WORK/source" APP_ABI=arm64-v8a APP_PLATFORM=android-23 -j4
mkdir -p "$WORK/output"
cp "$WORK/source/libs/arm64-v8a/libretro.so" "$WORK/output/libdosbox_pure_libretro_android.so"
cp "$WORK/source/LICENSE" "$WORK/output/DOSBox-Pure-LICENSE"
python3 "$ROOT/spikes/pc/audit_core_binary.py" "$WORK/output/libdosbox_pure_libretro_android.so" \
    --readelf "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" --output "$WORK/output/binary-audit.json"
git -C "$WORK/source" archive --format=tar.gz --output="$WORK/output/dosbox-pure-source.tar.gz" "$REVISION"
{
    printf 'namespace=pc-lab-core\nsource=https://github.com/schellingb/dosbox-pure\nrevision=%s\nndk=29.0.14206865\nproduction_eligible=false\n' "$REVISION"
    git -C "$WORK/source" submodule status --recursive
    sha256sum "$ROOT/spikes/pc/build_pc_core.sh" "$WORK/output/"*
} > "$WORK/output/provenance.txt"
