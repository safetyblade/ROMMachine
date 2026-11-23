#!/usr/bin/env python3
"""CARTMOD Maker reference-contract preview; not an emulator or ROM compiler.

Stdlib-only so the file can run in lightweight CI. Android implementation may
consume the same JSON project contract, but this script is not an Android runtime.
"""
from __future__ import annotations

import argparse
import copy
import json
import sys
import uuid
from pathlib import Path
from typing import Any

ALLOWED_TILES = frozenset(".#GC")
ALLOWED_TARGETS = frozenset(("GBC", "GBA"))
ALLOWED_EVENTS = frozenset(("enter", "interact"))
ALLOWED_COMPARISONS = frozenset(("eq", "ne", "gt", "gte", "lt", "lte"))
MAX_PROJECT_BYTES = 256_000
MAX_RULES = 32
MAX_STEPS = 128


class ProjectError(ValueError):
    """Untrusted or unsupported project contract."""


def _fail(message: str) -> None:
    raise ProjectError(message)


def _fields(value: Any, label: str, required: set[str], allowed: set[str]) -> dict:
    if not isinstance(value, dict):
        _fail(f"{label}: expected object")
    missing = required - set(value)
    unknown = set(value) - allowed
    if missing or unknown:
        _fail(f"{label}: missing {sorted(missing)}, unknown {sorted(unknown)}")
    return value


def _integer(value: Any, lo: int, hi: int, label: str) -> int:
    if type(value) is not int or not lo <= value <= hi:
        _fail(f"{label}: expected integer in {lo}..{hi}")
    return value


def _typed_value(value: Any, label: str) -> None:
    if type(value) is bool:
        return
    if type(value) is int and -9999 <= value <= 9999:
        return
    _fail(f"{label}: expected boolean or bounded integer")


def validate_project(data: Any) -> dict:
    """Validate the complete version-1, intentionally small no-code project format."""
    d = _fields(
        data, "project",
        {"schemaVersion", "projectId", "name", "targets", "board", "variables", "rules"},
        {"schemaVersion", "projectId", "name", "targets", "board", "variables", "rules"},
    )
    if d["schemaVersion"] != 1 or type(d["schemaVersion"]) is not int:
        _fail("schemaVersion: only version 1 is supported")
    try:
        uid = uuid.UUID(d["projectId"])
        if str(uid) != d["projectId"]:
            _fail("projectId: use canonical lowercase UUID")
    except (TypeError, ValueError, AttributeError):
        _fail("projectId: use canonical lowercase UUID")
    name = d["name"]
    if not isinstance(name, str) or not (1 <= len(name.strip()) <= 64):
        _fail("name: required, max 64 characters")
    if any(ord(ch) < 32 for ch in name):
        _fail("name: control characters not allowed")
    targets = d["targets"]
    if (not isinstance(targets, list) or not targets or len(set(map(str, targets))) != len(targets)
            or not set(targets) <= ALLOWED_TARGETS or len(targets) > 2):
        _fail("targets: unique nonempty GBC/GBA list required")
    board = _fields(
        d["board"], "board",
        {"width", "height", "rows", "player"},
        {"width", "height", "rows", "player"},
    )
    width = _integer(board["width"], 1, 30, "board.width")
    height = _integer(board["height"], 1, 20, "board.height")
    if "GBC" in targets and (width > 20 or height > 18):
        _fail("GBC board: first target profile is limited to 20x18 tiles")
    rows = board["rows"]
    if (not isinstance(rows, list) or len(rows) != height
            or any(not isinstance(row, str) or len(row) != width
                   or not set(row) <= ALLOWED_TILES for row in rows)):
        _fail("board.rows: exactly height ASCII tile rows of width characters")
    player = _fields(board["player"], "board.player", {"x", "y"}, {"x", "y"})
    px = _integer(player["x"], 0, width - 1, "player.x")
    py = _integer(player["y"], 0, height - 1, "player.y")
    if rows[py][px] == "#":
        _fail("player: cannot begin in a wall")
    variables = d["variables"]
    if not isinstance(variables, dict) or len(variables) > 32:
        _fail("variables: object of <=32 variables required")
    for key, value in variables.items():
        if (not isinstance(key, str) or not key.isidentifier() or len(key) > 32
                or key.startswith("_")):
            _fail("variables: identifiers must be public ASCII-like names <=32 chars")
        if any(ord(ch) > 127 for ch in key):
            _fail("variables: ASCII names required")
        _typed_value(value, f"variables.{key}")
    rules = d["rules"]
    if not isinstance(rules, list) or len(rules) > MAX_RULES:
        _fail(f"rules: array of at most {MAX_RULES} rules required")
    ids: set[str] = set()
    for i, rule in enumerate(rules):
        prefix = f"rules[{i}]"
        r = _fields(
            rule, prefix,
            {"id", "event", "tile", "conditions", "actions"},
            {"id", "event", "tile", "conditions", "actions"},
        )
        if not isinstance(r["id"], str) or not r["id"].isascii() or not r["id"].isidentifier():
            _fail(f"{prefix}.id: ASCII identifier required")
        if r["id"] in ids:
            _fail(f"{prefix}.id: duplicate")
        ids.add(r["id"])
        if r["event"] not in ALLOWED_EVENTS:
            _fail(f"{prefix}.event: unsupported")
        if r["tile"] not in ALLOWED_TILES - {"#"}:
            _fail(f"{prefix}.tile: unsupported")
        conditions = r["conditions"]
        actions = r["actions"]
        if (not isinstance(conditions, list) or len(conditions) > 8
                or not isinstance(actions, list) or not 1 <= len(actions) <= 8):
            _fail(f"{prefix}: <=8 conditions and 1..8 actions required")
        for j, cond in enumerate(conditions):
            c = _fields(
                cond, f"{prefix}.conditions[{j}]",
                {"variable", "compare", "value"},
                {"variable", "compare", "value"},
            )
            key = c["variable"]
            if key not in variables or c["compare"] not in ALLOWED_COMPARISONS:
                _fail(f"{prefix}: unknown variable/comparison")
            _typed_value(c["value"], f"{prefix}.condition.value")
            if type(c["value"]) is not type(variables[key]):
                _fail(f"{prefix}: condition type mismatch")
            if type(c["value"]) is bool and c["compare"] not in ("eq", "ne"):
                _fail(f"{prefix}: boolean supports only eq/ne")
        for j, action in enumerate(actions):
            a = _fields(
                action, f"{prefix}.actions[{j}]",
                {"type", "variable", "value"},
                {"type", "variable", "value"},
            )
            key = a["variable"]
            if key not in variables or a["type"] not in ("set", "add"):
                _fail(f"{prefix}: unsupported action or variable")
            _typed_value(a["value"], f"{prefix}.action.value")
            if a["type"] == "set" and type(a["value"]) is not type(variables[key]):
                _fail(f"{prefix}: set type mismatch")
            if a["type"] == "add" and (type(a["value"]) is not int or type(variables[key]) is not int):
                _fail(f"{prefix}: add requires integer variable and value")
    return d


def load_project(path: str | Path) -> dict:
    p = Path(path)
    if p.stat().st_size > MAX_PROJECT_BYTES:
        _fail("project is too large")
    with p.open("r", encoding="utf-8") as handle:
        return validate_project(json.load(handle))


def new_state(project: dict) -> dict:
    validate_project(project)
    return {"x": project["board"]["player"]["x"],
            "y": project["board"]["player"]["y"],
            "variables": copy.deepcopy(project["variables"]), "steps": 0}


def _compare(left: int | bool, cmp: str, right: int | bool) -> bool:
    if cmp == "eq":
        return left == right
    if cmp == "ne":
        return left != right
    if cmp == "gt":
        return left > right
    if cmp == "gte":
        return left >= right
    if cmp == "lt":
        return left < right
    if cmp == "lte":
        return left <= right
    raise AssertionError(cmp)


def _fire(project: dict, state: dict, event: str, tile: str) -> None:
    for rule in project["rules"]:
        if rule["event"] != event or rule["tile"] != tile:
            continue
        if not all(_compare(state["variables"][c["variable"]], c["compare"], c["value"])
                   for c in rule["conditions"]):
            continue
        for action in rule["actions"]:
            key = action["variable"]
            if action["type"] == "set":
                state["variables"][key] = action["value"]
            else:
                next_value = state["variables"][key] + action["value"]
                _integer(next_value, -9999, 9999, f"runtime.{key}")
                state["variables"][key] = next_value


def step(project: dict, state: dict, command: str) -> dict:
    """Simulate one touch action, with no scripts or mutable project content."""
    if state["steps"] >= MAX_STEPS:
        _fail("preview budget exceeded")
    if command not in ("U", "D", "L", "R", "I"):
        _fail("commands: use U, D, L, R, I")
    output = copy.deepcopy(state)
    board = project["board"]
    tile = board["rows"][output["y"]][output["x"]]
    if command == "I":
        _fire(project, output, "interact", tile)
    else:
        dx, dy = {"U": (0, -1), "D": (0, 1), "L": (-1, 0), "R": (1, 0)}[command]
        x, y = output["x"] + dx, output["y"] + dy
        if 0 <= x < board["width"] and 0 <= y < board["height"] and board["rows"][y][x] != "#":
            output["x"], output["y"] = x, y
            _fire(project, output, "enter", board["rows"][y][x])
    output["steps"] += 1
    return output


def preview(project: dict, commands: list[str]) -> dict:
    validate_project(project)
    if len(commands) > MAX_STEPS:
        _fail("preview action budget exceeded")
    state = new_state(project)
    for command in commands:
        state = step(project, state, command)
    return state


def main() -> int:
    parser = argparse.ArgumentParser(description="CARTMOD Maker isolated contract/preview experiment")
    sub = parser.add_subparsers(dest="command", required=True)
    for command in ("validate", "preview"):
        item = sub.add_parser(command)
        item.add_argument("project", help="Path to a .cartproj.json file")
        if command == "preview":
            item.add_argument("--actions", default="", help="Comma-separated U,D,L,R,I actions")
    args = parser.parse_args()
    try:
        project = load_project(args.project)
        if args.command == "validate":
            print(json.dumps({"valid": True, "name": project["name"], "targets": project["targets"]}))
        else:
            actions = args.actions.split(",") if args.actions else []
            print(json.dumps(preview(project, actions), sort_keys=True))
        return 0
    except (OSError, UnicodeError, json.JSONDecodeError, ProjectError) as exc:
        print(f"Invalid or unsupported project: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
