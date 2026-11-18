#!/usr/bin/env python3
"""Fail-closed tests for the remaining native component contracts.

These fixtures test source validation only and do not claim emulator builds.
"""
import hashlib
import contextlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from ci01_remaining_engines import SPECS, build_recipe_files, expected, prepare, verify


class RemainingNativeTests(unittest.TestCase):
    def test_generated_bytecode_never_changes_recipe_inputs(self):
        with tempfile.TemporaryDirectory() as tmp, contextlib.chdir(tmp):
            for engine, relative in (("xenia", "scripts/ci/xbox360"),
                                     ("cemu", "spikes/wiiu/vulkan-host")):
                root = Path(relative)
                root.mkdir(parents=True)
                (root / "source.cpp").write_text("pinned source fixture")
                before = build_recipe_files(engine)
                cache = root / "__pycache__"
                cache.mkdir()
                (cache / "prepare_source.cpython-312.pyc").write_bytes(b"machine-specific")
                (root / "old.pyc").write_bytes(b"legacy bytecode")
                self.assertEqual(build_recipe_files(engine), before)
                self.assertIn((root / "source.cpp").as_posix(), before)

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

    def test_libretrodroid_aar_is_retained_and_reverified(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "host"
            source.mkdir()
            (source / "source").mkdir()
            (source / "source/LICENSE").write_text("test fixture notice")
            aar = (source / "maven/com/github/Swordfish90/LibretroDroid/"
                   "0.14.0-cartdeck4/LibretroDroid-0.14.0-cartdeck4.aar")
            aar.parent.mkdir(parents=True)
            with zipfile.ZipFile(aar, "w") as archive:
                archive.writestr("jni/arm64-v8a/libretrodroid.so", b"fixture native payload")
            (source / "provenance.txt").write_text(
                "revision=" + SPECS["libretrodroid"]["sha"] + chr(10) +
                hashlib.sha256(aar.read_bytes()).hexdigest() + "  " + str(aar) + chr(10))
            output = root / "retained"
            from ci01_remaining_engines import produce
            produce("libretrodroid", source, output)
            self.assertIn("receipt.json", {p.name for p in output.iterdir()})
            self.assertIn(
                "maven/com/github/Swordfish90/LibretroDroid/"
                "0.14.0-cartdeck4/LibretroDroid-0.14.0-cartdeck4.aar",
                verify("libretrodroid", output),
            )
            (output / "maven/com/github/Swordfish90/LibretroDroid/"
             "0.14.0-cartdeck4/LibretroDroid-0.14.0-cartdeck4.aar").write_bytes(b"tampered")
            with self.assertRaises(ValueError):
                verify("libretrodroid", output)

    def test_unknown_component_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                prepare("unsupported", tmp, Path(tmp) / "output")


if __name__ == "__main__":
    unittest.main()
