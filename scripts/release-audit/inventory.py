"""PR inventory collection, dependabot classification, and PRRecord enrichment.

Ported from ``scripts/release-audit/lib/inventory.sh``. The live ``gh api``
calls are preserved (matches the bash original's data source) but the
data-shaping is done in Python instead of ``jq``.
"""
from __future__ import annotations

import json
import logging
import subprocess
from pathlib import Path
from typing import Any

import common

LOGGER = logging.getLogger("release_audit.inventory")


def _is_dependabot(pr: dict[str, Any]) -> bool:
    author = (pr.get("author") or {}).get("login", "") or ""
    labels = [label.get("name", "") for label in pr.get("labels", [])]
    return "dependabot" in author.lower() or "dependencies" in labels


def _build_prrecord(pr: dict[str, Any]) -> dict[str, Any]:
    """Map a raw ``gh pr list --json`` record to a PRRecord dict."""
    author = pr.get("author") or {}
    merge_commit = pr.get("mergeCommit") or {}
    return {
        "number": pr.get("number"),
        "title": pr.get("title", ""),
        "author": author.get("login", ""),
        "mergedAt": pr.get("mergedAt", ""),
        "baseRef": pr.get("baseRefName", ""),
        "mergeCommitSha": merge_commit.get("oid", ""),
        "dependabotFlag": _is_dependabot(pr),
        "jdk8OnlyFlag": False,
        "securityFlag": False,
        "modulePaths": [],
    }


def collect_prs(repo_root: Path, output_dir: Path, from_tag: str, to_tag: str) -> None:
    """Fetch merged PRs on ``development-8.1.x`` and write ``_raw_prs.json``.

    Mirrors the bash ``collect_prs`` function.
    """
    common.log_info(f"fetching PRs in window {from_tag}..{to_tag} on development-8.1.x")
    raw = output_dir / "_raw_prs.json"
    cmd = [
        "gh",
        "pr",
        "list",
        "--state",
        "merged",
        "--base",
        "development-8.1.x",
        "--limit",
        "1000",
        "--json",
        "number,title,author,mergedAt,baseRefName,labels,mergeCommit",
    ]
    result = subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(repo_root),
        timeout=600,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        common.log_warn(
            f"gh pr list returned rc={result.returncode}; raw file may be empty. "
            f"stderr (first 500 chars): {(result.stderr or '')[:500]}"
        )
    raw.write_text(result.stdout or "[]", encoding="utf-8")
    try:
        parsed = json.loads(result.stdout or "[]")
        count = len(parsed) if isinstance(parsed, list) else 0
    except json.JSONDecodeError:
        count = 0
    common.log_info(f"fetched {count} raw PRs on development-8.1.x (all bases)")


def classify_dependabot(
    repo_root: Path,
    raw_path: Path,
    output_dir: Path,
    include_dependabot: bool,
) -> None:
    """Partition raw PRs into ``inventory.json`` (non-dependabot) and
    ``dependabot-excluded.json`` (dependabot).
    """
    raw = json.loads(raw_path.read_text(encoding="utf-8") or "[]")
    inventory = output_dir / "inventory.json"
    excluded = output_dir / "dependabot-excluded.json"

    if include_dependabot:
        inv = [_build_prrecord(p) for p in raw]
        exc = [
            {"number": p.get("number"), "title": p.get("title", ""), "author": (p.get("author") or {}).get("login", ""), "mergedAt": p.get("mergedAt", "")}
            for p in raw
            if _is_dependabot(p)
        ]
    else:
        inv = [_build_prrecord(p) for p in raw if not _is_dependabot(p)]
        exc = [
            {"number": p.get("number"), "title": p.get("title", ""), "author": (p.get("author") or {}).get("login", ""), "mergedAt": p.get("mergedAt", "")}
            for p in raw
            if _is_dependabot(p)
        ]

    common.write_json(inventory, inv)
    common.write_json(excluded, exc)
    common.log_info(f"inventory: {len(inv)}, excluded: {len(exc)}")


def enrich_prrecord(repo_root: Path, inventory_path: Path, output_dir: Path) -> None:
    """For each PRRecord, fetch files-changed via ``gh api`` and derive
    ``modulePaths`` / ``jdk8OnlyFlag`` / ``securityFlag``.

    Implementation note: the bash version parallelizes with ``xargs -P 8``.
    The Python port uses a sequential loop for portability (xargs on Windows
    is not reliable). The gh API calls themselves are unchanged.
    """
    inventory = common.read_json(inventory_path)
    if not isinstance(inventory, list):
        common.log_warn("inventory is not a list; skipping enrichment")
        return

    common.log_info(f"enriching {len(inventory)} PRRecords (files-changed fetch)")

    for pr in inventory:
        n = pr.get("number")
        if not isinstance(n, int):
            continue
        files_json = _fetch_pr_files(repo_root, n)
        module_paths = _module_paths(files_json)
        pr["modulePaths"] = module_paths
        pr["jdk8OnlyFlag"] = _has_jdk8_idiom(files_json)
        pr["securityFlag"] = _has_security_keyword(files_json)

    common.write_json(inventory_path, inventory)
    common.log_info(f"PRRecord enrichment complete ({len(inventory)} PRs processed)")


def _fetch_pr_files(repo_root: Path, pr_number: int) -> list[dict[str, Any]]:
    """Fetch files-changed JSON for ``pr_number`` via ``gh api``."""
    cmd = [
        "gh",
        "api",
        f"repos/intersoftdatalabs-in/percussioncms/pulls/{pr_number}/files?per_page=100",
        "--paginate",
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
    if result.returncode != 0:
        return []
    try:
        parsed = json.loads(result.stdout)
        return parsed if isinstance(parsed, list) else []
    except json.JSONDecodeError:
        return []


def _module_paths(files: list[dict[str, Any]]) -> list[str]:
    """Compute the top-level module paths for a list of files-changed entries.

    Mirrors the bash jq that maps ``projects/X`` → ``projects/X``, and
    ``deliverytiersuite/delivery-tier-suite/X`` → ``deliverytiersuite/delivery-tier-suite/X``.
    """
    EXCLUDE = {"pom.xml", "mvnw", "CHANGES.md", ".github", "docs", ".gitignore"}
    out: list[str] = []
    seen: set[str] = set()
    for f in files:
        filename = f.get("filename", "")
        if not filename:
            continue
        parts = filename.split("/")
        if not parts:
            continue
        if parts[0] == "projects" and len(parts) >= 2:
            module = f"projects/{parts[1]}"
        elif (
            len(parts) >= 3
            and parts[0] == "deliverytiersuite"
            and parts[1] == "delivery-tier-suite"
        ):
            module = f"deliverytiersuite/delivery-tier-suite/{parts[2]}"
        else:
            module = parts[0]
        if module in EXCLUDE:
            continue
        if module not in seen:
            seen.add(module)
            out.append(module)
    return out


def _has_jdk8_idiom(files: list[dict[str, Any]]) -> bool:
    needles = ("javax/ws/rs", "javax/persistence", "javax/xml/bind", "sun/misc", "com/sun/")
    return any(any(n in (f.get("filename") or "") for n in needles) for f in files)


def _has_security_keyword(files: list[dict[str, Any]]) -> bool:
    pattern = "(?i)(^|/)(cve|security|shiro|tomcat|csp|authentication|authorization|jetty[-_ ]?maven|perc-security)"
    import re
    return any(re.search(pattern, f.get("filename") or "") for f in files)


def run_inventory_phase(
    repo_root: Path,
    output_dir: Path,
    from_tag: str,
    to_tag: str,
    include_dependabot: bool,
) -> None:
    """Run the inventory phase: collect + classify + enrich + cleanup."""
    collect_prs(repo_root, output_dir, from_tag, to_tag)
    classify_dependabot(repo_root, output_dir / "_raw_prs.json", output_dir, include_dependabot)
    enrich_prrecord(repo_root, output_dir / "inventory.json", output_dir)
    (output_dir / "_raw_prs.json").unlink(missing_ok=True)
