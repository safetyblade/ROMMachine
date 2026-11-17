#!/usr/bin/env python3
"""Retain source-compliance archives alongside, and covered by, native receipts."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path

from ci01_remaining_engines import expected, verify

FILES = {
    "hakux": ["source-compliance/hakux-corresponding-source.tar.xz",
              "source-compliance/sha256.txt"],
    "armsx2": ["rust-corresponding-source.tar.xz"],
    "xenia": ["corresponding-source.tar.xz"],
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check(engine, directory):
    directory = Path(directory)
    if directory.is_symlink():
        raise ValueError(engine + ": symlinked source directory")
    for relative in FILES[engine]:
        path = directory / relative
        if (not path.is_file() or path.is_symlink() or
                any((directory / p).is_symlink() for p in Path(relative).parents)):
            raise ValueError(engine + ": missing corresponding source: " + relative)
    if engine == "hakux":
        archive = directory / FILES[engine][0]
        lines = (directory / FILES[engine][1]).read_text().splitlines()
        if lines != [sha(archive) + "  " + archive.name]:
            raise ValueError("hakuX corresponding-source checksum mismatch")
    elif engine == "armsx2":
        proof = json.loads((directory / "native-evidence.json").read_text())
        if sha(directory / FILES[engine][0]) != proof["rust_dependencies"]["corresponding_source_sha256"]:
            raise ValueError("ARMSX2 corresponding-source checksum mismatch")
    else:
        proof = json.loads((directory / "source-manifest.json").read_text())
        if sha(directory / FILES[engine][0]) != proof["source_archive_sha256"]:
            raise ValueError("Xenia corresponding-source checksum mismatch")


def retain(engine, source, directory):
    source, directory = Path(source), Path(directory)
    verify(engine, directory)
    check(engine, source)
    receipt_path = directory / "receipt.json"
    receipt = json.loads(receipt_path.read_text())
    if receipt["identity"] != expected(engine):
        raise ValueError("Native identity changed during source retention")
    for relative in FILES[engine]:
        origin, target = source / relative, directory / relative
        if target.exists():
            if target.is_symlink() or sha(target) != sha(origin):
                raise ValueError("Refusing conflicting source-compliance output: " + relative)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(origin, target)
        receipt["outputs"][relative] = sha(target)
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    verify(engine, directory)
    check(engine, directory)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--engine", choices=FILES, required=True)
    parser.add_argument("--directory", required=True)
    parser.add_argument("--source")
    args = parser.parse_args()
    if args.source:
        retain(args.engine, args.source, args.directory)
    else:
        verify(args.engine, args.directory)
        check(args.engine, args.directory)
    print(args.engine + ": receipt-covered corresponding source verified")
