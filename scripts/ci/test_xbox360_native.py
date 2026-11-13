import importlib.util
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]


class Xbox360NativeTest(unittest.TestCase):
    def test_progress_does_not_promote_jit_or_present_to_gameplay(self):
        if not shutil.which('g++'):
            self.skipTest('C++ compiler required')
        header = ROOT / 'scripts/ci/xbox360/cartdeck_progress.h'
        with tempfile.TemporaryDirectory() as d:
            source = pathlib.Path(d) / 'test.cpp'
            source.write_text('#include "' + str(header) + '"\n#include <cassert>\n#include <cstring>\n'
                              'int main(){char b[512]; cartdeck::mark(cartdeck::GPU_READY);'
                              'cartdeck::jit_blocks++;cartdeck::report(b,sizeof(b));'
                              'assert(strstr(b,"jit_blocks=1"));assert(strstr(b,"gameplay=UNVERIFIED"));'
                              'cartdeck::fail(42);assert(cartdeck::error==42);return 0;}')
            exe = pathlib.Path(d) / 'test'
            subprocess.run(['g++', '-std=c++17', '-Wall', '-Wextra', '-Werror', str(source), '-o', str(exe)], check=True)
            subprocess.run([str(exe)], check=True)

    def test_build_is_pinned_separate_and_preserves_stock_vulkan(self):
        build = (ROOT / 'scripts/ci/build_xbox360_native.py').read_text()
        prepare = (ROOT / 'scripts/ci/xbox360/prepare_source.py').read_text()
        self.assertIn('779680ade075d2d2a4c589c6a8d57a8e26c116c0', build)
        self.assertIn('submodule', build)
        self.assertIn('verify_android_elf', build)
        self.assertIn('native.build_id', build)
        self.assertIn('corresponding-source.tar.xz', build)
        self.assertIn('vulkan_lib_path.clear()', prepare)
        self.assertIn('cartdeck_report', prepare)
        self.assertNotIn('include\', \':app', prepare)
