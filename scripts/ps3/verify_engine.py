#!/usr/bin/env python3
import hashlib
import pathlib
import subprocess
import sys
engine, directory = sys.argv[1:]
name = 'libarmsx3-core.so' if engine == 'armsx3' else 'rpcs3_libretro.so'
files = list(pathlib.Path(directory).rglob(name))
assert len(files) == 1, f'Expected one actual emulator ELF: {files}'
file = files[0]
assert file.stat().st_size > 1024 * 1024, 'Empty or wrapper-only emulator rejected'
header = subprocess.check_output(['readelf', '-h', str(file)], text=True)
assert 'AArch64' in header and 'ELF64' in header
symbols = subprocess.check_output(['readelf', '--dyn-syms', '--wide', str(file)], text=True)
required = ['_armsx3_initialize', '_armsx3_boot', '_armsx3_surfaceEvent', '_armsx3_overlayPadData', '_armsx3_installFw'] if engine == 'armsx3' else ['retro_init', 'retro_run', 'retro_load_game', 'retro_set_input_state', 'retro_set_audio_sample_batch']
for symbol in required:
    assert any(line.endswith(' ' + symbol) and ' UND ' not in line for line in symbols.splitlines()), symbol
print(engine, 'REAL_ARM64_ENGINE_BUILD_VERIFIED', hashlib.sha256(file.read_bytes()).hexdigest())
