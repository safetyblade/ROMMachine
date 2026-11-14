"""Retain the pinned shader Rust dependency graph, notices and corresponding source."""
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import zipfile

PIN = '87e8a97b50516d997defeaa168173dcd185d4022'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def reachable(metadata):
    packages = {p['id']: p for p in metadata['packages']}
    nodes = {p['id']: p for p in metadata['resolve']['nodes']}
    root = next(key for key, value in packages.items() if value['name'] == 'librashader-capi')
    visited, queue = set(), [root]
    while queue:
        key = queue.pop()
        if key in visited:
            continue
        visited.add(key)
        queue.extend(nodes[key]['dependencies'])
    result = [packages[key] for key in sorted(visited)]
    if any(not item.get('license') for item in result):
        raise ValueError('Rust runtime dependency has no licence declaration')
    return result


def retain(source, work, notices, ndk, librashader_source=None):
    roots = ([Path(librashader_source) / 'Cargo.toml'] if librashader_source else
             list((Path(source) / 'platforms/android/app/.cxx').glob('**/_deps/librashader_src-src/Cargo.toml')))
    if not roots:
        raise ValueError('Actual source-built Rust shader dependency missing')
    root = max(roots, key=lambda p: p.stat().st_mtime_ns).parent
    revision = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
    if revision != PIN:
        raise ValueError('Rust shader source pin differs')
    metadata = json.loads(subprocess.check_output([
        'cargo', 'metadata', '--locked', '--format-version', '1', '--filter-platform', 'aarch64-linux-android',
        '--manifest-path', str(root / 'librashader-capi/Cargo.toml'),
        '--no-default-features', '--features', 'runtime-vulkan,runtime-opengl',
    ], text=True))
    packages = reachable(metadata)
    def put(path, name):
        item = zipfile.ZipInfo(name)
        item.compress_type = zipfile.ZIP_DEFLATED
        notices.writestr(item, Path(path).read_bytes())
    records = []
    for package in packages:
        directory = Path(package['manifest_path']).parent
        prefix = 'rust/' + package['name'] + '-' + package['version']
        files = [p for p in directory.rglob('*') if p.is_file() and
                 p.name.lower().startswith(('license', 'licence', 'copying', 'notice', 'copyright'))]
        for path in files:
            put(path, prefix + '/' + path.relative_to(directory).as_posix())
        put(directory / 'Cargo.toml', prefix + '/Cargo.toml')
        records.append({'name': package['name'], 'version': package['version'],
                        'source': package.get('source'), 'license': package['license'],
                        'notice_files': len(files)})
    # Workspace members inherit their MPL/GPL licence from the pinned repository root.
    for path in root.iterdir():
        if path.is_file() and path.name.lower().startswith(('license', 'copying', 'notice')):
            put(path, 'rust/workspace/' + path.name)
    put(root / 'Cargo.lock', 'rust/Cargo.lock')
    for name in ('NOTICE', 'NOTICE.toolchain'):
        path = Path(ndk) / name
        if not path.is_file():
            raise ValueError('NDK runtime notices missing')
        put(path, 'android-ndk/' + name)
    notices.writestr('rust/resolved-package-licences.json', json.dumps(records, indent=2))
    archive_path = Path(work) / 'rust-corresponding-source.tar.xz'
    with tarfile.open(archive_path, 'w:xz', preset=1) as archive:
        def source_filter(member):
            return None if any(p in {'.git', 'target'} for p in Path(member.name).parts) else member
        archive.add(root, arcname='librashader', filter=source_filter)
        for package in packages:
            if package.get('source') is None:
                continue
            directory = Path(package['manifest_path']).parent
            archive.add(directory, arcname='registry/' + package['name'] + '-' + package['version'],
                        filter=source_filter)
    return {'librashader_revision': revision, 'cargo_lock_sha256': sha(root / 'Cargo.lock'),
            'resolved_packages': records, 'corresponding_source_sha256': sha(archive_path)}
