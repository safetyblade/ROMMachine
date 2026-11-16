#!/usr/bin/env python3
"""CI-01 PS1 consumer rejects stale provenance and changed build inputs."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from issue_native_receipt import issue_receipt
from ci01_legacy_engines import contract
from verify_ci01_ps1 import CORE, FLAGS, SOURCE_COMMIT, expected_identity, verify


class ReusedPs1Tests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.script = self.root / "build_ps1.sh"
        self.script.write_text("echo pinned")
        self.output = self.root / "out"
        self.output.mkdir()
        data = b"test fixture only; not an engine"
        (self.output / CORE).write_bytes(data)
        (self.output / "PCSX_UPSTREAM_COPYING").write_text("source notice")
        (self.output / "pcsx-rearmed-modern.properties").write_text(
            "revision=" + SOURCE_COMMIT + "\ncore.sha256=" + hashlib.sha256(data).hexdigest() + "\n")
        self.kwargs = {
            "output": self.output, "engine": "ps1",
            "version": "pcsx-rearmed-modern", "source_commit": SOURCE_COMMIT,
            "build_script": self.script, "patch_files": [],
            "ndk_version": "29.0.14206865", "api_level": 24,
            "flags": FLAGS, "notices": "PCSX_UPSTREAM_COPYING",
        }
        issue_receipt(**self.kwargs)

    def test_matching_artifact_reused(self):
        self.assertEqual(verify(self.output, self.script)["engine"], "ps1")

    def test_canonical_producer_receipt_reused(self):
        (self.output / "receipt.json").unlink()
        contract("ps1", self.output, issue=True)
        self.assertEqual(verify(self.output)["engine"], "ps1")

    def test_build_script_change_rejected(self):
        self.script.write_text("echo changed")
        with self.assertRaises(ValueError):
            verify(self.output, self.script)

    def test_corrupted_binary_rejected(self):
        (self.output / CORE).write_bytes(b"tampered")
        with self.assertRaises(ValueError):
            verify(self.output, self.script)

    def test_wrong_source_commit_rejected(self):
        receipt = json.loads((self.output / "receipt.json").read_text())
        receipt["identity"]["source_commit"] = "b" * 40
        (self.output / "receipt.json").write_text(json.dumps(receipt))
        with self.assertRaises(ValueError):
            verify(self.output, self.script)

    def test_expected_identity_has_pinned_source(self):
        self.assertEqual(expected_identity(self.script)["source_commit"], SOURCE_COMMIT)


if __name__ == "__main__":
    unittest.main()
