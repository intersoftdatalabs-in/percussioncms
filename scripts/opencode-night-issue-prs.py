#!/usr/bin/env python3
"""Launch the opencode night-issue-prs workflow.

Resolves the dedicated worktree, sets the NIGHT_* env vars that the
`.opencode/plugin/night.ts` plugin reads, and runs `opencode run` against
the `night-worker` host agent. Mirrors `.grok/workflows/night-issue-prs.rhai`
v2.0.6 — see `.grok/workflows/README.md` for the canonical phase list and
hard gates.

## Behavioral Notes

- Cross-platform Python 3.9+ per `scripts/README.md` "Conventions" (spec 994
  FR-001). No `shell=True`, `os.system`, or hardcoded path separators
  (FR-007, FR-008).
- Worktree default: `<home>/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs`
  Override with `--worktree PATH` or `NIGHT_WORKTREE_PATH` env var.
- Idempotent: if the worktree is missing, creates `night-issue-prs-main` from
  `origin/<base_branch>` and adds a worktree at the default path. Existing
  worktrees are reused; the script never wipes a dirty tree.
- Scratch lives under the worktree's `scratch/` directory (path-portable via
  `pathlib.Path`); never writes to `%TEMP%` / `$TMPDIR`.
- The `--dry-run` flag prints the resolved command without invoking `opencode`.
  Use it for cron wiring validation.

## Usage

    python3 scripts/opencode-night-issue-prs.py [--max-issues N] [--base-branch BR]
                                                [--worktree PATH] [--report-path PATH]
                                                [--max-prs N] [--include-pr-followup | --no-include-pr-followup]
                                                [--model MODEL] [--command NAME | --prompt TEXT]
                                                [--sync-from-local] [--no-worktree-create]
                                                [--dry-run] [-v]

## Examples

    # Live overnight: 3 unassigned issues, default worktree, default command
    python3 scripts/opencode-night-issue-prs.py --max-issues 3

    # PR-followup-only night (no new work)
    python3 scripts/opencode-night-issue-prs.py --max-issues 1 \\
        --max-prs 8 --no-include-pr-followup

    # Dry run (cron validation)
    python3 scripts/opencode-night-issue-prs.py --dry-run --max-issues 5

    # Dev testing without pushing local commits first
    python3 scripts/opencode-night-issue-prs.py --sync-from-local --max-issues 1

    # Verbose logging via short flag
    python3 scripts/opencode-night-issue-prs.py -v --dry-run

    # Override the command template with a custom prompt (debugging)
    python3 scripts/opencode-night-issue-prs.py --prompt "echo hello"

## Cron wiring (Linux / macOS)

    # /etc/cron.d/percussioncms-night-issue-prs  (or user crontab)
    NIGHT_WORKTREE_PATH=/home/ci/.opencode/worktrees/percussioncms/night-issue-prs
    0 1 * * * cd /path/to/percussioncms && python3 scripts/opencode-night-issue-prs.py \\
        --max-issues 3 --base-branch main \\
        >> /var/log/percussioncms-night.log 2>&1

## Task Scheduler wiring (Windows)

    schtasks /create /tn "percussioncms-night" /tr \\
        "cmd /c cd /d C:\\path\\to\\percussioncms && python scripts\\opencode-night-issue-prs.py --max-issues 3" \\
        /sc daily /st 01:00
"""
from __future__ import annotations

import argparse
import json
import logging
import os
import shlex
import subprocess
import sys
from pathlib import Path

REPO_SLUG = "intersoft-workspace-percussioncms"
COMPONENT = "night-issue-prs"
DEFAULT_BASE_BRANCH = "main"
AGENT_NAME = "night-worker"

LOGGER = logging.getLogger(__name__)


def default_worktree_path() -> Path:
    return Path.home() / ".opencode" / "worktrees" / REPO_SLUG / COMPONENT


def resolve_worktree(args: argparse.Namespace) -> Path:
    raw = args.worktree or os.environ.get("NIGHT_WORKTREE_PATH", "")
    return Path(raw).expanduser() if raw else default_worktree_path()


def resolve_base_branch(args: argparse.Namespace) -> str:
    return args.base_branch or os.environ.get("NIGHT_BASE_BRANCH", DEFAULT_BASE_BRANCH)


def resolve_report_path(args: argparse.Namespace, worktree: Path) -> Path:
    if args.report_path:
        return Path(args.report_path).expanduser()
    env = os.environ.get("NIGHT_REPORT_PATH", "")
    if env:
        return Path(env).expanduser()
    return worktree / "scratch" / "night-report.md"


def worktree_exists(worktree: Path) -> bool:
    """Return True when `worktree` looks like a registered git worktree.

    In a git worktree, the inner `.git` is a **file** (`gitdir: <main>/.git/worktrees/<name>`),
    not a directory. The presence of that file is sufficient evidence that git has
    registered the path as a worktree. A stale `.git` file (orphaned from a removed
    worktree) is the operator's problem to clean up via
    `scripts/prune-stale-worktrees.py`, not this launcher.
    """
    return (worktree / ".git").exists()


def ensure_worktree(worktree: Path, base_branch: str, repo_root: Path, sync_from_local: bool = False) -> None:
    """Sync the worktree to the configured base branch ref.

    Always fetches the latest remote (when syncing from origin), then:

    - **First run** (worktree missing): `git worktree add -B <branch> <ref>`
      creates the branch and the worktree atomically.
    - **Subsequent runs** (worktree exists): `git -C <worktree> reset --hard
      <ref>` moves the worktree's HEAD and the underlying branch together.
      We do NOT use `git branch -f` because git refuses to force-update a
      branch that is currently checked out by a worktree.

    The `--hard` reset discards uncommitted state from a crashed prior run.

    Default sync ref is `origin/<base_branch>` (production-safe). Pass
    `sync_from_local=True` to sync to local `<base_branch>` instead — for
    dev testing without pushing commits first.

    No-op when the worktree is missing AND `--no-worktree-create` is set
    (handled by the caller).
    """
    sync_ref = base_branch if sync_from_local else f"origin/{base_branch}"

    if not sync_from_local:
        fetch_cmd = ["git", "fetch", "origin", base_branch]
        LOGGER.info("fetching base branch: %s", shlex.join(fetch_cmd))
        subprocess.run(
            fetch_cmd,
            cwd=repo_root,
            shell=False,
            check=True,
            timeout=120,
        )

    if worktree_exists(worktree):
        reset_cmd = ["git", "reset", "--hard", sync_ref]
        LOGGER.info("syncing worktree to %s: %s", sync_ref, shlex.join(reset_cmd))
        subprocess.run(
            reset_cmd,
            cwd=worktree,
            shell=False,
            check=True,
            timeout=60,
        )
        return

    worktree.parent.mkdir(parents=True, exist_ok=True)
    add_cmd = ["git", "worktree", "add", "-B", COMPONENT, str(worktree), sync_ref]
    LOGGER.info("adding worktree: %s", shlex.join(add_cmd))
    subprocess.run(
        add_cmd,
        cwd=repo_root,
        shell=False,
        check=True,
        timeout=300,
    )


def build_workflow_args(args: argparse.Namespace) -> str:
    """Serialize workflow args as a JSON object for the agent to parse.

    The agent prompt template (`night-issue-prs.md`) reads these from
    `$ARGUMENTS` and parses either JSON or positional text.
    """
    payload = {
        "max_issues": args.max_issues,
        "base_branch": args.base_branch,
        "max_prs": args.max_prs,
        "include_pr_followup": bool(args.include_pr_followup),
    }
    return json.dumps(payload)


def build_opencode_command(args: argparse.Namespace) -> list[str]:
    """Build the `opencode run` command.

    Two modes:

    1. **Default** — invoke the `night-issue-prs` command template.
       `opencode run --command night-issue-prs --auto '<json-args>'`.
       The command's `agent: night-worker` frontmatter drives agent
       selection; the message fills `$ARGUMENTS` in the template body.
    2. **Override** — if `--prompt` is set, bypass the command template
       and send the prompt directly as the message positional. Specify
       `--agent` explicitly because no command frontmatter is in play.

    `--prompt` is a top-level opencode option that the `run` subcommand
    does not consume, so it is NOT used here even in override mode.
    """
    cmd = ["opencode", "run", "--auto"]
    if args.model:
        cmd.extend(["--model", args.model])

    if args.prompt:
        cmd.extend(["--agent", AGENT_NAME])
        cmd.append(args.prompt)
    else:
        cmd.extend(["--command", args.command])
        cmd.append(build_workflow_args(args))
    return cmd


def build_env(args: argparse.Namespace, worktree: Path, base_branch: str, report: Path) -> dict[str, str]:
    env = os.environ.copy()
    env["NIGHT_WORKTREE_PATH"] = str(worktree)
    env["NIGHT_BASE_BRANCH"] = base_branch
    env["NIGHT_REPORT_PATH"] = str(report)
    env["NIGHT_OPERATOR"] = "opencode"
    env.setdefault("NIGHT_CODING_TOOL", "OpenCode")
    # Keep PWD in sync with cwd so subprocess shells (and opencode's bash
    # tool) resolve relative paths from the worktree, not from wherever
    # this launcher was invoked. Windows MSYS / Git-Bash may re-derive
    # PWD from the actual cwd on each bash invocation, so this is a
    # best-effort sync for POSIX shells and most modern Windows tools.
    env["PWD"] = str(worktree)
    return env


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="opencode-night-issue-prs",
        description="Launch the opencode night-issue-prs workflow.",
    )
    parser.add_argument("--max-issues", type=int, default=3, help="Max items fully processed (default: 3).")
    parser.add_argument("--base-branch", default=DEFAULT_BASE_BRANCH, help=f"PR base branch (default: {DEFAULT_BASE_BRANCH}).")
    parser.add_argument("--worktree", default="", help="Override worktree path (default: ~/.opencode/worktrees/<repo>/night-issue-prs).")
    parser.add_argument("--report-path", default="", help="Override night-report.md path (default: <worktree>/scratch/night-report.md).")
    parser.add_argument("--max-prs", type=int, default=6, help="Max open PRs per follow-up pass (default: 6).")
    parser.add_argument(
        "--include-pr-followup",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Run PR follow-up drain (default: true). Pass --no-include-pr-followup to skip.",
    )
    parser.add_argument("--model", default="", help="Override model (provider/model-id format). Empty = project default.")
    parser.add_argument(
        "--command",
        default="night-issue-prs",
        help="OpenCode command template to invoke (default: night-issue-prs).",
    )
    parser.add_argument(
        "--prompt",
        default="",
        help=(
            "Override: skip the command template and send this text directly "
            "as the opencode run message. Useful for ad-hoc debugging."
        ),
    )
    parser.add_argument("--dry-run", action="store_true", help="Print the resolved command and exit without invoking opencode.")
    parser.add_argument("--no-worktree-create", action="store_true", help="Skip worktree creation; fail if missing.")
    parser.add_argument(
        "--sync-from-local",
        action="store_true",
        help=(
            "Sync the worktree to the local <base_branch> instead of "
            "origin/<base_branch>. Useful for dev testing without pushing "
            "commits first. Production runs should leave this off."
        ),
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
        help="Logging level (default: INFO).",
    )
    parser.add_argument(
        "-v",
        action="store_const",
        const="DEBUG",
        dest="log_level",
        help="Shortcut for --log-level DEBUG.",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s %(levelname)s %(message)s",
    )

    repo_root = Path(__file__).resolve().parent.parent
    worktree = resolve_worktree(args)
    base_branch = resolve_base_branch(args)
    report = resolve_report_path(args, worktree)

    LOGGER.info("repo_root=%s worktree=%s base_branch=%s report=%s", repo_root, worktree, base_branch, report)

    env = build_env(args, worktree, base_branch, report)
    cmd = build_opencode_command(args)

    if args.dry_run:
        LOGGER.info("dry-run: would invoke %s", shlex.join(cmd))
        LOGGER.info("dry-run: cwd=%s", worktree)
        LOGGER.info("dry-run: env NIGHT_WORKTREE_PATH=%s NIGHT_BASE_BRANCH=%s NIGHT_REPORT_PATH=%s", worktree, base_branch, report)
        if not worktree_exists(worktree):
            LOGGER.info("dry-run: worktree does not exist yet; would create from origin/%s", base_branch)
        return 0

    if args.no_worktree_create:
        if not worktree_exists(worktree):
            LOGGER.error("worktree missing at %s and --no-worktree-create set", worktree)
            return 2
    else:
        ensure_worktree(worktree, base_branch, repo_root, sync_from_local=args.sync_from_local)

    report.parent.mkdir(parents=True, exist_ok=True)

    LOGGER.info("invoking: %s", shlex.join(cmd))
    completed = subprocess.run(
        cmd,
        cwd=worktree,
        shell=False,
        check=False,
        timeout=None,
        env=env,
    )
    return completed.returncode


if __name__ == "__main__":
    sys.exit(main())
