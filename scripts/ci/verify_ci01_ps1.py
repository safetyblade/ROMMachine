#!/usr/bin/env python3
"""Verify a downloaded CI-01 PS1 artifact against current pinned source inputs."""
import argparse
import hashlib
import json
from pathlib import Path

from issue_native_receipt import fingerprint
from ci01_legacy_engines import SPECS
from resolve_native_artifacts import resolve_component

SOURCE_COMMIT, _, _, _, CORE, _, _, _, FLAGS = SPECS["ps1"]


def expected_identity(script=Path("scripts/ci/build_ps1_upstream_core.sh")):
    return {
        "engine": "ps1",
        "engine_version": "pcsx-rearmed-modern",
        "source_commit": SOURCE_COMMIT,
        "patch_sha256": hashlib.sha256(b"").hexdigest(),
        "build_script_sha256": fingerprint([Path(script)]),
        "ndk_version": "29.0.14206865",
        "abi": "arm64-v8a",
        "api_level": 24,
        "build_configuration": "script-defined",
        "compiler_flags_sha256": hashlib.sha256(FLAGS.encode()).hexdigest(),
    }


def verify(directory, script=Path("scripts/ci/build_ps1_upstream_core.sh")):
    directory = Path(directory)
    required = expected_identity(script)
    outputs = resolve_component(required, directory)
    if CORE not in outputs:
        raise ValueError("verified PS1 binary absent")
    properties = directory / "pcsx-rearmed-modern.properties"
    if "pcsx-rearmed-modern.properties" not in outputs:
        raise ValueError("PS1 properties not verified")
    props = dict(line.split("=", 1) for line in properties.read_text().splitlines()
                 if "=" in line)
    if props.get("revision") != SOURCE_COMMIT:
        raise ValueError("PS1 source revision differs from current pin")
    if props.get("core.sha256") != hashlib.sha256(Path(outputs[CORE]).read_bytes()).hexdigest():
        raise ValueError("PS1 core properties checksum mismatch")
    return {"engine": "ps1", "verified_files": len(outputs),
            "source_commit": SOURCE_COMMIT, "core_sha256": props["core.sha256"]}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", required=True)
    args = parser.parse_args()
    print(json.dumps(verify(args.directory), indent=2, sort_keys=True))
