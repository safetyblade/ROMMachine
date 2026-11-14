"""Prevent regressions in the CARTDECK early Android verification dependency graph.

Runs with stdlib unittest during source-preflight; no YAML dependency required.
"""

from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "cartdeck-wii-xbox-engineering.yml"


class KotlinGateContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")
        cls.jobs = dict(
            re.findall(
                r"(?ms)^  ([a-z][a-z0-9-]*):\n(.*?)(?=^  [a-z][a-z0-9-]*:\n|\Z)",
                cls.workflow,
            )
        )

    def test_stale_pr_check_precedes_builds(self):
        source = self.jobs["source-preflight"]
        self.assertIn("fetch-depth: 0", source)
        self.assertIn("git merge-base --is-ancestor", source)
        self.assertIn("Reject stale pull-request branches", source)

    def test_compile_gate_uses_exact_host_and_full_app_test_sources(self):
        gate = self.jobs["kotlin-verify"]
        self.assertIn("needs: [source-preflight, native-host]", gate)
        self.assertIn("name: patched-libretrodroid-host", gate)
        self.assertIn("cartdeckHostMavenRepo=", gate)
        self.assertIn("cartdeckLibretroDroidVersion=0.14.0-cartdeck3", gate)
        for task in (
            ":lemuroid-app:compileFreeBundleDebugKotlin",
            ":lemuroid-app:compileFreeBundleDebugUnitTestKotlin",
            ":lemuroid-app:testFreeBundleDebugUnitTest",
        ):
            with self.subTest(task=task):
                self.assertIn(task, gate)

    def test_native_engines_wait_for_common_app_verification(self):
        # The pinned patched Android host is the one native dependency needed
        # BEFORE Kotlin compilation. All emulation engines wait until afterwards.
        self.assertIn("needs: [source-preflight]", self.jobs["native-host"])
        for name, body in self.jobs.items():
            if name.startswith("native-") and name != "native-host":
                with self.subTest(job=name):
                    needs = re.search(r"(?m)^    needs: \[(.*?)\]", body)
                    self.assertIsNotNone(needs)
                    self.assertIn("kotlin-verify", needs.group(1).split(", "))

    def test_all_apk_variants_wait_for_common_app_verification(self):
        for name, body in self.jobs.items():
            if name.startswith("apk-"):
                with self.subTest(job=name):
                    needs = re.search(r"(?m)^    needs: \[(.*?)\]", body)
                    self.assertIsNotNone(needs)
                    self.assertIn("kotlin-verify", needs.group(1).split(", "))


if __name__ == "__main__":
    unittest.main()
