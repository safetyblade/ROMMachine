#!/usr/bin/env python3
"""Fail-closed Cemu Android ARM64 Libretro ABI gate; not a gameplay verifier.

WIIU-01 / issue #233: a passing ELF gate does not establish that CARTDECK
can negotiate Cemu's Vulkan context, play games, or restore NAND saves.
"""
import argparse
import hashlib
import json
import pathlib
import re
import subprocess

from verify_gamecube_core import verify_android_elf

UPSTREAM = "https://github.com/WizzardSK/cemu-libretro.git"
SOURCE_SHA = "444c585202302a7f4b319bafb4cf72156a8cac8b"
NDK_VERSION = "29.0.14206865"
REQUIRED_LIBRETRO = frozenset({
    "retro_init", "retro_deinit", "retro_load_game", "retro_unload_game",
    "retro_run", "retro_set_environment", "retro_set_video_refresh",
    "retro_set_audio_sample_batch", "retro_set_input_poll",
    "retro_set_input_state", "retro_get_system_info",
    "retro_get_system_av_info", "retro_set_controller_port_device",
})
FORBIDDEN_LIBRARIES = ("libc.so.6", "libstdc++.so.6", "ld-linux", "libGL.so")
SONAME = re.compile(r"lib[A-Za-z0-9_+.-]+\.so\Z")


CMAKE_SHA256 = "0710017b1bacaad2a2815b0f40c4efa060da67632fcb79529e4006de1bcf197a"
USB_GUARD = 'if (NOT CMAKE_SYSTEM_NAME STREQUAL "iOS" AND NOT CMAKE_SYSTEM_NAME STREQUAL "tvOS" AND NOT WEBOS)'
USB_GUARD_FIXED = 'if ((NOT DEFINED ENABLE_LIBUSB OR ENABLE_LIBUSB) AND NOT CMAKE_SYSTEM_NAME STREQUAL "iOS" AND NOT CMAKE_SYSTEM_NAME STREQUAL "tvOS" AND NOT WEBOS)'


def patch_android_manifest_features(root):
    """Make the dependency manifest respect the existing explicit raw-USB OFF option."""
    path = pathlib.Path(root) / "CMakeLists.txt"
    before = path.read_bytes()
    if hashlib.sha256(before).hexdigest() != CMAKE_SHA256:
        raise ValueError("Pinned Cemu CMake source drift before manifest patch")
    old, new = USB_GUARD.encode(), USB_GUARD_FIXED.encode()
    if before.count(old) != 1:
        raise ValueError("Pinned USB manifest guard missing/duplicated")
    path.write_bytes(before.replace(old, new))
    return {"path": "CMakeLists.txt", "before_sha256": CMAKE_SHA256,
            "after_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "purpose": "Respect explicit ENABLE_LIBUSB=OFF; Android controllers use Libretro"}


def command(*args, cwd=None):
    return subprocess.check_output([str(x) for x in args], cwd=cwd, text=True).strip()


def check_source(folder):
    """Refuse floating branches, incomplete submodules or non-Vulkan source."""
    root = pathlib.Path(folder)
    if command("git", "rev-parse", "HEAD", cwd=root) != SOURCE_SHA:
        raise ValueError("Cemu checkout does not match pinned SHA " + SOURCE_SHA)
    status = command("git", "submodule", "status", "--recursive", cwd=root)
    if not status or any(line[0] in "-+U" for line in status.splitlines()):
        raise ValueError("Cemu recursive submodules are missing or drifting")
    main = (root / "src/libretro/CemuLibretro.cpp").read_text(encoding="utf-8")
    cmake = (root / "CMakeLists.txt").read_text(encoding="utf-8")
    if "RETRO_HW_CONTEXT_VULKAN" not in main or "ENABLE_VULKAN" not in cmake:
        raise ValueError("Pinned Cemu Vulkan/libretro source contract missing")
    if not (root / "src/libretro/libretro_vulkan.h").is_file():
        raise ValueError("Libretro Vulkan context negotiation header missing")
    if "Mozilla Public License" not in (root / "LICENSE.txt").read_text(encoding="utf-8"):
        raise ValueError("MPL source licence not found")
    return {"source": UPSTREAM, "revision": SOURCE_SHA, "submodules": status.splitlines()}


def check_binary(data, dynamic, symbols, notes):
    """Reject an incompatible Android core; never infer gameplay from this."""
    verify_android_elf(data, dynamic)
    exported = {
        line.split()[-1].split("@", 1)[0]
        for line in symbols.splitlines()
        if " GLOBAL " in line and " UND " not in line and line.split()
    }
    missing = sorted(REQUIRED_LIBRETRO - exported)
    if missing:
        raise ValueError("Missing Libretro ABI exports: " + ", ".join(missing))
    dependencies = sorted(set(re.findall(r"Shared library:\s*\[([^\]]+)\]", dynamic)))
    if "libc.so" not in dependencies:
        raise ValueError("Android libc dependency missing")
    for dep in dependencies:
        if not SONAME.fullmatch(dep) or any(bad in dep for bad in FORBIDDEN_LIBRARIES):
            raise ValueError("Non-Android or unsupported desktop dependency: " + dep)
    match = re.search(r"Build ID:\s*([a-fA-F0-9]+)", notes)
    if not match:
        raise ValueError("GNU BuildID absent; cannot match crash symbols")
    return {
        "abi": "arm64-v8a",
        "elf": "AArch64 ET_DYN, 16KiB PT_LOAD",
        "libretro_exports": sorted(REQUIRED_LIBRETRO),
        "needed": dependencies,
        "build_id": match.group(1).lower(),
        "native_abi_pass": True,
        "vulkan_frontend_verified": False,
        "apk_packaged": False,
        "device_gameplay_verified": False,
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--source", required=True, type=pathlib.Path)
    p.add_argument("--binary", required=True, type=pathlib.Path)
    p.add_argument("--readelf", required=True)
    p.add_argument("--report", required=True, type=pathlib.Path)
    a = p.parse_args()
    source = check_source(a.source)
    if not a.binary.is_file() or a.binary.stat().st_size < 65536:
        raise ValueError("Missing substantial real compiled Android Cemu core")
    data = a.binary.read_bytes()
    dynamic = command(a.readelf, "-dW", a.binary)
    symbols = command(a.readelf, "--dyn-syms", "-W", a.binary)
    notes = command(a.readelf, "-nW", a.binary)
    result = check_binary(data, dynamic, symbols, notes)
    patches = a.source.parent / "source-patches.json"
    result.update({
        "source_patches": json.loads(patches.read_text()) if patches.is_file() else [],
        "source": source, "ndk": NDK_VERSION,
        "core_sha256": hashlib.sha256(data).hexdigest(),
        "core_size_bytes": len(data),
        "licence_gate": "pending dependency/SBOM sign-off",
        "known_limitation": "Cemu Android Vulkan Libretro frontend is not yet in CARTDECK",
    })
    a.report.parent.mkdir(parents=True, exist_ok=True)
    a.report.write_text(json.dumps(result, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("Cemu Android core source + ABI checked; Vulkan host and gameplay NOT VERIFIED")


if __name__ == "__main__":
    main()
