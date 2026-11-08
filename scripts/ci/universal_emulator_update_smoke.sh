#!/usr/bin/env bash
# Synthetic emulator update continuity proof for the standalone CARTDECK test package.
# This checks package/signing/version, the app-private sentinel, foreground launch,
# and real screenshots. It does NOT assert ROMs or actual game save states.
set -Eeuo pipefail
BASE_APK="${1:?Expected previous universal QA APK}"
NEW_APK="${2:?Expected new universal QA APK}"
OUT="${3:-qa/evidence}"
PACKAGE="dev.prestwich.cartdeck.debug"
ACTIVITY="com.swordfish.lemuroid.app.mobile.feature.main.MainActivity"
AAPT="${ANDROID_HOME:?Android SDK required}/build-tools/34.0.0/aapt"
SIGNER="$ANDROID_HOME/build-tools/34.0.0/apksigner"
EXPECTED_SIGNER="ce3ebf1d972c09583470e184d9b86edd20eb4679fedc632d31ac9f58e8741fc8"
mkdir -p "$OUT"

on_exit() {
  local code=$?
  trap - EXIT
  set +e
  echo "$code" > "$OUT/exit-code.txt"
  timeout 30s adb logcat -d -t 8000 > "$OUT/logcat.txt" 2> "$OUT/logcat-errors.txt"
  timeout 35s adb exec-out screencap -p > "$OUT/final-screen.png" 2> "$OUT/screenshot-errors.txt"
  if [[ ! -s "$OUT/final-screen.png" ]]; then rm -f "$OUT/final-screen.png"; fi
  exit "$code"
}
trap on_exit EXIT

fail() { echo "::error::CARTDECK emulator QA: $*" >&2; exit 1; }

[[ -s "$BASE_APK" && -s "$NEW_APK" ]] || fail "Missing universal QA APK(s)"
[[ -x "$AAPT" && -x "$SIGNER" ]] || fail "Android build-tools 34.0.0 unavailable"
sha256sum "$BASE_APK" "$NEW_APK" > "$OUT/apk-sha256.txt"
for phase in baseline updated; do
  apk="$BASE_APK"
  [[ "$phase" == updated ]] && apk="$NEW_APK"
  "$AAPT" dump badging "$apk" > "$OUT/$phase-badging.txt"
  "$SIGNER" verify --verbose --print-certs "$apk" > "$OUT/$phase-signing.txt"
  grep -Fq "package: name='$PACKAGE'" "$OUT/$phase-badging.txt" || fail "$phase has incorrect package"
  grep -Fiq "Signer #1 certificate SHA-256 digest: $EXPECTED_SIGNER" "$OUT/$phase-signing.txt" ||
    fail "$phase does not use preserved development signer"
  unzip -Z1 "$apk" > "$OUT/$phase-zip-paths.txt"
  grep -Eq '^lib/x86_64/.*\.so$' "$OUT/$phase-zip-paths.txt" ||
    fail "$phase universal APK has no x86_64 native libraries"
done
extract_version() {
  sed -n "s/^package:.*versionCode='\([0-9][0-9]*\)'.*/\1/p" "$1" | head -1
}
old_version="$(extract_version "$OUT/baseline-badging.txt")"
new_version="$(extract_version "$OUT/updated-badging.txt")"
[[ -n "$old_version" && -n "$new_version" && "$new_version" -gt "$old_version" ]] ||
  fail "Version code does not strictly increase"
printf 'old=%s\nnew=%s\n' "$old_version" "$new_version" > "$OUT/versions.txt"

adb wait-for-device
timeout 30s adb shell getprop ro.product.cpu.abi > "$OUT/emulator-abi.txt"
timeout 30s adb shell getprop ro.build.version.sdk > "$OUT/emulator-api.txt"
timeout 30s adb shell getconf PAGESIZE > "$OUT/emulator-page-size.txt"
grep -q 'x86_64' "$OUT/emulator-abi.txt" || fail "Expected x86_64 Android emulator"
adb shell settings put global window_animation_scale 0 || true
adb shell settings put global transition_animation_scale 0 || true
adb shell settings put global animator_duration_scale 0 || true

check_launch() {
  local label="$1"
  timeout 110s adb shell am start -W -n "$PACKAGE/$ACTIVITY" > "$OUT/$label-launch.txt" 2>&1 ||
    fail "$label activity launch returned nonzero"
  grep -Eq '^[[:space:]]*Status:[[:space:]]*ok[[:space:]]*$' "$OUT/$label-launch.txt" ||
    fail "$label activity did not report Status: ok"
  if grep -Eiq 'Status:[[:space:]]*timeout|Error:|Exception' "$OUT/$label-launch.txt"; then
    fail "$label activity returned a text-level error"
  fi
  sleep 5
  adb shell pidof "$PACKAGE" > "$OUT/$label-pid.txt" 2>&1 ||
    fail "$label application process disappeared"
  grep -Eq '[0-9]+' "$OUT/$label-pid.txt" || fail "$label application PID empty"
  timeout 30s adb shell dumpsys activity activities > "$OUT/$label-activity.txt"
  grep -E '(topResumedActivity|mResumedActivity)' "$OUT/$label-activity.txt" |
    grep -Fq "$PACKAGE" || fail "$label application is not resumed/foreground"
  timeout 35s adb exec-out screencap -p > "$OUT/$label-home.png"
  python3 - "$OUT/$label-home.png" <<'PY'
from pathlib import Path
import sys
data=Path(sys.argv[1]).read_bytes()
assert len(data)>8192 and data.startswith(b'\x89PNG\r\n\x1a\n'), 'Empty/invalid emulator screenshot'
PY
}

timeout 900s adb install -r "$BASE_APK" > "$OUT/baseline-install.txt" 2>&1 ||
  fail "Baseline universal QA APK failed to install"
grep -Eq '^Success[[:space:]]*$' "$OUT/baseline-install.txt" || fail "Baseline did not report Success"
check_launch baseline

# Synthetic data continuity: create an app-private marker in the debug test install.
# Never overwrite a genuine saved ROM/game file; no uninstall or clear-data occurs.
adb shell run-as "$PACKAGE" mkdir -p files/cartdeck-qa ||
  fail "debuggable run-as was not available for private-data probe"
sentinel="cartdeck-synthetic-upgrade-probe-v1"
printf '%s\n' "$sentinel" |
  adb shell run-as "$PACKAGE" sh -c 'cat > files/cartdeck-qa/upgrade-sentinel.txt' ||
  fail "could not write synthetic app-private marker"
before="$(adb shell run-as "$PACKAGE" cat files/cartdeck-qa/upgrade-sentinel.txt | tr -d '\r\n')"
[[ "$before" == "$sentinel" ]] || fail "sentinel not present before update"
timeout 40s adb shell am force-stop "$PACKAGE"
# An in-place install over the previous package. Never uninstall or clear data.
timeout 900s adb install -r "$NEW_APK" > "$OUT/updated-install.txt" 2>&1 ||
  fail "Updated universal QA APK failed to install over baseline"
grep -Eq '^Success[[:space:]]*$' "$OUT/updated-install.txt" || fail "Upgrade did not report Success"
after="$(adb shell run-as "$PACKAGE" cat files/cartdeck-qa/upgrade-sentinel.txt | tr -d '\r\n')"
[[ "$after" == "$sentinel" ]] || fail "app-private synthetic data was lost on update"
printf 'Synthetic marker preserved across APK in-place update.\n' > "$OUT/synthetic-retention.txt"
check_launch updated
timeout 45s adb shell run-as "$PACKAGE" sh -c 'ls -la databases 2>/dev/null || true' \
  > "$OUT/database-directory.txt" 2>&1 || true
echo 'PASS: both universal QA APKs signed; increasing version; retained private marker; installed and foreground after update; real home screenshots captured.' |
  tee "$OUT/result.txt"
echo 'LIMIT: Synthetic marker is NOT real ROM save/SRAM/Room migration/physical-device or populated-shelf QA.' |
  tee "$OUT/limitations.txt"
