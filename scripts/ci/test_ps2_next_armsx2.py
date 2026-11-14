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
                    with self.assertRaisesRegex(ValueError, "dependenc"):
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

    def test_shaderc_dependency_pins_match_source_review(self):
        expected = {
            "spirv-headers": "465055f6c9128772e20082e893d974146acf7a02",
            "spirv-tools": "e4bceacf59fdfe742047e94e41ef65a48999a0dd",
            "glslang": "09c541ee5b22bbac307987b50d86ec2b4f683d75",
        }
        self.assertEqual(
            expected,
            {name: details[1] for name, details in candidate.SHADERC_DEPS.items()},
        )

    def test_shaderc_revision_drift_fails_before_any_download(self):
        with tempfile.TemporaryDirectory() as tmp:
            deps = (pathlib.Path(tmp) / "platforms/android/app/src/main/cpp"
                    / "3rdparty/shaderc/DEPS")
            deps.parent.mkdir(parents=True)
            deps.write_text("vars = {}\n")
            with self.assertRaisesRegex(ValueError, "revision drift"):
                candidate.stage_shaderc_dependencies(tmp)

    def test_native_dependency_sonames_reject_path_traversal(self):
        expected = "Shared library: [libSDL3.so]\nShared library: [libc.so]"
        self.assertEqual({"libSDL3.so", "libc.so"}, candidate.needed_libraries(expected))
        for bad in ("../../libevil.so", "libc.so.6", "libstdc++.so.6"):
            with self.subTest(bad=bad):
                with self.assertRaises(ValueError):
                    candidate.needed_libraries("Shared library: [" + bad + "]")

    def test_native_dependency_closure_fails_closed_without_output_libraries(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            core = root / "libemucore_4k.so"
            core.write_bytes(fake_elf() + b"\0" * 65536)
            with patch.object(candidate, "inspect_dependency", return_value={
                "needed": ["libSDL3.so"], "sha256": "a" * 64,
            }):
                with self.assertRaisesRegex(ValueError, "Missing source-built Android ELF"):
                    candidate.stage_dependency_closure(
                        root, root, core, root / "output", "llvm-readelf",
                    )

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
