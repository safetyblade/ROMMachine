#!/usr/bin/env python3
"""Reject class/native overlap with the production host."""
from pathlib import Path
import io
import subprocess
import sys
import tempfile
import zipfile

aar, readelf = sys.argv[1:]
with zipfile.ZipFile(aar) as archive:
    classes = zipfile.ZipFile(io.BytesIO(archive.read("classes.jar"))).namelist()
    assert "dev/cartdeck/pc/runtime/GLRetroView.class" in classes
    assert not any(x.startswith("com/swordfish/libretrodroid/") for x in classes)
    assert "jni/arm64-v8a/liblibretrodroid.so" not in archive.namelist()
    with tempfile.TemporaryDirectory() as td:
        elf = Path(td) / "libcartdeckpcruntime.so"
        elf.write_bytes(archive.read("jni/arm64-v8a/libcartdeckpcruntime.so"))
        symbols = subprocess.check_output([readelf, "--dyn-syms", "--wide", str(elf)], text=True)
        assert "Java_dev_cartdeck_pc_runtime_LibretroDroid_create" in symbols
        assert "Java_com_swordfish_libretrodroid_" not in symbols
        info = subprocess.check_output([readelf, "-lW", str(elf)], text=True)
        loads = [line.split()[-1] for line in info.splitlines() if line.strip().startswith("LOAD ")]
        assert loads and all(int(x, 16) >= 16384 for x in loads)
print("PASS: separate PC Java namespace, JNI exports, ELF identity and 16KB loads")
