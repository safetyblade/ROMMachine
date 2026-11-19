#!/usr/bin/env python3
"""Relocate the candidate only, before native compilation. No production source mutation."""
from pathlib import Path
import subprocess
import sys

root = Path(sys.argv[1]).resolve()
files = subprocess.check_output(["git", "-C", str(root), "ls-files", "libretrodroid"], text=True).splitlines()
counts = [0, 0, 0, 0]
for name in files:
    path = root / name
    if not path.is_file() or path.suffix not in {".kt", ".java", ".cpp", ".h", ".gradle", ".xml", ".pro", ".txt"}:
        continue
    text = path.read_text()
    for i, (old, new) in enumerate((
        ("com.swordfish.libretrodroid", "dev.cartdeck.pc.runtime"),
        ("com/swordfish/libretrodroid", "dev/cartdeck/pc/runtime"),
        ("Java_com_swordfish_libretrodroid_", "Java_dev_cartdeck_pc_runtime_"),
        ('System.loadLibrary("libretrodroid")', 'System.loadLibrary("cartdeckpcruntime")'),
    )):
        counts[i] += text.count(old)
        text = text.replace(old, new)
    if path.name == "CMakeLists.txt" and path.parent.name == "cpp":
        for old in ("add_library(libretrodroid SHARED", "target_link_options(libretrodroid ", "target_link_libraries(libretrodroid"):
            if text.count(old) != 1:
                raise RuntimeError("PC CMake relocation anchor missing: " + old)
            text = text.replace(old, old.replace("libretrodroid", "cartdeckpcruntime"))
    path.write_text(text)
if not all(counts):
    raise RuntimeError(f"Missing namespace, descriptor, JNI or loader anchors: {counts}")
print("PC relocation replacements:", counts)
