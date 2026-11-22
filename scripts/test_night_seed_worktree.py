#!/usr/bin/env python3
"""Tests for night-seed-worktree.py.

Per spec 994 FR-009: colocated pytest module. CLI cases invoke the script
via ``subprocess.run([sys.executable, str(script_path), ...])``.

Run via:

    python3 -m pytest scripts/test_night_seed_worktree.py -v
"""
from __future__ import annotations

import importlib.util
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = SCRIPT_DIR / "night-seed-worktree.py"

SECRET_BODY = "DO-NOT-PRINT-this-operator-override-body\n"


def _load_module():
    spec = importlib.util.spec_from_file_location("night_seed_worktree", SCRIPT_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _git(cwd: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=str(cwd),
        check=check,
        shell=False,
        timeout=30,
        capture_output=True,
        text=True,
    )


def _seed_repo(tmp_path: Path, name: str = "primary") -> Path:
    """Create a synthetic origin + clone pair; return the clone (primary)."""
    origin = tmp_path / "origin.git"
    _git(tmp_path, "init", "--bare", str(origin))
    primary = tmp_path / name
    primary.mkdir()
    _git(tmp_path, "clone", str(origin), str(primary))
    _git(primary, "config", "user.email", "ci@example.com")
    _git(primary, "config", "user.name", "ci")
    (primary / "README.md").write_text("hi\n", encoding="utf-8")
    _git(primary, "add", "README.md")
    _git(primary, "commit", "-m", "init")
    _git(primary, "branch", "-M", "main")
    _git(primary, "push", "origin", "main")
    return primary


def _add_worktree(primary: Path, dest: Path, branch: str = "lane-a") -> Path:
    dest.parent.mkdir(parents=True, exist_ok=True)
    _git(primary, "worktree", "add", "-B", branch, str(dest), "main")
    return dest


def test_seed_copies_root_when_missing(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text(SECRET_BODY, encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    assert not (wt / "AGENTS.local.md").exists()
    copied = mod.seed_agents_local(wt, primary=primary)
    assert [p.as_posix() for p in copied] == ["AGENTS.local.md"]
    assert (wt / "AGENTS.local.md").read_text(encoding="utf-8") == SECRET_BODY


def test_seed_copies_nested_module_file(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("root\n", encoding="utf-8")
    nested = primary / "modules" / "webui"
    nested.mkdir(parents=True)
    (nested / "AGENTS.local.md").write_text("module\n", encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    copied = mod.seed_agents_local(wt, primary=primary)
    assert sorted(p.as_posix() for p in copied) == [
        "AGENTS.local.md",
        "modules/webui/AGENTS.local.md",
    ]
    assert (wt / "modules" / "webui" / "AGENTS.local.md").read_text(encoding="utf-8") == "module\n"


def test_seed_is_noop_when_primary_has_none(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    wt = _add_worktree(primary, tmp_path / "lane")
    copied = mod.seed_agents_local(wt, primary=primary)
    assert copied == []
    assert not (wt / "AGENTS.local.md").exists()


def test_seed_does_not_overwrite_by_default(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("primary\n", encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    (wt / "AGENTS.local.md").write_text("existing\n", encoding="utf-8")
    copied = mod.seed_agents_local(wt, primary=primary)
    assert copied == []
    assert (wt / "AGENTS.local.md").read_text(encoding="utf-8") == "existing\n"


def test_seed_overwrite_refreshes(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("v2\n", encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    (wt / "AGENTS.local.md").write_text("stale\n", encoding="utf-8")
    copied = mod.seed_agents_local(wt, primary=primary, overwrite=True)
    assert [p.as_posix() for p in copied] == ["AGENTS.local.md"]
    assert (wt / "AGENTS.local.md").read_text(encoding="utf-8") == "v2\n"


def test_seed_never_copies_back_to_primary(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("primary\n", encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    (wt / "AGENTS.local.md").write_text("worktree-edit\n", encoding="utf-8")
    mod.seed_agents_local(wt, primary=primary)
    assert (primary / "AGENTS.local.md").read_text(encoding="utf-8") == "primary\n"


def test_resolve_primary_from_worktree(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    wt = _add_worktree(primary, tmp_path / "lane")
    assert mod.resolve_primary_checkout(wt).resolve() == primary.resolve()


def test_iter_skips_nested_worktree_copies(tmp_path: Path) -> None:
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("root\n", encoding="utf-8")
    inner = _add_worktree(primary, primary / "inner-lane", branch="inner")
    (inner / "AGENTS.local.md").write_text("should-not-be-sourced\n", encoding="utf-8")
    rels = [p.as_posix() for p in mod.iter_agents_local(primary)]
    assert rels == ["AGENTS.local.md"]


def test_cli_seeds_and_does_not_print_contents(tmp_path: Path) -> None:
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text(SECRET_BODY, encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), str(wt)],
        check=False,
        shell=False,
        timeout=30,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    assert "seeded: AGENTS.local.md" in completed.stdout
    combined = completed.stdout + completed.stderr
    assert SECRET_BODY.strip() not in combined
    assert "DO-NOT-PRINT" not in combined
    assert (wt / "AGENTS.local.md").read_text(encoding="utf-8") == SECRET_BODY


def test_cli_check_ok_after_seed(tmp_path: Path) -> None:
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("ok\n", encoding="utf-8")
    wt = _add_worktree(primary, tmp_path / "lane")
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), "--check", str(wt)],
        check=False,
        shell=False,
        timeout=30,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    assert (wt / "AGENTS.local.md").is_file()


def test_cli_check_fails_when_still_missing(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    """--check exits 2 if the primary has a file the worktree still lacks."""
    mod = _load_module()
    primary = _seed_repo(tmp_path)
    (primary / "AGENTS.local.md").write_text("secret\n", encoding="utf-8")
    dest = tmp_path / "empty"
    dest.mkdir()
    monkeypatch.setattr(mod, "seed_agents_local", lambda *a, **k: [])
    rc = mod.main(["--check", "--primary", str(primary), str(dest)])
    assert rc == mod.EXIT_CHECK_FAILED


def test_cli_missing_worktree_exits_1(tmp_path: Path) -> None:
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), str(tmp_path / "missing")],
        check=False,
        shell=False,
        timeout=30,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 1
    assert "does not exist" in completed.stderr


def test_cli_check_is_ok_when_primary_has_none(tmp_path: Path) -> None:
    primary = _seed_repo(tmp_path)
    wt = _add_worktree(primary, tmp_path / "lane")
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), "--check", str(wt)],
        check=False,
        shell=False,
        timeout=30,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    assert "no AGENTS.local.md" in completed.stdout


def _plain_worktree(tmp_path: Path, name: str = "seat") -> Path:
    """A non-git directory standing in for a lane worktree (m2 tests)."""
    wt = tmp_path / name
    wt.mkdir()
    return wt


def test_m2_is_opt_in_without_flag(tmp_path: Path) -> None:
    """Without --m2 the seed must not create a per-worktree Maven repo."""
    primary = _seed_repo(tmp_path)
    wt = _add_worktree(primary, tmp_path / "lane")
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), str(wt)],
        check=False,
        shell=False,
        timeout=60,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    assert not (wt / "tmp" / "m2-worktree").exists()


def test_m2_no_seed_creates_repo_and_prints_export(tmp_path: Path) -> None:
    """--m2 --no-m2-seed creates the repo dir and prints an eval-able export."""
    mod = _load_module()
    wt = _plain_worktree(tmp_path)
    rc = mod.main(["--m2", "--no-m2-seed", "--primary", str(tmp_path), str(wt)])
    assert rc == 0
    repo = wt / "tmp" / "m2-worktree" / "repository"
    assert repo.is_dir()


def test_m2_cli_export_round_trips_through_canonical_parser(tmp_path: Path) -> None:
    """The printed export resolves back to the worktree repo via the helper."""
    wt = _plain_worktree(tmp_path)
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), "--m2", "--no-m2-seed", "--primary", str(tmp_path), str(wt)],
        check=False,
        shell=False,
        timeout=60,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    export = next(line for line in completed.stdout.splitlines() if "MAVEN_ARGS" in line)
    assert export.startswith("export MAVEN_ARGS=")
    helper_spec = importlib.util.spec_from_file_location(
        "maven_local_repo",
        SCRIPT_DIR.parent / "docker" / "scripts" / "maven_local_repo.py",
    )
    assert helper_spec is not None and helper_spec.loader is not None
    helper = importlib.util.module_from_spec(helper_spec)
    helper_spec.loader.exec_module(helper)
    resolved = helper.parse_repo_local(export)
    assert resolved is not None
    assert resolved == wt.resolve() / "tmp" / "m2-worktree" / "repository"
    assert "Per-worktree Maven local repo" in completed.stderr


def test_m2_seeds_third_party_and_skips_snapshots(tmp_path: Path) -> None:
    """--m2 copies release artifacts but never project SNAPSHOT jars."""
    mod = _load_module()
    wt = _plain_worktree(tmp_path)
    seed_from = tmp_path / "shared-m2"
    third_party = seed_from / "com" / "example" / "lib" / "1.0" / "lib-1.0.jar"
    third_party.parent.mkdir(parents=True)
    third_party.write_bytes(b"third-party")
    snapshot = seed_from / "com" / "percussion" / "rest" / "8.2.0-SNAPSHOT" / "rest-8.2.0-SNAPSHOT.jar"
    snapshot.parent.mkdir(parents=True)
    snapshot.write_bytes(b"poison")
    rc = mod.main(["--m2", "--m2-seed-from", str(seed_from), "--primary", str(tmp_path), str(wt)])
    assert rc == 0
    repo = wt / "tmp" / "m2-worktree" / "repository"
    assert (repo / "com" / "example" / "lib" / "1.0" / "lib-1.0.jar").is_file()
    assert not (repo / "com" / "percussion" / "rest" / "8.2.0-SNAPSHOT" / "rest-8.2.0-SNAPSHOT.jar").exists()
    # Second run with the same source is a marker-guarded no-op.
    rc = mod.main(["--m2", "--m2-seed-from", str(seed_from), "--primary", str(tmp_path), str(wt)])
    assert rc == 0


def test_m2_missing_helper_exits_1(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    """--m2 fails loudly when the canonical helper cannot load."""
    mod = _load_module()
    wt = _plain_worktree(tmp_path)
    monkeypatch.setattr(mod, "_M2_HELPER_REL", Path("docker") / "scripts" / "does-not-exist.py")
    rc = mod.main(["--m2", "--no-m2-seed", "--primary", str(tmp_path), str(wt)])
    assert rc == 1
