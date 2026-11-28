#!/usr/bin/env python3
"""PC-R03 preparation — synthetic guest package + registry verification.

Source-level proof only. This script verifies:
  1. The synthetic package (synthetic_hello.dosz, built by
     pcr03/make_synthetic_dosz.py) is a structurally valid ZIP container
     with exactly the synthetic payload at the archive root, and that the
     payload carries its synthetic provenance markers.
  2. The build is deterministic (rebuild -> identical SHA-256).
  3. The repo's DOS GameSystem registration resolves `.dosz` to
     CoreID.DOSBOX_PURE, and CoreID.DOSBOX_PURE names the audited binary
     libdosbox_pure_libretro_android.so (PC-R02, PR #386).

It does NOT execute a guest, boot DOS, or prove on-device behaviour.
PC-R03's execution half remains an on-device gate. Auto-start of the
payload .bat inside DOSBox Pure is UNVERIFIED by this script.

Usage:
  python3 spikes/pc/verify_pcr03.py [--repo-root PATH] [--package PATH]
Exit code 0 = all checks true; 1 = at least one check false.
"""
import argparse
import hashlib
import pathlib
import sys
import tempfile
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent / "pcr03"))
import make_synthetic_dosz as builder  # noqa: E402

EXPECTED_ENTRIES = {"HELLO.BAT", "HELLO.TXT"}
BAT_MARKER = b"CARTDECK PC-R03 SYNTHETIC GUEST PAYLOAD"
TXT_MARKER = b"PCR03-SYNTHETIC-HELLO-20261128"
GAME_SYSTEM = "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/GameSystem.kt"
CORE_ID = "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/CoreID.kt"


def check_package(package: pathlib.Path) -> dict:
    out = {}
    out["package_exists"] = package.is_file()
    if not out["package_exists"]:
        return out
    out["zip_magic"] = package.read_bytes()[:2] == b"PK"
    try:
        with zipfile.ZipFile(package) as z:
            names = set(z.namelist())
            out["entries_exact"] = names == EXPECTED_ENTRIES
            out["entries_at_root"] = all("/" not in n for n in names)
            if "HELLO.BAT" in names:
                out["bat_marker"] = BAT_MARKER in z.read("HELLO.BAT")
            if "HELLO.TXT" in names:
                out["txt_marker"] = TXT_MARKER in z.read("HELLO.TXT")
            out["zip_integrity"] = z.testzip() is None
    except zipfile.BadZipFile:
        out["zip_integrity"] = False
    return out


def check_deterministic() -> dict:
    with tempfile.TemporaryDirectory() as td:
        a = builder.build(pathlib.Path(td) / "a.dosz")
        b = builder.build(pathlib.Path(td) / "b.dosz")
        return {"deterministic_rebuild": builder.sha256(a) == builder.sha256(b)}


def check_registry(repo_root: pathlib.Path) -> dict:
    out = {}
    gs_path, ci_path = repo_root / GAME_SYSTEM, repo_root / CORE_ID
    out["gamesystem_present"] = gs_path.is_file()
    out["coreid_present"] = ci_path.is_file()
    if out["gamesystem_present"]:
        text = gs_path.read_text(encoding="utf-8", errors="replace")
        idx = text.find("SystemID.DOS")
        window = text[idx:idx + 3000] if idx >= 0 else ""
        out["dos_block_found"] = idx >= 0
        out["dos_scans_dosz"] = '"dosz"' in window
        out["dos_routes_dosbox_pure"] = "DOSBOX_PURE" in window
    if out["coreid_present"]:
        text = ci_path.read_text(encoding="utf-8", errors="replace")
        idx = text.find("DOSBOX_PURE")
        window = text[idx:idx + 400] if idx >= 0 else ""
        out["coreid_names_audited_binary"] = "libdosbox_pure_libretro_android.so" in window
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo-root", type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[2])
    ap.add_argument("--package", type=pathlib.Path, default=None)
    args = ap.parse_args()

    package = args.package
    if package is None:
        package = pathlib.Path(__file__).resolve().parent / "pcr03" / "synthetic_hello.dosz"
        if not package.is_file():
            builder.build(package)

    checks = {}
    checks.update(check_package(package))
    checks.update(check_deterministic())
    checks.update(check_registry(args.repo_root))
    for name, ok in checks.items():
        print(f"{'PASS' if ok else 'FAIL'}  {name}")
    status = all(checks.values()) and len(checks) >= 10
    print(f"status: {'PASS' if status else 'FAIL'} ({sum(checks.values())}/{len(checks)} checks true)")
    print(f"package sha256: {hashlib.sha256(package.read_bytes()).hexdigest()}")
    return 0 if status else 1


if __name__ == "__main__":
    raise SystemExit(main())
