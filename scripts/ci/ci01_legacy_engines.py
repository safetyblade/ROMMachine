#!/usr/bin/env python3
"""CI-01 verified receipt contract for the five existing Libretro engines.

The pinned source SHA is checked against the engine's own source-produced
properties. This is a private build/provenance contract, not gameplay approval.
"""
import argparse
import hashlib
import json
from pathlib import Path

from issue_native_receipt import issue_receipt
from resolve_native_artifacts import resolve_component

# Explicit pins agree with the existing upstream source build scripts.
SPECS = {
    "ps1": ("7b0e8f8fadfbce3d170d195b9d9eecaabfa08c8e", 24,
            "build_ps1_upstream_core.sh", "pcsx-rearmed-modern.properties",
            "libpcsx_rearmed_modern_libretro_android.so", "PCSX_UPSTREAM_COPYING",
            "pcsx-rearmed-modern", [], "ARCH=arm64;DYNAREC=ari64;BUILTIN_GPU=neon;16KB"),
    "psp": ("fa50bb1976065c4f8b1b47af227d367fe9771555", 24,
            "build_psp_upstream_core.sh", "ppsspp-modern.properties",
            "libppsspp_modern_libretro_android.so", "PPSSPP_UPSTREAM_LICENSE",
            "ppsspp-modern", [], "APP_ABI=arm64-v8a;APP_PLATFORM=android-24;16KB"),
    "dc": ("1b7c7d63b73ea6987fb23f9334a7d597401a50f8", 28,
           "build_dreamcast_upstream_core.sh", "flycast.properties",
           "libflycast_libretro_android.so", "FLYCAST_LICENSE",
           "flycast", [], "GLES3;Vulkan=OFF;android-28;16KB"),
    "gc": ("32c61f9d0e933331bb7087ba57e96883241b4960", 28,
           "build_gamecube_upstream_core.sh", "dolphin-modern.properties",
           "libdolphin_modern_libretro_android.so", "DOLPHIN_COPYING",
           "dolphin-modern", [], "LIBRETRO=ON;android-28;16KB"),
    "ps2": ("1f27232772e484bfb79364b751b84f41f4c87481", 28,
            "build_ps2_upstream_core.sh", "play-modern.properties",
            "libplay_modern_libretro_android.so", "PLAY_LICENSE.txt",
            "play-modern",
            ["scripts/ci/patches/play-android-private-storage-gles.patch",
             "scripts/ci/patches/play-java-vm-host-bridge.patch"],
            "Play!;android-28;16KB"),
}


def property_map(directory, name):
    result = {}
    for line in (directory / name).read_text().splitlines():
        if "=" in line:
            key, val = line.split("=", 1)
            result[key] = val
    return result


def validate_package(engine, directory):
    if engine not in SPECS:
        raise ValueError("unrecognised native engine")
    revision, api, script, props_file, library, notice, version, patches, flags = SPECS[engine]
    directory = Path(directory)
    props = property_map(directory, props_file)
    if props.get("revision", props.get("upstream.revision")) != revision:
        raise ValueError(engine + ": source-provided revision does not match pin")
    if not (directory / library).is_file() or not (directory / notice).is_file():
        raise ValueError(engine + ": missing core or source licence notice")
    actual = hashlib.sha256((directory / library).read_bytes()).hexdigest()
    if props.get("core.sha256") != actual:
        raise ValueError(engine + ": source-produced core hash mismatch")
    if engine in ("psp", "gc"):
        asset = "ppsspp-modern-assets.zip" if engine == "psp" else "dolphin-modern-assets.zip"
        if props.get("assets.sha256") != hashlib.sha256((directory / asset).read_bytes()).hexdigest():
            raise ValueError(engine + ": assets and core build are not matched")
    return (revision, api, "scripts/ci/" + script, notice, version, patches, flags)


def contract(engine, directory, issue=False):
    revision, api, script, notice, version, patches, flags = validate_package(engine, Path(directory))
    if issue:
        issued = issue_receipt(directory, engine, version, revision, script, patches,
                               "29.0.14206865", api, flags, notice)
        expected = issued["identity"]
    else:
        # Recalculate from CURRENT source inputs; never trust identity from receipt alone.
        from issue_native_receipt import fingerprint
        expected = {
            "engine": engine, "engine_version": version,
            "source_commit": revision,
            "patch_sha256": fingerprint([Path(p) for p in patches]) if patches
                            else hashlib.sha256(b"").hexdigest(),
            "build_script_sha256": fingerprint([Path(script)]),
            "ndk_version": "29.0.14206865", "abi": "arm64-v8a",
            "api_level": api, "build_configuration": "script-defined",
            "compiler_flags_sha256": hashlib.sha256(flags.encode()).hexdigest(),
        }
    verified = resolve_component(expected, directory)
    if SPECS[engine][4] not in verified:
        raise ValueError(engine + ": verified receipt omits core")
    return {"engine": engine, "verified_files": len(verified),
            "core_sha256": hashlib.sha256((Path(directory) / SPECS[engine][4]).read_bytes()).hexdigest()}


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--engine", required=True, choices=sorted(SPECS))
    p.add_argument("--directory", required=True)
    p.add_argument("--issue", action="store_true")
    args = p.parse_args()
    print(json.dumps(contract(args.engine, args.directory, args.issue), sort_keys=True))
