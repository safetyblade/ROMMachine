#!/usr/bin/env bash
# Rebuild the pinned, in-package SDL3/Vulkan Xbox backend (not a Libretro core).
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
exec python3 "$root/scripts/ci/build_hakux_native.py" "${1:?Expected output directory}"
