#!/usr/bin/env bash
# Retry only transient dependency transport errors, never compilation/style/test failures.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"
: "${GRADLE_BIN:?Run bootstrap_gradle.sh first}"
version="${1:?Expected development version code}"
shift
[[ "$version" =~ ^[0-9]+$ ]] && (( version > 2001 && version <= 2100000000 ))
evidence="${CARTDECK_OUTPUT:-dist/cartdeck}/evidence"
mkdir -p "$evidence"
for attempt in 1 2; do
  log="$evidence/build-and-tests-attempt-$attempt.log"
  if "$GRADLE_BIN" ktlintCheck :lemuroid-app:testFreeBundleDebugUnitTest :lemuroid-app:assembleFreeBundleDebug \
    -Pcartdeck.dev.standaloneId=true \
    -Pcartdeck.p0.phoneArm64Only=true \
    -Pcartdeck.p0.testVersionCode="$version" \
    -Pkotlin.compiler.execution.strategy=in-process \
    -Dorg.gradle.internal.http.connectionTimeout=60000 \
    -Dorg.gradle.internal.http.socketTimeout=120000 \
    --no-daemon --no-configuration-cache --max-workers=2 --stacktrace \
    "$@" 2>&1 | tee "$log"; then
    exit 0
  else
    result=$?
  fi
  if [[ "$attempt" -eq 2 ]] || ! grep -Eq \
    'Received status code (429|502|503|504)|Server returned HTTP response code: (429|502|503|504)|Downloading from .* failed: timeout|Could not (GET|HEAD).*timed out' "$log"; then
    exit "$result"
  fi
  echo 'Transient dependency download failed; retrying the same complete checks once.'
  sleep 3
done
