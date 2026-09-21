#!/usr/bin/env python3
"""Tests for opencode-night-issue-prs.py launcher.

Per spec 994 FR-009: every script in `scripts/` ships with a colocated
pytest module. Tests invoke the script via `subprocess.run([sys.executable,
str(script_path), ...])` per R4.

Run via:

    python3 -m pytest scripts/test_opencode_night_issue_prs.py -v

The script is non-mutating when invoked with `--dry-run`. Tests that
exercise the worktree-create path use `--no-worktree-create` against a
synthetic path so the script never touches git state.

## Behavioral Notes

- Tests do NOT create real worktrees or invoke `opencode` itself. The
  `opencode` binary is not required for the suite to pass.
- All filesystem work happens inside `tmp_path` (pytest fixture).
- Cross-platform: every path uses `pathlib.Path`. No hardcoded separators.
"""
from __future__ import annotations

import importlib.util
import json
import os
import subprocess
import sys
from pathlib import Path

import pytest

SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPT_PATH = SCRIPT_DIR / "opencode-night-issue-prs.py"


def _load_module():
    """Load the launcher as a module so we can unit-test its pure helpers."""
    spec = importlib.util.spec_from_file_location("opencode_night_issue_prs", SCRIPT_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_default_worktree_path_uses_home():
    mod = _load_module()
    path = mod.default_worktree_path()
    assert path == Path.home() / ".opencode" / "worktrees" / mod.REPO_SLUG / mod.COMPONENT
    assert path.is_absolute()


def test_resolve_worktree_precedence(tmp_path, monkeypatch):
    mod = _load_module()
    monkeypatch.delenv("NIGHT_WORKTREE_PATH", raising=False)

    args = type("Args", (), {"worktree": ""})()
    assert mod.resolve_worktree(args) == mod.default_worktree_path()

    monkeypatch.setenv("NIGHT_WORKTREE_PATH", "/tmp/from-env")
    assert mod.resolve_worktree(args) == Path("/tmp/from-env")

    monkeypatch.delenv("NIGHT_WORKTREE_PATH", raising=False)
    args = type("Args", (), {"worktree": str(tmp_path / "from-flag")})()
    assert mod.resolve_worktree(args) == tmp_path / "from-flag"


def test_resolve_base_branch_precedence(monkeypatch):
    mod = _load_module()
    monkeypatch.delenv("NIGHT_BASE_BRANCH", raising=False)
    args = type("Args", (), {"base_branch": ""})()
    assert mod.resolve_base_branch(args) == mod.DEFAULT_BASE_BRANCH

    monkeypatch.setenv("NIGHT_BASE_BRANCH", "release/8.2.x")
    assert mod.resolve_base_branch(args) == "release/8.2.x"

    monkeypatch.delenv("NIGHT_BASE_BRANCH", raising=False)
    args = type("Args", (), {"base_branch": "feature/foo"})()
    assert mod.resolve_base_branch(args) == "feature/foo"


def test_resolve_report_path_precedence(tmp_path, monkeypatch):
    mod = _load_module()
    worktree = tmp_path / "wt"

    monkeypatch.delenv("NIGHT_REPORT_PATH", raising=False)
    args = type("Args", (), {"report_path": ""})()
    assert mod.resolve_report_path(args, worktree) == worktree / "scratch" / "night-report.md"

    monkeypatch.setenv("NIGHT_REPORT_PATH", "/tmp/env-report.md")
    assert mod.resolve_report_path(args, worktree) == Path("/tmp/env-report.md")

    monkeypatch.delenv("NIGHT_REPORT_PATH", raising=False)
    args = type("Args", (), {"report_path": str(tmp_path / "flag-report.md")})()
    assert mod.resolve_report_path(args, worktree) == tmp_path / "flag-report.md"


def test_build_env_sets_all_night_vars(tmp_path, monkeypatch):
    mod = _load_module()
    monkeypatch.delenv("NIGHT_CODING_TOOL", raising=False)
    monkeypatch.delenv("NIGHT_OPERATOR", raising=False)
    monkeypatch.delenv("NIGHT_WORKTREE_PATH", raising=False)
    monkeypatch.delenv("NIGHT_BASE_BRANCH", raising=False)
    monkeypatch.delenv("NIGHT_REPORT_PATH", raising=False)

    worktree = tmp_path / "wt"
    args = type("Args", (), {"worktree": "", "base_branch": "main", "report_path": ""})()
    env = mod.build_env(args, worktree, "main", worktree / "scratch" / "night-report.md")

    assert env["NIGHT_WORKTREE_PATH"] == str(worktree)
    assert env["NIGHT_BASE_BRANCH"] == "main"
    assert env["NIGHT_REPORT_PATH"] == str(worktree / "scratch" / "night-report.md")
    assert env["NIGHT_OPERATOR"] == "opencode"
    assert env["NIGHT_CODING_TOOL"] == "OpenCode"
    # Regression: PWD must match the worktree so opencode's bash tool resolves
    # relative paths from the worktree, not from wherever the launcher ran.
    assert env["PWD"] == str(worktree), f"PWD not synced with worktree; got {env.get('PWD')!r}"


def test_build_env_preserves_caller_night_coding_tool(monkeypatch):
    mod = _load_module()
    monkeypatch.setenv("NIGHT_CODING_TOOL", "OpenCode Custom Build")
    args = type("Args", (), {"worktree": "", "base_branch": "main", "report_path": ""})()
    env = mod.build_env(args, Path("/tmp/wt"), "main", Path("/tmp/report.md"))
    assert env["NIGHT_CODING_TOOL"] == "OpenCode Custom Build"


def test_build_opencode_command_minimal():
    mod = _load_module()
    args = type("Args", (), {"model": "", "prompt": "", "command": "night-issue-prs", "max_issues": 3, "base_branch": "main", "max_prs": 6, "include_pr_followup": True})()
    cmd = mod.build_opencode_command(args)
    assert cmd[:4] == ["opencode", "run", "--auto", "--command"]
    assert cmd[4] == "night-issue-prs"
    payload = json.loads(cmd[5])
    assert payload["max_issues"] == 3
    assert payload["base_branch"] == "main"
    assert payload["max_prs"] == 6
    assert payload["include_pr_followup"] is True


def test_build_opencode_command_with_model_and_prompt():
    mod = _load_module()
    args = type("Args", (), {"model": "anthropic/claude-sonnet-4-6", "prompt": "do the thing"})()
    cmd = mod.build_opencode_command(args)
    assert "--model" in cmd and "anthropic/claude-sonnet-4-6" in cmd
    # Prompt is a positional argument on `opencode run`, NOT a `--prompt` flag.
    # `--prompt` is a top-level opencode option that `run` does not consume.
    assert cmd[-1] == "do the thing"
    assert "--prompt" not in cmd


def test_build_opencode_command_prompt_is_positional():
    mod = _load_module()
    args = type("Args", (), {"model": "", "prompt": "hello world"})()
    cmd = mod.build_opencode_command(args)
    assert cmd[-1] == "hello world", f"prompt should be the last positional arg, got: {cmd}"


def test_minus_v_shortcut_sets_debug_log_level():
    mod = _load_module()
    args = mod.parse_args(["-v"])
    assert args.log_level == "DEBUG"


def test_minus_v_overrides_explicit_log_level():
    mod = _load_module()
    args = mod.parse_args(["--log-level", "WARNING", "-v"])
    assert args.log_level == "DEBUG", "last flag wins; -v should override --log-level WARNING"


def test_build_opencode_command_model_only_skips_prompt_when_empty():
    mod = _load_module()
    args = type("Args", (), {"model": "openai/gpt-5", "prompt": "", "command": "night-issue-prs", "max_issues": 1, "base_branch": "main", "max_prs": 6, "include_pr_followup": False})()
    cmd = mod.build_opencode_command(args)
    assert "--prompt" not in cmd
    assert "openai/gpt-5" in cmd
    assert "--command" in cmd and "night-issue-prs" in cmd


def test_build_opencode_command_prompt_override_uses_agent():
    """When --prompt is supplied, no --command; --agent is explicit since the
    command's frontmatter is not in play."""
    mod = _load_module()
    args = type("Args", (), {"model": "", "prompt": "custom"})()
    cmd = mod.build_opencode_command(args)
    assert "--command" not in cmd
    assert "--agent" in cmd and mod.AGENT_NAME in cmd
    assert cmd[-1] == "custom"


def test_build_workflow_args_serializes_to_json():
    """The JSON payload must include all four knobs the agent reads from $ARGUMENTS."""
    mod = _load_module()
    args = type("Args", (), {
        "max_issues": 5,
        "base_branch": "release/8.2.x",
        "max_prs": 4,
        "include_pr_followup": True,
    })()
    payload = json.loads(mod.build_workflow_args(args))
    assert payload == {
        "max_issues": 5,
        "base_branch": "release/8.2.x",
        "max_prs": 4,
        "include_pr_followup": True,
    }


def test_build_workflow_args_handles_false_pr_followup():
    mod = _load_module()
    args = type("Args", (), {
        "max_issues": 1,
        "base_branch": "main",
        "max_prs": 6,
        "include_pr_followup": False,
    })()
    payload = json.loads(mod.build_workflow_args(args))
    assert payload["include_pr_followup"] is False


def test_worktree_exists_false_for_plain_directory(tmp_path):
    """A directory with no `.git` is not a worktree."""
    mod = _load_module()
    plain = tmp_path / "plain"
    plain.mkdir()
    assert not mod.worktree_exists(plain)


def test_worktree_exists_true_for_real_worktree(tmp_path):
    """Create a synthetic git worktree and confirm `worktree_exists` returns True."""
    mod = _load_module()
    main = tmp_path / "main"
    main.mkdir()
    subprocess.run(["git", "init", "--bare", str(main / "origin.git")], check=True, shell=False, timeout=30)
    seed = tmp_path / "seed"
    seed.mkdir()
    subprocess.run(["git", "clone", str(main / "origin.git"), str(seed)], check=True, shell=False, timeout=30)
    subprocess.run(["git", "-C", str(seed), "config", "user.email", "ci@example.com"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "config", "user.name", "ci"], check=True, shell=False, timeout=10)
    (seed / "README.md").write_text("hi")
    subprocess.run(["git", "-C", str(seed), "add", "README.md"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "commit", "-m", "init"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "branch", "-M", "main"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "push", "origin", "main"], check=True, shell=False, timeout=30)

    wt = tmp_path / "wt"
    subprocess.run(
        ["git", "-C", str(seed), "worktree", "add", str(wt), "-b", "night-issue-prs-main"],
        check=True,
        shell=False,
        timeout=60,
    )

    # The worktree's inner .git is a FILE, not a directory.
    assert (wt / ".git").is_file()
    assert not (wt / ".git").is_dir()
    assert mod.worktree_exists(wt)


def test_ensure_worktree_no_ops_when_worktree_exists(tmp_path):
    """Regression: Erlang Issue 1. The launcher must NOT call git worktree add
    on a second consecutive run when the worktree already exists."""
    mod = _load_module()
    main = tmp_path / "main"
    main.mkdir()
    subprocess.run(["git", "init", "--bare", str(main / "origin.git")], check=True, shell=False, timeout=30)
    seed = tmp_path / "seed"
    seed.mkdir()
    subprocess.run(["git", "clone", str(main / "origin.git"), str(seed)], check=True, shell=False, timeout=30)
    subprocess.run(["git", "-C", str(seed), "config", "user.email", "ci@example.com"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "config", "user.name", "ci"], check=True, shell=False, timeout=10)
    (seed / "README.md").write_text("hi")
    subprocess.run(["git", "-C", str(seed), "add", "README.md"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "commit", "-m", "init"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "branch", "-M", "main"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "push", "origin", "main"], check=True, shell=False, timeout=30)

    wt = tmp_path / "wt"
    subprocess.run(
        ["git", "-C", str(seed), "worktree", "add", str(wt), "-b", "night-issue-prs-main"],
        check=True,
        shell=False,
        timeout=60,
    )

    # Pre-condition: worktree is registered and exists.
    assert mod.worktree_exists(wt)

    # Second call must hard-reset the worktree to origin/main and not crash.
    mod.ensure_worktree(wt, "main", seed)
    assert mod.worktree_exists(wt)
    head_after = subprocess.run(
        ["git", "-C", str(wt), "rev-parse", "HEAD"],
        check=True, shell=False, timeout=10, capture_output=True, text=True,
    ).stdout.strip()
    head_origin = subprocess.run(
        ["git", "-C", str(seed), "rev-parse", "origin/main"],
        check=True, shell=False, timeout=10, capture_output=True, text=True,
    ).stdout.strip()
    assert head_after == head_origin, "ensure_worktree must reset worktree HEAD to origin/main"


def test_ensure_worktree_resets_to_new_origin_commit(tmp_path):
    """The worktree must follow origin/main across runs: push a 2nd commit
    after the worktree exists, then ensure_worktree must move HEAD to it."""
    mod = _load_module()
    main = tmp_path / "main"
    main.mkdir()
    subprocess.run(["git", "init", "--bare", str(main / "origin.git")], check=True, shell=False, timeout=30)
    seed = tmp_path / "seed"
    seed.mkdir()
    subprocess.run(["git", "clone", str(main / "origin.git"), str(seed)], check=True, shell=False, timeout=30)
    subprocess.run(["git", "-C", str(seed), "config", "user.email", "ci@example.com"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "config", "user.name", "ci"], check=True, shell=False, timeout=10)
    (seed / "README.md").write_text("v1")
    subprocess.run(["git", "-C", str(seed), "add", "README.md"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "commit", "-m", "v1"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "branch", "-M", "main"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "push", "origin", "main"], check=True, shell=False, timeout=30)

    wt = tmp_path / "wt"
    subprocess.run(
        ["git", "-C", str(seed), "worktree", "add", str(wt), "-b", "night-issue-prs-main"],
        check=True, shell=False, timeout=60,
    )

    head_v1 = subprocess.run(["git", "-C", str(wt), "rev-parse", "HEAD"], check=True, shell=False, timeout=10, capture_output=True, text=True).stdout.strip()
    assert (wt / "README.md").read_text() == "v1"

    # Push a new commit to origin/main while the worktree sits idle.
    subprocess.run(["git", "-C", str(seed), "fetch", "origin"], check=True, shell=False, timeout=30)
    (seed / "README.md").write_text("v2")
    subprocess.run(["git", "-C", str(seed), "add", "README.md"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "commit", "-m", "v2"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "branch", "-M", "main"], check=True, shell=False, timeout=10)
    subprocess.run(["git", "-C", str(seed), "push", "origin", "main"], check=True, shell=False, timeout=30)
    head_v2 = subprocess.run(["git", "-C", str(seed), "rev-parse", "origin/main"], check=True, shell=False, timeout=10, capture_output=True, text=True).stdout.strip()
    assert head_v1 != head_v2

    # ensure_worktree must move the worktree HEAD to head_v2.
    mod.ensure_worktree(wt, "main", seed)
    head_after = subprocess.run(["git", "-C", str(wt), "rev-parse", "HEAD"], check=True, shell=False, timeout=10, capture_output=True, text=True).stdout.strip()
    assert head_after == head_v2, f"expected {head_v2}, got {head_after}"
    assert (wt / "README.md").read_text() == "v2"


def test_ensure_worktree_sync_from_local_skips_fetch(monkeypatch, tmp_path):
    """When sync_from_local=True, no `git fetch` subprocess is invoked."""
    mod = _load_module()
    calls: list[list[str]] = []
    real_run = subprocess.run

    def fake_run(cmd, *args, **kwargs):
        calls.append(list(cmd))
        # Pretend the worktree exists so we go down the reset branch.
        if cmd[:2] == ["git", "reset"]:
            return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
        return real_run(cmd, *args, **kwargs)

    monkeypatch.setattr(subprocess, "run", fake_run)
    wt = tmp_path / "wt"
    wt.mkdir()
    (wt / ".git").write_text("gitdir: /tmp/fake")
    mod.ensure_worktree(wt, "main", tmp_path, sync_from_local=True)

    fetch_calls = [c for c in calls if c[:2] == ["git", "fetch"]]
    assert fetch_calls == [], f"sync_from_local must skip fetch, but saw: {fetch_calls}"
    reset_calls = [c for c in calls if c[:2] == ["git", "reset"]]
    assert reset_calls, "expected a reset call"
    assert reset_calls[0][-1] == "main", f"sync_from_local must reset to local main, got {reset_calls[0]}"


def test_sync_from_local_cli_flag(monkeypatch):
    mod = _load_module()
    args = mod.parse_args(["--sync-from-local"])
    assert args.sync_from_local is True
    args = mod.parse_args([])
    assert args.sync_from_local is False


def test_dry_run_does_not_create_directories(tmp_path, monkeypatch):
    """Regression: --dry-run must NOT touch the filesystem."""
    mod = _load_module()
    monkeypatch.delenv("NIGHT_WORKTREE_PATH", raising=False)
    monkeypatch.delenv("NIGHT_BASE_BRANCH", raising=False)
    monkeypatch.delenv("NIGHT_REPORT_PATH", raising=False)

    absent_wt = tmp_path / "does-not-exist-yet"
    assert not absent_wt.exists()

    args = mod.parse_args([
        "--worktree", str(absent_wt),
        "--dry-run",
        "--log-level", "INFO",
    ])
    rc = mod.main.__wrapped__ if hasattr(mod.main, "__wrapped__") else mod.main
    # Direct call to main is awkward because it uses sys.argv; we go through subprocess instead.
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH),
         "--worktree", str(absent_wt),
         "--dry-run",
         "--log-level", "WARNING"],
        shell=False,
        check=False,
        timeout=15,
        capture_output=True,
        text=True,
        cwd=str(tmp_path),
        env={**os.environ, "NIGHT_BASE_BRANCH": "main"},
    )
    assert completed.returncode == 0, completed.stderr
    # Worktree directory must NOT have been created.
    assert not absent_wt.exists()
    # scratch/ parent must NOT have been created either.
    assert not (absent_wt / "scratch").exists()


def test_script_help_works():
    """Smoke: the script's --help should exit 0 with usage text on stderr."""
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH), "--help"],
        shell=False,
        check=False,
        timeout=10,
        capture_output=True,
        text=True,
    )
    assert completed.returncode == 0
    assert "Launch the opencode night-issue-prs workflow" in completed.stdout


def test_no_worktree_create_fails_when_missing(tmp_path, monkeypatch):
    """--no-worktree-create should exit non-zero when the worktree is absent."""
    mod = _load_module()
    monkeypatch.delenv("NIGHT_WORKTREE_PATH", raising=False)
    monkeypatch.delenv("NIGHT_BASE_BRANCH", raising=False)
    monkeypatch.delenv("NIGHT_REPORT_PATH", raising=False)

    absent_wt = tmp_path / "never-created"
    completed = subprocess.run(
        [sys.executable, str(SCRIPT_PATH),
         "--worktree", str(absent_wt),
         "--no-worktree-create",
         "--log-level", "WARNING"],
        shell=False,
        check=False,
        timeout=15,
        capture_output=True,
        text=True,
        cwd=str(tmp_path),
        env={**os.environ, "NIGHT_BASE_BRANCH": "main"},
    )
    assert completed.returncode == 2
    assert b"worktree missing" in completed.stderr.encode() or b"worktree missing" in completed.stdout.encode()
