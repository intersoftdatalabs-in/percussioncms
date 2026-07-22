"""Per-PR verdict classification.

Ported from ``scripts/release-audit/lib/verdicts.sh``. Verdicts:

- ``already-present``: the v8.1.7 changes were already forward-ported to
  ``development``.
- ``needs-migration``: the v8.1.7 changes need a porting PR.
- ``not-applicable``: dependabot PR / JDK-8-only / merge commit unresolvable.
- ``superseded``: the v8.1.7 changes were superseded by a refactor on dev.
- ``conflicts-with-newer-design``: all diff target paths are absent on dev.
"""
from __future__ import annotations

import json
import logging
import re
import subprocess
from pathlib import Path
from typing import Any

import common

LOGGER = logging.getLogger("release_audit.verdicts")

VERDICT_ALREADY_PRESENT = "already-present"
VERDICT_NEEDS_MIGRATION = "needs-migration"
VERDICT_NOT_APPLICABLE = "not-applicable"
VERDICT_SUPERSEDED = "superseded"
VERDICT_CONFLICTS = "conflicts-with-newer-design"


def resolve_dev_path(path: str) -> str:
    """Map a v8.1.x path to its development-branch location.

    Mirrors the bash ``resolve_dev_path`` function: handles known file
    migrations (system/Packages → modules/perc-packages/src/main/resources/Packages/).
    """
    if path.startswith("system/Packages/"):
        return "modules/perc-packages/src/main/resources/Packages/" + path[len("system/Packages/") :]
    return path


def _git(repo_root: Path, args: list[str], *, timeout: int = 30) -> tuple[int, str]:
    result = subprocess.run(
        ["git", *args],
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=timeout,
        capture_output=True,
        text=True,
    )
    return (result.returncode, (result.stdout or "").strip())


def _diff_files(repo_root: Path, merge_commit_sha: str) -> list[str]:
    """Return the list of files changed by ``merge_commit_sha``."""
    if not merge_commit_sha:
        return []
    rc, out = _git(
        repo_root,
        ["show", "--name-only", "--pretty=format:", merge_commit_sha],
        timeout=30,
    )
    if rc != 0:
        return []
    return [line for line in out.splitlines() if line.strip()]


def _first_existing_path(
    repo_root: Path, paths: list[str], dev_paths_file: Path | None
) -> str | None:
    """Return the first path that exists on ``development`` (or None)."""
    if dev_paths_file is None or not dev_paths_file.is_file():
        return None
    dev_set = set(dev_paths_file.read_text(encoding="utf-8").splitlines())
    for p in paths:
        dp = resolve_dev_path(p)
        if dp in dev_set:
            return dp
    return None


def _commit_token(repo_root: Path, merge_commit_sha: str) -> str:
    """Extract a stable token from the commit subject for the heuristic scan."""
    rc, out = _git(repo_root, ["log", "-1", "--format=%s", merge_commit_sha], timeout=15)
    if rc != 0:
        return ""
    subject = out.lower()
    tokens = re.findall(r"[a-z][a-z_-]{7,}", subject)
    uniq = sorted(set(tokens))[:3]
    return "|".join(uniq)


def _token_in_dev_file(repo_root: Path, target_branch: str, dev_file: str, token: str) -> bool:
    """Check whether ``token`` appears in ``<target_branch>:<dev_file>``."""
    rc, out = _git(repo_root, ["show", f"{target_branch}:{dev_file}"], timeout=15)
    if rc != 0:
        return False
    if not token:
        return False
    return any(t and t in out.lower() for t in token.split("|") if t)


def _evidence_commit(repo_root: Path, target_branch: str, dev_file: str) -> str:
    rc, out = _git(
        repo_root,
        ["log", target_branch, "--oneline", "--", dev_file],
        timeout=15,
    )
    if rc != 0 or not out:
        return ""
    first = out.splitlines()[0]
    return first.split()[0] if first else ""


def classify_pr(
    repo_root: Path,
    pr: dict[str, Any],
    target_branch: str,
    evidence_dir: Path,
    dev_paths_file: Path | None,
) -> dict[str, Any]:
    """Return a PRVerdict JSON object for ``pr`` (and write ``<n>.json`` evidence)."""
    n = pr.get("number")
    merge_commit_sha = pr.get("mergeCommitSha", "") or ""
    dependabot_flag = bool(pr.get("dependabotFlag", False))
    jdk8 = bool(pr.get("jdk8OnlyFlag", False))
    sec = bool(pr.get("securityFlag", False))

    evidence_dir.mkdir(parents=True, exist_ok=True)

    if dependabot_flag:
        return _emit_verdict(
            n,
            VERDICT_NOT_APPLICABLE,
            "",
            "",
            "excluded as dependabot in inventory phase",
            jdk8,
            sec,
            evidence_dir,
        )

    if jdk8:
        return _emit_verdict(
            n,
            VERDICT_NOT_APPLICABLE,
            "",
            "",
            "JDK-8-only idiom detected in PR; superseded by JDK 21 / Jakarta EE 10 on development",
            jdk8,
            sec,
            evidence_dir,
        )

    diff_files = _diff_files(repo_root, merge_commit_sha)
    if not diff_files:
        return _emit_verdict(
            n,
            VERDICT_NOT_APPLICABLE,
            "",
            "",
            f"merge commit {merge_commit_sha} not resolvable in local clone; cannot classify",
            jdk8,
            sec,
            evidence_dir,
        )

    first_existing = _first_existing_path(repo_root, diff_files, dev_paths_file)
    if first_existing is None:
        return _emit_verdict(
            n,
            VERDICT_CONFLICTS,
            "",
            "",
            f"all diff target paths absent on development; likely deleted by a refactor (first_diff={diff_files[0]})",
            jdk8,
            sec,
            evidence_dir,
        )

    token = _commit_token(repo_root, merge_commit_sha)
    verdict = VERDICT_NEEDS_MIGRATION
    evidence_commit = ""
    evidence_file = ""
    evidence_note = f"not found at path on development (first_existing={first_existing})"
    if token and _token_in_dev_file(repo_root, target_branch, first_existing, token):
        verdict = VERDICT_ALREADY_PRESENT
        evidence_commit = _evidence_commit(repo_root, target_branch, first_existing)
        evidence_file = first_existing
        evidence_note = f"string token '{token}' found in development:{first_existing}"

    return _emit_verdict(
        n,
        verdict,
        evidence_commit,
        evidence_file,
        evidence_note,
        jdk8,
        sec,
        evidence_dir,
    )


def _emit_verdict(
    n: Any,
    verdict: str,
    commit: str,
    file: str,
    note: str,
    jdk8: bool,
    sec: bool,
    evidence_dir: Path,
) -> dict[str, Any]:
    payload = {
        "prNumber": n,
        "verdict": verdict,
        "evidenceCommit": commit,
        "evidenceFilePath": file,
        "evidenceNote": note,
        "jdk8Only": jdk8,
        "securityFlag": sec,
    }
    (evidence_dir / f"{n}.json").write_text(
        json.dumps(payload, indent=2), encoding="utf-8"
    )
    return payload


def run_verdicts_phase(
    repo_root: Path,
    output_dir: Path,
    target_branch: str,
) -> None:
    """Run the verdicts phase: classify each PRRecord into a PRVerdict."""
    inventory = output_dir / "inventory.json"
    if not inventory.is_file():
        common.log_warn(f"inventory not found: {inventory}; skipping verdicts phase")
        return
    records = common.read_json(inventory)
    if not isinstance(records, list):
        common.log_warn("inventory is not a list; skipping verdicts phase")
        return

    common.log_info(f"classifying verdicts against {target_branch}")

    rc, _ = _git(repo_root, ["rev-parse", "--verify", target_branch], timeout=15)
    if rc != 0:
        common.log_warn(f"target branch {target_branch} not local; relying on remote-tracking")
    dev_paths_file = output_dir / "_dev_paths.txt"
    rc2, out = _git(repo_root, ["ls-tree", "-r", "--name-only", target_branch], timeout=120)
    if rc2 == 0:
        dev_paths_file.write_text(out, encoding="utf-8")
        common.log_info(f"cached {out.count(chr(10))} dev paths")
    else:
        dev_paths_file.unlink(missing_ok=True)
        dev_paths_file = None

    evidence_dir = output_dir / "_evidence"
    verdicts: list[dict[str, Any]] = []
    for pr in records:
        verdict = classify_pr(repo_root, pr, target_branch, evidence_dir, dev_paths_file)
        verdicts.append(verdict)

    verdicts_path = output_dir / "verdicts.json"
    common.write_json(verdicts_path, verdicts)
    if dev_paths_file is not None:
        dev_paths_file.unlink(missing_ok=True)
    common.log_info(f"verdicts written: {len(verdicts)} entries")
