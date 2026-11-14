import importlib.util
import pathlib
import shutil
import subprocess
import tempfile
import unittest
import tarfile
from build_xbox360_native import source_archive_filter
from xbox360_native_abi import require_jni_entry

ROOT = pathlib.Path(__file__).resolve().parents[2]


class Xbox360NativeTest(unittest.TestCase):
    def test_corresponding_source_keeps_required_shader_build_scripts(self):
        for name in ('XenDroid/emulator-core/src/main/cpp/xenia/tools/build/compile_shader_spirv.py',
                     'XenDroid/emulator-core/src/main/cpp/xenia/tools/build/gen_android_spirv.py',
                     'XenDroid/build.gradle'):
            entry = tarfile.TarInfo(name)
            self.assertIs(entry, source_archive_filter(entry))
        for name in ('XenDroid/emulator-core/build/intermediates/native.so',
                     'XenDroid/app/build/output.apk', 'XenDroid/.git/config',
                     'XenDroid/emulator-core/.cxx/cache'):
            self.assertIsNone(source_archive_filter(tarfile.TarInfo(name)))

    def test_jni_loader_must_be_a_defined_exported_function(self):
        require_jni_entry('12: 00001000 128 FUNC GLOBAL DEFAULT 14 JNI_OnLoad')
        for row in (
            '12: 00000000 0 FUNC GLOBAL DEFAULT UND JNI_OnLoad',
            '12: 00001000 128 FUNC LOCAL DEFAULT 14 JNI_OnLoad',
            '12: 00001000 128 FUNC GLOBAL HIDDEN 14 JNI_OnLoad',
            '12: 00001000 128 OBJECT GLOBAL DEFAULT 14 JNI_OnLoad',
            '12: 00001000 128 FUNC GLOBAL DEFAULT 14 JNI_OnLoad_fake',
        ):
            with self.subTest(row=row), self.assertRaisesRegex(ValueError, 'defined, exported'):
                require_jni_entry(row)

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
