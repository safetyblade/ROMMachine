#!/usr/bin/env python3
"""Record actual APK command wall time; optionally repeat in the same workspace.

The caller cleans project outputs and disables the Gradle build cache for the
opt-in comparison. Dependency/toolchain caches remain available. Native producer
durations are never included or presented as APK timings.
"""
import argparse
import hashlib
import json
import subprocess
import time
from pathlib import Path


def inventory(root):
    return {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(root.rglob("*")) if p.is_file()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--repeat", choices=("true", "false", ""), default="false")
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not command:
        parser.error("APK build command required")
    root = Path("work/connected/ci01-native")
    before = inventory(root)
    report = {"scope": "Gradle checks and APK assembly; excludes native producers and staging",
              "native_input_files": len(before),
              "dependency_caches": "retained",
              "project_outputs_cleaned_and_build_cache_disabled": args.repeat == "true",
              "measurements": []}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    for label in (["workspace_cold", "workspace_warm"] if args.repeat == "true" else ["first_build"]):
        start = time.perf_counter()
        completed = subprocess.run(command, check=False)
        measurement = {"label": label, "wall_seconds": time.perf_counter() - start,
                       "exit_code": completed.returncode}
        apks = sorted(Path("lemuroid-app/build/outputs/apk/freeBundle/debug").glob("*.apk"))
        if completed.returncode == 0 and len(apks) == 1:
            measurement["apk_sha256"] = hashlib.sha256(apks[0].read_bytes()).hexdigest()
        report["measurements"].append(measurement)
        report["native_inputs_unchanged"] = inventory(root) == before
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
        if completed.returncode:
            return completed.returncode
        if "apk_sha256" not in measurement or not report["native_inputs_unchanged"]:
            raise RuntimeError("APK missing/ambiguous or native inputs changed during APK build")
    if len(report["measurements"]) == 2:
        report["same_apk_sha256"] = (report["measurements"][0]["apk_sha256"] ==
                                     report["measurements"][1]["apk_sha256"])
        args.report.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
        if not report["same_apk_sha256"]:
            raise RuntimeError("Warm APK differs from first build")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
