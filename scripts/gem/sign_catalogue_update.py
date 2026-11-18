#!/usr/bin/env python3
"""Sign a separately reviewed GEM full-catalogue JSON with an external release key.

Private keys are never generated, copied, logged, or stored by this script.
Run outside the application with a controlled private RSA key. Signature approval
does not substitute for source/licence review or Android catalogue verification.
"""
import argparse
import json
import re
import struct
import subprocess
from pathlib import Path

MAX_BYTES = 32 * 1024 * 1024
MAX_RELEASES = 100_000


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--sequence", required=True, type=int)
    parser.add_argument("--private-key", required=True, type=Path)
    args = parser.parse_args()
    if not 0 < args.sequence < 2**63:
        parser.error("sequence must be a positive signed 64-bit integer")
    if not 0 < args.manifest.stat().st_size <= MAX_BYTES:
        parser.error("manifest exceeds accepted size")
    payload = args.manifest.read_bytes()
    data = json.loads(payload)
    if (
        data.get("schemaVersion") != 1
        or not re.fullmatch(r"[A-Za-z0-9._-]{1,128}", data.get("catalogueVersion", ""))
        or not 0 < data.get("releaseCount", 0) <= MAX_RELEASES
        or len(data.get("releases", [])) != data["releaseCount"]
        or not data.get("sources")
    ):
        parser.error("manifest is not a valid candidate for schema 1")
    releases = data["releases"]
    if len({row["releaseId"] for row in releases}) != len(releases):
        parser.error("duplicate release ID")
    header = b"GEMC1" + struct.pack(">qI", args.sequence, len(payload))
    body = header + payload
    signature = subprocess.run(
        ["openssl", "dgst", "-sha256", "-sign", str(args.private_key)],
        input=body, stdout=subprocess.PIPE, check=True,
    ).stdout
    if not 256 <= len(signature) <= 1024:
        parser.error("expected RSA signature from a 2048–8192-bit key")
    # Exclusive creation prevents accidental replacement of an issued version.
    with args.output.open("xb") as output:
        output.write(body + struct.pack(">I", len(signature)) + signature)
    print(f"Signed {data['catalogueVersion']} sequence {args.sequence}; {len(releases)} records")


if __name__ == "__main__":
    main()
