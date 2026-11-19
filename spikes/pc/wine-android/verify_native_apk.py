#!/usr/bin/env python3
"""Verify exactly the native producer artifacts in the isolated CARTDECK APK."""
from pathlib import Path
import hashlib
import io
import sys
import zipfile

apk, runtime = map(Path, sys.argv[1:])
assets = runtime / "assets/cartdeck-wine"
manifest = (assets / "runtime.tsv").read_bytes()
def digest(data):
    return hashlib.sha256(data).hexdigest()
with zipfile.ZipFile(apk) as app:
    for name in ("runtime.tsv", "runtime.zip", "probe32.exe"):
        assert digest(app.read("assets/cartdeck-wine/" + name)) == digest((assets / name).read_bytes()), name
    native = set()
    expected = {}
    for line in manifest.decode("ascii").splitlines():
        kind, sha, path, lib, size = line.split("\t")
        assert kind in ("D", "N")
        assert path not in expected
        expected[path] = (kind, sha, int(size))
        if kind == "N":
            binary = app.read("lib/arm64-v8a/" + lib)
            assert digest(binary) == sha and len(binary) == int(size), path
            assert digest((runtime / "jniLibs/arm64-v8a" / lib).read_bytes()) == sha, path
            native.add("lib/arm64-v8a/" + lib)
    assert native == {name for name in app.namelist() if name.startswith("lib/arm64-v8a/libcartdeckwine_")}
    assert not any(name.startswith(("lib/armeabi-v7a/", "lib/x86/", "lib/x86_64/")) for name in app.namelist())
    with zipfile.ZipFile(io.BytesIO(app.read("assets/cartdeck-wine/runtime.zip"))) as data:
        rows = {path: (sha, size) for path, (kind, sha, size) in expected.items() if kind == "D"}
        assert sorted(data.namelist()) == sorted(rows)
        for name, (sha, size) in rows.items():
            payload = data.read(name)
            assert len(payload) == size and digest(payload) == sha, name
print("PASS: exact Android Wine ELF/PE/data package, no unverified binary substitution")
