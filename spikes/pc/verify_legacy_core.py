#!/usr/bin/env python3
"""PC-04: read-only existing CARTDECK DOSBox Pure native source/ABI audit.

This does not import, run, download or redistribute any PC game or runtime.
"""
import argparse
import hashlib
import json
import pathlib
import struct
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCES = pathlib.Path(__file__).with_name("pc04_sources.json")


def elf_abi(data: bytes):
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[4:6] != b"\x02\x01":
        return {"valid": False, "reason": "expected 64-bit little-endian ELF"}
    machine = struct.unpack_from("<H", data, 18)[0]
    phoff = struct.unpack_from("<Q", data, 32)[0]
    phsize = struct.unpack_from("<H", data, 54)[0]
    phcount = struct.unpack_from("<H", data, 56)[0]
    if phsize < 56 or phcount > 8192 or phoff + phsize * phcount > len(data):
        return {"valid": False, "reason": "invalid program headers"}
    alignment = [
        struct.unpack_from("<Q", data, phoff + i * phsize + 48)[0]
        for i in range(phcount)
        if struct.unpack_from("<I", data, phoff + i * phsize)[0] == 1
    ]
    congruent = all(
        (struct.unpack_from("<Q", data, phoff + i * phsize + 8)[0] -
         struct.unpack_from("<Q", data, phoff + i * phsize + 16)[0]) % 16384 == 0
        for i in range(phcount)
        if struct.unpack_from("<I", data, phoff + i * phsize)[0] == 1
    )
    return {
        "valid": machine == 183,
        "aarch64_machine": machine,
        "load_segment_alignments": alignment,
        "all_load_segments_aligned_16kb": bool(alignment) and congruent and all(a >= 16384 and a & (a - 1) == 0 for a in alignment),
    }


def inspect(strict_binary=False):
    src = json.loads(SOURCES.read_text(encoding="utf-8"))
    pin = src["existing_dosbox_pure"]
    core = ROOT / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/CoreID.kt"
    systems = ROOT / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/GameSystem.kt"
    checks, warnings = {}, []
    checks["core_id_registered"] = core.is_file() and "DOSBOX_PURE(" in core.read_text(encoding="utf-8")
    txt = systems.read_text(encoding="utf-8") if systems.is_file() else ""
    pos = txt.find("SystemID.DOS,")
    scope = txt[pos:pos + 2000] if pos >= 0 else ""
    checks["dos_system_routes_to_core"] = bool(scope) and "CoreID.DOSBOX_PURE" in scope
    checks["dosz_unique_extension"] = 'uniqueExtensions = listOf("dosz")' in scope
    checks["dos_path_scan"] = "scanByPathAndSupportedExtensions = true" in scope
    git = subprocess.run(["git", "ls-tree", "HEAD", "lemuroid-cores"],
                         cwd=ROOT, capture_output=True, text=True, check=False)
    parts = git.stdout.split()
    gitlink = parts[2] if git.returncode == 0 and len(parts) >= 3 else None
    checks["submodule_baseline_matches"] = gitlink == pin["gitlink_sha"]
    path = ROOT / pin["arm64_binary"]
    native = {"present": path.is_file()}
    if native["present"]:
        data = path.read_bytes()
        native.update(elf_abi(data))
        native["sha256"] = hashlib.sha256(data).hexdigest()
        native["bytes"] = len(data)
        checks["native_elf_arm64"] = native["valid"]
        if not native.get("all_load_segments_aligned_16kb"):
            warnings.append("16 KB ELF load alignment is not proven; native build replacement gate")
    else:
        warnings.append("Native binary not checked out: git submodule update --init lemuroid-cores")
    if not checks["submodule_baseline_matches"]:
        warnings.append("Submodule gitlink does not match recorded source baseline")
    valid_source = checks["core_id_registered"] and checks["dos_system_routes_to_core"]
    ok = valid_source and (not strict_binary or (native["present"] and native.get("valid", False) and native.get("all_load_segments_aligned_16kb", False) and checks["submodule_baseline_matches"]))
    return {
        "status": "FAIL" if not ok else "NATIVE_AUDIT_ONLY" if native["present"] else "SOURCE_AUDIT_ONLY",
        "gitlink": gitlink, "source_checks": checks, "native": native,
        "win95_guest_boot": "NOT_TESTED",
        "shandalar_boot": "NOT_TESTED",
        "owner_accepted": False,
        "warnings": warnings,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strict-binary", action="store_true")
    opts = parser.parse_args()
    result = inspect(opts.strict_binary)
    print(json.dumps(result, indent=2))
    sys.exit(1 if result["status"] == "FAIL" else 0)
