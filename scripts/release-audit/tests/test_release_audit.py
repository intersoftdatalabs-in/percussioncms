#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""pytest coverage for the release-audit pipeline.

The live ``gh api`` path requires network + an authenticated ``gh``; the tests
in this module cover the pure-function surfaces (CLI dispatch, priority
bucketing, ``resolve_dev_path`` mapping, ``flag_jdk8_idioms`` against a
synthetic diff, ``backlog`` / ``report`` Markdown rendering against synthetic
inventory+verdicts JSON).

The package directory is ``scripts/release-audit/`` (per the contract) which
has a dash — not a valid Python package name. The tests load each module via
``importlib`` after registering a synthetic ``release_audit`` package, so the
modules' flat imports (``import common``) and the tests' ``import release_audit``
both work.
"""
from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[3]
PKG_DIR = Path(__file__).resolve().parents[1]


def _load(name: str):
    """Load ``scripts/release-audit/<name>.py`` as ``release_audit.<name>``."""
    import sys
    import types

    pkg_name = "release_audit"
    pkg_path = str(PKG_DIR)
    if pkg_name not in sys.modules:
        pkg = types.ModuleType(pkg_name)
        pkg.__path__ = [pkg_path]  # type: ignore[attr-defined]
        sys.modules[pkg_name] = pkg
    spec = importlib.util.spec_from_file_location(
        f"{pkg_name}.{name}", PKG_DIR / f"{name}.py"
    )
    mod = importlib.util.module_from_spec(spec)
    sys.modules[f"{pkg_name}.{name}"] = mod
    assert spec.loader is not None
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod


# Load modules in dependency order (common first because others import it).
common = _load("common")
inventory = _load("inventory")
verdicts = _load("verdicts")
backlog = _load("backlog")
report = _load("report")
port = _load("port")


def _run_module(*args: str, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    """Invoke the package's ``__main__.py`` directly (the ``-m release_audit``
    form is impossible because the directory name has a dash)."""
    cmd = [sys.executable, str(PKG_DIR / "__main__.py"), *args]
    full_env = os.environ.copy()
    if env:
        full_env.update(env)
    return subprocess.run(
        cmd,
        shell=False,
        check=False,
        cwd=str(REPO_ROOT),
        timeout=120,
        capture_output=True,
        text=True,
        env=full_env,
    )


def test_help_exits_zero_and_prints_usage() -> None:
    result = _run_module("--help")
    assert result.returncode == 0, result.stderr
    assert "usage:" in result.stdout.lower()


def test_bogus_subcommand_exits_nonzero() -> None:
    result = _run_module("not-a-real-subcommand", "--skip-origin-check")
    assert result.returncode != 0
    combined = (result.stderr + result.stdout).lower()
    assert "invalid choice" in combined or "not-a-real-subcommand" in combined


def test_resolve_dev_path_packages_migration() -> None:
    """``system/Packages/X`` → ``modules/perc-packages/src/main/resources/Packages/X``."""
    assert (
        verdicts.resolve_dev_path("system/Packages/Foo.xml")
        == "modules/perc-packages/src/main/resources/Packages/Foo.xml"
    )
    assert verdicts.resolve_dev_path("system/Foo.java") == "system/Foo.java"


def test_priority_buckets() -> None:
    assert backlog._priority_for({"securityFlag": True, "modulePaths": []}) == "P0"
    assert backlog._priority_for({"securityFlag": False, "modulePaths": ["rest"]}) == "P1"
    assert backlog._priority_for({"securityFlag": False, "modulePaths": ["projects/sitemanage"]}) == "P1"
    assert backlog._priority_for({"securityFlag": False, "modulePaths": ["WebUI"]}) == "P2"
    assert backlog._priority_for({"securityFlag": False, "modulePaths": ["system"]}) == "P3"


def test_flag_jdk8_idioms_against_synthetic_diff(tmp_path: Path) -> None:
    diff = tmp_path / "diff.patch"
    diff.write_text(
        "--- a/x.java\n"
        "+++ b/x.java\n"
        "@@ -1,3 +1,5 @@\n"
        "+import javax.ws.rs.Path;\n"
        "+import sun.misc.Unsafe;\n"
        "+import java.util.List;\n",
        encoding="utf-8",
    )
    warnings = tmp_path / "warnings.txt"
    rc = port.flag_jdk8_idioms(diff, warnings)
    assert rc == 0
    body = warnings.read_text(encoding="utf-8")
    assert "javax.ws.rs" in body
    assert "sun.misc" in body


def test_module_paths_projects_and_dts() -> None:
    """The bash jq logic returns ``projects/X`` and ``dts/X/Y`` for the two
    special cases; everything else falls through to ``parts[0]`` (the first
    path segment). For ``modules/utils/...`` that means just ``modules``.
    Matches the bash original exactly.
    """
    files = [
        {"filename": "projects/sitemanage/src/main/java/Foo.java"},
        {"filename": "deliverytiersuite/delivery-tier-suite/feeds/src/main/java/Bar.java"},
        {"filename": "modules/utils/src/main/java/Baz.java"},
        {"filename": "pom.xml"},  # excluded
    ]
    paths = inventory._module_paths(files)
    assert paths == [
        "projects/sitemanage",
        "deliverytiersuite/delivery-tier-suite/feeds",
        "modules",
    ]


def test_backlog_phase_renders_markdown(tmp_path: Path) -> None:
    """End-to-end: write a synthetic inventory + verdicts + config, run the
    backlog phase, assert the Markdown contains the expected sections."""
    output_dir = tmp_path / "out"
    output_dir.mkdir()
    (output_dir / "inventory.json").write_text(
        json.dumps(
            [
                {
                    "number": 1,
                    "title": "Foo",
                    "mergedAt": "2026-01-01T00:00:00Z",
                    "mergeCommitSha": "abc",
                    "author": "alice",
                    "modulePaths": ["rest"],
                    "jdk8OnlyFlag": False,
                    "securityFlag": False,
                },
            ]
        ),
        encoding="utf-8",
    )
    (output_dir / "verdicts.json").write_text(
        json.dumps(
            [
                {
                    "prNumber": 1,
                    "verdict": "needs-migration",
                    "evidenceCommit": "",
                    "evidenceFilePath": "",
                    "evidenceNote": "x",
                    "jdk8Only": False,
                    "securityFlag": False,
                },
            ]
        ),
        encoding="utf-8",
    )
    (output_dir / "_audit_config.json").write_text(
        json.dumps(
            {
                "fromTag": "v8.1.6",
                "toTag": "v8.1.7",
                "targetBranch": "development",
                "runTimestamp": "2026-01-01T00:00:00Z",
            }
        ),
        encoding="utf-8",
    )
    backlog.run_backlog_phase(REPO_ROOT, output_dir)
    body = (output_dir / "migration-backlog.md").read_text(encoding="utf-8")
    assert "Migration Backlog: v8.1.6 → v8.1.7" in body
    assert "P1 — REST contract" in body
    assert "#1" in body


def test_report_phase_renders_sections(tmp_path: Path) -> None:
    output_dir = tmp_path / "out"
    output_dir.mkdir()
    (output_dir / "inventory.json").write_text(json.dumps([]), encoding="utf-8")
    (output_dir / "verdicts.json").write_text(json.dumps([]), encoding="utf-8")
    (output_dir / "dependabot-excluded.json").write_text(json.dumps([]), encoding="utf-8")
    (output_dir / "_audit_config.json").write_text(
        json.dumps({"fromTag": "v8.1.6", "toTag": "v8.1.7", "targetBranch": "development"}),
        encoding="utf-8",
    )
    report.run_report_phase(REPO_ROOT, output_dir)
    body = (output_dir / "v8.1.7-to-8.2-migration-report.md").read_text(encoding="utf-8")
    for section in (
        "TL;DR",
        "Verdict Distribution",
        "Top 10 Backlog Items",
        "Exclusions",
        "Open Questions",
        "Next Steps",
    ):
        assert section in body, f"report missing section: {section}"


if __name__ == "__main__":
    sys.exit(pytest.main([__file__, "-v"]))
