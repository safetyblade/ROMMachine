#!/usr/bin/env python3
"""Independent official ZIP/source audit; never packages third-party binaries into CARTDECK."""
import hashlib
import json
import pathlib
import struct
import sys
import zipfile

CASES = {
    "nethack": ("NetHack 3.6.7 Win32 GDI",
                "63de6c0122288031ef2edc050e99c3b3f7aba8b9483a45baa03e01dde1c1c6fb",
                "nethackw.exe"),
    "openttd": ("OpenTTD 15.3 Win32 portable",
                "3f092edc8f381c3d2d3a59458703899da6f876345b3850a3c76c0dffe68f0e74",
                "openttd.exe"),
}

def digest(data):
    return hashlib.sha256(data).hexdigest()

def audit(key, path):
    title, expected, target = CASES[key]
    archive = pathlib.Path(path)
    sha = digest(archive.read_bytes())
    assert sha == expected, key + ": downloaded archive SHA256 mismatch"
    with zipfile.ZipFile(archive) as z:
        names = [info.filename for info in z.infolist() if not info.is_dir()]
        assert 4 <= len(names) <= 4096, "Unexpected file count"
        assert len(set(n.lower() for n in names)) == len(names), "Windows case collision"
        assert all(n and not n.startswith("/") and ".." not in n.split("/") and "\\" not in n for n in names)
        paths = [n for n in names if n.split("/")[-1].lower() == target]
        assert len(paths) == 1, "Missing or ambiguous EXE"
        program = z.read(paths[0])
        assert len(program) >= 88 and program.startswith(b"MZ")
        offset = struct.unpack_from("<I", program, 60)[0]
        assert offset >= 64 and offset + 6 <= len(program)
        assert program[offset:offset + 4] == b"PE\0\0"
        assert struct.unpack_from("<H", program, offset + 4)[0] == 0x014c, "Not PE32 x86"
        return dict(game=title, source_archive_sha256=sha, file_count=len(names),
                    exe_relative_path=paths[0], exe_sha256=digest(program),
                    exe_size=len(program), architecture="x86 PE32",
                    android_execution="NOT_TESTED")

if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit("usage: pc_folder_fixture_audit.py nethack.zip openttd.zip result.json")
    results = [audit("nethack", sys.argv[1]), audit("openttd", sys.argv[2])]
    pathlib.Path(sys.argv[3]).write_text(json.dumps(results, indent=2) + "\n")
    for item in results:
        print("SOURCE PASS", item["game"], item["exe_relative_path"], item["file_count"])
    print("ANDROID EXECUTION: NOT TESTED")
