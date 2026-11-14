#!/usr/bin/env python3
"""Retain licences from the exact source and installed vcpkg dependencies."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile


def collect(work):
    work = Path(work)
    source = work / "source"
    entries = {}
    for directory in [source, *(source / "dependencies").iterdir()]:
        if not directory.is_dir():
            continue
        for path in directory.iterdir():
            if path.is_file() and path.name.lower().startswith(("license", "copying", "copyright")):
                entries["source/" + str(path.relative_to(source))] = path
    copyrights = sorted((work / "build/vcpkg_installed/arm64-android/share").glob("*/copyright"))
    if not entries or not copyrights:
        raise ValueError("Source or installed native dependency licences missing")
    for path in copyrights:
        entries["vcpkg/" + path.parent.name + "/copyright"] = path
    output = work / "cemu-licenses.zip"
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
        for name, path in sorted(entries.items()):
            archive.writestr(name, path.read_bytes())
        archive.writestr("source/android-manifest-patch.json", (work / "source-patches.json").read_bytes())
        archive.writestr("source/CMakeLists.txt", (source / "CMakeLists.txt").read_bytes())
    proof_path = work / "native-evidence.json"
    proof = json.loads(proof_path.read_text())
    proof["notices_sha256"] = hashlib.sha256(output.read_bytes()).hexdigest()
    proof["notices_files"] = len(entries)
    proof_path.write_text(json.dumps(proof, indent=2, sort_keys=True) + "\n")
    return proof["notices_sha256"]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", required=True)
    print(collect(parser.parse_args().work))
