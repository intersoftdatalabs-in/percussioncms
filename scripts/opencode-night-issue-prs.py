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
- Idempotent: if the worktree is missing, creates the sync branch from
  `origin/<base_branch>` and adds a worktree at the default path. Existing
  worktrees are reused and switched back to the sync branch (a prior
  run may have left a feature branch checked out); uncommitted state is
  stashed with a labeled entry first, never discarded and never carried
  onto the sync branch.
- Stale-worktree cleanup (HARD GATE, on by default): runs
  `scripts/prune-stale-worktrees.py --apply --force
  --delete-local-branches` after sync AND after the run (even on failure),
  removing worktrees whose branches have MERGED/CLOSED PRs and pruning
  metadata left by out-of-band deletions. Disable with `--no-cleanup`.
- Scratch lives under the worktree's `scratch/` directory (path-portable via
  `pathlib.Path`); never writes to `%TEMP%` / `$TMPDIR`.
- V2 `opencode run` has no `--command` flag. The launcher inlines the
  `.opencode/commands/<name>.md` body, substitutes `$ARGUMENTS` with a JSON
  payload, and passes the result as the message positional. The command name
  stays configurable with `--command` so the template remains the single
  source of truth.
- Runs with `--standalone` so the private server inherits the NIGHT_* env
  vars. A shared background service may have been started without them.
- Per-worktree Maven isolation (#6707, on by default): the launcher
  prepends `-Dmaven.repo.local=<worktree>/tmp/m2-worktree/repository` to
  `MAVEN_ARGS` and records the effective path in `NIGHT_M2_REPO`, so night
  agents never overwrite the shared `~/.m2` SNAPSHOTs another cell boots.
  An explicit `-Dmaven.repo.local` already present in `MAVEN_ARGS` keeps
  winning (Maven last-wins); `--no-m2-isolate` opts out entirely.
- Per-worktree Maven seed-once (#6739, on by default): after creating the
  directory, the launcher seeds third-party (non-`SNAPSHOT`) artifacts from
  the resolved shared repository exactly once (marker-guarded, idempotent),
  so the first worktree build does not re-download the internet. Project
  `SNAPSHOT`s are never copied — they are rebuilt in place, which is the
  isolation this exists for. `--no-m2-seed` skips seeding; `--m2-seed-from`
  overrides the source. Seeding is best-effort (warns, never aborts launch)
  and never runs under `--dry-run`.
- The `--dry-run` flag prints the resolved command without invoking `opencode`.
  Use it for cron wiring validation.

## Usage

    python3 scripts/opencode-night-issue-prs.py [--max-issues N] [--base-branch BR]
                                                [--worktree PATH] [--report-path PATH]
                                                [--max-prs N] [--max-parallel-work N]
                                                [--include-pr-followup | --no-include-pr-followup]
                                                [--model MODEL] [--command NAME | --prompt TEXT]
                                                [--sync-from-local] [--no-worktree-create]
                                                [--m2-isolate | --no-m2-isolate]
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
        "cmd /c cd /d C:\\path\\to\\percussioncms && python scripts\\opencode-night-issue-prs.py --max-issues 3" \\  # mkd-code-review[paths.hardcoded_sep]: adjudicated FP (issue #6924, precedent #6779/#6781) — Windows-only Task Scheduler doc example, never an OS path join.
        /sc daily /st 01:00
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import logging
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path
from typing import Optional

REPO_SLUG = "intersoft-workspace-percussioncms"
COMPONENT = "night-issue-prs"
DEFAULT_BASE_BRANCH = "main"
AGENT_NAME = "night-worker"
COMMAND_DIR = Path(".opencode") / "commands"
ARGUMENTS_PLACEHOLDER = "$ARGUMENTS"

# Per-worktree Maven local repository segment (#6707). The layout mirrors
# docker/scripts/maven_local_repo.worktree_local_repository so the launcher
# and the QA helpers resolve the same directory by construction. Seeding
# itself is delegated to that canonical helper (#6739) — no layout, marker,
# or copy logic is duplicated here.
_M2_WORKTREE_DIRNAME = "m2-worktree"
_M2_HELPER_REL = Path("docker") / "scripts" / "maven_local_repo.py"
_REPO_LOCAL_RE = re.compile(r"""-Dmaven\.repo\.local=(?:"([^"]*)"|'([^']*)'|(\S+))""")

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
    - **Subsequent runs** (worktree exists): `git checkout -B <branch>
      <ref>` switches the worktree back to the sync branch and hard-resets
      it. A bare `git reset --hard <ref>` is NOT enough: a previous run may
      have left the worktree sitting on a feature branch (e.g. a Work agent
      branched inside the dedicated worktree), and resetting without
      switching would move that feature branch pointer instead of the sync
      branch. Never use `git branch -f`; git refuses to force-update a
      branch that is currently checked out by a worktree.

    Uncommitted state is stashed with a labeled entry first — never
    discarded, never carried onto the sync branch.

    After either path, root and nested `AGENTS.local.md` files (gitignored,
    so never carried by git itself) are copied into the worktree one way;
    see `propagate_agents_local`.

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
        # A prior run may have left the worktree on a feature branch AND/OR
        # with uncommitted changes (an interrupted run, or a live session).
        # Preserve work first: stash dirty state with a labeled entry so
        # nothing is destroyed or carried onto the sync branch. Then
        # `checkout -B` both switches back to the sync branch (undoing any
        # feature branch a prior run left checked out) and resets it to
        # sync_ref. A bare `git reset --hard <ref>` is NOT enough: it would
        # move the feature branch pointer instead of the sync branch.
        # Never use `git branch -f`; git refuses to force-update a branch
        # that is currently checked out by a worktree.
        status = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=worktree,
            shell=False,
            check=False,
            timeout=60,
            capture_output=True,
            text=True,
        )
        if status.returncode == 0 and status.stdout.strip():
            stash_cmd = [
                "git", "stash", "push", "-m",
                f"night-issue-prs auto-stash before sync to {sync_ref}",
            ]
            LOGGER.info("stashing uncommitted worktree state: %s", shlex.join(stash_cmd))
            subprocess.run(
                stash_cmd,
                cwd=worktree,
                shell=False,
                check=True,
                timeout=120,
            )
        checkout_cmd = ["git", "checkout", "-B", COMPONENT, sync_ref]
        LOGGER.info("syncing worktree to %s: %s", sync_ref, shlex.join(checkout_cmd))
        subprocess.run(
            checkout_cmd,
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


def run_worktree_cleanup(repo_root: Path, *, include_no_pr: bool = False) -> None:
    """Remove stale agent worktrees (HARD GATE, best-effort, never raises).

    Runs ``scripts/prune-stale-worktrees.py --apply --force
    --delete-local-branches`` so worktrees whose branches have MERGED/CLOSED
    PRs are removed and their local branches deleted. Also repairs stale
    metadata left by out-of-band deletions (``git worktree prune``).

    Open-PR, locked, main, and current-directory worktrees are always kept
    by the prune script. Failures (missing ``gh``, no network, git errors)
    are logged and swallowed: cleanup must never fail a night run.
    """
    script = repo_root / "scripts" / "prune-stale-worktrees.py"
    if not script.is_file():
        LOGGER.warning("worktree cleanup skipped: %s not found", script)
        return
    cmd = [
        sys.executable,
        str(script),
        "--apply",
        "--force",
        "--delete-local-branches",
    ]
    if include_no_pr:
        cmd.append("--include-no-pr")
    LOGGER.info("running worktree cleanup: %s", shlex.join(cmd))
    try:
        completed = subprocess.run(
            cmd,
            cwd=repo_root,
            shell=False,
            check=False,
            timeout=300,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        LOGGER.warning("worktree cleanup failed to start: %s", exc)
        return
    if completed.stdout.strip():
        LOGGER.info("worktree cleanup output:\n%s", completed.stdout.strip())
    if completed.returncode != 0:
        LOGGER.warning(
            "worktree cleanup exited %d: %s",
            completed.returncode,
            (completed.stderr or "").strip(),
        )


def _load_seed_helper():
    """Load scripts/night-seed-worktree.py (hyphenated filename)."""
    path = Path(__file__).resolve().parent / "night-seed-worktree.py"
    spec = importlib.util.spec_from_file_location("night_seed_worktree", path)
    if spec is None or spec.loader is None:
        raise ImportError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def propagate_agents_local(repo_root: Path, worktree: Path) -> None:
    """Copy `AGENTS.local.md` into the worktree (one way, best effort).

    Root and nested module `AGENTS.local.md` files are gitignored, so
    `git worktree add` and `git reset --hard` never carry them over —
    without this copy, agents running in the worktree miss the
    personal/operator overrides. The copy runs on every sync so edits in
    the primary checkout propagate (`overwrite=True`); never copy back
    (worktree edits must not clobber the primary file). Missing source is
    a silent no-op. See `scripts/night-seed-worktree.py`.
    """
    try:
        helper = _load_seed_helper()
        copied = helper.seed_agents_local(
            Path(worktree), primary=Path(repo_root), overwrite=True
        )
    except OSError as exc:
        LOGGER.warning("could not propagate AGENTS.local.md: %s", exc)
        return
    if copied:
        names = ", ".join(rel.as_posix() for rel in copied)
        LOGGER.info("propagated AGENTS.local.md (%s) to %s", names, worktree)


def build_workflow_args(args: argparse.Namespace) -> str:
    """Serialize workflow args as a JSON object for the agent to parse.

    The agent prompt template (`night-issue-prs.md`) reads these from
    `$ARGUMENTS` and parses either JSON or positional text.
    """
    payload = {
        "max_issues": args.max_issues,
        "base_branch": args.base_branch,
        "max_prs": args.max_prs,
        "max_parallel_work": getattr(args, "max_parallel_work", 1),
        "include_pr_followup": bool(args.include_pr_followup),
        "low_priority_quota_pct": getattr(args, "low_priority_quota_pct", 20),
        "min_debt_slots": getattr(args, "min_debt_slots", 0),
        "max_erlang_residuals": getattr(args, "max_erlang_residuals", 100),
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
    env["NIGHT_MAX_PARALLEL_WORK"] = str(getattr(args, "max_parallel_work", 1))
    env.setdefault("NIGHT_CODING_TOOL", "OpenCode")
    if getattr(args, "m2_isolate", True):
        # Per-worktree Maven local repo (#6707): night agents used to share
        # ~/.m2 across shards, so a `clean install` from a feature branch
        # overwrote the rest/sitemanage SNAPSHOTs another cell's qa-up then
        # booted (Frankenstein classpath, Spring context failure). Prepend
        # (not append): Maven applies the LAST -Dmaven.repo.local, so an
        # explicit operator value already in MAVEN_ARGS keeps winning while
        # the default run isolates into the worktree.
        repo = worktree_maven_repo(worktree)
        existing = env.get("MAVEN_ARGS", "")
        flag = maven_repo_local_flag(repo)
        if str(repo) not in existing:
            env["MAVEN_ARGS"] = f"{flag} {existing}" if existing.strip() else flag
        explicit = last_repo_local_value(existing)
        env["NIGHT_M2_REPO"] = (
            str(Path(os.path.expanduser(explicit))) if explicit else str(repo)
        )
    # Keep PWD in sync with cwd so subprocess shells (and opencode's bash
    # tool) resolve relative paths from the worktree, not from wherever
    # this launcher was invoked. Windows MSYS / Git-Bash may re-derive
    # PWD from the actual cwd on each bash invocation, so this is a
    # best-effort sync for POSIX shells and most modern Windows tools.
    env["PWD"] = str(worktree)
    return env


def worktree_maven_repo(worktree: Path) -> Path:
    """Return the per-worktree Maven local repository for a checkout (#6707).

    Portable: built with ``pathlib`` joins only, never string separators.
    Distinct for every worktree because ``worktree`` differs per worktree.
    The directory lives under ``<worktree>/tmp`` (gitignored repo temp dir
    per root AGENTS.md) so it vanishes with the worktree and never pollutes
    ``git status``. Layout matches
    ``docker/scripts/maven_local_repo.worktree_local_repository`` (which
    also resolves, so the two agree even across symlinked ``/tmp``).
    """
    return Path(worktree).resolve() / "tmp" / _M2_WORKTREE_DIRNAME / "repository"


def maven_repo_local_flag(repo: Path) -> str:
    """Format ``-Dmaven.repo.local=...`` for ``MAVEN_ARGS`` / ``MAVEN_OPTS``.

    Values containing whitespace are wrapped in double quotes because the
    ``mvn`` launcher whitespace-splits those variables; quoting matches the
    canonical ``docker/scripts/maven_local_repo.repo_local_flag``.
    """
    text = str(repo)
    if any(ch.isspace() for ch in text):
        text = f'"{text}"'
    return f"-Dmaven.repo.local={text}"


def last_repo_local_value(text: str) -> str | None:
    """Return the last ``-Dmaven.repo.local=`` value in ``text`` (Maven: last wins)."""
    if not text:
        return None
    value = None
    for match in _REPO_LOCAL_RE.finditer(text):
        value = next(g for g in match.groups() if g is not None)
    return value


def ensure_night_m2_repo(worktree: Path) -> Path:
    """Create the per-worktree Maven local repository directory (#6707).

    Creates the parent chain only — never copies or downloads artifacts, so
    the launcher stays fast and side-effect-light. Third-party warm-start
    is a separate, marker-guarded step: see seed_worktree_m2_repo (#6739).
    Idempotent: safe to call on every run.
    """
    repo = worktree_maven_repo(worktree)
    repo.mkdir(parents=True, exist_ok=True)
    return repo


def _load_maven_local_repo():
    """Load the canonical ``docker/scripts/maven_local_repo.py`` helper (#6739).

    ``importlib`` from the file location (resolved from this file's checkout
    root): no ``sys.path`` pollution, no subprocess, Windows-safe. The
    launcher reuses ``resolve_m2_repository`` (shared-source resolution) and
    ``ensure_worktree_repository`` (seed-once copy + marker) instead of
    duplicating layout or copy logic.
    """
    helper = Path(__file__).resolve().parent.parent / _M2_HELPER_REL
    spec = importlib.util.spec_from_file_location("maven_local_repo", helper)
    if spec is None or spec.loader is None:
        raise LauncherError(f"cannot load Maven helper at {helper}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def seed_worktree_m2_repo(
    worktree: Path,
    *,
    seed_from: Optional[Path] = None,
    env: Optional[dict] = None,
) -> Path:
    """Seed third-party artifacts into the per-worktree repo, once (#6739).

    Copies non-``SNAPSHOT`` artifacts from ``seed_from`` (default: the shared
    local repository resolved the way ``mvn`` would from the launcher's own
    environment — before the worktree flag is applied) into
    ``worktree_maven_repo(worktree)``. Project ``SNAPSHOT`` jars are never
    copied; they are rebuilt in place by the worktree's own ``clean
    install``. A marker records the seed source, so a second call with the
    same source is a no-op (idempotent across runs).

    Best-effort by design: any failure is logged as a warning and the
    worktree repo path is still returned — a cold (unseeded) repo only costs
    the first build its downloads, and must never abort the night run.
    """
    repo = worktree_maven_repo(worktree)
    repo.mkdir(parents=True, exist_ok=True)
    try:
        helper = _load_maven_local_repo()
        source = (
            Path(seed_from)
            if seed_from is not None
            else helper.resolve_m2_repository(worktree, env=dict(os.environ) if env is None else env)
        )
        helper.ensure_worktree_repository(worktree, source)
        LOGGER.info("per-worktree Maven repo seeded from %s", source)
    except Exception as exc:  # noqa: BLE001 — warm-start must never abort launch
        LOGGER.warning("per-worktree Maven repo seed skipped: %s", exc)
    return repo


def should_seed_worktree_repo(args: argparse.Namespace, env: dict, m2_repo: Path) -> bool:
    """Decide whether the launcher seeds the per-worktree repo this run (#6739).

    Seeds only when isolation is on, seeding is on (``--m2-seed``), and the
    agent will actually build into the worktree repo — i.e. ``NIGHT_M2_REPO``
    (Maven last-wins over the prepended flag) still points at it. An
    explicit operator ``-Dmaven.repo.local`` redirects builds elsewhere, so
    seeding the worktree repo would be wasted I/O.
    """
    return (
        bool(getattr(args, "m2_isolate", True))
        and bool(getattr(args, "m2_seed", True))
        and env.get("NIGHT_M2_REPO", "") == str(m2_repo)
    )


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
        "--max-parallel-work",
        type=int,
        default=1,
        help=(
            "Max concurrent Work agents (default: 1 = sequential). Pilot 2 seats: "
            "pass 2; each seat must export a distinct PERC_QA_CELL_SUFFIX so QA "
            "cells stay isolated (see docker/scripts/perc-devctl.py qa-up)."
        ),
    )
    parser.add_argument(
        "--low-priority-quota-pct",
        type=int,
        default=20,
        help="Debt tax pct seated on top of max_issues (ceil division; default: 20). Pass 0 for no debt.",
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
        default=100,
        help="Cap on Erlang preexisting/complexity tech-debt residuals per PR (default: 100).",
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
    parser.add_argument(
        "--no-worktree-create", action="store_true", help="Skip worktree creation; fail if missing.")
    parser.add_argument(
        "--cleanup",
        action=argparse.BooleanOptionalAction,
        default=True,
        help=(
            "Run stale-worktree cleanup (prune-stale-worktrees.py --apply) "
            "after sync and again after the run, even on failure "
            "(default: true). Pass --no-cleanup to skip."
        ),
    )
    parser.add_argument(
        "--cleanup-include-no-pr",
        action="store_true",
        help="Also remove worktrees whose branch has no linked PR during cleanup.",
    )
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
        "--m2-isolate",
        action=argparse.BooleanOptionalAction,
        default=True,
        help=(
            "Isolate this run's Maven builds into the per-worktree local "
            "repository <worktree>/tmp/m2-worktree/repository via MAVEN_ARGS "
            "(default: true; #6707). Without it, branch-built rest/sitemanage "
            "SNAPSHOTs overwrite the shared ~/.m2 every other cell boots. "
            "Pass --no-m2-isolate only when the operator manages "
            "-Dmaven.repo.local explicitly. An explicit -Dmaven.repo.local "
            "already present in MAVEN_ARGS always wins (Maven last-wins)."
        ),
    )
    parser.add_argument(
        "--m2-seed",
        action=argparse.BooleanOptionalAction,
        default=True,
        help=(
            "Seed third-party (non-SNAPSHOT) artifacts into the per-worktree "
            "local repository once, from the resolved shared repository "
            "(default: true; #6739). Marker-guarded: repeat runs are a no-op. "
            "Project SNAPSHOTs are never copied. Pass --no-m2-seed on "
            "disk-constrained hosts (first build then downloads everything)."
        ),
    )
    parser.add_argument(
        "--m2-seed-from",
        default="",
        help=(
            "Override the shared repository used as the --m2-seed source "
            "(default: resolved the way mvn would from MAVEN_ARGS / "
            "mavenrc / settings.xml). Only used when the agent builds into "
            "the worktree repo (no explicit -Dmaven.repo.local)."
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
        LOGGER.info("dry-run: env NIGHT_WORKTREE_PATH=%s NIGHT_BASE_BRANCH=%s NIGHT_REPORT_PATH=%s NIGHT_MAX_PARALLEL_WORK=%s", worktree, base_branch, report, getattr(args, "max_parallel_work", 1))
        LOGGER.info(
            "dry-run: m2 isolation=%s repo=%s MAVEN_ARGS=%s",
            getattr(args, "m2_isolate", True),
            env.get("NIGHT_M2_REPO", "<shared>"),
            env.get("MAVEN_ARGS", "<unchanged>"),
        )
        if not worktree_exists(worktree):
            LOGGER.info("dry-run: worktree does not exist yet; would create from origin/%s", base_branch)
        return 0

    if args.no_worktree_create:
        if not worktree_exists(worktree):
            LOGGER.error("worktree missing at %s and --no-worktree-create set", worktree)
            return 2
    else:
        ensure_worktree(worktree, base_branch, repo_root, sync_from_local=args.sync_from_local)

    if args.cleanup:
        # Start-of-run sweep: remove worktrees left stale by previously
        # merged/closed PRs before the agent starts branching.
        run_worktree_cleanup(repo_root, include_no_pr=args.cleanup_include_no_pr)

    report.parent.mkdir(parents=True, exist_ok=True)

    if getattr(args, "m2_isolate", True):
        # Create the per-worktree repo dir now so the path exported in
        # MAVEN_ARGS exists before the agent's first `mvn` (which would
        # create it anyway — this just makes dry-run logs and preflight
        # resolution deterministic). Third-party warm-start is marker-guarded
        # and best-effort: see seed_worktree_m2_repo (#6739).
        m2_repo = ensure_night_m2_repo(worktree)
        LOGGER.info("per-worktree Maven local repo: %s", m2_repo)
        if should_seed_worktree_repo(args, env, m2_repo):
            seed_from = Path(args.m2_seed_from).expanduser() if getattr(args, "m2_seed_from", "") else None
            seed_worktree_m2_repo(worktree, seed_from=seed_from)
        else:
            LOGGER.info(
                "per-worktree Maven repo seed skipped (m2_seed=%s, NIGHT_M2_REPO=%s)",
                getattr(args, "m2_seed", True),
                env.get("NIGHT_M2_REPO", "<shared>"),
            )
    else:
        LOGGER.info("m2 isolation disabled (--no-m2-isolate); using shared local repo")

    LOGGER.info("invoking: %s", shlex.join(cmd))
    try:
        completed = subprocess.run(
            cmd,
            cwd=worktree,
            shell=False,
            check=False,
            timeout=None,
            env=env,
        )
        returncode = completed.returncode
    finally:
        if args.cleanup:
            # End-of-run sweep (HARD GATE): runs even when the agent run
            # fails or is interrupted, so merged/closed PR worktrees from
            # this run are never left behind to accumulate on disk.
            run_worktree_cleanup(repo_root, include_no_pr=args.cleanup_include_no_pr)
    return returncode


if __name__ == "__main__":
    sys.exit(main())
