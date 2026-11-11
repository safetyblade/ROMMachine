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
VERSION=0.14.0-cartdeck1
PATCH="$ROOT/scripts/ci/patches/libretrodroid-gles-context-validation.patch"
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
    # Permit an exact repeat of our own patch, never unrelated source edits.
    cmp <(git -C "$SOURCE" diff --binary) "$PATCH" || {
        echo 'Host checkout contains unrelated modifications' >&2; exit 1;
    }
    [[ "$(git -C "$SOURCE" rev-parse HEAD)" == "$REVISION" ]]
else
    git -C "$SOURCE" checkout --detach "$REVISION"
    git -C "$SOURCE" apply --check "$PATCH"
    git -C "$SOURCE" apply "$PATCH"
fi
git -C "$SOURCE" submodule update --init --recursive
[[ "$(git -C "$SOURCE" rev-parse HEAD)" == "$REVISION" ]]
# Use the installed app SDK while preserving upstream Kotlin/JNI/native source.
cat > "$WORK/local-publication.gradle" <<GRADLE
allprojects {
    afterEvaluate { project ->
        if (project.name == 'libretrodroid') {
            project.android.compileSdkVersion 35
            project.android.ndkVersion '$NDK_VERSION'
            project.android.defaultConfig.ndk.abiFilters.add('arm64-v8a')
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
{
    printf 'source=https://github.com/Swordfish90/LibretroDroid\nrevision=%s\n' "$REVISION"
    printf 'abi=arm64-v8a\nndk=%s\n' "$NDK_VERSION"
    sha256sum "$ARTIFACT" "$PATCH"
    git -C "$SOURCE" submodule status --recursive
} > "$WORK/provenance.txt"
printf 'Built %s\nBuild CARTDECK with -Dmaven.repo.local=%s\n' "$ARTIFACT" "$MAVEN_REPO"
