"""The reusable host publication must remain resolvable at its exact coordinates."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from retain_ci01_host_metadata import NAME, RELATIVE, check


class HostMetadataTests(unittest.TestCase):
    def fixture(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        folder = root / RELATIVE
        folder.mkdir(parents=True)
        (folder / (NAME + ".aar")).write_bytes(b"fixture only")
        pom = folder / (NAME + ".pom")
        pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0">'
                       '<groupId>com.github.Swordfish90</groupId><artifactId>LibretroDroid</artifactId>'
                       '<version>0.14.0-cartdeck4</version><packaging>aar</packaging></project>')
        return root, folder, pom

    def test_generated_pom_coordinates_and_packaging(self):
        root, _, _ = self.fixture()
        check(root)

    def test_missing_pom_is_actionable(self):
        root, _, pom = self.fixture()
        pom.unlink()
        with self.assertRaisesRegex(ValueError, "missing generated Maven POM"):
            check(root)

    def test_wrong_version_cannot_redirect_host_resolution(self):
        root, _, pom = self.fixture()
        pom.write_text(pom.read_text().replace("0.14.0-cartdeck4", "0.14.0"))
        with self.assertRaisesRegex(ValueError, "coordinate mismatch"):
            check(root)

    def test_module_metadata_must_describe_same_aar(self):
        root, folder, _ = self.fixture()
        data = dict(component=dict(group="com.github.Swordfish90", module="LibretroDroid",
                                   version="0.14.0-cartdeck4"),
                    variants=[dict(files=[dict(name=NAME + ".aar", sha256="0" * 64)])])
        module = folder / (NAME + ".module")
        module.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, "AAR checksum mismatch"):
            check(root)
        data["variants"][0]["files"][0]["sha256"] = hashlib.sha256(b"fixture only").hexdigest()
        module.write_text(json.dumps(data))
        check(root)

    def test_advertised_source_publication_cannot_be_omitted(self):
        root, folder, _ = self.fixture()
        name = NAME + "-sources.jar"
        data = dict(component=dict(group="com.github.Swordfish90", module="LibretroDroid",
                                   version="0.14.0-cartdeck4"),
                    variants=[dict(files=[dict(name=name, sha256=hashlib.sha256(b"sources fixture").hexdigest())])])
        (folder / (NAME + ".module")).write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, "Missing host publication file"):
            check(root)
        (folder / name).write_bytes(b"sources fixture")
        check(root)


if __name__ == "__main__":
    unittest.main()
