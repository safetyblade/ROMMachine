"""Bind the bundled Citra binary to the checked-out legacy package, not a guessed upstream source."""
import hashlib
import json
import pathlib
import struct
import subprocess
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SUBMODULE = ROOT / "lemuroid-cores"
CORE_PATH = "lemuroid_core_citra/src/main/jniLibs/arm64-v8a/libcitra_libretro_android.so"
APK_MEMBER = "lib/arm64-v8a/libcitra_libretro_android.so"


def git(directory, *args):
    return subprocess.check_output(["git", "-C", str(directory), *args], text=True).strip()


def details(apk_bytes, package_bytes, expected_blob):
    actual_blob = hashlib.sha1(b"blob " + str(len(package_bytes)).encode() + b"\0" + package_bytes).hexdigest()
    if actual_blob != expected_blob or package_bytes != apk_bytes:
        raise ValueError("Citra packaged native binary does not match the exact checked-out legacy blob")
    if len(apk_bytes) < 64 or apk_bytes[:6] != b"\x7fELF\x02\x01":
        raise ValueError("Citra must be a 64-bit little-endian ELF")
    if struct.unpack_from("<H", apk_bytes, 18)[0] != 183:
        raise ValueError("Citra must target AArch64")
    offset = struct.unpack_from("<Q", apk_bytes, 32)[0]
    entry_size, count = struct.unpack_from("<HH", apk_bytes, 54)
    if entry_size < 56 or offset + entry_size * count > len(apk_bytes):
        raise ValueError("Invalid ELF program header table")
    alignments = [
        struct.unpack_from("<Q", apk_bytes, offset + entry_size * i + 48)[0]
        for i in range(count)
        if struct.unpack_from("<I", apk_bytes, offset + entry_size * i)[0] == 1
    ]
    if not alignments:
        raise ValueError("No ELF loadable segments")
    return {
        "core_sha256": hashlib.sha256(apk_bytes).hexdigest(),
        "package_git_blob": actual_blob,
        "core_bytes": len(apk_bytes),
        "abi": "arm64-v8a",
        "load_segment_alignments": alignments,
        "all_load_segments_16k_alignment": all(x >= 16384 for x in alignments),
        "upstream_source_revision": None,
    }


def main():
    pin = git(ROOT, "rev-parse", "HEAD:lemuroid-cores")
    if git(SUBMODULE, "rev-parse", "HEAD") != pin:
        raise ValueError("LemuroidCores checkout does not match repository gitlink")
    blob = git(SUBMODULE, "rev-parse", "HEAD:" + CORE_PATH)
    with zipfile.ZipFile(sys.argv[1]) as apk:
        if apk.namelist().count(APK_MEMBER) != 1:
            raise ValueError("Expected exactly one ARM64 Citra member")
        evidence = details(apk.read(APK_MEMBER), (SUBMODULE / CORE_PATH).read_bytes(), blob)
    evidence["legacy_package_revision"] = pin
    evidence["scope"] = "packaged binary identity; not upstream source or gameplay verification"
    print(json.dumps(evidence, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
