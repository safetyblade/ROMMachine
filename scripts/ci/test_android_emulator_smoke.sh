#!/usr/bin/env bash
# Hermetic contract tests: no SDK, emulator, network, account or ROM required.
set -Eeuo pipefail
SRC="$(cd "$(dirname "$0")" && pwd)/android_emulator_smoke.sh"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT
mkdir -p "$T/bin"
cat > "$T/bin/adb" <<'MOCK'
#!/usr/bin/env bash
set -u
case "${1-}" in
  install) [[ "${MOCK_INSTALL:-success}" == success ]] && { echo Success; exit 0; } || { echo 'Failure [INSTALL_FAILED]'; exit 0; } ;;
  logcat) printf 'mock logcat\n'; exit 0 ;;
  exec-out) printf 'fakepng'; exit 0 ;;
  shell)
    case "${2-}" in
      getprop) echo 35; exit 0 ;;
      getconf) echo 4096; exit 0 ;;
      am)
        case "${MOCK_LAUNCH:-ok}" in
          ok) echo 'Status: ok'; exit 0 ;;
          timeout) echo 'Status: timeout'; exit 0 ;;
          error) echo 'Error: Activity class does not exist'; exit 0 ;;
          exit1) echo 'Status: ok'; exit 1 ;;
        esac ;;
      pidof) [[ "${MOCK_PID:-present}" == present ]] && { echo 1212; exit 0; } || exit 1 ;;
      dumpsys)
        if [[ "${MOCK_FOREGROUND:-correct}" == correct ]]; then
          echo 'topResumedActivity=ActivityRecord{ dev a com.swordfish.lemuroid.debug/com.swordfish.lemuroid.app.mobile.feature.main.MainActivity t1}'
        else
          echo 'topResumedActivity=ActivityRecord{ dev a com.android.launcher/com.android.launcher.Main t1}'
        fi
        exit 0 ;;
    esac ;;
esac
printf 'UNEXPECTED MOCK ADB CALL: %s\n' "$*" >&2
exit 19
MOCK
cat > "$T/bin/sleep" <<'MOCK'
#!/usr/bin/env bash
exit 0
MOCK
chmod +x "$T/bin/adb" "$T/bin/sleep"
APK="$T/test.apk"
printf APK > "$APK"

run() {
  local name="$1" expected="$2"; shift 2
  local evidence="$T/$name" rc=0
  env PATH="$T/bin:$PATH" CARTDECK_SMOKE_EVIDENCE_DIR="$evidence" "$@" \
    bash "$SRC" "$APK" com.swordfish.lemuroid.debug com.swordfish.lemuroid.app.mobile.feature.main.MainActivity > "$T/$name.log" 2>&1 || rc=$?
  if [[ "$rc" -ne "$expected" ]]; then
    echo "FAILED TEST $name: wanted $expected, saw $rc" >&2
    cat "$T/$name.log" >&2
    return 1
  fi
  if [[ "$expected" -eq 0 ]]; then
    grep -q '^PASS:' "$evidence/result.txt"
  else
    grep -q '^FAIL:' "$evidence/result.txt"
  fi
  [[ -s "$evidence/apk-sha256.txt" ]] && [[ -s "$evidence/logcat.txt" ]]
  echo "PASS $name (exit $rc)"
}
run success 0
run failed-install 1 MOCK_INSTALL=failed
run false-green-timeout 1 MOCK_LAUNCH=timeout
run activity-error 1 MOCK_LAUNCH=error
run adb-nonzero 1 MOCK_LAUNCH=exit1
run missing-process 1 MOCK_PID=missing
run wrong-foreground 1 MOCK_FOREGROUND=wrong
echo 'ALL CI-H1 shell contract tests PASS'
