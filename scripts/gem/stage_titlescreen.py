"""Prepare an exact, review-pinned branch candidate, not a release approval."""
import argparse, collections, hashlib, json, re
from pathlib import Path

VERSION = '2026.10.09'
ARCHIVE_SHA = 'b352e559ba211d646f32dadd4f2e44798bc75e2a44021a7c57ee79f094bd1555'
SOURCE_SHA = '71f3470b7dfd25c9461bf6673b2e7475894da06bba455fa9a9e22eead0a01160'
SOURCE = dict(id='titlescreen', name='Title Screen', url='https://titlescreen.org/downloads', licence='CC-BY-4.0', attribution='Data: Title Screen (https://titlescreen.org), CC BY 4.0; adapted by CARTDECK (commercial game metadata, filtered promotional records).', revision=VERSION + ' sha256:' + ARCHIVE_SHA)
# Quarantine obvious promotional/media markers, not generic Trial or Prototype game titles.
QUARANTINE = re.compile(r'\b(?:demo(?:nstration)?\b|bios\b|firmware\b|test disc\b|system update\b|bonus disc\b|video disc\b|magazine(?: game)? disc\b|trial (?:edition|version)\b)|店頭デモ|体験版|試遊|デモディスク', re.I)

def encode(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8')

def sha(data):
    return hashlib.sha256(data).hexdigest()

def stage(candidate, out):
    report = json.loads((candidate / 'coverage.json').read_text(encoding='utf-8'))
    if report['candidateVersion'] != VERSION or report['sourceSha256'] != SOURCE_SHA or report['candidateRecords'] != 75395:
        raise ValueError('Unexpected source snapshot')
    expected = {s: n for s, n in report['candidateBySystem'].items() if n}
    if len(expected) != 30 or sum(expected.values()) != 75395:
        raise ValueError('Unexpected source coverage')
    if {p.stem for p in candidate.glob('*.json') if p.name != 'coverage.json'} != set(expected):
        raise ValueError('Unexpected candidate file set')
    out.mkdir(parents=True, exist_ok=False)
    (out / 'titlescreen').mkdir()
    pins, excluded, seen = [], [], set()
    for system, expected_count in sorted(expected.items()):
        package = json.loads((candidate / (system + '.json')).read_text(encoding='utf-8'))
        version = 'gem-ts-' + VERSION + '-' + system
        if package['catalogueVersion'] != version or len(package['releases']) != expected_count:
            raise ValueError('Candidate shard mismatch: ' + system)
        kept = []
        for row in package['releases']:
            identity, title = row['releaseId'], row['title']
            if (set(row) - {'releaseId', 'sourceId', 'sourceRecordId', 'systemId', 'title', 'primaryGenre'} or row['systemId'] != system or row['sourceId'] != SOURCE['id'] or identity != 'TS:' + row['sourceRecordId'] or identity in seen or not re.fullmatch(r'r[a-z0-9]{7}', row['sourceRecordId']) or not title.strip() or not 0 < len(title.encode('utf-16-le')) // 2 <= 512 or any(ord(c) < 32 for c in title) or len(row.get('primaryGenre', '').encode('utf-16-le')) // 2 > 128):
                raise ValueError('Invalid candidate row: ' + identity)
            seen.add(identity)
            if QUARANTINE.search(title):
                excluded.append(row)
            else:
                kept.append(dict(row, verifiedSha256=[], verifiedSerials=[]))
        kept.sort(key=lambda row: row['releaseId'])
        if not kept:
            raise ValueError('Empty shard: ' + system)
        data = encode(dict(schemaVersion=1, catalogueVersion=version, releaseCount=len(kept), sources=[SOURCE], releases=kept))
        if len(data) > 32 * 1024 * 1024:
            raise ValueError('Oversize shard')
        (out / 'titlescreen' / (system + '.json')).write_bytes(data)
        pins.append(dict(path='gem/titlescreen/' + system + '.json', version=version, sha256=sha(data), count=len(kept), sources=[SOURCE]))
    if len(seen) != 75395:
        raise ValueError('Unexpected source count')
    data = encode(pins)
    (out / 'titlescreen-pins.json').write_bytes(data)
    (out / 'titlescreen-quarantine.json').write_bytes(encode(sorted(excluded, key=lambda row: row['releaseId'])))
    print(json.dumps(dict(sha256=sha(data), kept=sum(p['count'] for p in pins), quarantined=len(excluded), quarantineSha256=sha((out / 'titlescreen-quarantine.json').read_bytes()), counts={p['path']: p['count'] for p in pins}), indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('candidate', type=Path)
    parser.add_argument('out', type=Path)
    args = parser.parse_args()
    stage(args.candidate, args.out)
