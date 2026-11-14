#!/usr/bin/env python3
"""Reject missing and replaced ARMSX2 page-specific native payloads."""
import hashlib
import json
import tempfile
from pathlib import Path
import unittest

from stage_ps2_next_lab import SOURCE_SHA, create_bundle


class StagePs2NextLab(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.work = self.root / "work"
        self.work.mkdir()
        self.libs = self.work / "verified" / "page-4096"
        self.libs.mkdir(parents=True)
        self.expected = {}
        for name, marker in (("libemucore_4k.so", b"a"), ("libSDL3.so", b"b")):
            data = marker * 65536
            (self.libs / name).write_bytes(data)
            self.expected[name] = hashlib.sha256(data).hexdigest()
        self.manifest = {
            "result": "NATIVE_SOURCE_BUILD_VERIFIED_ONLY",
            "source_sha": SOURCE_SHA,
            "ndk": "29.0.14206865",
            "apk_packaged": False,
            "gameplay_tested": False,
            "artifacts": [{
                "host_page_size": 4096,
                "sha256": self.expected["libemucore_4k.so"],
                "dependency_closure": {
                    "libSDL3.so": {"sha256": self.expected["libSDL3.so"]},
                },
            }],
        }
        self.save_manifest()

    def save_manifest(self):
        (self.work / "native-evidence.json").write_text(
            json.dumps(self.manifest), encoding="utf-8",
        )

    def stage(self):
        return create_bundle(self.work, 4096, self.root / "bundle")

    def test_stages_only_verified_private_arm64_deps(self):
        result = self.stage()
        self.assertFalse(result["apk_built"])
        self.assertFalse(result["device_gameplay_verified"])
        self.assertFalse(result["bios_included"])
        self.assertEqual(result["libraries"], self.expected)
        self.assertTrue((self.root / "bundle/jniLibs/arm64-v8a/libemucore_4k.so").is_file())

    def test_tampered_payload_refused(self):
        (self.libs / "libSDL3.so").write_bytes(b"X" * 65536)
        with self.assertRaisesRegex(ValueError, "hash mismatch"):
            self.stage()

    def test_unverified_source_refused(self):
        self.manifest["source_sha"] = "0" * 40
        self.save_manifest()
        with self.assertRaisesRegex(ValueError, "drift"):
            self.stage()

    def test_wrong_page_variant_refused(self):
        with self.assertRaisesRegex(ValueError, "variant missing"):
            create_bundle(self.work, 16384, self.root / "bundle")

    def test_stale_output_refused(self):
        (self.root / "bundle").mkdir()
        with self.assertRaisesRegex(ValueError, "already exists"):
            self.stage()

    def test_unexpected_library_refused(self):
        (self.libs / "libextra.so").write_bytes(b"x" * 65536)
        with self.assertRaisesRegex(ValueError, "Unexpected"):
            self.stage()


if __name__ == "__main__":
    unittest.main()
