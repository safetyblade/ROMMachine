#!/usr/bin/env python3
"""Regression checks for the five existing engine provenance specifications."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from ci01_legacy_engines import SPECS, contract
from issue_native_receipt import issue_receipt


class LegacyEngineContractTests(unittest.TestCase):
    def test_all_five_native_engines_are_independent(self):
        self.assertEqual(set(SPECS), {"ps1", "psp", "dc", "gc", "ps2"})
        self.assertEqual(len(set(spec[0] for spec in SPECS.values())), 5)

    def test_pinned_source_shas_have_exact_length(self):
        for engine, spec in SPECS.items():
            with self.subTest(engine=engine):
                self.assertEqual(len(spec[0]), 40)
                int(spec[0], 16)

    def test_missing_native_output_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            for engine in SPECS:
                with self.subTest(engine=engine), self.assertRaises((OSError, ValueError)):
                    contract(engine, Path(tmp))

    def test_unknown_engine_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                contract("made-up-engine", Path(tmp))

    def test_missing_assets_are_not_silently_ignored(self):
        for engine in ("psp", "gc"):
            revision, api, script, props, lib, notice, version, patches, flags = SPECS[engine]
            with tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                binary = root / lib
                binary.write_bytes(b"fixture only")
                (root / notice).write_text("test notice")
                (root / props).write_text(
                    "revision=" + revision + "\ncore.sha256=" +
                    hashlib.sha256(binary.read_bytes()).hexdigest() +
                    "\nassets.sha256=" + "0" * 64 + "\n")
                if engine == "gc":
                    (root / props).write_text(
                        "upstream.revision=" + revision + "\ncore.sha256=" +
                        hashlib.sha256(binary.read_bytes()).hexdigest() +
                        "\nassets.sha256=" + "0" * 64 + "\n")
                with self.subTest(engine=engine), self.assertRaises(OSError):
                    contract(engine, root)


if __name__ == "__main__":
    unittest.main()
