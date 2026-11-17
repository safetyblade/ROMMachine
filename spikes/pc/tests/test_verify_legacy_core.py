"""PC-04 source audit regression tests (synthetic, no user game files)."""
import importlib.util
import pathlib
import struct
import unittest

PROBE = pathlib.Path(__file__).resolve().parents[1] / "verify_legacy_core.py"
spec = importlib.util.spec_from_file_location("pc04_verify", PROBE)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


def fake_aarch64_elf(alignment):
    data = bytearray(120)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 18, 183)
    struct.pack_into("<Q", data, 32, 64)
    struct.pack_into("<H", data, 54, 56)
    struct.pack_into("<H", data, 56, 1)
    struct.pack_into("<I", data, 64, 1)
    struct.pack_into("<Q", data, 64 + 48, alignment)
    return bytes(data)


class LegacyCoreProbeTests(unittest.TestCase):
    def test_unrecognised_header(self):
        self.assertFalse(mod.elf_abi(b"not an elf")["valid"])

    def test_arm64_16k_load_alignment(self):
        report = mod.elf_abi(fake_aarch64_elf(16384))
        self.assertTrue(report["valid"])
        self.assertTrue(report["all_load_segments_aligned_16kb"])

    def test_old_4k_load_alignment_not_accepted(self):
        report = mod.elf_abi(fake_aarch64_elf(4096))
        self.assertTrue(report["valid"])
        self.assertFalse(report["all_load_segments_aligned_16kb"])

    def test_misaligned_file_offset_rejected(self):
        data = bytearray(fake_aarch64_elf(16384))
        struct.pack_into("<Q", data, 64 + 8, 1)
        self.assertFalse(mod.elf_abi(bytes(data))["all_load_segments_aligned_16kb"])

    def test_wrong_machine_rejected(self):
        data = bytearray(fake_aarch64_elf(16384))
        struct.pack_into("<H", data, 18, 62)
        self.assertFalse(mod.elf_abi(bytes(data))["valid"])


if __name__ == "__main__":
    unittest.main()
