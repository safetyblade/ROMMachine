#!/usr/bin/env python3
"""Resolve CARTDECK native inputs by immutable component identity.

Receipt format is versioned and fail-closed. Artifact directories are supplied by
an authenticated workflow; this resolver does not trust filenames or GitHub cache
hits as evidence of compatibility.
"""
import argparse
import hashlib
import json
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
    if value["abi"] != "arm64-v8a":
        raise ValueError("unsupported native ABI")
    if type(value["api_level"]) is not int or value["api_level"] < 21:
        raise ValueError("invalid native API level")
    return {key: value[key] for key in REQUIRED_IDENTITY}


def resolve_component(required, artifact_dir):
    """Return the verified files for exactly one required component."""
    expected = validate_identity(required)
    directory = Path(artifact_dir)
    receipt_path = directory / "receipt.json"
    if not receipt_path.is_file() or receipt_path.is_symlink():
        raise ValueError("missing signed-off provenance receipt")
    receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
    if receipt.get("schema") != SCHEMA or receipt.get("identity") != expected:
        raise ValueError("native build identity mismatch")
    notices = receipt.get("licence_notices")
    if not isinstance(notices, str) or not notices:
        raise ValueError("missing licence notices reference")
    outputs = receipt.get("outputs")
    if not isinstance(outputs, dict) or not outputs:
        raise ValueError("missing native output checksum inventory")
    validated = {}
    for rel, checksum in outputs.items():
        if not isinstance(rel, str) or not isinstance(checksum, str) or len(checksum) != 64:
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
        except (ValueError, OSError, json.JSONDecodeError) as exc:
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
