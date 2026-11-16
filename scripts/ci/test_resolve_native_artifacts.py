#!/usr/bin/env python3
"""CI-01B fail-closed artifact resolution regression tests."""
import hashlib
import json
import shutil
import tempfile
import unittest
from pathlib import Path

from resolve_native_artifacts import resolve_inventory

IDENTITY = {
    "engine": "ps1",
    "engine_version": "1.0",
    "source_commit": "a" * 40,
    "patch_sha256": "b" * 64,
    "build_script_sha256": "c" * 64,
    "ndk_version": "29.0.14206865",
    "abi": "arm64-v8a",
    "api_level": 33,
    "build_configuration": "Release",
    "compiler_flags_sha256": "d" * 64,
}


class NativeResolverTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.engine = self.root / "ps1"
        self.engine.mkdir()
        self.binary = self.engine / "libcore.so"
        self.binary.write_bytes(b"real test binary bytes")
        self.notices = self.engine / "licenses.txt"
        self.notices.write_text("licence notice")
        self.receipt = {
            "schema": 1,
            "identity": dict(IDENTITY),
            "licence_notices": "licenses.txt",
            "outputs": {
                "libcore.so": hashlib.sha256(self.binary.read_bytes()).hexdigest(),
                "licenses.txt": hashlib.sha256(self.notices.read_bytes()).hexdigest(),
            },
        }
        self.write_receipt()

    def write_receipt(self):
        (self.engine / "receipt.json").write_text(json.dumps(self.receipt))

    def resolve(self, identity=None):
        return resolve_inventory([identity or dict(IDENTITY)], self.root)

    def test_verified_reuse(self):
        result = self.resolve()
        self.assertIn("ps1", result["verified"])
        self.assertFalse(result["missing_or_invalid"])

    def test_checksum_mismatch(self):
        self.binary.write_bytes(b"tampered")
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_missing_component_does_not_invalidate_other(self):
        second = dict(IDENTITY, engine="cemu")
        result = resolve_inventory([IDENTITY, second], self.root)
        self.assertIn("ps1", result["verified"])
        self.assertIn("cemu", result["missing_or_invalid"])

    def test_abi_mismatch(self):
        invalid = dict(IDENTITY, abi="x86_64")
        with self.assertRaises(ValueError):
            self.resolve(invalid)

    def test_toolchain_change_invalidates_only_component(self):
        changed = dict(IDENTITY, ndk_version="28.0")
        self.assertIn("ps1", self.resolve(changed)["missing_or_invalid"])

    def test_missing_receipt(self):
        (self.engine / "receipt.json").unlink()
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_unhashed_licence_notices_rejected(self):
        del self.receipt["outputs"]["licenses.txt"]
        self.write_receipt()
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_unsafe_path_rejected(self):
        self.receipt["outputs"]["../escape.so"] = "0" * 64
        self.write_receipt()
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_duplicate_engine_rejected(self):
        with self.assertRaises(ValueError):
            resolve_inventory([IDENTITY, IDENTITY], self.root)

    def test_unreceipted_staging_payload_rejected(self):
        (self.engine / "extra.so").write_bytes(b"not verified")
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_omitted_output_rejected_even_when_file_exists(self):
        del self.receipt["outputs"]["libcore.so"]
        self.write_receipt()
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_parent_symlink_rejected(self):
        external = self.root / "external"
        external.mkdir()
        (external / "core.so").write_bytes(b"outside artifact")
        (self.engine / "libs").symlink_to(external, target_is_directory=True)
        self.receipt["outputs"]["libs/core.so"] = hashlib.sha256(b"outside artifact").hexdigest()
        self.write_receipt()
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_component_symlink_rejected(self):
        moved = self.root / "moved"
        self.engine.rename(moved)
        self.engine.symlink_to(moved, target_is_directory=True)
        self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_malformed_receipts_fail_per_component(self):
        for contents in (b"[]", b"null", b"{", b"\xff"):
            with self.subTest(contents=contents):
                (self.engine / "receipt.json").write_bytes(contents)
                self.assertIn("ps1", self.resolve()["missing_or_invalid"])

    def test_engine_cannot_escape_artifact_root(self):
        with self.assertRaises(ValueError):
            self.resolve(dict(IDENTITY, engine="../ps1"))

    def test_source_change_invalidates_only_changed_engine(self):
        second = dict(IDENTITY, engine="psp")
        shutil.copytree(self.engine, self.root / "psp")
        second_receipt = dict(self.receipt, identity=second)
        (self.root / "psp/receipt.json").write_text(json.dumps(second_receipt))
        changed = dict(IDENTITY, source_commit="e" * 40)
        result = resolve_inventory([changed, second], self.root)
        self.assertEqual(set(result["verified"]), {"psp"})
        self.assertEqual(set(result["missing_or_invalid"]), {"ps1"})

    def test_missing_receipt_keeps_unrelated_component_reusable(self):
        second = dict(IDENTITY, engine="psp")
        shutil.copytree(self.engine, self.root / "psp")
        (self.root / "psp/receipt.json").unlink()
        result = resolve_inventory([IDENTITY, second], self.root)
        self.assertEqual(set(result["verified"]), {"ps1"})
        self.assertEqual(set(result["missing_or_invalid"]), {"psp"})


if __name__ == "__main__":
    unittest.main()
