"""Ensure CI retries transport failures without masking real build failures."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('build_standalone_apk.sh')


class BuildRetryTests(unittest.TestCase):
    def run_build(self, mode, version='2012'):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            counter = root / 'count'
            fake = root / 'gradle'
            fake.write_text('''#!/usr/bin/env bash
n=0
if [ -f "$COUNTER_FILE" ]; then n=$(cat "$COUNTER_FILE"); fi
n=$((n + 1))
printf '%s' "$n" > "$COUNTER_FILE"
if [ "$FAILURE_MODE" = network ] && [ "$n" -eq 1 ]; then
  echo 'Received status code 504 from server: Gateway Time-out'
  exit 1
fi
if [ "$FAILURE_MODE" = source ]; then
  echo 'Compilation error: unresolved reference'
  exit 1
fi
if [ "$FAILURE_MODE" = network_always ]; then
  echo 'Received status code 504 from server: Gateway Time-out'
  exit 1
fi
''')
            fake.chmod(0o755)
            env = dict(os.environ, GRADLE_BIN=str(fake), COUNTER_FILE=str(counter),
                       FAILURE_MODE=mode, CARTDECK_OUTPUT=str(root / 'output'))
            result = subprocess.run(['bash', str(SCRIPT), version], env=env, capture_output=True, text=True)
            return result.returncode, int(counter.read_text()) if counter.exists() else 0

    def test_transient_network_failure_retries_the_full_command(self):
        self.assertEqual((0, 2), self.run_build('network'))

    def test_source_failure_is_not_retried_or_hidden(self):
        self.assertEqual((1, 1), self.run_build('source'))

    def test_repeated_network_failure_stays_failed(self):
        self.assertEqual((1, 2), self.run_build('network_always'))

    def test_baseline_version_is_rejected_before_building(self):
        self.assertEqual((1, 0), self.run_build('network', version='2001'))


if __name__ == '__main__':
    unittest.main()
