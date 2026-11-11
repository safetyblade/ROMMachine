#!/usr/bin/env bash
# Build one private engineering APK from independently built/verified native engines.
# Example: --ps1 work/ps1/build --psp work/psp/build --dc work/dreamcast/output
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${CARTDECK_HOST_MAVEN_REPO:?Run build_libretrodroid_host.sh and set its Maven repository path}"
: "${CARTDECK_TEST_VERSION_CODE:?Set an upgrade-safe engineering version code}"
output=${CARTDECK_OUTPUT:-"$ROOT/work/native-apk-$(date -u +%Y%m%dT%H%M%SZ)"}
mkdir -p "$output"
output=$(cd "$output" && pwd)
bundle="$output/bundle"
python3 scripts/ci/stage_native_bundle.py --output "$bundle" "$@"
export CARTDECK_OUTPUT="$output"
export CARTDECK_VERSION_CODE="$CARTDECK_TEST_VERSION_CODE"
export CARTDECK_APK_MAX_MIB=${CARTDECK_APK_MAX_MIB:-256}
export GRADLE_BIN=${GRADLE_BIN:-"$ROOT/gradlew"}
systems=$(cat "$bundle/native-systems.txt")
bash scripts/ci/build_standalone_apk.sh "$CARTDECK_TEST_VERSION_CODE" \
    -PcartdeckNativeBundleDir="$bundle" -PcartdeckNativeSystems="$systems" \
    -PcartdeckHostMavenRepo="$CARTDECK_HOST_MAVEN_REPO" \
    -PcartdeckLibretroDroidVersion=0.14.0-cartdeck1
# The existing verifier copies the single APK and checks package/signature/alignment.
bash scripts/ci/verify_standalone_apk.sh
verification_args=(--host-aar "$CARTDECK_HOST_MAVEN_REPO/com/github/Swordfish90/LibretroDroid/0.14.0-cartdeck1/LibretroDroid-0.14.0-cartdeck1.aar")
if [[ "${CARTDECK_REQUIRE_FIVE_ENGINES:-false}" == true ]]; then
    verification_args+=(--require-five-engines)
fi
python3 scripts/ci/verify_native_bundle_apk.py \
    "$output/CARTDECK_STANDALONE_ARM64_DEBUG.apk" "$bundle" \
    "${verification_args[@]}" \
    > "$output/evidence/native-bundle-apk.json"
"$ANDROID_HOME/build-tools/34.0.0/zipalign" -c 4 "$output/CARTDECK_STANDALONE_ARM64_DEBUG.apk"
printf 'Private APK output: %s\n' "$output"
