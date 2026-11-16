"""Source-compliance handoffs reject omissions and changed archive bytes."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from retain_ci01_sources import FILES, check


class CorrespondingSourceTests(unittest.TestCase):
    def fixture(self, engine):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        archive = root / FILES[engine][0]
        archive.parent.mkdir(parents=True, exist_ok=True)
        archive.write_bytes(b"test archive fixture only")
        digest = hashlib.sha256(archive.read_bytes()).hexdigest()
        if engine == "hakux":
            (root / FILES[engine][1]).write_text(digest + "  " + archive.name + "\n")
        elif engine == "armsx2":
            (root / "native-evidence.json").write_text(json.dumps(
                {"rust_dependencies": {"corresponding_source_sha256": digest}}))
        else:
            (root / "source-manifest.json").write_text(json.dumps({"source_archive_sha256": digest}))
        return root, archive

    def test_matching_source_handoffs(self):
        for engine in FILES:
            with self.subTest(engine=engine):
                root, _ = self.fixture(engine)
                check(engine, root)

    def test_missing_archive_is_actionable(self):
        for engine in FILES:
            with self.subTest(engine=engine):
                root, archive = self.fixture(engine)
                archive.unlink()
                with self.assertRaisesRegex(ValueError, "missing corresponding source"):
                    check(engine, root)

    def test_changed_source_bytes_are_rejected(self):
        for engine in FILES:
            with self.subTest(engine=engine):
                root, archive = self.fixture(engine)
                archive.write_bytes(b"tampered fixture")
                with self.assertRaisesRegex(ValueError, "checksum mismatch"):
                    check(engine, root)

    def test_symlinked_archive_is_rejected(self):
        root, archive = self.fixture("hakux")
        other = root / "other"
        archive.rename(other)
        archive.symlink_to(other)
        with self.assertRaisesRegex(ValueError, "missing corresponding source"):
            check("hakux", root)


if __name__ == "__main__":
    unittest.main()
