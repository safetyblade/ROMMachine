#!/usr/bin/env python3
"""Rebuild the CARTDECK frontend; retain the existing pinned Cemu engine artifact."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import urllib.request

PIN = "444c585202302a7f4b319bafb4cf72156a8cac8b"
work = Path("work/connected/cemu")
headers = work / "pinned-headers"
headers.mkdir(parents=True, exist_ok=True)
for name in ("libretro.h", "libretro_vulkan.h"):
    url = f"https://raw.githubusercontent.com/WizzardSK/cemu-libretro/{PIN}/src/libretro/{name}"
    (headers / name).write_bytes(urllib.request.urlopen(url, timeout=120).read())
sdk = Path(os.environ["ANDROID_HOME"])
cmake = sdk / "cmake/3.30.3/bin/cmake"
# Execute the callback ABI regression test on the runner without a physical GPU.
# Limit the NDK include search to Vulkan's own portable headers.
portable = work / "portable-vulkan"
portable.mkdir(exist_ok=True)
for name in ("vulkan", "vk_video"):
    target = portable / name
    if not target.exists():
        target.symlink_to((sdk / "ndk/29.0.14206865/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/include" / name).resolve())
test = work / "vulkan-negotiation-test"
subprocess.run(["c++", "-std=c++17", "-I", str(headers), "-I", str(portable),
    "-I", "spikes/wiiu/vulkan-host", "spikes/wiiu/vulkan-host/tests/VulkanNegotiationTest.cpp",
    "-o", str(test)], check=True)
subprocess.run([str(test.resolve())], check=True)
build = work / "host-rebuild"
subprocess.run([
    str(cmake), "-S", "spikes/wiiu/vulkan-host", "-B", str(build),
    f"-DCMAKE_TOOLCHAIN_FILE={sdk}/ndk/29.0.14206865/build/cmake/android.toolchain.cmake",
    "-DANDROID_ABI=arm64-v8a", "-DANDROID_PLATFORM=android-29",
    "-DANDROID_STL=c++_static", "-DCMAKE_BUILD_TYPE=Release",
    f"-DCEMU_LIBRETRO_HEADERS={headers.resolve()}",
], check=True)
subprocess.run([str(cmake), "--build", str(build), "-j4"], check=True)
host = work / "cartdeck_wiiu_vulkan_host_arm64.so"
host.write_bytes((build / "libcartdeckwiiuvulkanhost.so").read_bytes())
proof_path = work / "native-evidence.json"
proof = json.loads(proof_path.read_text())
assert proof["source"]["revision"] == PIN
proof["host_sha256"] = hashlib.sha256(host.read_bytes()).hexdigest()
proof["adapter_source_revision"] = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
proof["adapter_headers"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in headers.iterdir()}
proof_path.write_text(json.dumps(proof, indent=2) + "\n")
