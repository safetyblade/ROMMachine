#!/usr/bin/env python3
"""Preserve the pinned host's generated Maven metadata with its verified AAR."""
import argparse
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

from ci01_remaining_engines import verify

RELATIVE = Path("maven/com/github/Swordfish90/LibretroDroid/0.14.0-cartdeck3")
NAME = "LibretroDroid-0.14.0-cartdeck3"


def check(directory):
    root = Path(directory) / RELATIVE
    pom = root / (NAME + ".pom")
    if not pom.is_file() or pom.is_symlink():
        raise ValueError("libretrodroid: missing generated Maven POM; rebuild host metadata handoff")
    document = ET.parse(pom).getroot()
    namespace = "{http://maven.apache.org/POM/4.0.0}"
    for field, expected in (("groupId", "com.github.Swordfish90"),
                            ("artifactId", "LibretroDroid"), ("version", "0.14.0-cartdeck3"),
                            ("packaging", "aar")):
        if document.findtext(namespace + field) != expected:
            raise ValueError("LibretroDroid Maven coordinate mismatch: " + field)
    module = root / (NAME + ".module")
    if module.exists():
        if module.is_symlink():
            raise ValueError("Symlinked host module metadata")
        data = json.loads(module.read_text())
        component = data["component"]
        if (component["group"], component["module"], component["version"]) != (
                "com.github.Swordfish90", "LibretroDroid", "0.14.0-cartdeck3"):
            raise ValueError("LibretroDroid module coordinate mismatch")
        aar = root / (NAME + ".aar")
        for variant in data["variants"]:
            for output in variant.get("files", []):
                if output["name"] == aar.name:
                    if output.get("sha256") != hashlib.sha256(aar.read_bytes()).hexdigest():
                        raise ValueError("Host module metadata AAR checksum mismatch")


def retain(source, directory):
    directory = Path(directory)
    verify("libretrodroid", directory)
    check(source)
    receipt_path = directory / "receipt.json"
    receipt = json.loads(receipt_path.read_text())
    for suffix in (".pom", ".module"):
        relative = RELATIVE / (NAME + suffix)
        origin, target = Path(source) / relative, directory / relative
        if not origin.exists():
            continue
        if origin.is_symlink() or target.is_symlink():
            raise ValueError("Symlinked Maven metadata")
        if target.exists() and target.read_bytes() != origin.read_bytes():
            raise ValueError("Refusing conflicting Maven metadata")
        shutil.copyfile(origin, target)
        receipt["outputs"][relative.as_posix()] = hashlib.sha256(target.read_bytes()).hexdigest()
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    verify("libretrodroid", directory)
    check(directory)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", required=True)
    parser.add_argument("--source")
    args = parser.parse_args()
    if args.source:
        retain(args.source, args.directory)
    else:
        verify("libretrodroid", args.directory)
        check(args.directory)
    print("LibretroDroid receipt-covered Maven metadata verified")
