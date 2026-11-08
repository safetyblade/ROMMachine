#!/usr/bin/env bash
# Install the exact wrapper distribution with verification and bounded download retries.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROPERTIES="$ROOT/gradle/wrapper/gradle-wrapper.properties"
url="$(sed -n 's/^distributionUrl=//p' "$PROPERTIES")"
url="${url//\\:/:}"
checksum="$(sed -n 's/^distributionSha256Sum=//p' "$PROPERTIES")"
if [[ ! "$url" =~ ^https://(services|downloads)\.gradle\.org/distributions/gradle-([0-9.]+)-bin\.zip$ ]]; then
  echo 'Unsupported wrapper URL; use the official pinned Gradle binary distribution.' >&2
  exit 1
fi
version="${BASH_REMATCH[2]}"
[[ "$checksum" =~ ^[0-9a-f]{64}$ ]] || { echo 'Missing pinned distribution SHA-256.' >&2; exit 1; }
cache="${CARTDECK_GRADLE_CACHE:-${RUNNER_TEMP:-/tmp}/cartdeck-gradle}"
mkdir -p "$cache"
archive="$cache/gradle-$version-bin.zip"
verify() { printf '%s  %s\n' "$checksum" "$1" | sha256sum --check --status; }
if [[ ! -f "$archive" ]] || ! verify "$archive"; then
  partial="$archive.part"
  trap 'rm -f "$partial"' EXIT
  curl --fail --location --silent --show-error \
    --retry 4 --retry-all-errors --retry-delay 3 \
    --connect-timeout 15 --max-time 180 "$url" -o "$partial"
  verify "$partial" || { echo 'Gradle checksum mismatch; refusing this download.' >&2; exit 1; }
  mv "$partial" "$archive"
  trap - EXIT
fi
# Re-extract the verified archive so an old cache cannot substitute executable contents.
unzip -oq "$archive" -d "$cache"
binary="$cache/gradle-$version/bin/gradle"
test -x "$binary"
if [[ -n "${GITHUB_ENV:-}" ]]; then
  printf 'GRADLE_BIN=%s\n' "$binary" >> "$GITHUB_ENV"
fi
"$binary" --version
