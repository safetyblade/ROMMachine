#!/usr/bin/env python3
"""Create only original synthetic DOS, optical and BIOS workloads."""
from pathlib import Path
import subprocess
import sys
import zipfile

root = Path(__file__).resolve().parent
out = Path(sys.argv[1]).resolve()
out.mkdir(parents=True, exist_ok=True)
subprocess.run(["nasm", "-f", "bin", str(root / "probe.asm"), "-o", str(out / "PCPROBE.COM")], check=True)
subprocess.run(["nasm", "-f", "bin", str(root / "boot.asm"), "-o", str(out / "boot.bin")], check=True)
boot = (out / "boot.bin").read_bytes()
assert len(boot) == 512 and boot[-2:] == b"\x55\xaa"
with (out / "probe.img").open("wb") as disk:
    disk.truncate(16 * 1024 * 1024)
    disk.write(boot)
    disk.write(b"PCEMPTY!")
for n in (1, 2):
    directory = out / ("disc" + str(n))
    directory.mkdir(exist_ok=True)
    (directory / "DISC.TXT").write_bytes(str(n).encode("ascii"))
    subprocess.run(["genisoimage", "-quiet", "-V", "PCPROBE" + str(n), "-o", str(out / f"disc{n}.iso"), str(directory)], check=True)
with zipfile.ZipFile(out / "probe.dosz", "w", compression=zipfile.ZIP_STORED) as z:
    z.write(out / "PCPROBE.COM", "PCPROBE.COM")
    z.writestr("DOSBOX.BAT", "@ECHO OFF\r\nIMGMOUNT D C:\\DISC1.ISO C:\\DISC2.ISO -T ISO\r\nPCPROBE.COM\r\n")
    for n in (1,2):
        z.write(out / f"disc{n}.iso", f"disc{n}.iso")
for name in ("PCPROBE.COM", "boot.bin", "disc1.iso", "disc2.iso"):
    (out / name).unlink()
for n in (1,2):
    directory=out / ("disc" + str(n))
    (directory/"DISC.TXT").unlink()
    directory.rmdir()
