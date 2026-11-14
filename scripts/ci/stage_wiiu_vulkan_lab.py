#!/usr/bin/env python3
"""Package pinned Cemu + CARTDECK Vulkan JNI host for a private Wii U lab APK.

Inputs are source-built native CI artifacts from the same commit, not downloads
or a third-party APK. A packaged APK does not demonstrate gameplay.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import re

from verify_gamecube_core import verify_android_elf
from verify_hakux_native import SYSTEM

CEMU_SHA = "444c585202302a7f4b319bafb4cf72156a8cac8b"
INPUTS = {
    "cemu_libretro_android_arm64.so": "libcemu_libretro_android.so",
    "cartdeck_wiiu_vulkan_host_arm64.so": "libcartdeckwiiuvulkanhost.so",
}


def sha(path):
    h = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def stage(work, output, readelf):
    work, output = Path(work), Path(output)
    if output.exists():
        raise ValueError("Output directory exists; refuse reuse")
    proof = json.loads((work / "native-evidence.json").read_text(encoding="utf-8"))
    if proof.get("native_abi_pass") is not True or (
        proof.get("source", {}).get("revision") != CEMU_SHA
    ) or proof.get("vulkan_frontend_verified") is not False:
        raise ValueError("Cemu native proof invalid or not pinned")
    verified = {}
    for source, target in INPUTS.items():
        f = work / source
        if not f.is_file() or f.stat().st_size < 65536:
            raise ValueError("Source-built ARM64 library missing: " + source)
        # The ELF verifier rejects non-Android arch, wrong page alignment and glibc.
        from subprocess import check_output
        dynamic = check_output([str(readelf), "-dW", str(f)], text=True)
        verify_android_elf(f.read_bytes(), dynamic)
        needed = set(re.findall(r'\(NEEDED\).*\[(.*?)\]', dynamic))
        missing = needed - SYSTEM - set(INPUTS.values())
        if missing:
            raise ValueError("Unbundled Wii U native dependencies: " + str(sorted(missing)))
        info = {"sha256": sha(f), "size_bytes": f.stat().st_size}
        key = "core_sha256" if source.startswith("cemu_") else "host_sha256"
        if info["sha256"] != proof.get(key):
            raise ValueError("Native binary SHA differs from evidence: " + source)
        verified[target] = info
    libs = output / "jniLibs/arm64-v8a"
    libs.mkdir(parents=True)
    for source, target in INPUTS.items():
        shutil.copyfile(work / source, libs / target)
        if sha(libs / target) != verified[target]["sha256"]:
            raise ValueError("Staged native ELF byte mismatch: " + target)
    report = {
        "result": "WIIU_SOURCE_VERIFIED_PRIVATE_JNI_BUNDLE_ONLY",
        "cemu_source_sha": CEMU_SHA,
        "libraries": verified,
        "vulkan_device_tested": False,
        "apk_packaged": False,
        "actual_game_frame_verified": False,
        "gameplay_tested": False,
    }
    (output / "wiiu-bundle-evidence.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--work", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--readelf", required=True)
    args = parser.parse_args()
    print(json.dumps(stage(args.work, args.output, args.readelf), indent=2))


if __name__ == "__main__":
    main()
