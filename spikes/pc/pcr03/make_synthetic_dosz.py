#!/usr/bin/env python3
"""Build the PC-R03 synthetic guest package (synthetic_hello.dosz).

A `.dosz` is the container the CARTDECK DOS GameSystem scans for
(uniqueExtensions = ["dosz"], routed to CoreID.DOSBOX_PURE). DOSBox Pure
loads ZIP containers as guest content, so the package is a plain ZIP
archive with the synthetic payload at the archive root.

Verified vs unverified, stated plainly:
- VERIFIED from repo source: the DOS system registration scans `.dosz`
  and routes it to the DOSBox Pure core (see verify_pcr03.py).
- VERIFIED from the container itself: a ZIP with the payload at root is
  structurally what the builder emits and the verifier checks.
- UNVERIFIED: DOSBox Pure's auto-start convention for a `.bat` whose
  name matches the archive. The payload is therefore also harmless if
  only mounted and browsed from the DOSBox Pure start menu; execution
  proof is an on-device act and is NOT claimed by this preparation.

The build is deterministic: fixed entry order, fixed timestamps, fixed
compression, so the package SHA-256 is stable and testable.
100% synthetic payload: no DOS, no game files, no third-party content.
"""
import hashlib
import pathlib
import zipfile

HERE = pathlib.Path(__file__).resolve().parent
PAYLOAD = ["hello.bat", "hello.txt"]  # stored in the archive upper-case, DOS-style
FIXED_DATE = (1980, 1, 1, 0, 0, 0)


def build(out_path: pathlib.Path) -> pathlib.Path:
    with zipfile.ZipFile(out_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in PAYLOAD:
            data = (HERE / name).read_bytes()
            info = zipfile.ZipInfo(filename=name.upper(), date_time=FIXED_DATE)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, data)
    return out_path


def sha256(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


if __name__ == "__main__":
    out = build(HERE / "synthetic_hello.dosz")
    print(f"built {out} sha256={sha256(out)}")
