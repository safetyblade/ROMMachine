#!/usr/bin/env python3
"""Pack all runtime ELF files as APK native entries; private runtime paths only link back to them."""
import hashlib
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

root, out, readelf = map(Path, sys.argv[1:])
root = root.resolve()
native = out / "jniLibs/arm64-v8a"
assets = out / "assets/cartdeck-wine"
native.mkdir(parents=True)
assets.mkdir(parents=True)
def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

required = [
    "arm64-v8a/bin/wine", "arm64-v8a/bin/wineserver",
    "arm64-v8a/lib/wine/aarch64-unix/ntdll.so",
    "arm64-v8a/lib/wine/aarch64-unix/wineandroid.so",
    "arm64-v8a/lib/wine/aarch64-windows/wowbox64.dll",
    "arm64-v8a/lib/wine/i386-windows/ntdll.dll",
    "arm64-v8a/lib/wine/aarch64-windows/ntdll.dll",
]
for rel in required:
    assert (root / rel).is_file(), rel

rows = []
seen = set()
with zipfile.ZipFile(assets / "runtime.zip", "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for directory in ("arm64-v8a/bin", "arm64-v8a/lib/wine", "share/wine", "arm64-v8a/lib/libfreetype.so.6"):
        start = root / directory
        for path in sorted([start] if start.is_file() else start.rglob("*")):
            if not path.is_file():
                continue
            real = path.resolve()
            assert real.is_relative_to(root)
            rel = path.relative_to(root).as_posix()
            assert rel not in seen and re.fullmatch(r"[A-Za-z0-9_.+/-]+", rel), rel
            seen.add(rel)
            sha = digest(real)
            with real.open("rb") as stream:
                magic = stream.read(4)
            if magic == b"\x7fELF":
                elf = subprocess.check_output([str(readelf), "-h", "-lW", "-d", str(real)], text=True)
                assert re.search(r"Machine:\s+AArch64", elf), rel
                loads = [line.split()[-1] for line in elf.splitlines() if line.strip().startswith("LOAD ")]
                assert loads and all(int(x, 16) >= 16384 for x in loads), rel + ": bad page alignment"
                assert "libc.so.6" not in elf, rel
                if "Requesting program interpreter:" in elf:
                    assert "/system/bin/linker64" in elf, rel
                name = "libcartdeckwine_" + hashlib.sha256(rel.encode()).hexdigest()[:24] + ".so"
                shutil.copyfile(real, native / name)
                rows.append(("N", sha, rel, name, str(real.stat().st_size)))
            else:
                # PE, fonts and supplemental data are never host-executed.
                archive.write(real, rel)
                rows.append(("D", sha, rel, "-", str(real.stat().st_size)))
    assert set(required) <= seen
(assets / "runtime.tsv").write_text("".join("\t".join(row) + "\n" for row in rows))
shutil.copyfile(out / "probe32.exe", assets / "probe32.exe")
print(f"PC Wine package: {len(rows)} records, {sum(x[0]=='N' for x in rows)} native ELF files")
