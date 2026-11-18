#!/usr/bin/env python3
"""Real installed-OS selection (synthetic MBR), two process boots, physical disk evidence."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys

core, frontend, fixtures, output = (Path(p).resolve() for p in sys.argv[1:])
output.mkdir(parents=True, exist_ok=True)
system = output / "system"
saves = output / "saves"
system.mkdir()
saves.mkdir()
disk = system / "probe.img"
source = fixtures / "probe.img"
shutil.copyfile(source, disk)
source_hash = hashlib.sha256(source.read_bytes()).hexdigest()
content = output / "boot.dosz"
shutil.copyfile(fixtures / "boot.dosz", content)
evidence = []
for n in (1, 2):
    result = subprocess.run([str(frontend), str(core), str(content), str(system), str(saves), "boot"],
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=100)
    (output / f"boot-session-{n}.log").write_text(result.stdout)
    print(result.stdout[-8000:])
    result.check_returncode()
    with disk.open("rb") as stream:
        stream.seek(512)
        marker = stream.read(9)
    if marker != b"PCBOOTOK" + bytes([n]):
        raise RuntimeError(f"Guest boot/write proof failed on session {n}: {marker.hex()}")
    if hashlib.sha256(source.read_bytes()).hexdigest() != source_hash:
        raise RuntimeError("Original source disk changed")
    evidence.append({"session": n, "sector_marker": marker[:8].decode(), "retained_counter": marker[8]})
(output / "raw-disk-proof.json").write_text(json.dumps({
    "platform": "Linux source-built runtime, not Android",
    "installed_os_discovery": "synthetic",
    "source_unchanged": True, "sessions": evidence,
    "windows": "NOT_TESTED"}, indent=2))
