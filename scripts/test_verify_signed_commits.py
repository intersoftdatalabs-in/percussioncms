#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# Copyright (c) 2026 Intersoft Data Labs, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""pytest coverage for scripts/verify-signed-commits.py (#6809).

Proves the pre-push gate fails closed on the #6769/#6770 defect class:

* PASS on an empty range (nothing to verify).
* FAIL on an unsigned commit even when the identity is fine (signature ``N``).
* FAIL on an ``opencode@local`` author / committer identity.
* FAIL on placeholder ``user.name`` / ``user.email`` / disabled
  ``commit.gpgsign`` via ``ensure-identity``, and PASS after
  ``--apply-from`` installs the work-step config.
* FAIL when ``GIT_AUTHOR_EMAIL``-style env overrides inject a placeholder.
* Unit cover for the row evaluator: only ``G`` passes; ``U``/``B``/``E``/
  ``X``/``Y``/``R``/``N`` all fail.
* Incident replay: an ``opencode@local`` unsigned commit fails on identity
  AND signature; re-attributing the author alone still fails on signature —
  the commit must be re-created signed (``git commit -S`` / rebase with
  ``--gpg-sign``), matching the issue's "re-create the two affected commits
  signed" remediation.

All git fixtures live under ``tmp_path`` — the real tree is never dirtied.
Real GPG keys are unavailable in test, so the trusted-signature pass path is
covered through the pure evaluator (``sig == "G"`` rows pass); every
subprocess assertion uses genuinely unsigned fixture commits.
"""
from __future__ import annotations

import importlib.util
import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT = SCRIPT_DIR / "verify-signed-commits.py"

GOOD_NAME = "Nate Chadwick"
GOOD_EMAIL = "263952448+natechadwick-intsof@users.noreply.github.com"

_spec = importlib.util.spec_from_file_location("verify_signed_commits", str(SCRIPT))
assert _spec is not None and _spec.loader is not None
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)


def _run(*args: str, cwd: Path | None = None, env: dict | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        shell=False,
        check=False,
        timeout=90,
        capture_output=True,
        text=True,
        cwd=str(cwd) if cwd else None,
        env=env,
    )


def _git(cwd: Path, *args: str, env: dict | None = None) -> None:
    subprocess.run(
        ["git", *args],
        shell=False,
        check=True,
        cwd=str(cwd),
        capture_output=True,
        text=True,
        env=env,
    )


def _scrub_signing_env(env: dict) -> dict:
    """Drop ambient identity env so fixtures are hermetic; keep PATH etc."""
    scrubbed = dict(env)
    for var in (
        "GIT_AUTHOR_NAME",
        "GIT_AUTHOR_EMAIL",
        "GIT_COMMITTER_NAME",
        "GIT_COMMITTER_EMAIL",
        "GIT_CONFIG_COUNT",
    ):
        scrubbed.pop(var, None)
    return scrubbed


def _init_repo(root: Path, name: str = GOOD_NAME, email: str = GOOD_EMAIL) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    env = _scrub_signing_env(dict(os.environ))
    _git(root, "init", "-q", env=env)
    _git(root, "config", "user.name", name, env=env)
    _git(root, "config", "user.email", email, env=env)
    _git(root, "config", "commit.gpgsign", "false", env=env)
    _git(root, "commit", "-q", "--allow-empty", "-m", "base", env=env)
    return root


def _commit_empty(repo: Path, message: str = "work", env_extra: dict | None = None) -> None:
    env = _scrub_signing_env(dict(os.environ))
    if env_extra:
        env.update(env_extra)
    _git(repo, "commit", "-q", "--allow-empty", "-m", message, env=env)


# ---------------------------------------------------------------------------
# check: empty range
# ---------------------------------------------------------------------------


def test_check_empty_range_passes(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    proc = _run("check", "--base", "HEAD", "--head", "HEAD", "--repo", str(repo))
    assert proc.returncode == 0, proc.stdout + proc.stderr
    assert "no commits" in proc.stdout


# ---------------------------------------------------------------------------
# check: unsigned commits fail, even with a good identity
# ---------------------------------------------------------------------------


def test_check_unsigned_commit_with_good_identity_fails(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    _commit_empty(repo, "unsigned work")
    proc = _run("check", "--base", "HEAD~1", "--head", "HEAD", "--repo", str(repo))
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "no valid signature" in proc.stdout
    assert GOOD_EMAIL in proc.stdout  # identity itself is fine; signature is the gap


def test_check_opencode_local_author_fails(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo", name="opencode", email="opencode@local")
    _commit_empty(repo, "follow-up fix")
    proc = _run("check", "--base", "HEAD~1", "--head", "HEAD", "--repo", str(repo))
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "opencode@local" in proc.stdout
    assert "no valid signature" in proc.stdout


def test_check_bad_committer_identity_fails(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    _commit_empty(
        repo,
        "rebased work",
        env_extra={"GIT_COMMITTER_EMAIL": "opencode@local", "GIT_COMMITTER_NAME": "opencode"},
    )
    proc = _run("check", "--base", "HEAD~1", "--head", "HEAD", "--repo", str(repo))
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "committer" in proc.stdout
    assert "opencode@local" in proc.stdout


def test_check_unknown_base_is_usage_error(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    proc = _run("check", "--base", "origin/does-not-exist", "--head", "HEAD", "--repo", str(repo))
    assert proc.returncode == 2
    assert "ERROR" in proc.stdout


# ---------------------------------------------------------------------------
# check: incident replay for #6769 / #6770
# ---------------------------------------------------------------------------


def test_incident_replay_identity_fix_alone_does_not_sign(tmp_path: Path) -> None:
    """Re-attributing an unsigned follow-up commit still fails the gate.

    Mirrors the issue remediation: the affected commits needed re-creation
    *signed* — fixing the author without a signature must keep failing.
    """
    repo = _init_repo(tmp_path / "repo", name="opencode", email="opencode@local")
    _commit_empty(repo, "follow-up fix")
    before = _run("check", "--base", "HEAD~1", "--head", "HEAD", "--repo", str(repo))
    assert before.returncode == 1
    assert "opencode@local" in before.stdout

    env = _scrub_signing_env(dict(os.environ))
    _git(repo, "config", "user.name", GOOD_NAME, env=env)
    _git(repo, "config", "user.email", GOOD_EMAIL, env=env)
    _git(repo, "commit", "-q", "--amend", "--allow-empty", "--no-edit", "--reset-author", env=env)

    after = _run("check", "--base", "HEAD~1", "--head", "HEAD", "--repo", str(repo))
    assert after.returncode == 1, after.stdout + after.stderr
    assert "opencode@local" not in after.stdout  # identity fixed...
    assert "no valid signature" in after.stdout  # ...but still unsigned


# ---------------------------------------------------------------------------
# evaluate_row: pure unit cover (incl. the trusted-signature pass path)
# ---------------------------------------------------------------------------


def _row(**overrides) -> dict:
    base = {
        "sha": "abc123",
        "author_name": GOOD_NAME,
        "author_email": GOOD_EMAIL,
        "committer_name": GOOD_NAME,
        "committer_email": GOOD_EMAIL,
        "sig": "G",
        "signer": GOOD_EMAIL,
    }
    base.update(overrides)
    return base


def test_evaluate_row_trusted_signature_passes() -> None:
    assert _mod.evaluate_row(_row()) == []


@pytest.mark.parametrize("sig", ["N", "U", "B", "E", "X", "Y", "R", ""])
def test_evaluate_row_untrusted_signatures_fail(sig: str) -> None:
    reasons = _mod.evaluate_row(_row(sig=sig))
    assert any("signature" in reason for reason in reasons), reasons


@pytest.mark.parametrize(
    "email",
    [
        "opencode@local",
        "mkd-agent@localhost",
        "root@localhost",
        "",
        "dev@example.com",
        "someone@host.local",
        "not-an-email",
    ],
)
def test_evaluate_row_denied_emails_fail(email: str) -> None:
    reasons = _mod.evaluate_row(_row(sig="G", author_email=email))
    assert any("author" in reason for reason in reasons), (email, reasons)


def test_evaluate_row_denied_names_fail() -> None:
    for name in ("opencode", "mkd-agent", "", "root"):
        reasons = _mod.evaluate_row(_row(sig="G", author_name=name))
        assert any("author" in reason for reason in reasons), (name, reasons)


# ---------------------------------------------------------------------------
# ensure-identity
# ---------------------------------------------------------------------------


def test_ensure_identity_missing_config_fails(tmp_path: Path) -> None:
    repo = tmp_path / "bare"
    repo.mkdir(parents=True, exist_ok=True)
    env = _scrub_signing_env(dict(os.environ))
    _git(repo, "init", "-q", env=env)
    proc = _run("ensure-identity", "--repo", str(repo))
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "user.email" in proc.stdout


def test_ensure_identity_passes_with_work_config(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    env = _scrub_signing_env(dict(os.environ))
    _git(repo, "config", "commit.gpgsign", "true", env=env)
    proc = _run("ensure-identity", "--repo", str(repo))
    assert proc.returncode == 0, proc.stdout + proc.stderr
    assert GOOD_EMAIL in proc.stdout


def test_ensure_identity_signing_disabled_fails(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")  # commit.gpgsign=false by fixture design
    proc = _run("ensure-identity", "--repo", str(repo))
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "gpgsign" in proc.stdout


def test_ensure_identity_env_override_flagged(tmp_path: Path) -> None:
    repo = _init_repo(tmp_path / "repo")
    env = _scrub_signing_env(dict(os.environ))
    _git(repo, "config", "commit.gpgsign", "true", env=env)
    poisoned = dict(os.environ)
    poisoned["GIT_AUTHOR_EMAIL"] = "opencode@local"
    proc = _run("ensure-identity", "--repo", str(repo), env=_scrub_signing_env(poisoned) | {"GIT_AUTHOR_EMAIL": "opencode@local"})
    assert proc.returncode == 1, proc.stdout + proc.stderr
    assert "GIT_AUTHOR_EMAIL" in proc.stdout


def test_ensure_identity_apply_from_installs_work_config(tmp_path: Path) -> None:
    source = _init_repo(tmp_path / "source")
    env = _scrub_signing_env(dict(os.environ))
    _git(source, "config", "commit.gpgsign", "true", env=env)

    target = tmp_path / "target"
    target.mkdir(parents=True, exist_ok=True)
    _git(target, "init", "-q", env=env)

    before = _run("ensure-identity", "--repo", str(target))
    assert before.returncode == 1

    applied = _run("ensure-identity", "--repo", str(target), "--apply-from", str(source))
    assert applied.returncode == 0, applied.stdout + applied.stderr
    assert GOOD_EMAIL in applied.stdout

    # Identity now matches the source; the gate passes even though the
    # pre-existing fixture commits remain unsigned (separate concern).
    after = _run("ensure-identity", "--repo", str(target))
    assert after.returncode == 0, after.stdout + after.stderr


def test_ensure_identity_apply_from_non_repo_is_usage_error(tmp_path: Path) -> None:
    target = _init_repo(tmp_path / "target")
    proc = _run("ensure-identity", "--repo", str(target), "--apply-from", str(tmp_path / "nope"))
    assert proc.returncode == 2
    assert "ERROR" in proc.stdout
