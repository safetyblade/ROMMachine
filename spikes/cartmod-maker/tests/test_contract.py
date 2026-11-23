"""Contract-only tests. No Android, native compiler, emulator or ROM artifact required."""
import copy
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
import maker_reference as maker


def sample(name="pocket-puzzle"):
    return maker.load_project(ROOT / "examples" / f"{name}.cartproj.json")


class MakerContractTests(unittest.TestCase):
    def test_both_sample_projects_validate(self):
        for name in ("pocket-puzzle", "pocket-shop"):
            project = sample(name)
            self.assertEqual(project["targets"], ["GBC", "GBA"])

    def test_project_schema_is_valid_json_with_required_contract(self):
        with (ROOT / "project.schema.json").open(encoding="utf-8") as file:
            schema = json.load(file)
        self.assertEqual(schema["properties"]["schemaVersion"]["const"], 1)
        self.assertIn("rules", schema["required"])

    def test_puzzle_reaches_goal(self):
        state = maker.preview(sample(), ["R", "R", "D"])
        self.assertEqual((state["x"], state["y"]), (3, 2))
        self.assertTrue(state["variables"]["won"])

    def test_wall_does_not_move_player(self):
        state = maker.preview(sample(), ["U"])
        self.assertEqual((state["x"], state["y"]), (1, 1))
        self.assertFalse(state["variables"]["won"])

    def test_shop_sells_until_out_of_stock(self):
        state = maker.preview(sample("pocket-shop"), ["I", "I", "I"])
        self.assertEqual(state["variables"], {"coins": 6, "stock": 0, "won": True})
        self.assertEqual(state["steps"], 3)

    def test_preview_does_not_mutate_editable_source(self):
        project = sample("pocket-shop")
        old = copy.deepcopy(project)
        maker.preview(project, ["I", "I"])
        self.assertEqual(project, old)

    def test_unknown_target_rejected(self):
        project = sample()
        project["targets"] = ["PSP"]
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)

    def test_unhashable_target_rejected(self):
        project = sample()
        project["targets"] = [{"system": "GBC"}]
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)

    def test_color_tile_budget(self):
        project = sample()
        project["board"] = {
            "width": 21, "height": 1, "rows": ["." * 21], "player": {"x": 0, "y": 0}
        }
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)
        project["targets"] = ["GBA"]
        maker.validate_project(project)

    def test_invalid_rule_condition_rejected(self):
        project = sample()
        project["rules"][0]["conditions"] = [
            {"variable": "unknown", "compare": "gte", "value": 1}
        ]
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)

    def test_add_to_boolean_rejected(self):
        project = sample()
        project["rules"][0]["actions"] = [
            {"type": "add", "variable": "won", "value": 1}
        ]
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)

    def test_duplicate_rule_ids_rejected(self):
        project = sample()
        project["rules"].append(copy.deepcopy(project["rules"][0]))
        with self.assertRaises(maker.ProjectError):
            maker.validate_project(project)

    def test_invalid_command_rejected(self):
        with self.assertRaises(maker.ProjectError):
            maker.preview(sample(), ["R", "RUN SHELL"])

    def test_preview_budget_rejected(self):
        with self.assertRaises(maker.ProjectError):
            maker.preview(sample(), ["U"] * 129)

    def test_int_overflow_does_not_wrap(self):
        project = sample("pocket-shop")
        project["rules"][0]["actions"] = [
            {"type": "add", "variable": "coins", "value": 9999}
        ]
        with self.assertRaises(maker.ProjectError):
            maker.preview(project, ["I", "I"])

    def test_example_json_round_trips(self):
        for name in ("pocket-puzzle", "pocket-shop"):
            project = sample(name)
            self.assertEqual(maker.validate_project(json.loads(json.dumps(project))), project)

    def test_cli_preview(self):
        project = ROOT / "examples" / "pocket-puzzle.cartproj.json"
        run = subprocess.run(
            [sys.executable, str(ROOT / "maker_reference.py"), "preview",
             str(project), "--actions", "R,R,D"],
            text=True, capture_output=True, check=True, timeout=10,
        )
        self.assertTrue(json.loads(run.stdout)["variables"]["won"])

    def test_rejects_oversized_untrusted_project(self):
        with tempfile.TemporaryDirectory() as tmp:
            file = Path(tmp) / "bad.json"
            file.write_text(" " * (maker.MAX_PROJECT_BYTES + 1), encoding="utf-8")
            with self.assertRaises(maker.ProjectError):
                maker.load_project(file)


if __name__ == "__main__":
    unittest.main()
