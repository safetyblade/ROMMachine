#!/usr/bin/env python3
"""Verify the actual packaged ARM64 DOSBox Pure binary, not merely source registration."""
import argparse
import hashlib
import json
import struct
import zipfile
from verify_legacy_core import elf_abi

ENTRY = "lib/arm64-v8a/libdosbox_pure_libretro_android.so"

def inspect(apk, expected_sha256):
    with zipfile.ZipFile(apk) as archive:
        matches = [i for i in archive.infolist() if i.filename == ENTRY]
        if len(matches) != 1:
            raise ValueError("APK must contain exactly one ARM64 DOSBox Pure core")
        entry = matches[0]
        data = archive.read(entry)
        digest = hashlib.sha256(data).hexdigest()
        result = elf_abi(data)
        if not result.get("valid") or not result.get("all_load_segments_aligned_16kb"):
            raise ValueError("Invalid or incompatible ARM64 ELF")
        if digest != expected_sha256.lower():
            raise ValueError("Packaged core does not match verified source artifact")
        # Uncompressed native libraries must be page-aligned for direct mmap.
        with open(apk, "rb") as source:
            source.seek(entry.header_offset)
            header = source.read(30)
            if header[:4] != b"PK\x03\x04":
                raise ValueError("Invalid local ZIP header")
            name, extra = struct.unpack_from("<HH", header, 26)
        offset = entry.header_offset + 30 + name + extra
        if entry.compress_type == zipfile.ZIP_STORED and offset % 16384:
            raise ValueError("Uncompressed native library is not 16KB ZIP aligned")
        return {"apk_entry": ENTRY, "sha256": digest, "elf": result,
                "zip_offset": offset, "compression": entry.compress_type,
                "guest_execution": "NOT_TESTED"}

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    parser.add_argument("--sha256", required=True)
    args = parser.parse_args()
    print(json.dumps(inspect(args.apk, args.sha256), indent=2))
