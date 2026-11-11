#!/usr/bin/env bash
# Shared by all development APK workflows: builds from different workflows must
# still install over each other while keeping the established package and key.
set -euo pipefail
code=$(( $(date -u +%s) - 1704067200 + 10000 ))
(( code > 2232 && code <= 2100000000 ))
printf '%s\n' "$code"
