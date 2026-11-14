#!/usr/bin/env bash
# CARTDECK PS2-NEXT: private Samsung device test, NEVER a gameplay auto-PASS.
# Requires owner's own BIOS, lawful PS2 disc and source-reviewed resources.
set -euo pipefail
usage() {
  echo "usage: $0 VERIFIED_PRIVATE_LAB_APK LAWFUL_DISC_ISO OWN_BIOS_FILE LICENSE_REVIEWED_RESOURCES_DIR REPORT_DIR" >&2
  exit 2
}
[[ $# -eq 5 ]] || usage
APK=$1; DISC=$2; BIOS=$3; RESOURCES=$4; REPORT=$5
for f in "$APK" "$DISC" "$BIOS"; do
  [[ -f "$f" && -s "$f" ]] || { echo "Missing real input: $f" >&2; exit 1; }
done
[[ -d "$RESOURCES" && -n "$(find "$RESOURCES" -type f -print -quit)" ]] ||
  { echo "Missing source-reviewed ARMSX2 resources" >&2; exit 1; }
[[ ! -e "$REPORT" ]] || { echo "Report directory exists; refuse overwrite" >&2; exit 1; }
command -v adb >/dev/null
command -v aapt >/dev/null || { echo "Android aapt required to check private package" >&2; exit 1; }
command -v apksigner >/dev/null || { echo "Android apksigner required" >&2; exit 1; }
PACKAGE=dev.prestwich.cartdeck.ps2lab.debug
EXPECTED='package: name='\''dev.prestwich.cartdeck.ps2lab.debug'\'''
[[ "$(adb devices -l | grep -cE $'\tdevice\b' || true)" -eq 1 ]] ||
  { echo "Exactly one authorised ADB device required" >&2; exit 1; }
[[ "$(adb shell getprop ro.product.model | tr -d '\r')" == "SM-S721B" ]] ||
  { echo "Only approved Samsung SM-S721B is in this QA script" >&2; exit 1; }
sdk=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
[[ "$sdk" -ge 29 ]] || { echo "Android SDK <29 unsupported" >&2; exit 1; }
aapt dump badging "$APK" | grep -F "$EXPECTED" >/dev/null ||
  { echo "Not isolated CARTDECK PS2 laboratory package" >&2; exit 1; }
apksigner verify --verbose "$APK" >/dev/null
mkdir -p "$REPORT"
adb shell getprop ro.product.model > "$REPORT/device-model.txt"
adb shell getprop ro.build.version.sdk > "$REPORT/android-api.txt"
sha256sum "$APK" > "$REPORT/private-apk-sha256.txt"
# No user game/BIOS byte streams go into the evidence directory.
adb install -r "$APK"
adb shell run-as "$PACKAGE" id >/dev/null ||
  { echo "Lab APK must be debuggable and accessible via run-as" >&2; exit 1; }
game_id=c3882b50-8b41-47cf-8219-f00b70c79e20
sandbox="files/native/ps2/armsx2/cartridges/$game_id"
adb shell run-as "$PACKAGE" mkdir -p \
  files/ps2-next-lab files/native/ps2/armsx2/bios "$sandbox/resources"
prefix="/data/local/tmp/cartdeck-ps2-probe-$$"
cleanup() { adb shell rm -rf "$prefix" >/dev/null 2>&1 || true; }
trap cleanup EXIT
adb shell mkdir -p "$prefix"
adb push "$DISC" "$prefix/fixture.iso" >/dev/null
adb push "$BIOS" "$prefix/user-bios.bin" >/dev/null
adb push "$RESOURCES/." "$prefix/resources/" >/dev/null
adb shell run-as "$PACKAGE" cp "$prefix/fixture.iso" files/ps2-next-lab/fixture.iso
adb shell run-as "$PACKAGE" cp "$prefix/user-bios.bin" files/native/ps2/armsx2/bios/user-bios.bin
adb shell run-as "$PACKAGE" cp -R "$prefix/resources/." "$sandbox/resources/"
# Remove world-readable staging inputs immediately (cleanup trap also handles failure).
cleanup
trap - EXIT
adb logcat -c
adb shell am start -n "$PACKAGE/com.swordfish.lemuroid.app.debug.Armsx2RuntimeProbeActivity" \
  > "$REPORT/start-result.txt"
# The tester now plays and observes video/audio/input. No automatic pass is possible.
adb shell pidof "$PACKAGE:ps2next" > "$REPORT/native-process-id.txt" || true
adb logcat -d -s CARTDECK_PS2_NEXT:I AndroidRuntime:E DEBUG:E \
  > "$REPORT/sanitized-startup-tags.txt" || true
cat > "$REPORT/qa-checklist.md" <<'CHECKLIST'
# PS2-NEXT owner / Marcus handset QA — UNVERIFIED until witnessed
- [ ] Native JNI library loaded (not only installed)
- [ ] User PS2 BIOS and emulator resources recognised
- [ ] Real Soulcalibur III gameplay beyond logo/core frame
- [ ] Moving game visuals and audible sound
- [ ] Responsive face buttons, D-pad, two analog sticks, L1/R1/L2/R2
- [ ] Real in-game memory-card save and successful re-load after full exit
- [ ] Separate Play! saves unchanged
- [ ] 15-minute stable gameplay, measured emulation speed, frame pace and thermals
- [ ] App backgrounds and foregrounds without SIGSEGV/reboot
- [ ] Marcus independent QA and Daniel licence review
Status: NOT VERIFIED. This script does not tick boxes or claim game success.
CHECKLIST
echo "Device probe launched; evidence in $REPORT. Real gameplay acceptance is still manual."
