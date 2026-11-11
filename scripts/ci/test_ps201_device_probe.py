"""Exercise the real staging/probe script with an Android command boundary double."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('ps201_device_probe.sh').resolve()


class PS2DeviceProbeTest(unittest.TestCase):
    def run_probe(self, blocked=False, sdk=28):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'bin').mkdir()
            (root / 'game').mkdir()
            for name in ['test.apk', 'play.so', 'game/test.iso']:
                (root / name).write_bytes(b'private fixture')
            adb = root / 'bin/adb'
            adb.write_text('''#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$ADB_COMMANDS"
case "$*" in
  get-state) echo device;;
  'shell getprop ro.product.cpu.abi') echo arm64-v8a;;
  'shell getprop ro.build.version.sdk') echo "$PROBE_SDK";;
  'shell getconf PAGESIZE') echo 16384;;
  install*) echo Success;;
  'shell -T '*) cat >/dev/null;;
  'shell am start '*) echo 'Status: ok';;
  'logcat -d '*) echo 'CARTDECK_PS201 FRAME_RENDERED count=1';
    if [[ "$PROBE_BLOCKED" == 1 ]]; then echo 'CARTDECK_PS201 PS201_BLOCKED bad graphics'; fi;;
  'exec-out screencap -p') printf 'png-fixture';;
esac
''')
            adb.chmod(0o755)
            sleep = root / 'bin/sleep'
            sleep.write_text('#!/bin/sh\nexit 0\n')
            sleep.chmod(0o755)
            env = dict(os.environ, PATH=str(root / 'bin') + ':' + os.environ['PATH'],
                       ADB_COMMANDS=str(root / 'commands'), PROBE_BLOCKED=str(int(blocked)), PROBE_SDK=str(sdk),
                       PS201_EVIDENCE_DIR=str(root / 'evidence'))
            result = subprocess.run(['bash', str(SCRIPT), str(root / 'test.apk'), str(root / 'play.so'),
                                     str(root / 'game'), 'test.iso', 'dev.cartdeck.debug'],
                                    env=env, text=True, capture_output=True)
            return result, (root / 'commands').read_text()

    def test_launches_ps2_host_and_preserves_private_save_folder(self):
        result, commands = self.run_probe()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('debug.PlayStation2RuntimeProbeActivity', commands)
        self.assertNotIn('DreamcastRuntimeProbeActivity', commands)
        self.assertIn('files/ps2-lab/saves', commands)
        self.assertIn("sh -c 'cat > files/ps2-lab/play_libretro.so'", commands)
        self.assertIn("sh -c 'cd files/ps2-lab/game && tar -xf -'", commands)
        self.assertNotIn('rm ', commands)

    def test_older_device_fails_before_install_or_staging(self):
        result, commands = self.run_probe(sdk=27)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('requires Android API 28', result.stderr)
        self.assertNotIn('install ', commands)

    def test_ps2_blocked_marker_overrides_frame_event(self):
        result, _ = self.run_probe(blocked=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('native core not initialised', result.stderr)


if __name__ == '__main__':
    unittest.main()
