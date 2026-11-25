#!/usr/bin/env python3
"""Resolve CARTDECK native inputs by immutable component identity.

Receipt format is versioned and fail-closed. Artifact directories are supplied by
an authenticated workflow; this resolver does not trust filenames or GitHub cache
hits as evidence of compatibility.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

REQUIRED_IDENTITY = (
    "engine", "engine_version", "source_commit", "patch_sha256",
    "build_script_sha256", "ndk_version", "abi", "api_level",
    "build_configuration", "compiler_flags_sha256",
)
SCHEMA = 1


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def validate_identity(value):
    if not isinstance(value, dict) or any(
        key not in value or value[key] in (None, "") for key in REQUIRED_IDENTITY
    ):
        raise ValueError("incomplete immutable native build identity")
    if not isinstance(value["engine"], str) or not re.fullmatch(r"[a-z0-9][a-z0-9_-]*", value["engine"]):
        raise ValueError("invalid native engine identity")
    if value["abi"] != "arm64-v8a":
        raise ValueError("unsupported native ABI")
    if type(value["api_level"]) is not int or value["api_level"] < 21:
        raise ValueError("invalid native API level")
    return {key: value[key] for key in REQUIRED_IDENTITY}


def resolve_component(required, artifact_dir):
    """Return the verified files for exactly one required component."""
    expected = validate_identity(required)
    directory = Path(artifact_dir)
    if directory.is_symlink() or not directory.is_dir():
        raise ValueError("missing or symlinked native component directory")
    receipt_path = directory / "receipt.json"
    if not receipt_path.is_file() or receipt_path.is_symlink():
        raise ValueError("missing signed-off provenance receipt")
    receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    if not isinstance(receipt, dict) or receipt.get("schema") != SCHEMA:
        raise ValueError("native build identity mismatch: invalid receipt schema")
    supplied = receipt.get("identity")
    if not isinstance(supplied, dict):
        raise ValueError("native build identity mismatch: missing identity fields")
    if supplied != expected:
        changed = [key for key in REQUIRED_IDENTITY if supplied.get(key) != expected[key]]
        if set(supplied) != set(expected):
            changed.append("unexpected/missing fields")
        # Reveal field names only. Never log identities, paths, compiler flags or binary bytes.
        raise ValueError("native build identity mismatch: " + ", ".join(changed))
    notices = receipt.get("licence_notices")
    if not isinstance(notices, str) or not notices:
        raise ValueError("missing licence notices reference")
    outputs = receipt.get("outputs")
    if not isinstance(outputs, dict) or not outputs:
        raise ValueError("missing native output checksum inventory")
    # Downstream staging copies complete directories. Every file it can consume
    # must be covered by the receipt, including assets and dependency libraries.
    actual_files = set()
    for entry in directory.rglob("*"):
        if entry.is_symlink():
            raise ValueError("symlinked native output: " + str(entry))
        if entry.is_file() and entry != receipt_path:
            actual_files.add(entry.relative_to(directory).as_posix())
    if actual_files != set(outputs):
        raise ValueError("native output inventory differs from receipt")
    validated = {}
    for rel, checksum in outputs.items():
        if not isinstance(rel, str) or not isinstance(checksum, str) or not re.fullmatch(r"[0-9a-f]{64}", checksum):
            raise ValueError("invalid native output entry")
        path = Path(rel)
        if path.is_absolute() or ".." in path.parts or str(path) in (".", ""):
            raise ValueError("unsafe native output path")
        actual = directory / path
        if actual.is_symlink() or not actual.is_file() or digest(actual) != checksum:
            raise ValueError("native output checksum mismatch: " + rel)
        validated[rel] = str(actual)
    if notices not in outputs:
        raise ValueError("licence notices must be hashed output")
    return validated


def resolve_inventory(required, root):
    root = Path(root)
    if not isinstance(required, list) or not required:
        raise ValueError("empty native component inventory")
    result = {"verified": {}, "missing_or_invalid": {}}
    engines = set()
    for component in required:
        identity = validate_identity(component)
        engine = identity["engine"]
        if not isinstance(engine, str) or not engine or engine in engines:
            raise ValueError("duplicate/invalid native engine identity")
        engines.add(engine)
        try:
            result["verified"][engine] = resolve_component(identity, root / engine)
        except (ValueError, OSError, UnicodeError) as exc:
            result["missing_or_invalid"][engine] = str(exc)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--requirements", type=Path, required=True)
    parser.add_argument("--artifacts", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    result = resolve_inventory(json.loads(args.requirements.read_text()), args.artifacts)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    if result["missing_or_invalid"]:
        parser.exit(2, "Missing/invalid native components: " +
                    ", ".join(sorted(result["missing_or_invalid"])) + "\n")


if __name__ == "__main__":
    main()
