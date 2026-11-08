#!/usr/bin/env python3
"""Launch the opencode night-issue-prs workflow.

Resolves the dedicated worktree, sets the NIGHT_* env vars that the
`.opencode/plugins/night/index.ts` plugin reads, and runs `opencode run`
against the `night-worker` host agent. Mirrors
`.grok/workflows/night-issue-prs.rhai` v2.2.5 — see
`.grok/workflows/README.md` for the canonical phase list and hard gates.
Decision-front script ranks the queue; empty leftover Erlang / cluster skips
from 2.1.0; TypeSafe pre-screen remains (fail-open). Work implementers run the
Erlang skill CLI before opening a PR; host/peer phases still spawn the
`erlang-review` subagent.

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
- V2 `opencode run` has no `--command` flag. The launcher inlines the
  `.opencode/commands/<name>.md` body, substitutes `$ARGUMENTS` with a JSON
  payload, and passes the result as the message positional. The command name
  stays configurable with `--command` so the template remains the single
  source of truth.
- Runs with `--standalone` so the private server inherits the NIGHT_* env
  vars. A shared background service may have been started without them.
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
import shutil
import subprocess
import sys
from pathlib import Path

REPO_SLUG = "intersoft-workspace-percussioncms"
COMPONENT = "night-issue-prs"
DEFAULT_BASE_BRANCH = "main"
AGENT_NAME = "night-worker"
COMMAND_DIR = Path(".opencode") / "commands"
ARGUMENTS_PLACEHOLDER = "$ARGUMENTS"

LOGGER = logging.getLogger(__name__)


class LauncherError(RuntimeError):
    """Raised when the launcher cannot build a valid opencode invocation."""


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

    After either path, root `AGENTS.local.md` (gitignored, so never carried
    by git itself) is copied into the worktree one way; see
    `propagate_agents_local`.

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
        propagate_agents_local(repo_root, worktree)
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
    propagate_agents_local(repo_root, worktree)


def propagate_agents_local(repo_root: Path, worktree: Path) -> None:
    """Copy root `AGENTS.local.md` into the worktree (one way, best effort).

    `AGENTS.local.md` is gitignored, so `git worktree add` and
    `git reset --hard` never carry it over — without this copy, agents
    running in the worktree miss the personal/operator overrides. The copy
    runs on every sync so edits in the primary checkout propagate; never
    copy back (worktree edits must not clobber the primary file). Missing
    source is a silent no-op.
    """
    source = Path(repo_root) / "AGENTS.local.md"
    if not source.is_file():
        return
    try:
        shutil.copy2(source, Path(worktree) / "AGENTS.local.md")
    except OSError as exc:
        LOGGER.warning("could not propagate AGENTS.local.md: %s", exc)
    else:
        LOGGER.info("propagated AGENTS.local.md to %s", worktree)


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
        "low_priority_quota_pct": getattr(args, "low_priority_quota_pct", 0),
        "min_debt_slots": getattr(args, "min_debt_slots", 0),
        "max_erlang_residuals": getattr(args, "max_erlang_residuals", 3),
        "labels": getattr(args, "labels", ""),
    }
    return json.dumps(payload)


def strip_frontmatter(text: str) -> str:
    """Return the Markdown body with a leading YAML frontmatter block removed.

    Command templates carry `description:` / `agent:` frontmatter that is only
    meaningful to opencode's command loader. The launcher inlines the body as a
    plain message, so the block is dropped instead of being sent to the model.
    Line endings are normalized to `\n` so the prompt is identical on every OS.
    """
    lines = text.splitlines()
    if not lines or lines[0].strip() != "---":
        return text
    for index in range(1, len(lines)):
        if lines[index].strip() == "---":
            body = "\n".join(lines[index + 1 :]).strip("\n")
            return f"{body}\n" if body else body
    return text


def command_template_path(command: str, repo_root: Path | None = None) -> Path:
    root = repo_root or Path(__file__).resolve().parent.parent
    return root / COMMAND_DIR / f"{command}.md"


def load_command_template(command: str, repo_root: Path | None = None) -> str:
    """Read the body of `.opencode/commands/<command>.md`.

    V2 `opencode run` has no `--command` flag, so the launcher supplies the
    command body inline. Fail loudly when the template is missing or has no
    `$ARGUMENTS` placeholder — silently running a partially-substituted prompt
    would let an unattended night run ignore `--max-issues` / `--base-branch`.
    """
    path = command_template_path(command, repo_root)
    if not path.is_file():
        raise LauncherError(f"command template not found: {path} (use --prompt to bypass the template)")
    body = strip_frontmatter(path.read_text(encoding="utf-8"))
    if ARGUMENTS_PLACEHOLDER not in body:
        raise LauncherError(f"command template has no {ARGUMENTS_PLACEHOLDER} placeholder: {path}")
    return body


def build_workflow_prompt(args: argparse.Namespace, repo_root: Path | None = None) -> str:
    """Inline the command template with `$ARGUMENTS` replaced by the JSON payload."""
    template = load_command_template(args.command, repo_root)
    return template.replace(ARGUMENTS_PLACEHOLDER, build_workflow_args(args))


def build_opencode_command(args: argparse.Namespace, repo_root: Path | None = None) -> list[str]:
    """Build the `opencode run` command.

    Two modes:

    1. **Default** — inline the `night-issue-prs` command template and send the
       rendered text as the message positional. V2 has no `--command` flag, so
       the template's `agent: night-worker` frontmatter cannot select the agent;
       pass `--agent` explicitly.
    2. **Override** — if `--prompt` is set, bypass the command template and send
       the prompt directly as the message positional.

    `--standalone` runs a private server so the `NIGHT_*` env vars exported by
    `build_env` reach the plugin and the bash tool. A shared background service
    may have been started without them.

    `--prompt` is a top-level opencode option that the `run` subcommand does not
    consume, so it is NOT used here.
    """
    cmd = ["opencode", "--standalone", "run", "--auto", "--agent", AGENT_NAME]
    if args.model:
        cmd.extend(["--model", args.model])

    if args.prompt:
        cmd.append(args.prompt)
    else:
        cmd.append(build_workflow_prompt(args, repo_root))
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
        "--low-priority-quota-pct",
        type=int,
        default=0,
        help="Percent of max_issues reserved for p7/p8 tech-debt (ceil division; default: 0).",
    )
    parser.add_argument(
        "--min-debt-slots",
        type=int,
        default=0,
        help="Explicit tech-debt count floor: guarantees at least N debt seats (default: 0).",
    )
    parser.add_argument(
        "--max-erlang-residuals",
        type=int,
        default=3,
        help="Cap on Erlang preexisting/complexity tech-debt residuals per PR (default: 3).",
    )
    parser.add_argument(
        "--labels",
        default="",
        help="Optional discovery label filter, comma-separated (e.g. 'tech-debt'). Empty = all labels.",
    )
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
        help=(
            "OpenCode command template to inline (default: night-issue-prs). "
            "Reads .opencode/commands/<name>.md and substitutes $ARGUMENTS."
        ),
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

    try:
        cmd = build_opencode_command(args, repo_root)
    except LauncherError as exc:
        LOGGER.error("%s", exc)
        return 2

    if args.dry_run:
        # The final argv entry is the message (inlined template or --prompt text).
        # Log its size instead of the whole body so dry-run output stays readable.
        LOGGER.info(
            "dry-run: would invoke %s <message: %d chars>",
            shlex.join(cmd[:-1]),
            len(cmd[-1]),
        )
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
