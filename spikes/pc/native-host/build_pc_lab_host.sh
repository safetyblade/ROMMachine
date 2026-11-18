#!/usr/bin/env bash
# Build the pinned host plus Android GLES negotiation and analog trigger fixes.
# No remote repository or dependency substitution is applied to CARTDECK.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
WORK=${PC_LAB_BUILD_DIR:?Specify a fresh independent PC laboratory build directory}
SOURCE="$WORK/source"
MAVEN_REPO="$WORK/maven"
REVISION=8835c3098514390a271e36983957f7bb5f40abf1
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
NDK_VERSION=${HOST_NDK_VERSION:-29.0.14206865}
VERSION=0.14.0-pc-lab2
PATCHES=(
    "$ROOT/spikes/pc/native-host/base-patches/libretrodroid-gles-context-validation.patch"
    "$ROOT/spikes/pc/native-host/base-patches/libretrodroid-frontend-gl-state.patch"
    "$ROOT/spikes/pc/native-host/base-patches/libretrodroid-graphics-error.patch"
    "$ROOT/spikes/pc/native-host/base-patches/libretrodroid-core-frame-signal.patch"
    "$ROOT/spikes/pc/native-host/base-patches/libretrodroid-play-vm-bridge.patch"
)
for patch in "${PATCHES[@]}"; do
    [[ -f "$patch" ]] || { echo "Missing required host patch: $patch" >&2; exit 1; }
done

[[ -f "$ANDROID_HOME/ndk/$NDK_VERSION/source.properties" ]] || {
    echo "Missing Android NDK $NDK_VERSION" >&2; exit 1;
}
[[ -d "$ANDROID_HOME/cmake/3.22.1" ]] || {
    echo 'Missing Android SDK CMake 3.22.1' >&2; exit 1;
}
[[ ! -e "$SOURCE" ]] || { echo 'PC lab requires a fresh checkout' >&2; exit 1; }
mkdir -p "$WORK" "$MAVEN_REPO"
git clone --no-checkout https://github.com/Swordfish90/LibretroDroid.git "$SOURCE"
git -C "$SOURCE" checkout --detach "$REVISION"
for patch in "${PATCHES[@]}"; do
    git -C "$SOURCE" apply --check "$patch"
    git -C "$SOURCE" apply "$patch"
done
python3 "$ROOT/spikes/pc/native-host/apply_pc_input.py" "$SOURCE"
python3 "$ROOT/spikes/pc/native-host/apply_pc_guest.py" "$SOURCE"
git -C "$SOURCE" add -N -- .
git -C "$SOURCE" diff --binary > "$WORK/applied-host.patch"
git -C "$SOURCE" submodule update --init --recursive
# A clean parent Git link does not establish that dependency source is clean.
git -C "$SOURCE" submodule foreach --recursive 'test -z "$(git status --porcelain)"'
[[ "$(git -C "$SOURCE" rev-parse HEAD)" == "$REVISION" ]]
# PC-specific input contract, separate from the console fixture.
CPP="$SOURCE/libretrodroid/src/main/cpp"
WORK_INC="$WORK/input-tests/include"
mkdir -p "$WORK_INC/android"
printf '#pragma once\n#define AKEY_EVENT_ACTION_DOWN 0\n#define AKEY_EVENT_ACTION_UP 1\n' > "$WORK_INC/android/input.h"
ln -s "$ANDROID_HOME/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include/android/keycodes.h" "$WORK_INC/android/keycodes.h"
HEADER=$(find "$SOURCE" -name libretro.h -print -quit)
test -n "$HEADER"
c++ -std=c++17 -Wall -Wextra -I"$WORK_INC" -I"$CPP" -I"$(dirname "$HEADER")" \
    "$ROOT/spikes/pc/native-host/libretrodroid_input_test.cpp" "$CPP/input.cpp" -o "$WORK/input-test"
"$WORK/input-test"
# Use the installed app SDK while preserving upstream Kotlin/JNI/native source.
cat > "$WORK/local-publication.gradle" <<GRADLE
allprojects {
    afterEvaluate { project ->
        if (project.name == 'libretrodroid') {
            project.android.compileSdkVersion 35
            project.android.ndkVersion '$NDK_VERSION'
            project.android.defaultConfig.ndk.abiFilters.add('arm64-v8a')
            project.android.defaultConfig.externalNativeBuild.cmake {
                cFlags '-g'
                cppFlags '-g'
            }
            project.publishing.publications.release {
                groupId = 'dev.cartdeck.pc.lab'
                artifactId = 'PcLibretroHost'
                version = '$VERSION'
            }
        }
    }
}
GRADLE
(
    cd "$SOURCE"
    ./gradlew :libretrodroid:publishReleasePublicationToMavenLocal \
        --init-script "$WORK/local-publication.gradle" \
        -Dmaven.repo.local="$MAVEN_REPO" \
        --no-daemon --max-workers="${BUILD_JOBS:-4}"
)
ARTIFACT="$MAVEN_REPO/dev/cartdeck/pc/lab/PcLibretroHost/$VERSION/PcLibretroHost-$VERSION.aar"
[[ -f "$ARTIFACT" ]]
git -C "$SOURCE" archive --format=tar.gz --output="$WORK/host-base-source.tar.gz" "$REVISION"
python3 "$ROOT/scripts/ci/native_symbols.py" \
    --readelf "$ANDROID_HOME/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" \
    retain-aar --aar "$ARTIFACT" --search "$SOURCE/libretrodroid" --output "$WORK/symbols"
{
    printf 'source=https://github.com/Swordfish90/LibretroDroid\nrevision=%s\n' "$REVISION"
    printf 'abi=arm64-v8a\nndk=%s\n' "$NDK_VERSION"
    sha256sum "$ARTIFACT" "$ROOT/spikes/pc/native-host/build_pc_lab_host.sh" "${PATCHES[@]}" "$ROOT/spikes/pc/native-host/apply_pc_input.py" "$ROOT/spikes/pc/native-host/apply_pc_guest.py" "$WORK/applied-host.patch" "$WORK/host-base-source.tar.gz"
    printf 'namespace=pc-lab\nprotocol=pc-guest-2-experimental\nproduction_eligible=false\n'
    git -C "$SOURCE" submodule status --recursive
} > "$WORK/provenance.txt"
printf 'Built PC lab host %s\nPC-only Maven repository: %s\n' "$ARTIFACT" "$MAVEN_REPO"
