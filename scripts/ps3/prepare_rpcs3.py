#!/usr/bin/env python3
"""Apply the small CARTDECK virtual-user adapter to one exact upstream revision.
The modified RPCS3 file remains under its upstream GPLv2 licence. No binary distribution clearance is implied.
"""
import pathlib
import subprocess
import sys

root = pathlib.Path(sys.argv[1])
assert subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip() == '993a9914a05361e6b3afe0487c463d9cb5f5b12b'
path = root / 'rpcs3/libretro/libretro_core.cpp'
source = path.read_text()
old = '    Emu.SetUsr("00000001");'
new = '''    const char* cartdeck_user = get_option_value("cartdeck_ps3_user", "00000001");
    std::string user(cartdeck_user);
    if (user.size() != 8 || user.find_first_not_of("0123456789") != std::string::npos || user == "00000000")
        user = "00000001";
    Emu.SetUsr(user);'''
# get_option_value returns std::string in this pin; keep ownership in C++.
new = new.replace('const char* cartdeck_user =', 'const std::string cartdeck_user =')
assert source.count(old) == 1
source = source.replace(old, new)
old = ' + "home/00000001/exdata/";'
assert source.count(old) == 1
source = source.replace(old, ' + "home/" + Emu.GetUsr() + "/exdata/";')
path.write_text(source)

# Imported cross-build LLVMConfig reports the correct LLVM_VERSION_MAJOR (22),
# while its aggregate LLVM_PACKAGE_VERSION can inherit a nested dependency's 1.0.26.
# Enforce the real major-version floor, rather than weakening the LLVM requirement.
cmake = root / '3rdparty/llvm/CMakeLists.txt'
text = cmake.read_text()
assert text.count('if (LLVM_VERSION VERSION_LESS 18)') == 1
cmake.write_text(text.replace('if (LLVM_VERSION VERSION_LESS 18)', 'if (LLVM_VERSION_MAJOR LESS 18)'))
