#!/usr/bin/env bash
# Build the pinned host plus Android GLES negotiation and analog trigger fixes.
# No remote repository or dependency substitution is applied to CARTDECK.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
WORK=${HOST_BUILD_DIR:-"$ROOT/work/libretrodroid-host"}
SOURCE=${LIBRETRODROID_SOURCE_DIR:-"$WORK/source"}
MAVEN_REPO=${HOST_MAVEN_REPO:-"$WORK/maven"}
REVISION=8835c3098514390a271e36983957f7bb5f40abf1
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
NDK_VERSION=${HOST_NDK_VERSION:-29.0.14206865}
VERSION=0.14.0-cartdeck3
PATCHES=(
    "$ROOT/scripts/ci/patches/libretrodroid-gles-context-validation.patch"
    "$ROOT/scripts/ci/patches/libretrodroid-frontend-gl-state.patch"
    "$ROOT/scripts/ci/patches/libretrodroid-graphics-error.patch"
    "$ROOT/scripts/ci/patches/libretrodroid-core-frame-signal.patch"
)
for patch in "${PATCHES[@]}"; do
    [[ -f "$patch" ]] || { echo "Missing required host patch: $patch" >&2; exit 1; }
done

verify_patched_source() {
    # Compare exact trees using an isolated index. This permits an exact repeat
    # of multiple patches in any file order, without touching the real index or
    # accepting extra edits elsewhere in an already patched file.
    local index expected actual
    index=$(mktemp "$WORK/.host-index.XXXXXX")
    rm "$index"
    (
        trap 'rm -f "$index" "$index.lock"' EXIT
        export GIT_INDEX_FILE="$index"
        git -C "$SOURCE" read-tree "$REVISION"
        for patch in "${PATCHES[@]}"; do
            git -C "$SOURCE" apply --cached "$patch"
        done
        expected=$(git -C "$SOURCE" write-tree)
        git -C "$SOURCE" read-tree "$REVISION"
        git -C "$SOURCE" add -A -- .
        actual=$(git -C "$SOURCE" write-tree)
        [[ "$actual" == "$expected" ]] || {
            echo 'Host checkout contains unrelated modifications' >&2; exit 1;
        }
    )
}
[[ -f "$ANDROID_HOME/ndk/$NDK_VERSION/source.properties" ]] || {
    echo "Missing Android NDK $NDK_VERSION" >&2; exit 1;
}
[[ -d "$ANDROID_HOME/cmake/3.22.1" ]] || {
    echo 'Missing Android SDK CMake 3.22.1' >&2; exit 1;
}
mkdir -p "$WORK" "$MAVEN_REPO"
if [[ ! -d "$SOURCE/.git" ]]; then
    git clone --no-checkout https://github.com/Swordfish90/LibretroDroid.git "$SOURCE"
    git -C "$SOURCE" checkout --detach "$REVISION"
fi
if [[ -n "$(git -C "$SOURCE" status --porcelain)" ]]; then
    [[ "$(git -C "$SOURCE" rev-parse HEAD)" == "$REVISION" ]]
    verify_patched_source
else
    git -C "$SOURCE" checkout --detach "$REVISION"
    for patch in "${PATCHES[@]}"; do
        git -C "$SOURCE" apply --check "$patch"
        git -C "$SOURCE" apply "$patch"
    done
fi
git -C "$SOURCE" submodule update --init --recursive
# A clean parent Git link does not establish that dependency source is clean.
git -C "$SOURCE" submodule foreach --recursive 'test -z "$(git status --porcelain)"'
[[ "$(git -C "$SOURCE" rev-parse HEAD)" == "$REVISION" ]]
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
                groupId = 'com.github.Swordfish90'
                artifactId = 'LibretroDroid'
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
ARTIFACT="$MAVEN_REPO/com/github/Swordfish90/LibretroDroid/$VERSION/LibretroDroid-$VERSION.aar"
[[ -f "$ARTIFACT" ]]
python3 "$ROOT/scripts/ci/native_symbols.py" \
    --readelf "$ANDROID_HOME/ndk/$NDK_VERSION/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" \
    retain-aar --aar "$ARTIFACT" --search "$SOURCE/libretrodroid" --output "$WORK/symbols"
{
    printf 'source=https://github.com/Swordfish90/LibretroDroid\nrevision=%s\n' "$REVISION"
    printf 'abi=arm64-v8a\nndk=%s\n' "$NDK_VERSION"
    sha256sum "$ARTIFACT" "${PATCHES[@]}"
    git -C "$SOURCE" submodule status --recursive
} > "$WORK/provenance.txt"
printf 'Built %s\nBuild CARTDECK with -Dmaven.repo.local=%s\n' "$ARTIFACT" "$MAVEN_REPO"
