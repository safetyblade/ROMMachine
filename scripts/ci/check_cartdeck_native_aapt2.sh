#!/usr/bin/env bash
# ASSET-H1 independent Android AAPT2 resource compile verification (no Gradle).
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SOURCE="${1:-$ROOT/lemuroid-app/build/generated/cartdeck-native/res}"
OUT="${2:-$ROOT/lemuroid-app/build/generated/cartdeck-native/aapt2-evidence}"
mkdir -p "$OUT"
test -d "$SOURCE/drawable-nodpi"
test -s "$SOURCE/values/cartdeck_edition_colors.xml"

if command -v aapt2 >/dev/null 2>&1; then
  AAPT2="$(command -v aapt2)"
else
  SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
  test -d "$SDK/build-tools" || {
    echo "::error::Android SDK build-tools unavailable; AAPT2 resource validation NOT TESTED" >&2
    exit 2
  }
  mapfile -t TOOLS < <(find "$SDK/build-tools" -maxdepth 2 -type f -name 'aapt2' | sort -V)
  test "${#TOOLS[@]}" -gt 0 || {
    echo "::error::No AAPT2 executable; cannot claim native resource compilation" >&2
    exit 2
  }
  AAPT2="${TOOLS[-1]}"
fi

"$AAPT2" version > "$OUT/aapt2-version.txt" 2>&1
printf 'AAPT2 binary: %s\n' "$AAPT2" | tee "$OUT/toolchain.txt"
printf 'Source path: %s\n' "$SOURCE" >> "$OUT/toolchain.txt"
printf 'CI SHA: %s\n' "${GITHUB_SHA:-local}" >> "$OUT/toolchain.txt"
# This is compilation of Android XML resources, NOT linking into an APK,
# not a game launch, and not a replacement for a real Gradle build.
"$AAPT2" compile --dir "$SOURCE" -o "$OUT/cartdeck-h1.flat.zip" \
  > "$OUT/aapt2-stdout.txt" 2> "$OUT/aapt2-stderr.txt"

test -s "$OUT/cartdeck-h1.flat.zip"
unzip -Z1 "$OUT/cartdeck-h1.flat.zip" > "$OUT/compiled-resources.txt"
count="$(wc -l < "$OUT/compiled-resources.txt")"
printf 'Compiled entries: %s\n' "$count" | tee "$OUT/summary.txt"
# 78 drawables + at least one merged values resource.
test "$count" -ge 79 || {
  echo "::error::AAPT2 compiled fewer resource entries than the 78-vector catalog" >&2
  exit 1
}
# The complete native manifest and per-edition names are checked by
# test_cartdeck_native_assets.py, run immediately before this step. AAPT2
# successfully compiling the whole directory is the independent Android
# resource check; ZIP entry naming varies across build-tools versions.
test "$count" -ge 82 || {
  echo "::error::Missing generated Android resources: expected 81 vectors and values" >&2
  exit 1
}
sha256sum "$OUT/cartdeck-h1.flat.zip" > "$OUT/aapt2-flat-sha256.txt"
printf 'PASS: Android AAPT2 parsed all Original/Pink/Green resources (not APK/device acceptance)\n'
