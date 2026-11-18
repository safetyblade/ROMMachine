import io
import pathlib
import re
import unittest
import zipfile

from perf03_inventory import LEGACY, MODERN, NATIVE, engine_row, revision_for_manifest


class InventoryTest(unittest.TestCase):
    def test_registry_coverage(self):
        root = pathlib.Path(__file__).resolve().parents[2]
        coreid = root / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/CoreID.kt"
        configured = set(re.findall(r'"lib([^"]+)_libretro_android\.so"', coreid.read_text()))
        self.assertEqual(configured, set(LEGACY) | set(MODERN))
        self.assertEqual(len(LEGACY) + len(MODERN) + len(NATIVE), 31)

    def test_packaging_is_not_acceptance_and_manifest_requires_digest(self):
        blob = io.BytesIO()
        with zipfile.ZipFile(blob, "w") as apk:
            apk.writestr("assets/cartdeck/test.properties", "revision=" + "a" * 40 + "\ncore.sha256=" + "b" * 64)
        records = {"lib/arm64-v8a/test.so": {"sha256": "b" * 64, "size_bytes": "5", "elf_16kb": "ELF_16KB_PASS"}}
        with zipfile.ZipFile(blob) as apk:
            row = engine_row("test", ["sample"], ["test.so"], records, apk, "cartdeck/test.properties")
            self.assertTrue(row["packaged"])
            self.assertEqual(row["physical_acceptance"], "NOT_TESTED")
            self.assertEqual(row["components"][0]["upstream_revision_verified_by_digest"], "a" * 40)
            self.assertIsNone(row["metrics"]["cold_launch_ms"])
            missing = engine_row("test", ["sample"], ["other.so"], records, apk)
            self.assertFalse(missing["packaged"])
            self.assertIsNone(missing["components"][0]["sha256"])
        self.assertIsNone(revision_for_manifest({"revision": "a" * 40, "core.sha256": "c" * 64},
                                                "test.so", "b" * 64))
