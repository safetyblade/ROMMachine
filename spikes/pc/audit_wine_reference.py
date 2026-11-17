#!/usr/bin/env python3
"""Audit exact Winlator references; do not extract or execute binary payloads."""
import hashlib
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PIN = json.loads((ROOT / "pc/wine_reference.json").read_text(encoding="utf-8"))


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], text=True).strip()


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def main():
    root, app = map(pathlib.Path, sys.argv[1:3])
    assert git(root, "rev-parse", "HEAD") == PIN["root"]["commit"]
    assert git(app, "rev-parse", "HEAD") == PIN["app"]["commit"]
    assert git(root, "ls-tree", "HEAD", "app").split()[2] == PIN["app"]["commit"]
    results = {}
    for relative, expected_blob in PIN["assets"].items():
        assert git(app, "hash-object", relative) == expected_blob, relative
        path = app / relative
        listing = subprocess.check_output(["tar", "--zstd", "-tf", str(path)], text=True).splitlines()
        results[relative] = {
            "sha256": digest(path), "size": path.stat().st_size,
            "entries": len(listing),
            "wine_entries": [line for line in listing if "/bin/wine" in line][:25],
            "box64_entries": [line for line in listing if line.endswith("/box64")][:25],
        }
    for relative in PIN["source"]:
        assert (app / relative).is_file(), relative
    print(json.dumps({
        "status": "REFERENCE_AUDIT_ONLY",
        "assets": results,
        "android_execution": "NOT_TESTED",
        "distribution": "NOT_CLEARED",
        "requirements": PIN["requirements"],
    }, indent=2))


if __name__ == "__main__":
    main()
