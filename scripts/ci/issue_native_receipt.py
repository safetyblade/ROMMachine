#!/usr/bin/env python3
"""Write a reproducible CI-01 receipt for a verified single native build.

Receipts are provenance records, not proof of distribution permission. The
consumer must independently validate the identity and all output checksums.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

from resolve_native_artifacts import REQUIRED_IDENTITY, resolve_component


def sha(data):
    return hashlib.sha256(data).hexdigest()


def fingerprint(files):
    h = hashlib.sha256()
    for file in sorted(files, key=lambda value: str(value)):
        path = Path(file)
        if not path.is_file() or path.is_symlink():
            raise ValueError("missing or symlinked identity input: " + str(path))
        h.update(str(path.as_posix()).encode("utf-8") + b"\\0")
        h.update(bytes.fromhex(sha(path.read_bytes())))
    return h.hexdigest()


def issue_receipt(output, engine, version, source_commit, build_script,
                  patch_files, ndk_version, api_level, flags, notices):
    output = Path(output)
    if not re.fullmatch(r"[0-9a-f]{40}", source_commit):
        raise ValueError("source commit must be exact SHA-1")
    if not output.is_dir() or not output.resolve().is_dir():
        raise ValueError("native build output missing")
    notice = Path(notices)
    if notice.is_absolute() or ".." in notice.parts or str(notice) == ".":
        raise ValueError("unsafe licence-notice path")
    if not (output / notice).is_file():
        raise ValueError("source licence notices missing from output")
    script_hash = fingerprint([Path(build_script)])
    patch_hash = fingerprint([Path(p) for p in patch_files]) if patch_files else sha(b"")
    identity = {
        "engine": engine, "engine_version": version, "source_commit": source_commit,
        "patch_sha256": patch_hash, "build_script_sha256": script_hash,
        "ndk_version": ndk_version, "abi": "arm64-v8a",
        "api_level": api_level, "build_configuration": "script-defined",
        "compiler_flags_sha256": sha(flags.encode("utf-8")),
    }
    if set(identity) != set(REQUIRED_IDENTITY):
        raise ValueError("receipt identity schema drift")
    outputs = {}
    for path in sorted(output.rglob("*")):
        if path.is_symlink():
            raise ValueError("native build output contains symlink: " + str(path))
        if path.is_file():
            relative = path.relative_to(output).as_posix()
            if relative == "receipt.json":
                raise ValueError("refusing to overwrite existing receipt")
            outputs[relative] = sha(path.read_bytes())
    if not outputs or not any(key.endswith((".so", ".aar")) for key in outputs):
        raise ValueError("missing actual native engine library or verified host AAR")
    receipt = {"schema": 1, "identity": identity,
               "licence_notices": notice.as_posix(), "outputs": outputs}
    (output / "receipt.json").write_text(json.dumps(receipt, sort_keys=True, indent=2) + "\n")
    resolve_component(identity, output)
    return receipt


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--output", required=True)
    p.add_argument("--engine", required=True)
    p.add_argument("--engine-version", required=True)
    p.add_argument("--source-commit", required=True)
    p.add_argument("--build-script", required=True)
    p.add_argument("--patch", action="append", default=[])
    p.add_argument("--ndk-version", required=True)
    p.add_argument("--api-level", type=int, required=True)
    p.add_argument("--compiler-flags", required=True)
    p.add_argument("--licence-notices", required=True)
    a = p.parse_args()
    issue_receipt(a.output, a.engine, a.engine_version, a.source_commit,
                  a.build_script, a.patch, a.ndk_version, a.api_level,
                  a.compiler_flags, a.licence_notices)


if __name__ == "__main__":
    main()
