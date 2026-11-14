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
# Source-pinned shaderc DEPS from ARMSX2's vendored shaderc tree.  The
# upstream snapshot contains CMake but omits these source checkouts.
# Tests and unrelated libraries are disabled by upstream Android CMake.
SHADERC_DEPS = {
    "spirv-headers": (
        "https://github.com/KhronosGroup/SPIRV-Headers.git",
        "465055f6c9128772e20082e893d974146acf7a02",
    ),
    "spirv-tools": (
        "https://github.com/KhronosGroup/SPIRV-Tools.git",
        "e4bceacf59fdfe742047e94e41ef65a48999a0dd",
    ),
    "glslang": (
        "https://github.com/KhronosGroup/glslang.git",
        "09c541ee5b22bbac307987b50d86ec2b4f683d75",
    ),
}
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
# Libraries supplied by Android itself, not app-private ELF payloads.
ANDROID_SYSTEM_LIBRARIES = frozenset({
    "libc.so", "libm.so", "libdl.so", "liblog.so", "libandroid.so",
    "libEGL.so", "libGLESv1_CM.so", "libGLESv2.so", "libGLESv3.so", "libvulkan.so",
    "libz.so", "libjnigraphics.so", "libmediandk.so", "libOpenSLES.so",
    "libaaudio.so", "libnativewindow.so", "libbinder_ndk.so",
    "libsync.so",
})
# Upstream Android Java loads these optional UI runtimes by name, not DT_NEEDED.
# They still require actual Android ELF validation before a private APK is built.
UPSTREAM_LOAD_LIBRARY_EXTRAS = ("libflurry.so", "libsavers.so")


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



def needed_libraries(dynamic):
    """Reject non-bare SONAMEs. Android resolves packaged dependencies by filename."""
    names = set(re.findall(r"Shared library:\s*\[([^\]]+)\]", dynamic))
    for name in names:
        if not re.fullmatch(r"lib[A-Za-z0-9_+.-]+\.so", name):
            raise ValueError("Unresolvable native DT_NEEDED SONAME: " + name)
        if any(bad in name for bad in DENIED_DEPENDENCIES):
            raise ValueError("Forbidden desktop/proprietary native dependency: " + name)
    return names


def inspect_dependency(path, readelf):
    """Validate every packaged JNI dependency, not only the main PS2 core."""
    library = pathlib.Path(path)
    if not library.is_file() or library.stat().st_size < 65536:
        raise ValueError("Missing complete native dependency: " + library.name)
    dynamic = output(readelf, "-dW", library)
    verify_android_elf(library.read_bytes(), dynamic)
    notes = output(readelf, "-nW", library)
    build = re.search(r"Build ID:\s*([0-9a-fA-F]+)", notes)
    if not build:
        raise ValueError("Missing ELF build ID: " + library.name)
    return {
        "sha256": sha256(library),
        "build_id": build.group(1).lower(),
        "needed": sorted(needed_libraries(dynamic)),
        "size_bytes": library.stat().st_size,
    }


def stage_dependency_closure(source, ndk, main_binary, destination, readelf):
    """Copy the full source-built Android ELF closure; fail instead of omitting libs."""
    app = pathlib.Path(source) / "platforms/android/app"
    ndk_root = pathlib.Path(ndk)
    target = ndk_root / (
        "toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib"
        "/aarch64-linux-android/libc++_shared.so"
    )
    candidates = {}
    for root in (app / ".cxx", app / "build/intermediates"):
        if root.is_dir():
            for candidate in root.rglob("*.so"):
                if candidate.is_file():
                    candidates.setdefault(candidate.name, []).append(candidate)
    result = {}
    initial = inspect_dependency(main_binary, readelf)["needed"]
    queue = list(set(initial) | set(UPSTREAM_LOAD_LIBRARY_EXTRAS))
    while queue:
        name = queue.pop()
        if name in ANDROID_SYSTEM_LIBRARIES or name in result:
            continue
        if not re.fullmatch(r"lib[A-Za-z0-9_+.-]+\.so", name):
            raise ValueError("Invalid private dependency name: " + name)
        possible = [target] if name == "libc++_shared.so" else candidates.get(name, [])
        possible = [p for p in possible if p.is_file()]
        if not possible:
            raise ValueError("Missing source-built Android ELF dependency: " + name)
        # Different Gradle intermediate paths are possible; use only real AArch64 ELF.
        source_lib = max(possible, key=lambda p: p.stat().st_mtime_ns)
        proof = inspect_dependency(source_lib, readelf)
        installed = destination / name
        if installed.exists():
            raise ValueError("Duplicate private dependency destination: " + name)
        shutil.copyfile(source_lib, installed)
        if sha256(installed) != proof["sha256"]:
            raise ValueError("Native dependency changed in staging: " + name)
        result[name] = proof
        queue.extend(proof["needed"])
    return result


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



def stage_shaderc_dependencies(source):
    """Check and restore exact source commits named by upstream shaderc/DEPS."""
    root = pathlib.Path(source) / "platforms/android/app/src/main/cpp/3rdparty/shaderc"
    deps_file = root / "DEPS"
    if not deps_file.is_file():
        raise ValueError("Pinned upstream shaderc DEPS missing")
    declarations = deps_file.read_text(encoding="utf-8")
    for name, (_, revision) in SHADERC_DEPS.items():
        if revision not in declarations:
            raise ValueError("Upstream shaderc DEPS revision drift for " + name)
    recorded = {}
    for name, (url, revision) in SHADERC_DEPS.items():
        destination = root / "third_party" / name
        if destination.exists() and not (destination / ".git").exists():
            raise ValueError("Unverifiable shaderc dependency directory: " + str(destination))
        if not destination.exists():
            destination.mkdir(parents=True)
            run("git", "init", "-q", destination)
            run("git", "-C", destination, "remote", "add", "origin", url)
            run("git", "-C", destination, "fetch", "--depth=1", "--no-tags", "origin", revision)
            run("git", "-C", destination, "checkout", "--detach", revision)
        head = output("git", "-C", destination, "rev-parse", "HEAD")
        if head != revision:
            raise ValueError("Shaderc dependency source mismatch: " + name + ": " + head)
        recorded[name] = {"revision": revision, "upstream": url}
    return recorded


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


def patch_rust_linker(source):
    """Retain build IDs and 16 KiB alignment in the actual Rust link, not a verifier exception."""
    path = pathlib.Path(source) / (
        "platforms/android/app/src/main/cpp/3rdparty/librashader/CMakeLists.txt"
    )
    before = path.read_bytes()
    old = b'"RUSTFLAGS=-C link-arg=-Wl,-soname,liblibrashader_capi.so"'
    new = (b'"RUSTFLAGS=-C link-arg=-Wl,-soname,liblibrashader_capi.so '
           b'-C link-arg=-Wl,--build-id=sha1 -C link-arg=-Wl,-z,max-page-size=16384"')
    if before.count(old) != 1:
        raise ValueError("Pinned librashader linker flags drift; refuse patch")
    path.write_bytes(before.replace(old, new))
    return {
        "path": path.relative_to(source).as_posix(),
        "before_sha256": hashlib.sha256(before).hexdigest(),
        "after_sha256": sha256(path),
        "purpose": "GNU build ID and 16 KiB Rust ELF alignment",
    }


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
    page_dir = output_dir / ("page-" + str(page))
    page_dir.mkdir(parents=True, exist_ok=False)
    destination = page_dir / ("lib" + name + ".so")
    shutil.copyfile(built, destination)
    if sha256(destination) != details["sha256"]:
        raise ValueError("Native output checksum changed while copying")
    ndk = pathlib.Path(os.environ["ANDROID_HOME"]) / "ndk" / NDK_REV
    details["dependency_closure"] = stage_dependency_closure(
        source, ndk, built, page_dir, readelf,
    )
    details["library"] = destination.relative_to(output_dir).as_posix()
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
    shaderc = stage_shaderc_dependencies(source)
    notices = collect_notices(source)
    rust_linker_patch = patch_rust_linker(source)
    binaries = root / "verified"
    binaries.mkdir()
    report = {
        "result": "NATIVE_BUILD_PENDING",
        "upstream": UPSTREAM,
        "source_sha": SOURCE_SHA,
        "ndk": NDK_REV,
        "submodules": submodules.splitlines(),
        "shaderc_dependencies": shaderc,
        "source_patches": [rust_linker_patch],
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
