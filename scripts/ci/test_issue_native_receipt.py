#!/usr/bin/env python3
"""Proof of CI-01 native artifact receipt issuance and validation."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from issue_native_receipt import issue_receipt
from resolve_native_artifacts import resolve_component


class NativeReceiptTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.output = self.root / "engine"
        self.output.mkdir()
        (self.output / "core.so").write_bytes(b"sample bytes, not a genuine emulator")
        (self.output / "LICENSE").write_text("test-only licence notice")
        self.script = self.root / "build.sh"
        self.script.write_text("echo pinned")
        self.kwargs = {
            "output": self.output,
            "engine": "ps1",
            "version": "test",
            "source_commit": "a" * 40,
            "build_script": self.script,
            "patch_files": [],
            "ndk_version": "29.0.14206865",
            "api_level": 24,
            "flags": "-O2",
            "notices": "LICENSE",
        }

    def test_issue_and_verify_receipt(self):
        receipt = issue_receipt(**self.kwargs)
        self.assertEqual("ps1", receipt["identity"]["engine"])
        self.assertIn("core.so", resolve_component(receipt["identity"], self.output))
        self.assertEqual(hashlib.sha256(b"sample bytes, not a genuine emulator").hexdigest(),
                         receipt["outputs"]["core.so"])

    def test_missing_licence_rejected(self):
        self.kwargs["notices"] = "MISSING"
        with self.assertRaises(ValueError):
            issue_receipt(**self.kwargs)

    def test_repeat_issuance_rejected(self):
        issue_receipt(**self.kwargs)
        with self.assertRaises(ValueError):
            issue_receipt(**self.kwargs)

    def test_tampering_after_receipt_rejected(self):
        receipt = issue_receipt(**self.kwargs)
        (self.output / "core.so").write_bytes(b"tampered")
        with self.assertRaises(ValueError):
            resolve_component(receipt["identity"], self.output)

    def test_source_change_alters_build_script_identity(self):
        first = issue_receipt(**self.kwargs)["identity"]["build_script_sha256"]
        (self.output / "receipt.json").unlink()
        self.script.write_text("echo changed")
        second = issue_receipt(**self.kwargs)["identity"]["build_script_sha256"]
        self.assertNotEqual(first, second)


if __name__ == "__main__":
    unittest.main()
