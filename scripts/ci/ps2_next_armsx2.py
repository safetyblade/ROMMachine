#!/usr/bin/env python3
"""PS2-NEXT-A: build and prove a pinned ARMSX2 Android JNI core (never ship to users).

This lane is intentionally independent of the CARTDECK APK/Play! pipeline.
A successful build is *native feasibility*, not Android startup or gameplay QA.
"""
import argparse
import hashlib
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone

from verify_gamecube_core import verify_android_elf

UPSTREAM = "https://github.com/ARMSX2/ARMSX2.git"
SOURCE_SHA = "7a999782377e61fb96eca65e476b5ec0d182058e"
NDK_REV = "29.0.14206865"
WRAPPER = pathlib.Path("platforms/android/gradlew")
REQUIRED_JNI = (
    "Java_kr_co_iefriends_pcsx2_NativeApp_initialize",
    "Java_kr_co_iefriends_pcsx2_NativeApp_runVMThread",
    "Java_kr_co_iefriends_pcsx2_NativeApp_onNativeSurfaceChanged",
    "Java_kr_co_iefriends_pcsx2_NativeApp_onNativeSurfaceDestroyed",
    "Java_kr_co_iefriends_pcsx2_NativeApp_setPadButton",
    "Java_kr_co_iefriends_pcsx2_NativeApp_shutdown",
)
DENIED_DEPENDENCIES = ("libc.so.6", "libstdc++.so.6", "ld-linux", "discord_partner")


def run(*args, cwd=None):
    command = [str(arg) for arg in args]
    print("+", " ".join(command), flush=True)
    subprocess.run(command, cwd=cwd, check=True)


def output(*args):
    return subprocess.check_output([str(arg) for arg in args], text=True).strip()


def sha256(path):
    with pathlib.Path(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def validate_jni_symbols(symbol_table):
    """Require upstream's actual Android JNI ABI; generic libretro exports do not qualify."""
    exported = {line.split()[-1].split("@", 1)[0] for line in symbol_table.splitlines()
                if " GLOBAL " in line and " UND " not in line and line.split()}
    missing = sorted(set(REQUIRED_JNI) - exported)
    if missing:
        raise ValueError("Missing Android JNI symbols: " + ", ".join(missing))
    return sorted(set(REQUIRED_JNI) & exported)


def validate_native(path, readelf):
    """Fail closed on host ELF, 4K-aligned LOAD, missing JNI, or proprietary linkage."""
    path = pathlib.Path(path)
    if not path.is_file() or path.stat().st_size < 65536:
        raise ValueError("No complete Android ARM64 native engine: " + str(path))
    dynamic = output(readelf, "-dW", path)
    verify_android_elf(path.read_bytes(), dynamic)
    symbols = output(readelf, "--dyn-syms", "-W", path)
    exported = validate_jni_symbols(symbols)
    note = output(readelf, "-nW", path)
    build_id = re.search(r"Build ID:\s*([0-9a-fA-F]+)", note)
    if not build_id:
        raise ValueError("Native ELF build ID missing")
    dependencies = sorted(set(re.findall(r"Shared library:\s*\[([^\]]+)\]", dynamic)))
    if any(bad in dep for dep in dependencies for bad in DENIED_DEPENDENCIES):
        raise ValueError("Non-Android or proprietary library dependency: " + str(dependencies))
    if not dependencies or "libc.so" not in dependencies:
        raise ValueError("Native dependency table is missing Android libc")
    return {
        "sha256": sha256(path),
        "size_bytes": path.stat().st_size,
        "build_id": build_id.group(1).lower(),
        "jni_symbols": exported,
        "needed": dependencies,
        "elf": "AArch64 ET_DYN, little-endian, all PT_LOAD >= 16 KiB",
        "device_tested": False,
        "gameplay_tested": False,
    }


def pinned_checkout(source):
    """Always checkout a full SHA, never a mutable branch or release."""
    source = pathlib.Path(source).resolve()
    if source.exists():
        if not (source / ".git").exists():
            raise ValueError("Source exists without git provenance: " + str(source))
    else:
        source.mkdir(parents=True)
        run("git", "init", "-q", source)
        run("git", "-C", source, "remote", "add", "origin", UPSTREAM)
    actual = output("git", "-C", source, "rev-parse", "HEAD") if (
        source / ".git/HEAD"
    ).is_file() and subprocess.run(
        ["git", "-C", str(source), "rev-parse", "--verify", "HEAD"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    ).returncode == 0 else None
    if actual != SOURCE_SHA:
        run("git", "-C", source, "fetch", "--depth=1", "--no-tags", "origin", SOURCE_SHA)
        run("git", "-C", source, "checkout", "--detach", SOURCE_SHA)
    actual = output("git", "-C", source, "rev-parse", "HEAD")
    if actual != SOURCE_SHA:
        raise ValueError("Source SHA drift: " + actual)
    run("git", "-C", source, "submodule", "update", "--init", "--recursive")
    if output("git", "-C", source, "status", "--porcelain", "--untracked-files=no"):
        raise ValueError("Pinned upstream source has tracked local changes")
    return source


def collect_notices(source):
    """Inventory source licences. This does not itself clear distribution."""
    root = pathlib.Path(source)
    if not (root / "COPYING.GPLv3").is_file():
        raise ValueError("Upstream GPLv3 text unavailable; stop before distribution")
    notices = []
    for path in root.rglob("*"):
        if not path.is_file() or ".git" in path.parts:
            continue
        name = path.name.lower()
        if name.startswith(("license", "licence", "copying", "notice")):
            notices.append(path.relative_to(root).as_posix())
    if not notices:
        raise ValueError("No source notices found")
    return sorted(notices)


def locate_build(source, stem):
    root = source / "platforms/android/app"
    # Gradle's AGP CXX outputs are not stable across plugin versions.
    found = [p for p in root.rglob("lib" + stem + ".so")
             if p.is_file() and (".cxx" in p.parts or "intermediates" in p.parts)]
    if not found:
        raise ValueError("Gradle did not produce lib" + stem + ".so")
    return max(found, key=lambda p: p.stat().st_mtime_ns)


def build_one(source, output_dir, page, readelf):
    if page not in (4096, 16384):
        raise ValueError("Unsupported page model")
    name = "emucore_4k" if page == 4096 else "emucore_16k"
    root = source / "platforms/android"
    if not (source / WRAPPER).is_file():
        raise ValueError("Pinned upstream Android Gradle wrapper missing")
    run("bash", root / "gradlew", ":app:cleanCxx", cwd=root)
    run(
        "bash", root / "gradlew", ":app:externalNativeBuildGithubDebug",
        "-Parmsx2.ndkVersion=" + NDK_REV,
        "-Parmsx2.hostPageSize=0x" + format(page, "x"),
        "-Parmsx2.nativeLibName=" + name,
        "--no-daemon", "--max-workers=2", cwd=root,
    )
    built = locate_build(source, name)
    details = validate_native(built, readelf)
    destination = output_dir / ("lib" + name + ".so")
    if destination.exists():
        raise ValueError("Refusing to overwrite an existing native build")
    shutil.copyfile(built, destination)
    if sha256(destination) != details["sha256"]:
        raise ValueError("Native output checksum changed while copying")
    details["library"] = destination.name
    details["host_page_size"] = page
    return details


def execute(args):
    root = pathlib.Path(args.work).resolve()
    if root.exists() and any(root.iterdir()):
        raise ValueError("Use a new empty work root, never reuse source or previous outputs")
    root.mkdir(parents=True, exist_ok=True)
    sdk = pathlib.Path(os.environ.get("ANDROID_HOME", ""))
    ndk = sdk / "ndk" / NDK_REV
    readelf = ndk / "toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
    if not (ndk / "build/cmake/android.toolchain.cmake").is_file() or not readelf.is_file():
        raise ValueError("Pinned Android NDK " + NDK_REV + " not found in ANDROID_HOME")
    source = pinned_checkout(root / "source")
    submodules = output("git", "-C", source, "submodule", "status", "--recursive")
    notices = collect_notices(source)
    binaries = root / "verified"
    binaries.mkdir()
    report = {
        "result": "NATIVE_BUILD_PENDING",
        "upstream": UPSTREAM,
        "source_sha": SOURCE_SHA,
        "ndk": NDK_REV,
        "submodules": submodules.splitlines(),
        "notices": notices,
        "notice_count": len(notices),
        "artifacts": [],
        "apk_packaged": False,
        "android_runtime_tested": False,
        "gameplay_tested": False,
    }
    sizes = (4096, 16384) if args.pages == "both" else (
        (4096,) if args.pages == "4k" else (16384,)
    )
    for page in sizes:
        report["artifacts"].append(build_one(source, binaries, page, readelf))
    report["result"] = "NATIVE_SOURCE_BUILD_VERIFIED_ONLY"
    report["verified_at_utc"] = datetime.now(timezone.utc).isoformat()
    (root / "native-evidence.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({
        "status": report["result"], "source": SOURCE_SHA,
        "binaries": [{
            "name": item["library"], "sha256": item["sha256"],
            "build_id": item["build_id"],
        } for item in report["artifacts"]],
        "no_apk_or_gameplay_claim": True,
    }, indent=2))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("work", help="New empty work directory; source and artefacts stay isolated")
    parser.add_argument("--pages", choices=("4k", "16k", "both"), default="4k")
    args = parser.parse_args()
    try:
        execute(args)
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print("PS2-NEXT-A FAILED: " + str(error), file=sys.stderr)
        raise SystemExit(1) from error


if __name__ == "__main__":
    main()
