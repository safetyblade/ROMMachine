#!/usr/bin/env python3
"""Verify the real source-built execution ELF and bind its build provenance."""
import hashlib
import json
import pathlib
import re
import subprocess
import sys

PINS = {'armsx3': 'd396f0b6b8372789e6423a4f4d242cab4c5135eb', 'rpcs3-libretro': '993a9914a05361e6b3afe0487c463d9cb5f5b12b'}
engine, directory = sys.argv[1:]
build = pathlib.Path(directory)
cache = (build / 'CMakeCache.txt').read_text()
source = pathlib.Path(re.search(r'^CMAKE_HOME_DIRECTORY:INTERNAL=(.+)$', cache, re.M)[1])
revision = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
assert revision == PINS[engine], 'Native source pin mismatch'
assert re.search(r'^ANDROID_ABI:[A-Z_]+=arm64-v8a$', cache, re.M), 'Wrong build ABI'
name = 'libarmsx3-core.so' if engine == 'armsx3' else 'rpcs3_libretro.so'
files = list(build.rglob(name))
assert len(files) == 1, f'Expected one actual emulator ELF: {files}'
file = files[0]
assert file.stat().st_size > 1024 * 1024, 'Empty or wrapper-only emulator rejected'
header = subprocess.check_output(['readelf', '-h', str(file)], text=True)
assert 'AArch64' in header and 'ELF64' in header
segments = subprocess.check_output(['readelf', '-l', '--wide', str(file)], text=True)
loads = [int(line.split()[-1], 16) for line in segments.splitlines() if line.strip().startswith('LOAD ')]
assert loads and all(value >= 16384 for value in loads), 'ARM64 16 KiB LOAD alignment failed'
symbols = subprocess.check_output(['readelf', '--dyn-syms', '--wide', str(file)], text=True)
required = ['_armsx3_initialize', '_armsx3_boot', '_armsx3_surfaceEvent', '_armsx3_overlayPadData', '_armsx3_installFw', '_armsx3_getFramePeriodNs'] if engine == 'armsx3' else ['retro_init', 'retro_run', 'retro_load_game', 'retro_set_input_state', 'retro_set_audio_sample_batch']
for symbol in required:
    assert any(line.endswith(' ' + symbol) and ' UND ' not in line for line in symbols.splitlines()), symbol
h = hashlib.sha256()
with file.open('rb') as stream:
    while chunk := stream.read(1048576):
        h.update(chunk)
diff = subprocess.check_output(['git', '-C', str(source), 'diff', '--binary'])
receipt = {'engine': engine, 'revision': revision, 'source_elf_sha256': h.hexdigest(), 'source_elf_bytes': file.stat().st_size, 'source_patch_sha256': hashlib.sha256(diff).hexdigest(), 'cmake_cache_sha256': hashlib.sha256(cache.encode()).hexdigest(), 'submodules': subprocess.check_output(['git', '-C', str(source), 'submodule', 'status', '--recursive'], text=True).splitlines(), 'arm64_16k_load_alignment': 'PASS', 'exported_native_abi': 'PASS', 'gameplay': 'UNVERIFIED'}
(build / 'cartdeck-engine-receipt.json').write_text(json.dumps(receipt, indent=2))
print(engine, 'REAL_ARM64_ENGINE_BUILD_VERIFIED', h.hexdigest())
