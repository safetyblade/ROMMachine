#!/usr/bin/env python3
"""Packaging regressions: a compiled backend must include its actual runtime ABI."""
import pathlib
import struct
import tempfile
import unittest
from unittest.mock import patch

from verify_hakux_native import verify


def elf(machine=183, alignment=16384):
    data = bytearray(120)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<HH', data, 16, 3, machine)
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 1)
    struct.pack_into('<I', data, 64, 1)
    struct.pack_into('<Q', data, 112, alignment)
    return data


METHODS = ['SDL_main'] + ['Java_com_rfandango_haku_1x_NativeCoreActivity_' + name for name in
                          ['nativeGetFatal', 'nativeGetRelaunch', 'nativeGetQuit', 'nativeSetPads',
                           'nativeSetPaused', 'nativeSetOverlayPad', 'nativeSetPad']]


class NativePackageTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.directory = pathlib.Path(self.temp.name)
        for name in ('libhakuxcore.so', 'libSDL3.so'):
            (self.directory / name).write_bytes(elf())

    def readelf(self, command, text):
        if '-dW' in command:
            return ' (NEEDED) Shared library: [libSDL3.so]\n (NEEDED) Shared library: [libc.so]'
        return '\n'.join(' GLOBAL DEFAULT ' + name for name in METHODS + ['JNI_OnLoad'])

    def check(self):
        with patch('verify_hakux_native.subprocess.check_output', side_effect=self.readelf):
            return verify(self.directory)

    def test_runtime_dependency_closure_and_host_abi(self):
        self.assertEqual(len(self.check()), 2)

    def test_sdl_default_symbol_versions_are_valid_android_exports(self):
        def versioned(command, text):
            return self.readelf(command, text).replace('JNI_OnLoad', 'JNI_OnLoad@@SDL3_0.0.0')
        with patch('verify_hakux_native.subprocess.check_output', side_effect=versioned):
            self.assertEqual(len(verify(self.directory)), 2)

    def test_missing_sdl_is_rejected(self):
        (self.directory / 'libSDL3.so').unlink()
        with self.assertRaisesRegex(ValueError, 'runtime'):
            self.check()

    def test_4kb_shared_library_is_rejected(self):
        (self.directory / 'libSDL3.so').write_bytes(elf(alignment=4096))
        with self.assertRaisesRegex(ValueError, '16KB'):
            self.check()

    def test_wrong_architecture_is_rejected(self):
        (self.directory / 'libhakuxcore.so').write_bytes(elf(machine=62))
        with self.assertRaisesRegex(ValueError, 'ARM64'):
            self.check()

    def test_unbundled_dependency_is_rejected(self):
        with patch('verify_hakux_native.subprocess.check_output', return_value=' (NEEDED) Shared library: [libc++.so]'):
            with self.assertRaisesRegex(ValueError, 'dependencies'):
                verify(self.directory)

    def test_missing_native_host_entry_is_rejected(self):
        def missing(command, text):
            return self.readelf(command, text).replace('nativeGetQuit', 'unrelatedSymbol')
        with patch('verify_hakux_native.subprocess.check_output', side_effect=missing):
            with self.assertRaisesRegex(ValueError, 'nativeGetQuit'):
                verify(self.directory)


if __name__ == '__main__':
    unittest.main()
