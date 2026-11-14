#!/usr/bin/env python3
"""Verify the actual built CARTDECK PS2 lab APK bytes before publishing artifact.

Never equate packaging/ABI success to real PS2 gameplay.
"""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

EXPECTED_PACKAGE = "dev.prestwich.cartdeck.ps2lab.debug"


def digest(payload):
    return hashlib.sha256(payload).hexdigest()


def verify(apk, bundle, badging):
    apk, bundle = Path(apk), Path(bundle)
    if EXPECTED_PACKAGE not in badging:
        raise ValueError("Private lab package ID not found; refuse replacing CARTDECK")
    meta = json.loads((bundle / "ps2-native-bundle-evidence.json").read_text(encoding="utf-8"))
    if meta["result"] != "PRIVATE_DEBUG_PAYLOAD_STAGED_ONLY":
        raise ValueError("Native source proof not present")
    if meta["apk_built"] or meta["device_gameplay_verified"]:
        raise ValueError("Unexpected pre-claimed APK or gameplay success")
    source = meta["libraries"]
    if not apk.is_file() or not zipfile.is_zipfile(apk):
        raise ValueError("Installable ZIP/APK does not exist")
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        unexpected_abi = [n for n in names if n.startswith("lib/") and not n.startswith("lib/arm64-v8a/")]
        if unexpected_abi:
            raise ValueError("Non-ARM64 native ELF packaged: " + str(unexpected_abi[:4]))
        for name, sha in source.items():
            path = "lib/arm64-v8a/" + name
            if path not in names:
                raise ValueError("Missing verified native dependency in APK: " + path)
            if digest(archive.read(path)) != sha:
                raise ValueError("Packaged native ELF changed from source-verified file: " + name)
    return {
        "result": "SOURCE_VERIFIED_PRIVATE_PS2_DEBUG_APK",
        "package_name": EXPECTED_PACKAGE,
        "apk_sha256": digest(apk.read_bytes()),
        "apk_size_bytes": apk.stat().st_size,
        "host_page_size": meta["host_page_size"],
        "native_libraries": source,
        "apk_packaged": True,
        "native_load_on_device_verified": False,
        "bios_resources_game_boot_verified": False,
        "gameplay_audio_input_saves_verified": False,
        "public_release_approved": False,
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--apk", required=True)
    p.add_argument("--bundle", required=True)
    p.add_argument("--aapt-badging", required=True)
    p.add_argument("--report", required=True)
    args = p.parse_args()
    badging = Path(args.aapt_badging).read_text(encoding="utf-8")
    outcome = verify(args.apk, args.bundle, badging)
    Path(args.report).write_text(json.dumps(outcome, indent=2, sort_keys=True) + "\n")
    print("PS2 native APK bytes verified; device launch and gameplay remain UNVERIFIED.")


if __name__ == "__main__":
    main()
