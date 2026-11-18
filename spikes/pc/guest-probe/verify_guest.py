#!/usr/bin/env python3
"""Run the actual source-built Linux core twice and require guest-written evidence."""
from pathlib import Path
import json
import subprocess
import sys
import zipfile

core, frontend, fixture, output = (Path(p).resolve() for p in sys.argv[1:])
output.mkdir(parents=True, exist_ok=True)
system = output / "system"
saves = output / "saves"
system.mkdir(exist_ok=True)
saves.mkdir(exist_ok=True)

def read_guest_file(name):
    hits = []
    for path in saves.rglob("*"):
        if not path.is_file():
            continue
        if path.name.upper() == name:
            hits.append(path.read_bytes())
        elif zipfile.is_zipfile(path):
            with zipfile.ZipFile(path) as z:
                for entry in z.namelist():
                    if Path(entry.replace("\\", "/")).name.upper() == name:
                        hits.append(z.read(entry))
    if len(hits) != 1:
        raise RuntimeError(f"Expected one guest-written {name}, found {len(hits)}")
    return hits[0]

evidence = []
for n in (1, 2):
    result = subprocess.run([str(frontend), str(core), str(fixture), str(system), str(saves)],
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=100)
    (output / f"guest-session-{n}.log").write_text(result.stdout)
    print(result.stdout[-6000:])
    result.check_returncode()
    count = read_guest_file("PCSTATE.DAT")
    inputs = read_guest_file("PCINPUT.DAT")
    if count != bytes([n]):
        raise RuntimeError(f"Unexpected retained counter: {count.hex()}")
    if len(inputs) != 4 or inputs[0] not in (ord("a"), ord("A")) or inputs[1] == 0 or inputs[2] not in (ord("1"), ord("2")) or inputs[3] != 1:
        raise RuntimeError(f"Guest keyboard/mouse/optical result invalid: {inputs.hex()}")
    evidence.append({"session": n, "counter": count[0], "keyboard": chr(inputs[0]),
                     "mouse_driver_present": bool(inputs[1]), "optical_byte": chr(inputs[2]),
                     "mouse_clicked": bool(inputs[3])})
(output / "guest-proof.json").write_text(json.dumps({"platform": "Linux host build, not Android",
    "sessions": evidence, "raw_disk_boot": "NOT_TESTED", "windows": "NOT_TESTED"}, indent=2))
