#!/usr/bin/env python3
"""Independent DOSBox Pure ELF and exported Libretro ABI audit."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
from verify_legacy_core import elf_abi

REQUIRED = set("""retro_api_version retro_init retro_deinit retro_get_system_info
retro_get_system_av_info retro_set_environment retro_set_video_refresh
retro_set_audio_sample retro_set_audio_sample_batch retro_set_input_poll
retro_set_input_state retro_set_controller_port_device retro_load_game
retro_unload_game retro_run retro_reset retro_get_region retro_serialize_size
retro_serialize retro_unserialize retro_get_memory_data retro_get_memory_size
retro_cheat_reset retro_cheat_set retro_load_game_special""".split())

def audit(core, readelf):
    data = core.read_bytes()
    elf = elf_abi(data)
    symbols = subprocess.check_output([readelf, "--dyn-syms", "--wide", str(core)], text=True)
    exported = set()
    for line in symbols.splitlines():
        fields = line.split()
        if len(fields) >= 8 and fields[0].endswith(":") and fields[4] in ("GLOBAL", "WEAK") and fields[5] in ("DEFAULT", "PROTECTED") and fields[6] != "UND":
            exported.add(fields[7].split("@")[0])
    absent = sorted(REQUIRED - exported)
    report = {
        "sha256": hashlib.sha256(data).hexdigest(),
        "bytes": len(data), "elf": elf, "missing_libretro_exports": absent,
        "binary_gate_pass": bool(elf.get("valid") and elf.get("all_load_segments_aligned_16kb") and not absent),
        "guest_execution": "NOT_TESTED", "source_equivalence": "NOT_ESTABLISHED",
    }
    return report

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("core", type=Path)
    parser.add_argument("--readelf", default="readelf")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.core, args.readelf)
    args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    raise SystemExit(0 if report["binary_gate_pass"] else 1)
