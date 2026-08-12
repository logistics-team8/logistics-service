#!/usr/bin/env python3
"""Reject self-hosted runner references outside the Dev deployment workflow."""

from __future__ import annotations

import re
import sys
from pathlib import Path


ALLOWED_WORKFLOW = "deploy-dev.yml"
POLICY_WORKFLOW = "self-hosted-runner-policy.yml"
SELF_HOSTED = re.compile(r"(?<![A-Za-z0-9_-])self-hosted(?![A-Za-z0-9_-])")
RUNS_ON = re.compile(r"^(?P<indent>[ ]*)runs-on[ ]*:[ ]*(?P<value>.*)$")


def without_yaml_comment(line: str) -> str:
    """Remove an unquoted YAML comment from one line."""
    single_quoted = False
    double_quoted = False
    escaped = False

    for index, character in enumerate(line):
        if escaped:
            escaped = False
            continue
        if character == "\\" and double_quoted:
            escaped = True
            continue
        if character == "'" and not double_quoted:
            single_quoted = not single_quoted
            continue
        if character == '"' and not single_quoted:
            double_quoted = not double_quoted
            continue
        if character == "#" and not single_quoted and not double_quoted:
            return line[:index]
    return line


def direct_runs_on_violations(lines: list[str]) -> list[int]:
    """Find scalar, flow-sequence, and block-sequence runs-on labels."""
    violations: list[int] = []
    index = 0

    while index < len(lines):
        code = without_yaml_comment(lines[index]).rstrip()
        match = RUNS_ON.match(code)
        if not match:
            index += 1
            continue

        base_indent = len(match.group("indent"))
        value = match.group("value")
        if SELF_HOSTED.search(value):
            violations.append(index + 1)

        cursor = index + 1
        if not value:
            while cursor < len(lines):
                nested = without_yaml_comment(lines[cursor]).rstrip()
                if not nested.strip():
                    cursor += 1
                    continue
                indentation = len(nested) - len(nested.lstrip(" "))
                if indentation <= base_indent:
                    break
                if SELF_HOSTED.search(nested):
                    violations.append(cursor + 1)
                cursor += 1
        index = max(index + 1, cursor)

    return violations


def find_violations(workflows_dir: Path) -> list[tuple[Path, int]]:
    violations: set[tuple[Path, int]] = set()
    workflow_files = sorted(workflows_dir.glob("*.yml")) + sorted(
        workflows_dir.glob("*.yaml")
    )

    for workflow in workflow_files:
        if workflow.name == ALLOWED_WORKFLOW:
            continue

        lines = workflow.read_text(encoding="utf-8").splitlines()
        for line_number in direct_runs_on_violations(lines):
            violations.add((workflow, line_number))

        # A matrix or expression can indirectly feed a self-hosted label into
        # runs-on. Conservatively reject the token anywhere outside this policy
        # workflow, while still checking the policy workflow's runs-on directly.
        if workflow.name != POLICY_WORKFLOW:
            for line_number, line in enumerate(lines, start=1):
                if SELF_HOSTED.search(without_yaml_comment(line)):
                    violations.add((workflow, line_number))

    return sorted(violations, key=lambda item: (str(item[0]), item[1]))


def main() -> int:
    workflows_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".github/workflows")
    violations = find_violations(workflows_dir)
    for workflow, line_number in violations:
        print(
            f"{workflow}:{line_number}: self-hosted runners are only allowed in "
            f"{ALLOWED_WORKFLOW}",
            file=sys.stderr,
        )
    return 1 if violations else 0


if __name__ == "__main__":
    raise SystemExit(main())
