#!/usr/bin/env python3
"""PC-R02: mechanical audit of the shipped DOSBox Pure core.

Verifies, from the checked-out tree only, the PC-R02 gate facts recorded in
pcr02_sources.json:

- the lemuroid-cores gitlink still matches the audited pin;
- the shipped arm64 binary is byte-identical to the audited binary
  (size, SHA-256 and git blob SHA-1);
- the ELF is AArch64 with every LOAD segment 16 KB aligned and congruent
  (Android 16 KB page-size loader requirement);
- the dynamic symbol table exports every libretro entry point the host
  (LibretroDroid Core::open) resolves with dlsym and hard-fails on;
- DT_NEEDED stays within Android system libraries;
- the DOS registry in CoreID/GameSystem still routes .dosz content to
  this binary;
- the licence recorded here still agrees with pc04_sources.json.

This does not import, run, download or redistribute any PC game or runtime,
and it proves nothing about guest execution: Win95/Shandalar boot and
final-APK binary inclusion remain separate gates and are reported as such.
"""
import argparse
import hashlib
import json
import pathlib
import struct
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCES = pathlib.Path(__file__).with_name("pcr02_sources.json")
PC04_SOURCES = pathlib.Path(__file__).with_name("pc04_sources.json")

# Entry points LibretroDroid's Core::open resolves via dlsym; it throws
# ("Missing libretro core symbol") if any one is absent.
HOST_REQUIRED_SYMBOLS = [
    "retro_api_version",
    "retro_cheat_reset",
    "retro_cheat_set",
    "retro_deinit",
    "retro_get_memory_data",
    "retro_get_memory_size",
    "retro_get_system_av_info",
    "retro_get_system_info",
    "retro_init",
    "retro_load_game",
    "retro_reset",
    "retro_run",
    "retro_serialize",
    "retro_serialize_size",
    "retro_set_audio_sample",
    "retro_set_audio_sample_batch",
    "retro_set_controller_port_device",
    "retro_set_environment",
    "retro_set_input_poll",
    "retro_set_input_state",
    "retro_set_video_refresh",
    "retro_unload_game",
    "retro_unserialize",
]

# Libraries an Android system-provided libretro core may link against.
ALLOWED_NEEDED = {
    "libandroid.so",
    "libc.so",
    "libdl.so",
    "liblog.so",
    "libm.so",
    "libz.so",
}

PT_LOAD, PT_DYNAMIC = 1, 2
DT_NEEDED, DT_HASH, DT_STRTAB, DT_SYMTAB, DT_STRSZ, DT_SYMENT = 1, 4, 5, 6, 10, 11


def _phdrs(data):
    phoff = struct.unpack_from("<Q", data, 32)[0]
    phsize = struct.unpack_from("<H", data, 54)[0]
    phcount = struct.unpack_from("<H", data, 56)[0]
    if phsize < 56 or phcount > 8192 or phoff + phsize * phcount > len(data):
        return []
    return [
        struct.unpack_from("<IIQQQQQQ", data, phoff + i * phsize)
        for i in range(phcount)
    ]


def _vaddr_to_offset(loads, vaddr):
    for seg in loads:
        start = seg[3]
        if start <= vaddr < start + max(seg[5], seg[6]):
            return seg[2] + (vaddr - start)
    return None


def elf_report(data):
    """Structural ELF facts; never executes the binary."""
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[4:6] != b"\x02\x01":
        return {"valid": False, "reason": "expected 64-bit little-endian ELF"}
    machine = struct.unpack_from("<H", data, 18)[0]
    segments = _phdrs(data)
    loads = [seg for seg in segments if seg[0] == PT_LOAD]
    alignment = [seg[7] for seg in loads]
    congruent = bool(loads) and all(
        seg[7] >= 16384
        and seg[7] & (seg[7] - 1) == 0
        and seg[2] % seg[7] == seg[3] % seg[7]
        and seg[2] + seg[5] <= len(data)
        and seg[5] <= seg[6]
        for seg in loads
    )
    report = {
        "valid": machine == 183,
        "aarch64_machine": machine,
        "load_segment_alignments": alignment,
        "all_load_segments_aligned_16kb": congruent,
    }
    dynamic = next((seg for seg in segments if seg[0] == PT_DYNAMIC), None)
    if dynamic is None:
        report["reason"] = "no PT_DYNAMIC segment"
        return report
    dyn_off = dynamic[2]
    dyn_filesz = dynamic[5]
    tags = {}
    for i in range(dyn_filesz // 16):
        tag, value = struct.unpack_from("<qQ", data, dyn_off + i * 16)
        if tag == 0:
            break
        tags.setdefault(tag, []).append(value)
    strtab_off = _vaddr_to_offset(loads, tags[DT_STRTAB][0]) if DT_STRTAB in tags else None
    strsz = tags.get(DT_STRSZ, [0])[0]

    def dyn_string(offset):
        if strtab_off is None or offset >= strsz:
            return ""
        start = strtab_off + offset
        end = data.find(b"\x00", start, strtab_off + strsz)
        return data[start:end].decode("utf-8", "replace")

    report["dt_needed"] = [dyn_string(v) for v in tags.get(DT_NEEDED, [])]
    report["unexpected_needed"] = sorted(
        set(report["dt_needed"]) - ALLOWED_NEEDED
    )
    exported = set()
    if DT_SYMTAB in tags and DT_HASH in tags and strtab_off is not None:
        symtab_off = _vaddr_to_offset(loads, tags[DT_SYMTAB][0])
        hash_off = _vaddr_to_offset(loads, tags[DT_HASH][0])
        syment = tags.get(DT_SYMENT, [24])[0] or 24
        if symtab_off is not None and hash_off is not None:
            nchain = struct.unpack_from("<I", data, hash_off + 4)[0]
            for i in range(nchain):
                base = symtab_off + i * syment
                st_name, st_info, _st_other, st_shndx = struct.unpack_from(
                    "<IBBH", data, base
                )
                if st_shndx != 0 and (st_info >> 4) in (1, 2) and st_name:
                    exported.add(dyn_string(st_name))
    report["exported_symbols"] = sorted(exported)
    report["missing_host_symbols"] = sorted(
        set(HOST_REQUIRED_SYMBOLS) - exported
    )
    return report


def git_blob_sha(data):
    return hashlib.sha1(b"blob %d\x00" % len(data) + data).hexdigest()


def inspect():
    record = json.loads(SOURCES.read_text(encoding="utf-8"))
    pin = record["shipped_dosbox_pure"]
    checks, warnings = {}, []

    git = subprocess.run(
        ["git", "ls-tree", "HEAD", "lemuroid-cores"],
        cwd=ROOT, capture_output=True, text=True, check=False,
    )
    parts = git.stdout.split()
    gitlink = parts[2] if git.returncode == 0 and len(parts) >= 3 else None
    checks["gitlink_matches_pin"] = gitlink == pin["gitlink_sha"]
    if not checks["gitlink_matches_pin"]:
        warnings.append("lemuroid-cores gitlink differs from the PC-R02 audited pin")

    core = ROOT / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/CoreID.kt"
    systems = ROOT / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/GameSystem.kt"
    core_txt = core.read_text(encoding="utf-8") if core.is_file() else ""
    binary_name = pathlib.PurePath(pin["arm64_binary"]).name
    checks["core_id_registered"] = "DOSBOX_PURE(" in core_txt and binary_name in core_txt
    sys_txt = systems.read_text(encoding="utf-8") if systems.is_file() else ""
    pos = sys_txt.find("SystemID.DOS,")
    scope = sys_txt[pos:pos + 2000] if pos >= 0 else ""
    checks["dos_system_routes_to_core"] = bool(scope) and "CoreID.DOSBOX_PURE" in scope
    checks["dosz_unique_extension"] = 'uniqueExtensions = listOf("dosz")' in scope

    if PC04_SOURCES.is_file():
        pc04 = json.loads(PC04_SOURCES.read_text(encoding="utf-8"))
        pc04_license = pc04["existing_dosbox_pure"]["license"]
        checks["license_agrees_with_pc04_record"] = pc04_license.startswith(
            pin["license"]
        )
    else:
        checks["license_agrees_with_pc04_record"] = False
        warnings.append("pc04_sources.json missing; licence cross-check skipped")

    path = ROOT / pin["arm64_binary"]
    native = {"present": path.is_file()}
    if native["present"]:
        data = path.read_bytes()
        native.update(elf_report(data))
        native["bytes"] = len(data)
        native["sha256"] = hashlib.sha256(data).hexdigest()
        native["git_blob_sha"] = git_blob_sha(data)
        checks["binary_size_matches"] = native["bytes"] == pin["binary_bytes"]
        checks["binary_sha256_matches"] = native["sha256"] == pin["sha256"]
        checks["binary_blob_sha_matches"] = (
            native["git_blob_sha"] == pin["github_blob_sha"]
        )
        checks["native_elf_arm64"] = native.get("valid", False)
        checks["elf_16kb_aligned"] = native.get(
            "all_load_segments_aligned_16kb", False
        )
        checks["host_symbols_complete"] = not native.get("missing_host_symbols")
        checks["needed_system_libraries_only"] = not native.get(
            "unexpected_needed"
        )
        native["capability_markers"] = {
            marker: (marker.encode() in data)
            for marker in record["capability_markers_in_binary"]
        }
        checks["capability_markers_present"] = all(
            native["capability_markers"].values()
        )
    else:
        warnings.append(
            "Native binary not checked out: git submodule update --init lemuroid-cores"
        )

    ok = all(checks.values()) and native["present"]
    return {
        "status": "PASS" if ok else "FAIL",
        "gate": "PC-R02",
        "gitlink": gitlink,
        "checks": checks,
        "native": native,
        "license": pin["license"],
        "canonical_source_revision": pin["canonical_source_revision"],
        "win95_guest_boot": "NOT_TESTED",
        "shandalar_boot": "NOT_TESTED",
        "final_apk_binary_inclusion": "NOT_PROVEN_G2_BUILD_GATE",
        "owner_accepted": False,
        "warnings": warnings,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args()
    result = inspect()
    print(json.dumps(result, indent=2))
    sys.exit(1 if result["status"] == "FAIL" else 0)
