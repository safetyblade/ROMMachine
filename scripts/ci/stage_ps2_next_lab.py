#!/usr/bin/env python3
"""Stage a source-verified page-specific ARMSX2 JNI payload for private APK QA.

No APK is created here and no game/BIOS is ever shipped. Both the manifest and
source ELF checks must have passed in the same isolated CI run.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil

SOURCE_SHA = "7a999782377e61fb96eca65e476b5ec0d182058e"
NDK = "29.0.14206865"
MAIN_BY_PAGE = {4096: "libemucore_4k.so", 16384: "libemucore_16k.so"}
SAFE_NAME = re.compile(r"lib[A-Za-z0-9_.+-]+\.so\Z")


def file_hash(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for part in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(part)
    return h.hexdigest()


def create_bundle(work, page, bundle):
    work, bundle = Path(work).resolve(), Path(bundle).resolve()
    if bundle.exists():
        raise ValueError("Bundle already exists; refuse stale debug-native payload")
    manifest = json.loads((work / "native-evidence.json").read_text(encoding="utf-8"))
    if manifest.get("result") != "NATIVE_SOURCE_BUILD_VERIFIED_ONLY":
        raise ValueError("No actual verified native ARM64 build evidence")
    if manifest.get("source_sha") != SOURCE_SHA or manifest.get("ndk") != NDK:
        raise ValueError("ARMSX2 native provenance drift")
    if manifest.get("apk_packaged") is not False or manifest.get("gameplay_tested") is not False:
        raise ValueError("Invalid native-only evidence status")
    artifacts = [x for x in manifest.get("artifacts", []) if x.get("host_page_size") == page]
    if len(artifacts) != 1:
        raise ValueError("Required host-page ABI variant missing or duplicated")
    info = artifacts[0]
    expected = {MAIN_BY_PAGE[page]: info.get("sha256")}
    for name, proof in info.get("dependency_closure", {}).items():
        if not SAFE_NAME.fullmatch(name) or not isinstance(proof, dict):
            raise ValueError("Untrusted dependency filename/proof")
        expected[name] = proof.get("sha256")
    if len(expected) < 2:
        raise ValueError("Native library dependency closure absent")
    source_dir = work / "verified" / ("page-" + str(page))
    target_dir = bundle / "jniLibs" / "arm64-v8a"
    staged = []
    for name, expected_hash in sorted(expected.items()):
        if not isinstance(expected_hash, str) or not re.fullmatch("[0-9a-f]{64}", expected_hash):
            raise ValueError("Malformed native SHA-256 for " + name)
        path = source_dir / name
        if not path.is_file() or path.stat().st_size < 65536:
            raise ValueError("Native page-specific dependency missing: " + name)
        if file_hash(path) != expected_hash:
            raise ValueError("Native file hash mismatch: " + name)
        # Create payload only after verifying every input, below.
        staged.append((name, path, expected_hash))
    if set(p.name for p in source_dir.glob("*.so")) != set(expected):
        raise ValueError("Unexpected/missing shared library in native closure")
    target_dir.mkdir(parents=True, exist_ok=False)
    for name, path, _ in staged:
        shutil.copyfile(path, target_dir / name)
    result = {
        "result": "PRIVATE_DEBUG_PAYLOAD_STAGED_ONLY",
        "host_page_size": page,
        "upstream_sha": SOURCE_SHA,
        "ndk": NDK,
        "libraries": {name: digest for name, _, digest in staged},
        "bios_included": False,
        "roms_included": False,
        "apk_built": False,
        "device_gameplay_verified": False,
    }
    (bundle / "ps2-native-bundle-evidence.json").write_text(
        json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8",
    )
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", type=Path, required=True)
    parser.add_argument("--page", type=int, choices=(4096, 16384), required=True)
    parser.add_argument("--bundle", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(create_bundle(args.work, args.page, args.bundle), indent=2))


if __name__ == "__main__":
    main()
