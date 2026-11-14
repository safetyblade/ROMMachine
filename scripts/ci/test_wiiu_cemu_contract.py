#!/usr/bin/env python3
"""Offline fail-closed tests for WIIU-01's source/ELF gate."""
import struct
import unittest
import hashlib
import pathlib
import subprocess
import tempfile
from unittest.mock import patch
import wiiu_cemu_contract as contract

from wiiu_cemu_contract import (
    REQUIRED_LIBRETRO, SOURCE_SHA, check_binary,
)


def fake_arm64_elf(align=16384):
    data = bytearray(512)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 16, 3)  # ET_DYN
    struct.pack_into("<H", data, 18, 183)  # AArch64
    struct.pack_into("<Q", data, 32, 64)  # program headers offset
    struct.pack_into("<H", data, 54, 56)  # program header size
    struct.pack_into("<H", data, 56, 1)  # count
    struct.pack_into("<I", data, 64, 1)  # PT_LOAD
    struct.pack_into("<Q", data, 64 + 48, align)
    return bytes(data)


def exports(*excluded):
    return "\n".join(
        "   12: 000000 0 FUNC GLOBAL DEFAULT 12 " + name
        for name in sorted(REQUIRED_LIBRETRO - set(excluded))
    )


class WiiuCemuContractTests(unittest.TestCase):
    dynamic = "0x00000001 (NEEDED) Shared library: [libc.so]"
    notes = "Build ID: aabbccddee"

    def valid(self, data=None, dynamic=None, symbols=None, notes=None):
        return check_binary(
            fake_arm64_elf() if data is None else data,
            self.dynamic if dynamic is None else dynamic,
            exports() if symbols is None else symbols,
            self.notes if notes is None else notes,
        )

    def test_source_is_immutable_full_commit(self):
        self.assertRegex(SOURCE_SHA, r"^[0-9a-f]{40}$")

    def test_valid_elf_never_claims_host_or_gameplay(self):
        result = self.valid()
        self.assertTrue(result["native_abi_pass"])
        self.assertFalse(result["vulkan_frontend_verified"])
        self.assertFalse(result["apk_packaged"])
        self.assertFalse(result["device_gameplay_verified"])

    def test_missing_libretro_entrypoint_fails(self):
        with self.assertRaisesRegex(ValueError, "Missing Libretro"):
            self.valid(symbols=exports("retro_run"))

    def test_linux_dependency_fails(self):
        with self.assertRaisesRegex(ValueError, "Linux host"):
            self.valid(dynamic="Shared library: [libc.so.6]")

    def test_missing_android_libc_fails(self):
        with self.assertRaisesRegex(ValueError, "Android libc"):
            self.valid(dynamic="Shared library: [libvulkan.so]")

    def test_desktop_gl_dependency_fails(self):
        with self.assertRaisesRegex(ValueError, "desktop dependency"):
            self.valid(dynamic=self.dynamic + "\nShared library: [libGL.so]")

    def test_unaligned_elf_fails(self):
        with self.assertRaisesRegex(ValueError, "16 KiB"):
            self.valid(data=fake_arm64_elf(4096))

    def test_no_build_id_fails(self):
        with self.assertRaisesRegex(ValueError, "BuildID"):
            self.valid(notes="no build id")



class ManifestUsbOptionTests(unittest.TestCase):
    def test_explicit_raw_usb_off_does_not_pull_usb_build_dependencies(self):
        for setting, expected in ((None, 'usb'), ('ON', 'usb'), ('OFF', '')):
            with self.subTest(setting=setting), tempfile.TemporaryDirectory() as tmp:
                root = pathlib.Path(tmp)
                declared = '' if setting is None else 'set(ENABLE_LIBUSB ' + setting + ')\n'
                source = declared + 'set(CMAKE_SYSTEM_NAME Android)\n' + contract.USB_GUARD + '\nlist(APPEND VCPKG_MANIFEST_FEATURES usb)\nendif()\nmessage(STATUS "FEATURES=${VCPKG_MANIFEST_FEATURES}")\n'
                path = root / 'CMakeLists.txt'
                path.write_text(source)
                before = hashlib.sha256(path.read_bytes()).hexdigest()
                with patch.object(contract, 'CMAKE_SHA256', before):
                    proof = contract.patch_android_manifest_features(root)
                self.assertNotEqual(proof['before_sha256'], proof['after_sha256'])
                result = subprocess.check_output(['cmake', '-P', str(path)], text=True)
                self.assertEqual('-- FEATURES=' + expected, result.strip())

    def test_pinned_build_patch_rejects_modified_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            (root / 'CMakeLists.txt').write_text('unreviewed source')
            with self.assertRaisesRegex(ValueError, 'source drift'):
                contract.patch_android_manifest_features(root)

if __name__ == "__main__":
    unittest.main()
