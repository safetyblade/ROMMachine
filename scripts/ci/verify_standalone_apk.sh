#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
: "${ANDROID_HOME:?Android SDK is required}"
: "${CARTDECK_VERSION_CODE:?Expected version code is required}"
out="${CARTDECK_OUTPUT:-dist/cartdeck}"
evidence="$out/evidence"
mkdir -p "$evidence"
rm -f "$out/CARTDECK_STANDALONE_ARM64_DEBUG.apk"
mapfile -t apks < <(find lemuroid-app/build/outputs/apk -type f -name '*.apk' | sort)
[[ "${#apks[@]}" -eq 1 ]] || { printf 'Expected exactly one APK; found %s\n' "${#apks[@]}" >&2; exit 1; }
case "${apks[0]}" in
  */freeBundle/debug/*.apk) ;;
  *) echo 'Unexpected APK variant.' >&2; exit 1 ;;
esac
source="${apks[0]}"
"$ANDROID_HOME/build-tools/34.0.0/aapt" dump badging "$source" > "$evidence/package.txt"
grep -F "package: name='dev.prestwich.cartdeck.debug'" "$evidence/package.txt"
grep -F "versionCode='$CARTDECK_VERSION_CODE'" "$evidence/package.txt"
grep -Ei 'application-label.*CARTDECK' "$evidence/package.txt"
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify --verbose --print-certs "$source" > "$evidence/signing.txt"
grep -Fi 'Signer #1 certificate SHA-256 digest: ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8' "$evidence/signing.txt"
python3 scripts/p0_apk_inventory.py "$source" "$evidence"
if grep -qE 'lib/(x86|x86_64|armeabi-v7a)/' "$evidence/native-libraries.tsv"; then
  echo 'Unexpected non-ARM64 native libraries.' >&2
  exit 1
fi
python3 - "$source" <<'PY'
import sys
from pathlib import Path
assert Path(sys.argv[1]).stat().st_size < 100 * 1024 * 1024, 'APK exceeds 100 MiB'
PY
# Only stage the APK after all identity and signing checks succeed.
cp "$source" "$out/CARTDECK_STANDALONE_ARM64_DEBUG.apk"
(cd "$out" && sha256sum CARTDECK_STANDALONE_ARM64_DEBUG.apk) > "$evidence/sha256.txt"
wc -c "$out/CARTDECK_STANDALONE_ARM64_DEBUG.apk" > "$evidence/size.txt"
