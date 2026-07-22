"""Per-item porting workflow helpers.

Ported from ``scripts/release-audit/lib/port.sh``. Provides the helpers a
porter uses to take a single ``needs-migration`` item and produce a
development-branch PR.

Per Constitution Principle III (Test Discipline) and IX (PR Review Comment
Resolution), porting PRs MUST include regression tests and follow the
inline-reply + ``resolveReviewThread`` procedure (see root ``AGENTS.md``).
"""
from __future__ import annotations

import logging
import re
import subprocess
from pathlib import Path
from typing import Any

import common

LOGGER = logging.getLogger("release_audit.port")


def cherry_pick_pr(
    repo_root: Path,
    pr_number: int,
    target_branch: str = "development",
) -> tuple[int, str]:
    """Cherry-pick the merge commit of ``pr_number`` into a new feature branch.

    Returns ``(returncode, feature_branch_or_empty)``.
    Mirrors the bash ``cherry_pick_pr`` helper.
    """
    cmd = [
        "gh",
        "pr",
        "view",
        str(pr_number),
        "--repo",
        "intersoftdatalabs-in/percussioncms",
        "--json",
        "mergeCommit",
        "--jq",
        ".mergeCommit.oid",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=60,
        capture_output=True,
        text=True,
    )
    merge_sha = result.stdout.strip()
    if not merge_sha:
        common.log_error(f"could not resolve merge commit for PR #{pr_number}")
        return (2, "")

    feature_branch = f"005-migrate-{pr_number}"
    common.log_info(f"creating branch {feature_branch} from {target_branch}")
    rc = subprocess.run(
        ["git", "switch", "-c", feature_branch, target_branch],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=60,
    ).returncode
    if rc != 0:
        return (rc, "")

    common.log_info(f"cherry-picking {merge_sha} from PR #{pr_number}")
    rc = subprocess.run(
        ["git", "cherry-pick", "-x", merge_sha],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=600,
    ).returncode
    if rc != 0:
        common.log_warn(
            "cherry-pick had conflicts; resolve and run `git cherry-pick --continue`"
        )
        return (rc, "")
    return (0, feature_branch)


JDK8_NEEDLES = ("javax.ws.rs", "javax.persistence", "javax.xml.bind", "sun.misc", "com.sun.")


def flag_jdk8_idioms(diff_path: Path, warnings_path: Path) -> int:
    """Scan ``diff_path`` for JDK 8 idioms and write matches to ``warnings_path``.

    Mirrors the bash ``flag_jdk8_idioms`` function. Returns 0 on success,
    2 if the diff file is missing.
    """
    if not diff_path.is_file():
        common.log_error(f"diff file not found: {diff_path}")
        return 2

    common.log_info(
        "scanning diff for JDK 8 idioms (javax.ws.rs, javax.persistence, "
        "javax.xml.bind, sun.misc, com.sun.)"
    )
    warnings_path.parent.mkdir(parents=True, exist_ok=True)
    warnings_path.write_text("", encoding="utf-8")

    current_file = ""
    lineno = 0
    for line in diff_path.read_text(encoding="utf-8").splitlines():
        if line.startswith("+++ b/"):
            current_file = line[len("+++ b/") :]
            lineno = 0
            continue
        if line.startswith("@@"):
            m = re.match(r"^@@ .*\+(\d+)", line)
            if m:
                try:
                    lineno = int(m.group(1))
                except ValueError:
                    lineno = 0
            continue
        if line.startswith("+") and not line.startswith("+++"):
            if any(n in line for n in JDK8_NEEDLES):
                with warnings_path.open("a", encoding="utf-8") as fp:
                    fp.write(f"{current_file}:{lineno}: {line}\n")
            lineno += 1

    count = sum(1 for _ in warnings_path.open("r", encoding="utf-8"))
    if count > 0:
        common.log_warn(
            f"JDK 8 idioms detected ({count}); see {warnings_path} — "
            "translate to jakarta.* / java.* equivalents"
        )
    else:
        warnings_path.unlink(missing_ok=True)
        common.log_info("no JDK 8 idioms detected")
    return 0


def verify_tests(repo_root: Path, module: str, test_class: str) -> int:
    """Run ``mvn -pl <module> -am test -Dtest=<test_class>``. Returns the exit code."""
    common.log_info(f"running tests: ./mvn-env.sh -pl {module} -am test -Dtest={test_class}")
    return subprocess.run(
        [
            "./mvn-env.sh",
            "-pl",
            module,
            "-am",
            "test",
            f"-Dtest={test_class}",
        ],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=1800,
    ).returncode


def spotless_check(repo_root: Path, module: str) -> int:
    """Run Spotless on a single module. Returns non-zero if formatting needs fix."""
    common.log_info(f"running spotless:check on {module}")
    return subprocess.run(
        ["./mvn-env.sh", "-pl", module, "-am", "spotless:check"],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=1800,
    ).returncode
