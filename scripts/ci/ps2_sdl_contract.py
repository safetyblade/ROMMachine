#!/usr/bin/env python3
"""Verify Java SDL contract fixture against the exact pinned native sources."""
import hashlib
import json
from pathlib import Path
import re

REPO = Path(__file__).resolve().parents[2]
FIXTURE = REPO / 'lemuroid-app/src/test/resources/ps2-sdl-contract.json'
SDL = Path('platforms/android/app/src/main/cpp/3rdparty/sdl3')


def verify(source):
    proof = json.loads(FIXTURE.read_text())
    expected = set()
    sources = {}
    for path, digest in proof['source_files'].items():
        data = (Path(source) / SDL / path).read_bytes()
        if hashlib.sha256(data).hexdigest() != digest:
            raise ValueError('Pinned SDL native source differs: ' + path)
        sources[path] = data.decode()
    android = sources['src/core/android/SDL_android.c']
    hid = sources['src/hidapi/android/hid.cpp']
    for name in ('SDLActivity', 'SDLInputConnection', 'SDLAudioManager', 'SDLControllerManager', 'HIDDeviceManager'):
        text = hid if name == 'HIDDeviceManager' else android
        block = re.search(r'JNINativeMethod\s+' + name + r'_tab\[\d*\]\s*=\s*\{(.*?)\n\};', text, re.S)
        if not block:
            raise ValueError('Native registration table missing: ' + name)
        for method, signature in re.findall(r'\{\s*"(\w+)",\s*"([^"\n]+)"', block.group(1)):
            expected.add('|'.join(('N', 'I' if name == 'HIDDeviceManager' else 'S', 'org.libsdl.app.' + name, method, signature)))
    mapping = {'mActivityClass': 'SDLActivity', 'mAudioManagerClass': 'SDLAudioManager', 'mControllerManagerClass': 'SDLControllerManager'}
    for target, method, signature in re.findall(r'GetStaticMethodID\(env,\s*(mActivityClass|mAudioManagerClass|mControllerManagerClass),\s*"(\w+)",\s*"([^"]+)"', android):
        expected.add('|'.join(('J', 'S', 'org.libsdl.app.' + mapping[target], method, signature)))
    for method, signature in re.findall(r'GetMethodID\(\s*g_HIDDeviceManagerCallbackClass,\s*"(\w+)",\s*"([^"]+)"', hid):
        expected.add('|'.join(('J', 'I', 'org.libsdl.app.HIDDeviceManager', method, signature)))
    if expected != set(proof['legacy']):
        raise ValueError('SDL contract fixture does not match pinned native registrations/callbacks')
    return {'pinned_entries': len(expected), 'existing_modern_native_entries': len(proof['existing_modern']),
            'fixture_sha256': hashlib.sha256(FIXTURE.read_bytes()).hexdigest(), 'source_files': proof['source_files']}
