#!/usr/bin/env python3
"""Require both host binaries and the independent PC core byte for byte."""
from pathlib import Path
import sys
import zipfile

apk, pc_aar, console_aar, core = sys.argv[1:]
with zipfile.ZipFile(apk) as app, zipfile.ZipFile(pc_aar) as pc, zipfile.ZipFile(console_aar) as console:
    for module, source in (("libcartdeckpcruntime.so", pc), ("liblibretrodroid.so", console)):
        assert app.read("lib/arm64-v8a/" + module) == source.read("jni/arm64-v8a/" + module), module
    assert app.read("lib/arm64-v8a/libcartdeckpc_dosbox.so") == Path(core).read_bytes()
print("PASS: console host unchanged; separate PC host and independently verified PC core packaged")
