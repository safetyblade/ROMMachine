import pathlib
import sys
import tempfile
import unittest
import zipfile
import hashlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
from verify_pc_apk import inspect, ENTRY
from test_verify_legacy_core import fake_aarch64_elf

class PcApkAuditTests(unittest.TestCase):
    def test_matching_compressed_core(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = pathlib.Path(tmp) / "test.apk"
            data = fake_aarch64_elf(16384)
            with zipfile.ZipFile(apk, "w", compression=zipfile.ZIP_DEFLATED) as z:
                z.writestr(ENTRY, data)
            self.assertEqual(inspect(apk, hashlib.sha256(data).hexdigest())["guest_execution"], "NOT_TESTED")
            with self.assertRaises(ValueError):
                inspect(apk, "0" * 64)

    def test_old_elf_and_unaligned_zip_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = pathlib.Path(tmp) / "test.apk"
            for alignment, compression in [(4096, zipfile.ZIP_DEFLATED), (16384, zipfile.ZIP_STORED)]:
                data = fake_aarch64_elf(alignment)
                with zipfile.ZipFile(apk, "w", compression=compression) as z:
                    z.writestr(ENTRY, data)
                with self.assertRaises(ValueError):
                    inspect(apk, hashlib.sha256(data).hexdigest())
