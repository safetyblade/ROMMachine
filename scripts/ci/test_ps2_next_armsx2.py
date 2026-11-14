#!/usr/bin/env python3
"""Offline, deterministic fail-closed tests for PS2-NEXT-A native verification."""
import pathlib
import struct
import tempfile
import unittest
from unittest.mock import patch

import ps2_next_armsx2 as candidate


def fake_elf(machine=183, align=16384, shared=3):
    """Construct an ELF64 header + one PT_LOAD (no real native code)."""
    value = bytearray(128)
    value[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", value, 16, shared)
    struct.pack_into("<H", value, 18, machine)
    struct.pack_into("<Q", value, 32, 64)
    struct.pack_into("<H", value, 54, 56)
    struct.pack_into("<H", value, 56, 1)
    struct.pack_into("<I", value, 64, 1)
    struct.pack_into("<Q", value, 64 + 48, align)
    return bytes(value)


def dyn_symbols(missing=None):
    return "\n".join(
        "  1: 00000000 1 FUNC GLOBAL DEFAULT 1 " + value
        for value in candidate.REQUIRED_JNI if value != missing
    )


class NativeGuardTests(unittest.TestCase):
    def test_exact_pinned_revision_is_full_sha(self):
        self.assertRegex(candidate.SOURCE_SHA, r"^[0-9a-f]{40}$")
        self.assertEqual("7a999782377e61fb96eca65e476b5ec0d182058e", candidate.SOURCE_SHA)
        self.assertEqual("29.0.14206865", candidate.NDK_REV)

    def test_complete_jni_contract(self):
        self.assertEqual(len(candidate.REQUIRED_JNI), len(candidate.validate_jni_symbols(dyn_symbols())))

    def test_missing_jni_entry_point_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "Missing Android JNI"):
            candidate.validate_jni_symbols(dyn_symbols(candidate.REQUIRED_JNI[0]))

    def test_wrong_machine_wrong_class_or_4k_only_alignment_fails_closed(self):
        for value in (fake_elf(machine=62), fake_elf(align=4096),
                      fake_elf(shared=2), fake_elf()[:30]):
            with self.subTest(size=len(value)):
                with self.assertRaises(ValueError):
                    candidate.verify_android_elf(value, "[libc.so]")

    def test_native_verification_rejects_non_android_dependencies(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "libemucore_4k.so"
            path.write_bytes(fake_elf() + b"\0" * 65536)
            for dep in ("libc.so.6", "libstdc++.so.6", "discord_partner_sdk.so"):
                with self.subTest(dep=dep), patch.object(candidate, "output", side_effect=[
                    "Shared library: [libc.so]\nShared library: [" + dep + "]",
                    dyn_symbols(), "Build ID: abc123",
                ]):
                    with self.assertRaisesRegex(ValueError, "dependency"):
                        candidate.validate_native(path, "llvm-readelf")

    def test_native_verification_reports_no_playability(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = pathlib.Path(tmp) / "libemucore_4k.so"
            path.write_bytes(fake_elf() + b"\0" * 65536)
            with patch.object(candidate, "output", side_effect=[
                "Shared library: [libc.so]\nShared library: [libandroid.so]",
                dyn_symbols(), "Build ID: abc123",
            ]):
                found = candidate.validate_native(path, "llvm-readelf")
            self.assertFalse(found["gameplay_tested"])
            self.assertFalse(found["device_tested"])
            self.assertEqual("abc123", found["build_id"])
            self.assertEqual(64, len(found["sha256"]))

    def test_no_licence_inventory_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaisesRegex(ValueError, "GPLv3"):
                candidate.collect_notices(tmp)

    def test_nonempty_build_root_rejected_before_any_source_fetch(self):
        with tempfile.TemporaryDirectory() as tmp:
            work = pathlib.Path(tmp)
            (work / "previous").write_text("do not overwrite")
            with self.assertRaisesRegex(ValueError, "new empty"):
                candidate.execute(type("Options", (), {"work": tmp, "pages": "4k"})())


if __name__ == "__main__":
    unittest.main()
