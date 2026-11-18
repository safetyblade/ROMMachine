"""Build-bound engine coverage baseline, never a physical runtime benchmark."""
import argparse
import csv
import hashlib
import json
import pathlib
import re
import subprocess
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
# Each implementation appears once, even when shared across several systems.
# APK presence is evaluated separately from configuration and runtime acceptance.
LEGACY = {
    "stella": ["atari2600"], "prosystem": ["atari7800"], "handy": ["lynx"],
    "fceumm": ["nes"], "snes9x": ["snes"], "gambatte": ["gb", "gbc"],
    "mgba": ["gba"], "genesis_plus_gx": ["sms", "genesis", "segacd", "gg"],
    "mednafen_pce_fast": ["pc_engine"], "mednafen_ngp": ["ngp", "ngc"],
    "mednafen_wswan": ["ws", "wsc"], "mupen64plus_next_gles3": ["n64"],
    "pcsx_rearmed": ["psx"], "ppsspp": ["psp"], "desmume": ["nds"],
    "melonds": ["nds"], "citra": ["3ds"], "fbneo": ["fbneo"],
    "mame2003_plus": ["mame2003plus"], "dosbox_pure": ["dos"],
}
MODERN = {
    "pcsx_rearmed_modern": (["psx"], "cartdeck/pcsx-rearmed-modern.properties"),
    "ppsspp_modern": (["psp"], "cartdeck/ppsspp-modern.properties"),
    "flycast": (["dreamcast"], "cartdeck/flycast.properties"),
    "dolphin_modern": (["gamecube", "wii"], "cartdeck/dolphin-modern.properties"),
    "play_modern": (["ps2"], "cartdeck/play-modern.properties"),
}
NATIVE = {
    "armsx2": (["ps2"], ["libemucore_4k.so", "libemucore_16k.so"], "cartdeck/armsx2/armsx2.properties"),
    "hakux": (["xbox"], ["libhakuxcore.so"], "cartdeck/hakux.properties"),
    "xenia": (["xbox360"], ["libcartdeck_xenia.so"], "cartdeck/xenia.properties"),
    "cemu": (["wiiu"], ["libcemu_libretro_android.so"], "cartdeck/cemu.properties"),
    "armsx3": (["ps3"], ["libarmsx3-core.so"], "cartdeck/ps3/armsx3.properties"),
    "rpcs3-libretro": (["ps3"], ["librpcs3_libretro.so"], "cartdeck/ps3/rpcs3-libretro.properties"),
}
METRICS = ("cold_launch_ms", "first_visible_frame_ms", "guest_speed_ratio", "present_interval_p95_ms",
           "audio_underruns", "memory_peak_bytes", "sustained_thermal", "input_latency_ms")


def sha_file(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def git(directory, *args):
    return subprocess.check_output(["git", "-C", str(directory), *args], text=True).strip()


def manifest_values(archive, path):
    name = "assets/" + path
    if name not in archive.namelist():
        return {}
    values = {}
    for line in archive.read(name).decode("utf-8", errors="replace").splitlines():
        key, sep, value = line.partition("=")
        if sep and not key.lstrip().startswith(("#", "!")):
            key = key.strip()
            if key in values:
                return {}
            values[key] = value.strip()
    return values


def revision_for_manifest(values, name, digest):
    declared = values.get("library." + name + ".sha256") or values.get("core.sha256")
    revision = values.get("revision") or values.get("upstream.revision")
    if (declared and declared.lower() == digest.lower() and revision
            and re.fullmatch(r"[0-9a-fA-F]{40}", revision)):
        return revision
    return None


def package_identity(submodule, pin, name, digest):
    matches = list(submodule.glob("*/src/main/jniLibs/arm64-v8a/" + name))
    if len(matches) != 1 or not matches[0].is_file():
        return None
    path = matches[0]
    if sha_file(path) != digest:
        return None
    relative = path.relative_to(submodule).as_posix()
    blob = git(submodule, "rev-parse", "HEAD:" + relative)
    # The APK digest matches checked-out bytes; also bind those bytes to the git blob.
    if git(submodule, "hash-object", relative) != blob:
        return None
    return {"legacy_package_revision": pin, "package_git_blob": blob}


def engine_row(engine, systems, libraries, records, archive, manifest=None, legacy=None):
    components = []
    props = manifest_values(archive, manifest) if manifest else {}
    for lib in libraries:
        row = records.get("lib/arm64-v8a/" + lib)
        result = {"library": lib, "packaged": row is not None, "sha256": None,
                  "upstream_revision_verified_by_digest": None, "legacy_package": None}
        if row:
            result.update(sha256=row["sha256"], size_bytes=int(row["size_bytes"]),
                          elf_16kb=row["elf_16kb"])
            if props:
                result["upstream_revision_verified_by_digest"] = revision_for_manifest(props, lib, row["sha256"])
            if legacy:
                result["legacy_package"] = package_identity(*legacy, lib, row["sha256"])
        components.append(result)
    return {"engine": engine, "systems": systems,
            "packaged": all(c["packaged"] for c in components),
            "components": components, "physical_acceptance": "NOT_TESTED",
            "metrics": dict.fromkeys(METRICS)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=pathlib.Path, required=True)
    parser.add_argument("--evidence", type=pathlib.Path, required=True)
    args = parser.parse_args()
    source = git(ROOT, "rev-parse", "HEAD")
    submodule = ROOT / "lemuroid-cores"
    pin = git(ROOT, "rev-parse", "HEAD:lemuroid-cores")
    if git(submodule, "rev-parse", "HEAD") != pin:
        raise ValueError("Legacy package checkout does not match app source")
    digest = sha_file(args.apk)
    summary = (args.evidence / "16kb-elf-summary.txt").read_text()
    if f"APK_SHA256: {digest}\n" not in summary:
        raise ValueError("Native inventory belongs to a different APK")
    with (args.evidence / "native-libraries.tsv").open(newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    records = {row["apk_entry"]: row for row in rows}
    if len(records) != len(rows):
        raise ValueError("Duplicate native inventory entries")
    with zipfile.ZipFile(args.apk) as archive:
        names = [item.filename for item in archive.infolist() if item.filename.endswith(".so")]
        if len(names) != len(set(names)) or set(names) != set(records):
            raise ValueError("APK and native inventory disagree")
        engines = [
            engine_row(name, systems, ["lib" + name + "_libretro_android.so"], records, archive,
                       legacy=(submodule, pin))
            for name, systems in LEGACY.items()
        ]
        engines += [
            engine_row(name, systems, ["lib" + name + "_libretro_android.so"], records, archive, manifest)
            for name, (systems, manifest) in MODERN.items()
        ]
        engines += [
            engine_row(name, systems, libraries, records, archive, manifest)
            for name, (systems, libraries, manifest) in NATIVE.items()
        ]
        host = records.get("lib/arm64-v8a/liblibretrodroid.so")
        if host is None:
            raise ValueError("LibretroDroid host missing from connected APK")
    report = {"schema": 1, "app_source": source, "apk_sha256": digest,
              "legacy_package_revision": pin, "host": {"sha256": host["sha256"],
              "elf_16kb": host["elf_16kb"], "exact_bundle_proof": "preserved-engines.json"},
              "configured_implementation_count": len(engines),
              "packaged_implementation_count": sum(row["packaged"] for row in engines),
              "engines": engines, "note": "Binary inventory only; actual selected rollback binaries, graphics, and gameplay require device evidence."}
    (args.evidence / "perf03-engine-baseline.json").write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print(f"PERF-03 inventory: {report['packaged_implementation_count']}/{len(engines)} configured implementations packaged")


if __name__ == "__main__":
    main()
