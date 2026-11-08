#!/usr/bin/env python3
"""Reproducible read-only inventory of every native ELF file packaged in a debug APK.

P0-A evidence only. Does NOT certify runtime 16 KB compatibility, licenses or Play readiness.
Requires Python 3 and binutils/readelf on the GitHub Actions runner.
"""
import argparse
import csv
import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

PAGE_SIZE = 16 * 1024


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def load_alignment(blob, filename, tempdir):
    path = tempdir / "native-library.so"
    path.write_bytes(blob)
    if not shutil.which("readelf"):
        return [], "READELF_UNAVAILABLE"
    completed = subprocess.run(
        ["readelf", "-lW", str(path)], text=True, capture_output=True, check=False
    )
    if completed.returncode:
        return [], "READELF_FAILED: " + completed.stderr.strip()[:250]
    try:
        alignments = [
            int(line.split()[-1], 16)
            for line in completed.stdout.splitlines()
            if line.strip().startswith("LOAD ")
        ]
    except ValueError:
        return [], "ALIGNMENT_PARSE_FAILED"
    if not alignments:
        return [], "NO_PT_LOAD"
    if min(alignments) < PAGE_SIZE:
        return alignments, "ELF_16KB_FAIL"
    return alignments, "ELF_16KB_PASS"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    apk = args.apk.resolve()
    if not apk.is_file():
        parser.error(f"APK missing: {apk}")

    records = []
    embedded_rom_files = []
    with tempfile.TemporaryDirectory(prefix="cartdeck-p0-") as scratch:
        with zipfile.ZipFile(apk) as archive:
            for member in sorted(archive.infolist(), key=lambda item: item.filename):
                if member.is_dir():
                    continue
                suffix = Path(member.filename).suffix.lower()
                if suffix in {".gb", ".gbc", ".gba"}:
                    embedded_rom_files.append(member.filename)
                if suffix != ".so":
                    continue
                data = archive.read(member)
                align, status = load_alignment(data, member.filename, Path(scratch))
                parts = member.filename.split("/")
                abi = parts[1] if len(parts) >= 3 and parts[0] == "lib" else "OTHER_PATH"
                records.append({
                    "apk_entry": member.filename,
                    "abi": abi,
                    "size_bytes": len(data),
                    "sha256": sha256(data),
                    "compression": "STORED" if member.compress_type == zipfile.ZIP_STORED else "COMPRESSED",
                    "pt_load_alignments": ",".join(hex(value) for value in align),
                    "elf_16kb": status,
                })

    fields = ["apk_entry", "abi", "size_bytes", "sha256", "compression",
              "pt_load_alignments", "elf_16kb"]
    with (args.output / "native-libraries.tsv").open("w", encoding="utf-8", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        writer.writerows(records)

    arm64 = [x for x in records if x["abi"] == "arm64-v8a"]
    arm64_bad = [x for x in arm64 if x["elf_16kb"] != "ELF_16KB_PASS"]
    statuses = sorted({x["elf_16kb"] for x in records})
    if not records:
        arm64_summary = "NOT_VERIFIED: no packaged native libraries found"
    elif not arm64:
        arm64_summary = "NOT_VERIFIED: no arm64-v8a libraries found"
    elif arm64_bad:
        arm64_summary = "FAIL_OR_UNVERIFIED: arm64-v8a ELF PT_LOAD alignment"
    else:
        arm64_summary = "ELF_ALIGNMENT_ONLY_PASS: all arm64 PT_LOAD alignments >= 16KB"

    lines = [
        f"APK: {apk.name}",
        f"APK_SHA256: {sha256(apk.read_bytes())}",
        f"APK_BYTES: {apk.stat().st_size}",
        f"NATIVE_LIBRARY_COUNT: {len(records)}",
        f"ARM64_LIBRARY_COUNT: {len(arm64)}",
        f"ARM64_ELF_RESULT: {arm64_summary}",
        f"ALL_ELF_STATUSES: {', '.join(statuses) if statuses else 'NONE'}",
        f"EMBEDDED_GB_GBC_GBA_FILES: {embedded_rom_files or 'NONE_DETECTED'}",
        "DISCLAIMER: ELF alignment alone does not prove zip packaging, device install,"
        " runtime or licensing compatibility. Audit every native library and test a 16KB device.",
    ]
    report = "\n".join(lines) + "\n"
    (args.output / "16kb-elf-summary.txt").write_text(report, encoding="utf-8")
    print(report)
    if embedded_rom_files:
        print("ERROR: Game ROM file extension(s) are packaged in APK.", file=sys.stderr)
        return 2
    # Do not fail the build on already-present native alignment problems:
    # surface them as evidence for architecture review and later separate remediation.
    return 0


if __name__ == "__main__":
    sys.exit(main())
