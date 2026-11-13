#!/usr/bin/env python3
"""Retain and symbolicate native evidence only when ELF build IDs match exactly."""
import argparse
import hashlib
import json
import pathlib
import re
import shutil
import subprocess
import tempfile
import zipfile


def build_id(binary, readelf):
    output = subprocess.check_output([str(readelf), '-n', str(binary)], text=True)
    ids = set(re.findall(r'Build ID:\s*([a-fA-F0-9]+)', output))
    if len(ids) != 1:
        raise ValueError(f'Expected exactly one ELF build ID: {binary.name}')
    return ids.pop().lower()


def retain(unstripped, packaged, output, readelf):
    exact = build_id(packaged, readelf)
    if build_id(unstripped, readelf) != exact:
        raise ValueError('Refusing symbols from a different ELF build ID')
    sections = subprocess.check_output([str(readelf), '-SW', str(unstripped)], text=True)
    if '.debug_info' not in sections or '.symtab' not in sections:
        raise ValueError('Unstripped DWARF and symbol table are required')
    output.mkdir(parents=True, exist_ok=True)
    target = output / (packaged.name + '.debug')
    shutil.copyfile(unstripped, target)
    metadata = {'module': packaged.name, 'build_id': exact,
                'packaged_sha256': hashlib.sha256(packaged.read_bytes()).hexdigest(),
                'symbols_sha256': hashlib.sha256(target.read_bytes()).hexdigest()}
    (output / (packaged.name + '.json')).write_text(json.dumps(metadata, indent=2) + '\n')
    return metadata


def retain_aar(aar, search, output, readelf):
    with tempfile.TemporaryDirectory() as scratch:
        packaged = pathlib.Path(scratch) / 'liblibretrodroid.so'
        with zipfile.ZipFile(aar) as archive:
            packaged.write_bytes(archive.read('jni/arm64-v8a/liblibretrodroid.so'))
        exact = build_id(packaged, readelf)
        for candidate in sorted(search.rglob('liblibretrodroid.so')):
            if build_id(candidate, readelf) != exact:
                continue
            sections = subprocess.check_output([str(readelf), '-SW', str(candidate)], text=True)
            if '.debug_info' in sections and '.symtab' in sections:
                return retain(candidate, packaged, output, readelf)
        raise ValueError('No matching unstripped host binary found for the packaged AAR')


def symbolize(report, directory, readelf, symbolizer):
    frames = re.findall(r'(?m)^#\d+ pc ([a-fA-F0-9]+) ([A-Za-z0-9_.+-]+\.so) .*\(BuildId: ([a-fA-F0-9]+)\)$', report)
    if not frames:
        raise ValueError('No module-relative frames with build IDs; obtain a native trace first')
    results = []
    for pc, module, expected in frames:
        binary = directory / (module + '.debug')
        if not binary.is_file():
            # Versioned installations use core.so; ELF identity, not the copied
            # basename, decides which exact engine symbols are eligible.
            matches = [candidate for candidate in directory.glob('*.debug')
                       if build_id(candidate, readelf) == expected.lower()]
            if len(matches) != 1:
                results.append(f'{module}: matching symbols not supplied (BuildId: {expected})')
                continue
            binary = matches[0]
        if build_id(binary, readelf) != expected.lower():
            raise ValueError(f'Refusing mismatched symbols for {module}')
        resolved = subprocess.check_output([str(symbolizer), '--demangle', '--inlines',
                                             '--obj=' + str(binary), '0x' + pc], text=True)
        results.append(f'{module} pc {pc} (BuildId: {expected})\n{resolved.strip()}')
    return '\n\n'.join(results)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--readelf', required=True, type=pathlib.Path)
    commands = parser.add_subparsers(dest='command', required=True)
    keep = commands.add_parser('retain')
    for name in ('unstripped', 'packaged', 'output'):
        keep.add_argument('--' + name, type=pathlib.Path, required=True)
    host = commands.add_parser('retain-aar')
    for name in ('aar', 'search', 'output'):
        host.add_argument('--' + name, type=pathlib.Path, required=True)
    trace = commands.add_parser('symbolize')
    for name in ('report', 'symbols', 'symbolizer'):
        trace.add_argument('--' + name, type=pathlib.Path, required=True)
    args = parser.parse_args()
    if args.command == 'retain':
        print(json.dumps(retain(args.unstripped, args.packaged, args.output, args.readelf)))
    elif args.command == 'retain-aar':
        print(json.dumps(retain_aar(args.aar, args.search, args.output, args.readelf)))
    else:
        print(symbolize(args.report.read_text(), args.symbols, args.readelf, args.symbolizer))


if __name__ == '__main__':
    main()
