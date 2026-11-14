#!/usr/bin/env python3
"""Combine already verified stage outputs without replacing any differing native bytes."""
import argparse
from pathlib import Path
import shutil


def merge(source, destination):
    source, destination = Path(source), Path(destination)
    entries = []
    for prefix in ('assets', 'jniLibs'):
        root = source / prefix
        if not root.is_dir():
            raise ValueError('Verified payload directory missing: ' + prefix)
        for item in root.rglob('*'):
            if item.is_symlink():
                raise ValueError('Symlinks are not native payload files')
            if item.is_file():
                target = destination / item.relative_to(source)
                if target.exists() and target.read_bytes() != item.read_bytes():
                    raise ValueError('Different payload bytes collide: ' + str(item.relative_to(source)))
                entries.append((item, target))
    for item, target in entries:
        target.parent.mkdir(parents=True, exist_ok=True)
        if not target.exists():
            shutil.copyfile(item, target)
    return len(entries)


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--source', required=True)
    p.add_argument('--destination', required=True)
    a = p.parse_args()
    print('Preserved native payload files:', merge(a.source, a.destination))
