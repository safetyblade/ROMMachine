#!/usr/bin/env python3
"""Reuse the exact owner-tested atomic64 core; rebuild only its Android host.

Called after the CI-01 trusted artifact resolver, never from an arbitrary download.
A different core requires a deliberate source/identity review, not silent reuse.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "scripts/ci"))

from resolve_native_artifacts import resolve_component
from wiiu_cemu_contract import require_guest_atomic64, SOURCE_SHA

CORE = "18911ab654df005c7142ff67b3b34e43c1adfe4733c5bba5cc97d09b58a2c48c"
BUILD_ID = "ceed2b7dd2291941a10ce38eb5d99339ed751896"


def stage(source, target):
    source, target = Path(source), Path(target)
    receipt = json.loads((source / "receipt.json").read_text())
    resolve_component(receipt["identity"], source)
    if receipt["identity"]["engine"] != "cemu":
        raise ValueError("not a Cemu producer")
    proof = json.loads((source / "native-evidence.json").read_text())
    require_guest_atomic64(proof)
    core = source / "cemu_libretro_android_arm64.so"
    actual = hashlib.sha256(core.read_bytes()).hexdigest()
    if (actual != CORE or proof["core_sha256"] != CORE or proof["build_id"] != BUILD_ID
            or proof["source"]["revision"] != SOURCE_SHA):
        raise ValueError("owner-tested Cemu core identity mismatch")
    if target.exists():
        raise ValueError("fresh host-only staging directory required")
    target.mkdir(parents=True)
    for name in (core.name, "native-evidence.json", "cemu-licenses.zip", "source-patches.json"):
        shutil.copyfile(source / name, target / name)
    proof["reused_core_receipt_sha256"] = hashlib.sha256((source / "receipt.json").read_bytes()).hexdigest()
    (target / "native-evidence.json").write_text(json.dumps(proof, indent=2) + "\n")


if __name__ == "__main__":
    stage(sys.argv[1], "work/connected/cemu")
    subprocess.run([sys.executable, "scripts/ci/rebuild_wiiu_host.py"], check=True)
