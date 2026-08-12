#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).with_name("check_self_hosted_runner_policy.py")
SPEC = importlib.util.spec_from_file_location("runner_policy", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
RUNNER_POLICY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RUNNER_POLICY)


class SelfHostedRunnerPolicyTest(unittest.TestCase):
    def find(self, files: dict[str, str]) -> list[tuple[str, int]]:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for name, contents in files.items():
                (root / name).write_text(contents, encoding="utf-8")
            return [
                (path.name, line)
                for path, line in RUNNER_POLICY.find_violations(root)
            ]

    def test_rejects_scalar_flow_and_block_sequence_labels(self) -> None:
        files = {
            "scalar.yml": "jobs:\n  test:\n    runs-on: self-hosted\n",
            "flow.yml": "jobs:\n  test:\n    runs-on: [self-hosted, linux]\n",
            "block.yml": "jobs:\n  test:\n    runs-on:\n      - linux\n      - self-hosted\n",
        }

        self.assertEqual(
            self.find(files),
            [("block.yml", 5), ("flow.yml", 3), ("scalar.yml", 3)],
        )

    def test_rejects_indirect_matrix_label(self) -> None:
        workflow = """jobs:
  test:
    strategy:
      matrix:
        runner: [ubuntu-latest, self-hosted]
    runs-on: ${{ matrix.runner }}
"""

        self.assertEqual(self.find({"matrix.yml": workflow}), [("matrix.yml", 5)])

    def test_allows_github_hosted_runner_and_comments(self) -> None:
        workflow = """jobs:
  test:
    runs-on: ubuntu-latest # self-hosted is forbidden here
"""

        self.assertEqual(self.find({"hosted.yml": workflow}), [])

    def test_allows_only_deploy_dev_and_checks_policy_runs_on(self) -> None:
        files = {
            "deploy-dev.yml": "jobs:\n  deploy:\n    runs-on: [self-hosted, linux, arm64]\n",
            "self-hosted-runner-policy.yml": (
                "name: Self-hosted Runner Policy\n"
                "jobs:\n  check:\n    runs-on: self-hosted\n"
            ),
        }

        self.assertEqual(
            self.find(files), [("self-hosted-runner-policy.yml", 4)]
        )


if __name__ == "__main__":
    unittest.main()
