#!/usr/bin/env bash
# GC-01: user-local physical Android proof, never an APK/core/ROM distributor.
set -Eeuo pipefail
if [[ "$#" -ne 5 ]]; then
  echo "Usage: $0 DEBUG_APK DOLPHIN_ARM64_SO LAWFUL_GAME_DIR DISC_NAME ANDROID_PACKAGE" >&2
  echo "Example disc: game.gdi plus ALL companion tracks in LAWFUL_GAME_DIR" >&2
  exit 2
fi
apk="$1"
core="$2"
game_dir="$3"
disc="$4"
package="$5"
out="${DC01_EVIDENCE_DIR:-gc01-device-evidence}"
mkdir -p "$out"

fail() { echo "GC-01 FAIL: $*" >&2; exit 1; }
command -v adb >/dev/null || fail "adb missing"
[[ -s "$apk" && -s "$core" && -d "$game_dir" ]] || fail "missing APK, Dolphin core or fixture folder"
[[ "$disc" =~ ^[a-zA-Z0-9][a-zA-Z0-9._\ -]{0,180}\.(gcm|GCM|iso|ISO)$ ]] || fail "disc filename must be a simple local GCM or ISO name"
[[ -s "$game_dir/$disc" ]] || fail "disc descriptor/content missing"
[[ "$package" =~ ^[a-zA-Z][a-zA-Z0-9_.]+$ ]] || fail "invalid Android package"
if find "$game_dir" -type l -print -quit | grep -q .; then
  fail "symlinks in fixture folder are not permitted"
fi
printf '%s\n' 'PRIVATE LAB ONLY: no third-party binary, firmware or copyrighted ROM upload.' > "$out/scope.txt"
sha256sum "$apk" "$core" > "$out/input-sha256.txt"
adb get-state | grep -qx device || fail "one physical Android device must be connected"
abi="$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
printf '%s\n' "$abi" > "$out/device-abi.txt"
[[ "$abi" == "arm64-v8a" ]] || fail "this GC-01 Dolphin core requires arm64-v8a; got $abi"
adb shell getconf PAGESIZE | tr -d '\r' > "$out/page-size.txt"

adb install -r "$apk" > "$out/install.txt" 2>&1 || fail "APK install failed"
grep -qx 'Success' "$out/install.txt" || fail "adb install did not report success"
adb shell run-as "$package" mkdir -p files/gamecube-lab/game files/gamecube-lab/system files/gamecube-lab/saves ||
  fail "run-as denied; verify debug application ID/signature"
# The package's private files are staged from the developer workstation only.
adb shell -T run-as "$package" sh -c 'cat > files/gamecube-lab/dolphin_libretro.so' < "$core" ||
  fail "core staging failed"
# Relative-path archive only; no copyrighted fixture enters repository or hosted CI.
(cd "$game_dir" && tar -cf - .) |
  adb shell -T run-as "$package" sh -c 'cd files/gamecube-lab/game && tar -xf -' ||
  fail "disc staging failed"
adb logcat -c
component="$package/com.swordfish.lemuroid.app.debug.DreamcastRuntimeProbeActivity"
adb shell am start -W -n "$component" --es disc "$disc" > "$out/start.txt" 2>&1 ||
  fail "activity did not start"
grep -q 'Status: ok' "$out/start.txt" || fail "Android activity reported unsuccessful launch"
sleep 15
adb logcat -d -v threadtime > "$out/logcat.txt"
grep -F 'CARTDECK_GC01' "$out/logcat.txt" > "$out/gc01-filtered.log" || true
adb exec-out screencap -p > "$out/screenshot.png"
[[ -s "$out/screenshot.png" ]] || fail "screenshot missing"
if grep -Eq 'DC01_BLOCKED|LIBRETRODROID_INIT_FAILED' "$out/gc01-filtered.log"; then
  fail "native core not initialised; inspect $out/gc01-filtered.log"
fi
if ! grep -q 'FRAME_RENDERED' "$out/gc01-filtered.log"; then
  echo "GC-01 NOT PROVEN: no native frame event; inspect logcat/EGL, test fixture and screen." >&2
  exit 1
fi
echo "GC-01 FRAME EVENT DETECTED (NOT independent visual/audio/VMU acceptance)." | tee "$out/result.txt"
echo "Now inspect screenshot, actual moving video, audible output and controller input on-device."
echo "Close app, restart and re-run to inspect clean native unload/reload; preserve files/gamecube-lab/saves."
