# PC-R03 preparation — synthetic guest package

Source-level preparation for gate **PC-R03** (synthetic guest execution).
**On-device execution is not done and is not claimed**; it is the gate's
remaining half and happens on device, later, as its own proof.

Contents:

- `hello.bat`, `hello.txt` — the synthetic payload. 100% synthetic:
  authored for this gate; no DOS system files, no game data, no
  third-party material.
- `make_synthetic_dosz.py` — deterministic builder. Emits
  `synthetic_hello.dosz` (a ZIP container with the payload at the
  archive root; fixed entry order/timestamps/compression, stable
  SHA-256). The built `.dosz` is a generated artefact and is not
  committed; rebuild it with this script.
- `../verify_pcr03.py` — verifier: package structure + payload markers
  + deterministic rebuild + the repo's DOS registration resolving
  `.dosz` to `CoreID.DOSBOX_PURE` (the binary audited in PC-R02).
- `../tests/test_verify_pcr03.py` — synthetic unit tests (unittest).

Verified vs unverified:

- VERIFIED: DOS system scans `.dosz` and routes to DOSBox Pure (repo
  source, checked mechanically); package structure (checked mechanically).
- UNVERIFIED: DOSBox Pure's auto-start convention for a payload `.bat`.
  If auto-start does not occur, the payload is still reachable from the
  DOSBox Pure start menu once mounted — the device run will record
  which behaviour the shipped binary actually has.
