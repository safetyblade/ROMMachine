#!/usr/bin/env python3
"""Fail-closed tests for the remaining native component contracts.

These fixtures test source validation only and do not claim emulator builds.
"""
import json
import tempfile
import unittest
from pathlib import Path

from ci01_remaining_engines import SPECS, expected, prepare, verify


class RemainingNativeTests(unittest.TestCase):
    def test_five_distinct_pinned_components(self):
        self.assertEqual(set(SPECS), {"hakux", "xenia", "armsx2", "cemu", "libretrodroid"})
        for name, spec in SPECS.items():
            with self.subTest(component=name):
                self.assertEqual(len(spec["sha"]), 40)
                int(spec["sha"], 16)
                self.assertEqual(expected(name)["engine"], name)

    def test_missing_output_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            for name in SPECS:
                with self.subTest(component=name), self.assertRaises((OSError, ValueError)):
                    verify(name, Path(tmp) / name)

    def test_missing_native_sources_cannot_be_published(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for name in SPECS:
                with self.subTest(component=name), self.assertRaises((OSError, ValueError)):
                    prepare(name, root / "missing", root / ("out-" + name))

    def test_existing_destination_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "output").mkdir()
            with self.assertRaises(ValueError):
                prepare("hakux", root, root / "output")

    def test_unknown_component_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                prepare("unsupported", tmp, Path(tmp) / "output")


if __name__ == "__main__":
    unittest.main()
