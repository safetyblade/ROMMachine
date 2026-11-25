#!/usr/bin/env python3
"""PERF-XB: compare explicit device observations without inventing guest FPS.

Accepts only manually exported, sanitized CARTDECK reports plus tester-supplied
fixture metadata. Native host presents, guest swaps and generated frames are
never treated as unique correct game frames.
"""
import argparse
import json
import math
import re
from dataclasses import dataclass
from pathlib import Path


METRIC_LINE = re.compile(
    r"^(?P<name>[a-z][a-z0-9_]+) count=(?P<count>[0-9]+) "
    r"p50_ms=(?P<p50>[0-9.]+) p95_ms=(?P<p95>[0-9.]+) "
    r"p99_ms=(?P<p99>[0-9.]+)"
)


@dataclass(frozen=True)
class Run:
    console: str
    title_id: str
    title_sha256: str
    scene: str
    native_build_id: str
    device: str
    driver: str
    settings: str
    cache_state: str
    run_seconds: int
    report: str
    integrity: str = "NOT_TESTED"

    @classmethod
    def from_mapping(cls, value):
        allowed = set(cls.__dataclass_fields__)
        if set(value) - allowed:
            raise ValueError("Unknown benchmark fields")
        obj = cls(**value)
        if obj.console not in {"XBOX", "XBOX360"}:
            raise ValueError("Unsupported benchmark console")
        if obj.integrity not in {"PASS", "FAIL", "NOT_TESTED"}:
            raise ValueError("Unknown graphics-integrity result")
        if not re.fullmatch(r"[0-9a-fA-F]{64}", obj.title_sha256):
            raise ValueError("Title SHA-256 is required")
        if not all((obj.title_id, obj.scene, obj.native_build_id, obj.device,
                    obj.driver, obj.settings, obj.cache_state)):
            raise ValueError("Comparison fixture identity is incomplete")
        if not isinstance(obj.run_seconds, int) or obj.run_seconds < 1:
            raise ValueError("Measured run duration required")
        return obj


def metrics(text):
    """CPU-monotonic event durations, not actual GPU execution or game FPS."""
    rows = {}
    for line in text.splitlines():
        match = METRIC_LINE.match(line.strip())
        if match:
            name = match.group("name")
            if name in rows:
                raise ValueError("Duplicate metric: " + name)
            row = {key: float(match.group(key)) for key in ("p50", "p95", "p99")}
            row["count"] = int(match.group("count"))
            if not all(math.isfinite(v) and v >= 0 for v in row.values()):
                raise ValueError("Nonfinite/negative timing")
            if row["count"] > 0 and not (row["p50"] <= row["p95"] <= row["p99"]):
                raise ValueError("Malformed percentile ordering")
            rows[name] = row
    return rows


def compare(baseline, candidate, name="guest_swap_interval"):
    """Strictly comparable diagnostic evidence; never claims device acceptance."""
    comparable = (
        "console", "title_id", "title_sha256", "scene",
        "device", "driver", "settings", "cache_state"
    )
    different = [field for field in comparable if
                 getattr(baseline, field) != getattr(candidate, field)]
    if different:
        raise ValueError("Different benchmark fixtures: " + ", ".join(different))
    if baseline.console == "XBOX360" and (
        "performance_schema=" not in baseline.report or
        "performance_schema=" not in candidate.report
    ):
        raise ValueError("Missing native performance report")
    if baseline.integrity != "PASS" or candidate.integrity != "PASS":
        return {"result": "NOT_ACCEPTED", "reason": "graphics integrity not confirmed"}
    a, b = metrics(baseline.report).get(name), metrics(candidate.report).get(name)
    if a is None or b is None or a["count"] < 50 or b["count"] < 50 or a["p95"] == 0:
        return {"result": "INSUFFICIENT", "reason": "measured timing sample missing"}
    change = 100.0 * (a["p95"] - b["p95"]) / a["p95"]
    return {
        "result": "DIAGNOSTIC_ONLY",
        "event": name,
        "baseline_p95_ms": a["p95"],
        "candidate_p95_ms": b["p95"],
        "change_pct": round(change, 2),
        "fps_verified": False,
        "guest_simulation_verified": False,
        "graphics_verified": True,
        "message": "Event pacing only; owner gameplay and real-frame checks required",
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--metric", default="guest_swap_interval")
    args = parser.parse_args()
    base = Run.from_mapping(json.loads(args.baseline.read_text()))
    candidate = Run.from_mapping(json.loads(args.candidate.read_text()))
    print(json.dumps(compare(base, candidate, args.metric), indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
