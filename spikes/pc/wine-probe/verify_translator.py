#!/usr/bin/env python3
import pathlib
import re
import sys
data = pathlib.Path(sys.argv[1]).read_text()
assert re.search(r"Machine:\s+AArch64", data), "wrong architecture"
assert re.search(r"Type:\s+DYN", data), "not a PIE executable"
assert "/system/bin/linker64" in data, "not Android's dynamic loader"
loads = [line.split()[-1] for line in data.splitlines() if line.strip().startswith("LOAD ")]
assert loads and all(int(x, 16) >= 16384 for x in loads), "not 16 KiB compatible"
assert "libc.so.6" not in data, "glibc Linux binary is not an Android runner"
print("PASS Android AArch64 PIE / 16KiB ELF candidate; execution not tested")
