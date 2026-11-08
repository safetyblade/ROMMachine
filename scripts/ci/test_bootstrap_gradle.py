"""Exercise verified official-API fallback and fail-closed bootstrap behaviour."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import zipfile

SOURCE = Path(__file__).with_name('bootstrap_gradle.sh')


class BootstrapTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        script = self.root / 'scripts/ci/bootstrap_gradle.sh'
        script.parent.mkdir(parents=True)
        shutil.copyfile(SOURCE, script)
        self.script = script
        self.archive = self.root / 'fixture.zip'
        with zipfile.ZipFile(self.archive, 'w') as archive:
            entry = zipfile.ZipInfo('gradle-8.10.2/bin/gradle')
            entry.create_system = 3
            entry.external_attr = 0o100755 << 16
            archive.writestr(entry, '#!/usr/bin/env bash\nprintf "verified" > "$EXECUTED"\n')
        self.checksum = hashlib.sha256(self.archive.read_bytes()).hexdigest()
        self.properties = self.root / 'gradle/wrapper/gradle-wrapper.properties'
        self.properties.parent.mkdir(parents=True)
        self.write_properties(self.checksum)
        self.metadata = self.root / 'release.json'
        self.metadata.write_text(json.dumps({
            'tag_name': 'v8.10.2',
            'assets': [{
                'name': 'gradle-8.10.2-bin.zip',
                'browser_download_url': 'https://github.com/gradle/gradle-distributions/releases/download/v8.10.2/gradle-8.10.2-bin.zip',
                'url': 'https://api.github.com/repos/gradle/gradle-distributions/releases/assets/194456564',
            }],
        }))
        binary = self.root / 'bin/curl'
        binary.parent.mkdir()
        binary.write_text('''#!/usr/bin/env python3
import os, pathlib, shutil, sys
args = sys.argv[1:]
url = next(x for x in args if x.startswith('https://'))
with open(os.environ['REQUESTS'], 'a') as log: log.write(url + '\\n')
output = args[args.index('-o') + 1]
if '/releases/tags/' in url:
    shutil.copyfile(os.environ['METADATA'], output)
elif '/releases/assets/' in url:
    assert 'Accept: application/octet-stream' in args
    shutil.copyfile(os.environ['ARCHIVE'], output)
else:
    print('HTTP 503: upstream browser route unavailable', file=sys.stderr)
    sys.exit(22)
''')
        binary.chmod(0o755)
        self.env = dict(os.environ, PATH=str(binary.parent) + os.pathsep + os.environ['PATH'],
                        ARCHIVE=str(self.archive), METADATA=str(self.metadata),
                        REQUESTS=str(self.root / 'requests'), EXECUTED=str(self.root / 'executed'),
                        CARTDECK_GRADLE_CACHE=str(self.root / 'cache'), GITHUB_ENV=str(self.root / 'github-env'))

    def tearDown(self):
        self.temporary.cleanup()

    def write_properties(self, checksum):
        self.properties.write_text(
            'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10.2-bin.zip\n'
            + 'distributionSha256Sum=' + checksum + '\n'
        )

    def run_bootstrap(self):
        return subprocess.run(['bash', str(self.script)], env=self.env, capture_output=True, text=True)

    def test_official_api_fallback_is_verified_and_cached(self):
        first = self.run_bootstrap()
        self.assertEqual(0, first.returncode, first.stderr)
        self.assertTrue((self.root / 'executed').exists())
        requests = (self.root / 'requests').read_text()
        self.assertIn('/releases/tags/v8.10.2', requests)
        self.assertIn('/releases/assets/194456564', requests)
        self.assertEqual(0, self.run_bootstrap().returncode)
        self.assertEqual(requests, (self.root / 'requests').read_text())

    def test_checksum_mismatch_never_executes_the_distribution(self):
        self.write_properties('0' * 64)
        result = self.run_bootstrap()
        self.assertNotEqual(0, result.returncode)
        self.assertIn('checksum mismatch', result.stderr)
        self.assertFalse((self.root / 'executed').exists())

    def test_corrupt_cache_is_replaced_and_reverified(self):
        self.assertEqual(0, self.run_bootstrap().returncode)
        cached = self.root / 'cache/gradle-8.10.2-bin.zip'
        cached.write_bytes(b'corrupt cache')
        self.assertEqual(0, self.run_bootstrap().returncode)
        self.assertEqual(self.checksum, hashlib.sha256(cached.read_bytes()).hexdigest())

    def test_unexpected_asset_origin_is_rejected(self):
        metadata = json.loads(self.metadata.read_text())
        metadata['assets'][0]['browser_download_url'] = 'https://example.invalid/gradle.zip'
        self.metadata.write_text(json.dumps(metadata))
        self.assertNotEqual(0, self.run_bootstrap().returncode)
        self.assertFalse((self.root / 'executed').exists())


if __name__ == '__main__':
    unittest.main()
