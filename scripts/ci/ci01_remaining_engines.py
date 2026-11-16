#!/usr/bin/env python3
"""Package and verify CI-01 remaining five native components independently.

Never upload source checkout/build trees, firmware, games, user data or symbols.
The existing source-specific staging checks still run before APK publication.
"""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path

from issue_native_receipt import issue_receipt, fingerprint
from resolve_native_artifacts import resolve_component

SPECS = {
    "hakux": dict(sha="a7cdb5a975e31730d488c47f6f10440b16c1e6f4", script="build_hakux_native.py", api=28, version="hakux-android"),
    "xenia": dict(sha="779680ade075d2d2a4c589c6a8d57a8e26c116c0", script="build_xbox360_native.py", api=29, version="xenia-arm64-vulkan"),
    "armsx2": dict(sha="7a999782377e61fb96eca65e476b5ec0d182058e", script="ps2_next_armsx2.py", api=28, version="armsx2-dual-page"),
    "cemu": dict(sha="444c585202302a7f4b319bafb4cf72156a8cac8b", script="build_wiiu_cemu_arm64.sh", api=28, version="cemu-vulkan"),
    "libretrodroid": dict(sha="8835c3098514390a271e36983957f7bb5f40abf1", script="build_libretrodroid_host.sh", api=21, version="0.14.0-cartdeck3"),
}
EXTRA_SCRIPTS = {
    "hakux": ["scripts/ci/build_hakux_native.sh", "scripts/ci/verify_hakux_native.py"],
    "xenia": ["scripts/ci/xbox360_native_abi.py", "scripts/ci/verify_gamecube_core.py"],
    "armsx2": ["scripts/ci/package_ps2_runtime_support.py",
               "scripts/ci/ps2_rust_notices.py", "scripts/ci/ps2_sdl_contract.py"],
    "cemu": ["scripts/ci/wiiu_notices.py", "scripts/ci/wiiu_cemu_contract.py",
             "scripts/ci/wiiu_cemu_contract.py", "scripts/ci/rebuild_wiiu_host.py"],
    "libretrodroid": ["scripts/ci/native_symbols.py"],
}


def build_recipe_files(engine):
    spec = SPECS[engine]
    scripts = ["scripts/ci/" + spec["script"],
               "scripts/ci/ci01_remaining_engines.py", "scripts/ci/issue_native_receipt.py"]
    scripts.extend(EXTRA_SCRIPTS.get(engine, []))
    if engine == "xenia":
        scripts.extend(p.as_posix() for p in sorted(Path("scripts/ci/xbox360").rglob("*")) if p.is_file())
    if engine == "cemu":
        scripts.extend(p.as_posix() for p in sorted(Path("spikes/wiiu/vulkan-host").rglob("*")) if p.is_file())
    return sorted(set(scripts))

BUILD_FLAGS = {
    "hakux": "cmake;ANDROID_ABI=arm64-v8a;ANDROID_PLATFORM=android-28;ANDROID_STL=c++_static;CMAKE_BUILD_TYPE=Release;max-page-size=16384",
    "xenia": "Android ARM64 Vulkan;api=29;NDK=29.0.14206865;strip=llvm-strip",
    "armsx2": "Android ARM64;page-sizes=4096,16384;rust-linker=16KiB;source-pinned shaderc",
    "cemu": "ENABLE_LIBRETRO=ON;ENABLE_VULKAN=ON;ANDROID_ABI=arm64-v8a;ANDROID_PLATFORM=android-28;CMAKE_BUILD_TYPE=Release;16KiB",
    "libretrodroid": "arm64-v8a;compileSdk=35;ndkVersion=29.0.14206865;cFlags=-g;cppFlags=-g;publishRelease",
}
PATCHES = {
    "hakux": [
        "hakux-libslirp-4.7-ssize.patch", "hakux-vulkan-loader-version.patch",
        "hakux-confirmed-presentation.patch", "hakux-gpu-report.patch",
        "hakux-presentation-diagnostics.patch", "hakux-pixel-stage-probe.patch",
        "hakux-guest-publication-recovery.patch", "hakux-guest-submission-progress.patch"
    ],
    "libretrodroid": [
        "libretrodroid-gles-context-validation.patch", "libretrodroid-frontend-gl-state.patch",
        "libretrodroid-graphics-error.patch", "libretrodroid-core-frame-signal.patch",
        "libretrodroid-play-vm-bridge.patch"
    ],
}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def copy_file(source, dest, relative):
    origin = source / relative
    if origin.is_symlink() or not origin.is_file():
        raise ValueError("missing or symlinked verified native input: " + relative)
    target = dest / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(origin, target)


def prepare(engine, source, dest):
    if engine not in SPECS:
        raise ValueError("unsupported component")
    source, dest = Path(source), Path(dest)
    if dest.exists():
        raise ValueError("fresh native output directory required")
    spec = SPECS[engine]
    if not source.is_dir():
        raise ValueError("missing native build output")
    entries = []
    if engine == "hakux":
        props = dict(line.split("=", 1) for line in (source / "hakux.properties").read_text().splitlines() if "=" in line)
        if props.get("revision") != spec["sha"]:
            raise ValueError("hakuX source pin mismatch")
        entries = [p.name for p in source.iterdir() if p.is_file() and p.suffix in (".so", ".zip", ".json", ".properties", ".txt", ".xz")]
        for required in ("libhakuxcore.so", "hakux.properties", "hakux-licenses.zip", "source-manifest.json", "sha256.txt"):
            if required not in entries:
                raise ValueError("hakuX missing " + required)
    elif engine == "xenia":
        props = dict(line.split("=", 1) for line in (source / "xenia.properties").read_text().splitlines() if "=" in line)
        if props.get("revision") != spec["sha"]:
            raise ValueError("Xenia source pin mismatch")
        entries = [p.name for p in source.iterdir() if p.is_file() and p.suffix in (".so", ".zip", ".json", ".properties", ".txt")]
        for required in ("libcartdeck_xenia.so", "xenia-licenses.zip", "xenia.properties", "source-manifest.json", "sha256.txt"):
            if required not in entries:
                raise ValueError("Xenia missing " + required)
    elif engine == "armsx2":
        proof = json.loads((source / "native-evidence.json").read_text())
        if proof.get("source_sha") != spec["sha"] or proof.get("result") != "NATIVE_SOURCE_BUILD_VERIFIED_ONLY":
            raise ValueError("ARMSX2 source proof mismatch")
        if sorted(x.get("host_page_size") for x in proof.get("artifacts", [])) != [4096, 16384]:
            raise ValueError("ARMSX2 requires both page models")
        entries = ["native-evidence.json", "armsx2-licenses.zip", "armsx2-resources.zip"]
        for page in (4096, 16384):
            folder = source / "verified" / ("page-" + str(page))
            if not folder.is_dir():
                raise ValueError("ARMSX2 page directory missing")
            entries.extend(p.relative_to(source).as_posix() for p in folder.iterdir() if p.is_file())
        if not any(p.endswith(".so") for p in entries):
            raise ValueError("ARMSX2 binary closure missing")
    elif engine == "cemu":
        proof = json.loads((source / "native-evidence.json").read_text())
        if proof.get("source", {}).get("revision") != spec["sha"] or proof.get("native_abi_pass") is not True:
            raise ValueError("Cemu source proof mismatch")
        entries = ["native-evidence.json", "cemu-licenses.zip",
                   "cemu_libretro_android_arm64.so", "cartdeck_wiiu_vulkan_host_arm64.so",
                   "source-patches.json"]
        if sha(source / "cemu_libretro_android_arm64.so") != proof.get("core_sha256"):
            raise ValueError("Cemu core evidence checksum mismatch")
        if sha(source / "cartdeck_wiiu_vulkan_host_arm64.so") != proof.get("host_sha256"):
            raise ValueError("Cemu host evidence checksum mismatch")
    else:
        relative = "maven/com/github/Swordfish90/LibretroDroid/0.14.0-cartdeck3/LibretroDroid-0.14.0-cartdeck3.aar"
        entries = [relative, "provenance.txt"]
        src = source / "source"
        notice = next((p for p in ("LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING")
                       if (src / p).is_file()), None)
        if not notice:
            raise ValueError("LibretroDroid pinned source licence missing")
        entries.append("source/" + notice)
        if "revision=" + spec["sha"] not in (source / "provenance.txt").read_text():
            raise ValueError("LibretroDroid provenance pin mismatch")
    for entry in entries:
        copy_file(source, dest, entry)
    if engine in ("armsx2", "cemu"):
        notice = "armsx2-licenses.zip" if engine == "armsx2" else "cemu-licenses.zip"
    elif engine == "hakux":
        notice = "hakux-licenses.zip"
    elif engine == "xenia":
        notice = "xenia-licenses.zip"
    else:
        notice = next(s for s in entries if s.startswith("source/"))
    return notice


def expected(engine):
    spec = SPECS[engine]
    scripts = build_recipe_files(engine)
    patches = ["scripts/ci/patches/" + p for p in PATCHES.get(engine, [])]
    # Dependencies of a component are part of its immutable recipe.
    return dict(engine=engine, engine_version=spec["version"],
                source_commit=spec["sha"], patch_sha256=fingerprint([Path(p) for p in patches])
                if patches else hashlib.sha256(b"").hexdigest(),
                build_script_sha256=fingerprint([Path(p) for p in scripts]),
                ndk_version="29.0.14206865", abi="arm64-v8a", api_level=spec["api"],
                build_configuration="script-defined",
                compiler_flags_sha256=hashlib.sha256(
                    BUILD_FLAGS[engine].encode()).hexdigest())


def verify(engine, directory):
    directory = Path(directory)
    identity = expected(engine)
    files = resolve_component(identity, directory)
    required = {
        "hakux": ["libhakuxcore.so", "hakux.properties", "hakux-licenses.zip"],
        "xenia": ["libcartdeck_xenia.so", "xenia.properties", "xenia-licenses.zip"],
        "armsx2": ["native-evidence.json", "armsx2-resources.zip", "armsx2-licenses.zip",
                   "verified/page-4096/libemucore_4k.so",
                   "verified/page-16384/libemucore_16k.so"],
        "cemu": ["cemu_libretro_android_arm64.so", "cartdeck_wiiu_vulkan_host_arm64.so",
                 "native-evidence.json", "cemu-licenses.zip"],
        "libretrodroid": ["maven/com/github/Swordfish90/LibretroDroid/0.14.0-cartdeck3/LibretroDroid-0.14.0-cartdeck3.aar",
                         "provenance.txt"],
    }[engine]
    if any(item not in files for item in required):
        raise ValueError(engine + ": missing required native staging contract")
    return files


def produce(engine, source, dest):
    notice = prepare(engine, source, dest)
    identity = expected(engine)
    scripts = build_recipe_files(engine)
    patches = ["scripts/ci/patches/" + p for p in PATCHES.get(engine, [])]
    # The generic issuer fingerprints one path, so use a stable compound
    # fingerprint from the same source recipe as the consumer.
    from issue_native_receipt import sha as sha_bytes
    from resolve_native_artifacts import resolve_component
    issued = issue_receipt(dest, engine, SPECS[engine]["version"], SPECS[engine]["sha"],
                           scripts[0], patches, "29.0.14206865", SPECS[engine]["api"],
                           BUILD_FLAGS[engine], notice)
    # Replace single-script fingerprint with complete recipe fingerprint;
    # output digests stay immutable and are immediately re-verified.
    issued["identity"]["build_script_sha256"] = identity["build_script_sha256"]
    (Path(dest) / "receipt.json").write_text(json.dumps(issued, sort_keys=True, indent=2) + "\n")
    verify(engine, dest)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--engine", choices=sorted(SPECS), required=True)
    parser.add_argument("--directory", required=True)
    parser.add_argument("--source")
    args = parser.parse_args()
    if args.source:
        produce(args.engine, args.source, args.directory)
    else:
        verify(args.engine, args.directory)
    print(json.dumps({"component": args.engine, "provenance_verified": True}))


if __name__ == "__main__":
    main()
