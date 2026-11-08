#!/usr/bin/env bash
# CARTDECK CI-H1: fail-closed Android emulator install and launch smoke.
# Invoked as ONE bash process by android-emulator-runner; no shell-line/pipeline masking.
set -Eeuo pipefail

APK="${1:?Usage: android_emulator_smoke.sh APK PACKAGE ACTIVITY}"
PACKAGE="${2:?Missing Android application package}"
ACTIVITY="${3:?Missing launch activity class}"
OUT="${CARTDECK_SMOKE_EVIDENCE_DIR:-emulator-evidence}"
mkdir -p "$OUT"

finish() {
  result=$?
  trap - EXIT
  set +e
  printf '%s\n' "$result" > "$OUT/exit-code.txt"
  timeout 45s adb logcat -d -t 5000 > "$OUT/logcat.txt" 2> "$OUT/logcat-error.txt"
  timeout 35s adb exec-out screencap -p > "$OUT/screenshot.png" 2> "$OUT/screenshot-error.txt"
  if [[ ! -s "$OUT/screenshot.png" ]]; then rm -f "$OUT/screenshot.png"; fi
  exit "$result"
}
trap finish EXIT

fail() {
  printf '::error::CARTDECK emulator smoke failed: %s\n' "$*" >&2
  printf 'FAIL: %s\n' "$*" >> "$OUT/result.txt"
  exit 1
}

[[ -s "$APK" ]] || fail "APK does not exist or is empty: $APK"
sha256sum "$APK" > "$OUT/apk-sha256.txt"
printf '%s\n' "$PACKAGE/$ACTIVITY" > "$OUT/launch-component.txt"
printf 'source=%s\n' "${GITHUB_SHA:-unknown}" > "$OUT/ci-source.txt"

# Android CLI may return 0 despite a text-level failure; check both.
# Previous 212 MB full APK took >8 minutes to install on hosted x86_64; allow 15 minutes.
if ! timeout 900s adb install -r "$APK" > "$OUT/install.txt" 2>&1; then
  fail "adb install command failed"
fi
grep -Eq '^Success[[:space:]]*$' "$OUT/install.txt" || fail "adb install did not report Success"

if ! timeout 30s adb shell getprop ro.build.version.sdk > "$OUT/android-api.txt" 2>&1; then
  fail "cannot query emulator Android API"
fi
if ! timeout 30s adb shell getprop ro.product.cpu.abi > "$OUT/android-abi.txt" 2>&1; then
  fail "cannot query emulator ABI"
fi
if ! timeout 30s adb shell getconf PAGESIZE > "$OUT/page-size.txt" 2>&1; then
  fail "cannot query emulator page size"
fi

if ! timeout 110s adb shell am start -W -n "$PACKAGE/$ACTIVITY" > "$OUT/launch.txt" 2>&1; then
  fail "activity launch command failed"
fi
if grep -Eiq 'Status:[[:space:]]*timeout|Error:|Exception|unable to resolve' "$OUT/launch.txt"; then
  fail "activity reported timeout or error"
fi
grep -Eq '^[[:space:]]*Status:[[:space:]]*ok[[:space:]]*$' "$OUT/launch.txt" || fail "activity did not report Status: ok"

# Recheck the process after launch to catch immediate death. Short bounded polls.
found=0
for attempt in 1 2 3 4 5 6; do
  if timeout 20s adb shell pidof "$PACKAGE" > "$OUT/running-pid.txt" 2>&1 &&
     grep -Eq '[0-9]+' "$OUT/running-pid.txt"; then
    found=1
    break
  fi
  sleep 2
done
[[ "$found" -eq 1 ]] || fail "target application process absent after launch"
sleep 3
if ! timeout 20s adb shell pidof "$PACKAGE" > "$OUT/running-pid-after-grace.txt" 2>&1 ||
   ! grep -Eq '[0-9]+' "$OUT/running-pid-after-grace.txt"; then
  fail "application died immediately after launch"
fi

if ! timeout 30s adb shell dumpsys activity activities > "$OUT/activity-state.txt" 2>&1; then
  fail "cannot inspect foreground activity"
fi
# Count only the top/resumed activity line, not any historical activity record.
grep -E '(topResumedActivity|mResumedActivity)' "$OUT/activity-state.txt" > "$OUT/resumed-activity.txt" ||
  fail "emulator does not expose the resumed activity"
grep -Fq "$PACKAGE" "$OUT/resumed-activity.txt" ||
  fail "CARTDECK is not the resumed foreground application"

printf 'PASS: installed, launch Status ok, PID stable and foreground activity verified\n' > "$OUT/result.txt"
printf 'CARTDECK CI-H1 smoke passed: %s\n' "$PACKAGE"
