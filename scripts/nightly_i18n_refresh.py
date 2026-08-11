#!/usr/bin/env python3
"""Nightly i18n translation refresh for Percussion CMS.

Rotates through 16 base locales (day_of_year % 16), translates missing TUVs,
and creates a PR with the changes.

Usage:
    python3 scripts/nightly_i18n_refresh.py
    python3 scripts/nightly_i18n_refresh.py --locale de
    python3 scripts/nightly_i18n_refresh.py --locale ar --dry-run
    python3 scripts/nightly_i18n_refresh.py --locale fr --verbose

Note: This wrapper intentionally does NOT run ``mvnw spotless:apply``. The
perc-i18n spotless config targets JSON (which we excluded due to the 4 MB
translation cache) and the eclipseWtp XML formatter hangs on this
environment's Eclipse OSGi classloader. TMX formatting is left to the
translation script itself per modules/perc-i18n/AGENTS.md.
"""
from __future__ import annotations

import argparse
import datetime
import fcntl
import json
import logging
import logging.handlers
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

AGENT_MODEL = "MiniMax-M3"
AGENT_MODEL_SLUG = "minimax-m3"
AGENT_NAME = "kilo"
CO_AUTHORED_FOOTER = (
    f"> Automated Co-Authored by Kilo using {AGENT_MODEL} with agent {AGENT_NAME}."
)

OPERATOR_LABEL = "operator:kilo"
MODEL_LABEL = f"model:{AGENT_MODEL_SLUG}"
PR_LABELS = (OPERATOR_LABEL, MODEL_LABEL)

DEFAULT_WORKTREE = Path.home() / ".kilo" / "worktrees" / "nightly-i18n-refresh"

REPO_ROOT = Path(__file__).resolve().parent.parent
LOCK_FILE = REPO_ROOT / "tmp" / "nightly-i18n.lock"
LOG_DIR = Path.home() / "logs"
LOG_FILE = LOG_DIR / "nightly-i18n-refresh.log"

BASE_LOCALES = (
    "ar", "bn", "de", "es", "fr", "he", "hi", "it",
    "nl", "pl", "pt", "ru", "sv", "te", "tr", "uk"
)

I18N_MODULE = REPO_ROOT / "modules" / "perc-i18n"
I18N_SCRIPTS_DIR = I18N_MODULE / "scripts"
TRANSLATE_SCRIPT = I18N_SCRIPTS_DIR / "i18n_translate_direct.py"

TMX_DIR = Path("modules/perc-i18n/src/main/resources/i18n")
TMX_FILES = (
    "modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx",
    "modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx",
    "modules/perc-i18n/src/main/resources/i18n/DeveloperUi.tmx",
)
CACHE_FILE = I18N_SCRIPTS_DIR / "cache" / "i18n_translate.json"


@dataclass
class TranslationResult:
    missing: int = 0
    inserted: int = 0
    fixed: int = 0
    skipped: int = 0
    had_error: bool = False
    error_message: str = ""


def setup_logging(verbose: bool) -> logging.Logger:
    """Configure rotating file logger."""
    LOG_DIR.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger("nightly_i18n_refresh")
    logger.setLevel(logging.DEBUG if verbose else logging.INFO)

    if logger.handlers:
        return logger

    file_handler = logging.handlers.RotatingFileHandler(
        LOG_FILE,
        maxBytes=10 * 1024 * 1024,
        backupCount=5,
    )
    file_handler.setLevel(logging.DEBUG if verbose else logging.INFO)

    console_handler = logging.StreamHandler(sys.stderr)
    console_handler.setLevel(logging.DEBUG if verbose else logging.INFO)

    fmt = "%(asctime)s %(levelname)s %(message)s"
    file_handler.setFormatter(logging.Formatter(fmt))
    console_handler.setFormatter(logging.Formatter(fmt))

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    return logger


def run(
    args: list[str],
    *,
    cwd: Optional[Path] = None,
    check: bool = False,
    capture_output: bool = True,
) -> subprocess.CompletedProcess:
    """Run a command, returning the result."""
    return subprocess.run(
        args,
        cwd=str(cwd) if cwd else None,
        text=True,
        capture_output=capture_output,
        check=check,
    )


def ensure_worktree(worktree: Path, logger: logging.Logger) -> bool:
    """Ensure the dedicated worktree exists and is on main branch."""
    worktree_list = run(["git", "worktree", "list", "--porcelain"])

    if worktree.exists():
        for line in worktree_list.stdout.splitlines():
            if line.startswith("worktree ") and worktree.resolve() == Path(line[9:]).resolve():
                logger.debug(f"Worktree {worktree} already exists")
                break
        else:
            logger.warning(f"Path {worktree} exists but is not a git worktree")
            return False
    else:
        logger.info(f"Creating new worktree at {worktree}")
        parent = worktree.parent
        parent.mkdir(parents=True, exist_ok=True)

        cp = run(["git", "worktree", "add", "--checkout", str(worktree), "origin/main"])
        if cp.returncode != 0:
            logger.error(f"Failed to create worktree: {cp.stderr}")
            return False

    run(["git", "fetch", "origin", "main"], cwd=worktree)

    current_branch = run(["git", "branch", "--show-current"], cwd=worktree).stdout.strip()
    if current_branch:
        logger.info(f"Worktree on branch '{current_branch}', checking out origin/main (detached)")
        run(["git", "checkout", "origin/main"], cwd=worktree)
    else:
        head = run(["git", "rev-parse", "HEAD"], cwd=worktree).stdout.strip()
        origin_main = run(["git", "rev-parse", "origin/main"], cwd=worktree).stdout.strip()
        if head != origin_main:
            logger.info(f"Worktree not at origin/main, resetting")
            run(["git", "reset", "--hard", "origin/main"], cwd=worktree)

    return True


def is_working_tree_clean_worktree(worktree: Path) -> bool:
    """Check if the worktree's working tree is clean."""
    cp = run(["git", "status", "--porcelain"], cwd=worktree)
    return cp.stdout.strip() == ""


def get_current_branch_worktree(worktree: Path) -> str:
    """Get the current branch name in the worktree."""
    cp = run(["git", "branch", "--show-current"], cwd=worktree)
    return cp.stdout.strip()


def check_gh_auth() -> bool:
    """Check if the active gh account is authenticated.

    Uses ``--active`` so inactive multi-account entries with expired tokens
    do not fail the preflight when the active account is usable.
    """
    cp = run(["gh", "auth", "status", "--active"])
    return cp.returncode == 0


def check_trans_available() -> bool:
    """Check if trans (translate-shell) is on PATH."""
    return shutil.which("trans") is not None


def get_open_pr_for_branch(branch: str) -> Optional[int]:
    """Check if there's an open PR for the given branch."""
    cp = run(["gh", "pr", "list", "--head", branch, "--state", "open", "--json", "number"])
    if cp.returncode != 0:
        return None
    try:
        data = json.loads(cp.stdout or "[]")
        if data and isinstance(data, list):
            return data[0].get("number")
    except json.JSONDecodeError:
        pass
    return None


def branch_age_days(branch: str, cwd: Optional[Path] = None) -> Optional[int]:
    """Get the age of a branch in days by checking the latest commit date."""
    cp = run(["git", "log", "-1", "--format=%ci", branch], cwd=cwd)
    if cp.returncode != 0:
        return None
    try:
        commit_date = datetime.datetime.strptime(cp.stdout.strip(), "%Y-%m-%d %H:%M:%S %z")
        now = datetime.datetime.now(datetime.timezone.utc)
        return (now - commit_date.astimezone(datetime.timezone.utc)).days
    except (ValueError, TypeError):
        return None


def get_branch_head_commit(branch: str, cwd: Optional[Path] = None) -> Optional[str]:
    """Get the commit hash at the tip of a branch."""
    cp = run(["git", "rev-parse", branch], cwd=cwd)
    if cp.returncode != 0:
        return None
    return cp.stdout.strip()


def build_commit_message(
    locale: str,
    *,
    had_error: bool = False,
    error_message: str = "",
) -> str:
    """Assemble the commit message body (header + footer).

    Footer always carries the Automated and Co-Authored lines per
    ``.kilo/rules/co-author-attribution.md`` and ``corrections.md``
    (``github_coauthor_footer_required``); an optional WARNING line is
    appended when the translation pass errored mid-run.
    """
    commit_msg = f"chore(i18n): Nightly i18n refresh for {locale}"

    warning_line = ""
    if had_error:
        error_excerpt = error_message[:50] if error_message else "translation error"
        warning_line = f"\n\nWARNING: Translation ended with error: {error_excerpt}"

    footer = f"""> Automated via nightly-i18n-refresh cron
{CO_AUTHORED_FOOTER}{warning_line}"""

    return f"{commit_msg}\n\n{footer}"


def build_pr_body(locale: str, today: str, inserted: int, fixed: int) -> str:
    """Assemble the PR body markdown.

    The Co-Authored footer is required here per
    ``corrections.md`` (``agents_local_md_footer_all_github``) — ``gh pr
    create`` is one of the GitHub interactions covered by the footer rule.
    """
    return f"""## Nightly i18n refresh for `{locale}`
- Locale: `{locale}`
- Date: {today}
- TUVs inserted: {inserted}
- TUVs fixed (matching-en): {fixed}

Auto-generated by `scripts/nightly_i18n_refresh.py`. See `{LOG_FILE}` for the full run log.

{CO_AUTHORED_FOOTER}
"""


def is_branch_stale(age_days: Optional[int], threshold_days: int = 7) -> bool:
    """Return True if a branch whose tip is ``age_days`` old should be retired.

    ``None`` age (no parseable commit date) is treated as stale — the worst
    case (we'd rather delete and re-cut than carry a phantom branch).
    """
    if age_days is None:
        return True
    return age_days >= threshold_days


def has_unpushed_commits(branch: str, worktree: Path) -> bool:
    """Return True if ``branch`` has commits not present on ``origin/<branch>``.

    Used to guard against silently deleting a branch whose previous run
    committed TUVs but failed to push (e.g., GitHub 5xx, transient network).
    Without this check, the next nightly run sees no new diffs (everything
    is already in the last commit) and deletes the branch — taking the
    un-pushed commits to reflog-only territory and eventually garbage
    collection.
    """
    cp = run(["git", "rev-list", "--count", f"origin/{branch}..{branch}"], cwd=worktree)
    if cp.returncode != 0:
        # origin/<branch> may not exist yet (never pushed). Fall back to
        # checking against origin/main: any commit on this branch that
        # isn't reachable from origin/main is un-pushed.
        cp = run(
            ["git", "rev-list", "--count", f"origin/main..{branch}"],
            cwd=worktree,
        )
        if cp.returncode != 0:
            return False
    try:
        return int(cp.stdout.strip()) > 0
    except ValueError:
        return False


def select_locale(override: Optional[str], today: Optional[datetime.datetime] = None) -> str:
    """Select locale: use override or rotate via day_of_year % 16.

    ``today`` is injectable for unit tests; defaults to current UTC time at
    runtime.
    """
    if override:
        if override not in BASE_LOCALES:
            raise SystemExit(f"error: locale must be one of {BASE_LOCALES}")
        return override

    if today is None:
        today = datetime.datetime.now(datetime.timezone.utc)
    day_of_year = today.timetuple().tm_yday
    return BASE_LOCALES[day_of_year % 16]


def parse_translate_output(output: str) -> TranslationResult:
    """Parse i18n_translate_direct.py output to extract result counts."""
    result = TranslationResult()

    match = re.search(r"Missing:\s*(\d+)", output)
    if match:
        result.missing = int(match.group(1))

    match = re.search(r"inserted:\s*(\d+)", output)
    if match:
        result.inserted = int(match.group(1))

    match = re.search(r"fixed:\s*(\d+)", output)
    if match:
        result.fixed = int(match.group(1))

    match = re.search(r"skipped:\s*(\d+)", output)
    if match:
        result.skipped = int(match.group(1))

    # Match anchored error markers emitted by i18n_translate_direct.py,
    # not the loose substring "error"/"exception" anywhere in the log
    # (which would false-positive on benign text like "0 errors detected").
    had_error_match = re.search(
        r"(?im)^(?:ERROR on tuid=|error: |Exception\(|\[ERROR\])",
        output,
    )
    result.had_error = bool(had_error_match)

    return result


def run_translate(locale: str, dry_run: bool, worktree: Path, fix_matching: bool = False) -> TranslationResult:
    """Run the translation script and return parsed results.

    Streams stdout+stderr line-by-line to the rotating log file to avoid the
    pipe deadlock that ``subprocess.run(capture_output=True)`` causes on long
    translation passes (the child fills its stdout buffer while we wait for
    it to finish).

    Note: ``i18n_translate_direct.py`` does not have a ``--verbose`` flag;
    the wrapper's verbose flag is used only to enable Python ``logging``
    DEBUG output here, not to forward to the child.
    """
    translate_script_in_worktree = worktree / "modules" / "perc-i18n" / "scripts" / "i18n_translate_direct.py"
    args = [
        sys.executable,
        str(translate_script_in_worktree),
        "--target",
        locale,
    ]

    if fix_matching:
        args.append("--fix-matching-en")

    if dry_run:
        args.append("--dry-run")
        args.append("--limit")
        args.append("5")

    logger = logging.getLogger("nightly_i18n_refresh")
    logger.debug(f"Running: {' '.join(args)}")

    captured = {"stdout": ""}
    log_handle = open(LOG_FILE, "a", encoding="utf-8")
    proc = None
    try:
        proc = subprocess.Popen(
            args,
            cwd=worktree,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        assert proc.stdout is not None  # mypy: stdout=PIPE always sets this
        for line in proc.stdout:
            log_handle.write(line)
            log_handle.flush()
            captured["stdout"] += line
        proc.wait()
    except BaseException:
        if proc is not None:
            try:
                proc.kill()
                proc.wait()
            except OSError:
                pass
        raise
    finally:
        log_handle.close()

    output = captured["stdout"]
    result = parse_translate_output(output)

    if proc.returncode != 0 and not result.had_error:
        result.had_error = True
        result.error_message = f"Exit code {proc.returncode}"

    return result


def get_tmx_diff(worktree: Path) -> tuple[bool, int]:
    """Check if TMX files have changes. Returns (has_changes, file_count)."""
    cp = run(["git", "diff", "--name-only", "--", "*.tmx"], cwd=worktree)
    changed_files = [f for f in cp.stdout.strip().split("\n") if f]
    return len(changed_files) > 0, len(changed_files)


def get_cache_diff(worktree: Path) -> bool:
    """Check if cache file has changes."""
    cp = run(["git", "diff", "--name-only", "--", str(CACHE_FILE.relative_to(REPO_ROOT))], cwd=worktree)
    return cp.stdout.strip() != ""


def stage_and_commit(locale: str, result: TranslationResult, dry_run: bool, worktree: Path) -> bool:
    """Stage changed TMX + cache files and commit.

    Stages only TMX files that have a diff under
    ``modules/perc-i18n/src/main/resources/i18n/`` plus the cache file if
    it changed. Using diff-driven paths (not the full ``TMX_FILES``
    tuple) ensures we never ``git add`` a path that does not exist on
    the current branch.
    """
    logger = logging.getLogger("nightly_i18n_refresh")

    cp = run(
        ["git", "diff", "--name-only", "--", *[str(p) for p in TMX_DIR.glob("*.tmx")]],
        cwd=worktree,
    )
    if cp.returncode != 0:
        logger.error(f"Failed to enumerate changed TMX files: {cp.stderr}")
        return False
    files_to_stage = [f for f in cp.stdout.splitlines() if f]
    if get_cache_diff(worktree):
        files_to_stage.append(str(CACHE_FILE.relative_to(REPO_ROOT)))

    if not files_to_stage:
        logger.info("No files to commit")
        return False

    for f in files_to_stage:
        cp = run(["git", "add", f], cwd=worktree)
        if cp.returncode != 0:
            logger.error(f"Failed to stage {f}: {cp.stderr}")
            return False

    full_commit_msg = build_commit_message(
        locale,
        had_error=result.had_error,
        error_message=result.error_message,
    )
    commit_msg = f"chore(i18n): Nightly i18n refresh for {locale}"

    if dry_run:
        logger.info(f"[DRY-RUN] Would commit: {commit_msg}")
        return True

    cp = run(["git", "commit", "-m", full_commit_msg], cwd=worktree)
    if cp.returncode != 0:
        logger.error(f"Failed to commit: {cp.stderr}")
        return False

    logger.info(f"Committed: {commit_msg}")
    return True


def push_and_create_pr(locale: str, branch_name: str, result: TranslationResult, dry_run: bool, worktree: Path, retries: int = 3) -> bool:
    """Push branch and create PR. Returns True on success."""
    logger = logging.getLogger("nightly_i18n_refresh")

    if dry_run:
        logger.info(f"[DRY-RUN] Would push and create PR for {branch_name}")
        return True

    backoff = [10, 30, 60]

    for attempt in range(retries):
        cp = run(["git", "push", "-u", "origin", branch_name], cwd=worktree)
        if cp.returncode == 0:
            break
        if attempt < retries - 1:
            wait_time = backoff[attempt] if attempt < len(backoff) else 60
            logger.warning(f"Push failed, retrying in {wait_time}s: {cp.stderr}")
            time.sleep(wait_time)
        else:
            logger.error(f"Push failed after {retries} attempts: {cp.stderr}")
            return False

    today = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")

    pr_body = build_pr_body(locale, today, result.inserted, result.fixed)

    pr_body_file = worktree / "tmp" / "nightly-i18n-pr-body.md"
    pr_body_file.parent.mkdir(parents=True, exist_ok=True)
    pr_body_file.write_text(pr_body, encoding="utf-8")

    title = f"chore(i18n): Nightly i18n refresh for {locale}"

    gh_args = [
        "gh", "pr", "create",
        "--base", "main",
        "--head", branch_name,
        "--title", title,
        "--body-file", str(pr_body_file),
    ]
    for label in PR_LABELS:
        gh_args.extend(["--label", label])

    cp = run(gh_args)

    pr_body_file.unlink(missing_ok=True)

    if cp.returncode != 0:
        logger.error(f"Failed to create PR: {cp.stderr}")
        return False

    logger.info(f"Created PR: {cp.stdout.strip()}")
    return True


def delete_branch(branch_name: str, worktree: Path) -> bool:
    """Delete a local branch.

    If the worktree is currently checked out on ``branch_name``, switch to a
    detached HEAD at ``origin/main`` first so git allows the delete (git
    refuses to delete a branch that is checked out, and the primary checkout
    already holds ``main`` so we cannot check it out here).
    """
    logger = logging.getLogger("nightly_i18n_refresh")

    current_branch = run(["git", "branch", "--show-current"], cwd=worktree).stdout.strip()
    if current_branch == branch_name:
        detached = run(["git", "checkout", "origin/main"], cwd=worktree)
        if detached.returncode != 0:
            logger.warning(
                f"Cannot checkout origin/main before deleting {branch_name}: {detached.stderr}"
            )
            return False

    cp = run(["git", "branch", "-D", branch_name], cwd=worktree)
    if cp.returncode != 0:
        logger.warning(f"Failed to delete branch {branch_name}: {cp.stderr}")
        return False
    logger.info(f"Deleted branch: {branch_name}")
    return True


def acquire_lock(lock_file: Path) -> Optional[int]:
    """Acquire flock on lock file. Returns file descriptor or None on contention.

    ``flock`` is per-open-file-description across processes; opening the
    same file twice in the same process and locking each FD separately
    gives two independent locks, so a single in-process retry never blocks.
    That's the correct behavior for our use case (cron-launched siblings)
    and is covered by ``TestLockContention`` via subprocesses.
    """
    lock_file.parent.mkdir(parents=True, exist_ok=True)
    fd = os.open(str(lock_file), os.O_RDWR | os.O_CREAT, 0o644)
    try:
        fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        return fd
    except BlockingIOError:
        os.close(fd)
        return None


def release_lock(fd: int) -> None:
    """Release flock and close file descriptor."""
    try:
        fcntl.flock(fd, fcntl.LOCK_UN)
    except OSError:
        pass
    try:
        os.close(fd)
    except OSError:
        pass


def is_on_main_or_detached(worktree: Path) -> bool:
    """Return True if ``worktree`` is on ``main`` or detached HEAD at origin/main.

    Git refuses two worktrees on the same branch, so the dedicated nightly
    worktree operates in detached HEAD at origin/main. Either state passes
    this check.
    """
    branch = get_current_branch_worktree(worktree)
    if branch == "main":
        return True
    if not branch:
        head = run(["git", "rev-parse", "HEAD"], cwd=worktree).stdout.strip()
        origin_main = run(["git", "rev-parse", "origin/main"], cwd=worktree).stdout.strip()
        return head == origin_main
    return False


def run_preflight_checks(logger: logging.Logger, worktree: Path) -> bool:
    """Run all pre-flight checks. Returns True if all pass."""
    checks: list[tuple[str, "callable[[], bool]"]] = [
        ("trans on PATH", check_trans_available),
        (f"working tree clean ({worktree})", lambda: is_working_tree_clean_worktree(worktree)),
        (f"on main branch ({worktree})", lambda: is_on_main_or_detached(worktree)),
        ("gh authenticated", check_gh_auth),
    ]
    all_passed, _failures = _evaluate_preflight_checks(
        checks,
        log=lambda msg, *a, **kw: (
            logger.error(msg) if "failed" in msg or "error" in msg else logger.debug(msg)
        ),
    )
    return all_passed


def _evaluate_preflight_checks(
    checks: list[tuple[str, "callable[[], bool]"]],
    log: "callable[[str], Any]",
) -> tuple[bool, list[str]]:
    """Run ``checks`` and return (all_passed, [failed_check_names]).

    Pure helper, factored out so the dispatch / exception-handling logic is
    unit-testable without spinning up logging handlers.
    """
    all_passed = True
    failures: list[str] = []
    for name, check_fn in checks:
        try:
            ok = bool(check_fn())
        except Exception as e:
            log(f"Pre-flight error ({name}): {e}")
            ok = False
        if ok:
            log(f"Pre-flight passed: {name}")
        else:
            log(f"Pre-flight failed: {name}")
            all_passed = False
            failures.append(name)
    return all_passed, failures


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Nightly i18n translation refresh")
    parser.add_argument(
        "--locale",
        type=str,
        help="Override locale (default: day_of_year %% 16)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Skip git operations; pass --limit 5 to translation",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Enable DEBUG logging",
    )
    parser.add_argument(
        "--worktree",
        type=Path,
        default=DEFAULT_WORKTREE,
        help="Path to the dedicated worktree (default: ~/.kilo/worktrees/nightly-i18n-refresh)",
    )

    args = parser.parse_args(argv)

    logger = setup_logging(args.verbose)
    logger.info("Starting nightly i18n refresh")

    lock_fd = acquire_lock(LOCK_FILE)
    if lock_fd is None:
        logger.error("Could not acquire lock, another instance may be running")
        return 1

    try:
        worktree = args.worktree.resolve()
        logger.info(f"Using worktree: {worktree}")

        if not ensure_worktree(worktree, logger):
            return 1

        if not run_preflight_checks(logger, worktree):
            logger.error("Pre-flight checks failed, aborting")
            return 1

        root = worktree
        logger.debug(f"Worktree root: {root}")

        locale = select_locale(args.locale)
        logger.info(f"Selected locale: {locale}")

        run(["git", "fetch", "origin", "main"], cwd=worktree)

        branch_name = f"chore/nightly-i18n-refresh-{locale}"

        open_pr = get_open_pr_for_branch(branch_name)
        if open_pr:
            logger.info(f"Open PR #{open_pr} exists for {branch_name}, skipping")
            return 0

        local_branch_exists = get_branch_head_commit(branch_name, worktree) is not None
        if local_branch_exists:
            age = branch_age_days(branch_name, worktree)
            logger.debug(f"Branch {branch_name} exists, age: {age} days")
            if is_branch_stale(age, threshold_days=7):
                if has_unpushed_commits(branch_name, worktree):
                    logger.warning(
                        f"Branch {branch_name} is stale (>7 days) but has unpushed "
                        f"commits; keeping for manual recovery"
                    )
                else:
                    logger.info(f"Branch {branch_name} is stale (>7 days), deleting")
                    delete_branch(branch_name, worktree)
                    local_branch_exists = False

        if local_branch_exists:
            logger.info(f"Reusing existing branch: {branch_name}")
            run(["git", "checkout", branch_name], cwd=worktree)
        else:
            logger.info(f"Creating new branch: {branch_name}")
            run(["git", "checkout", "-b", branch_name, "origin/main"], cwd=worktree)

        logger.info(f"Running translation (target: {locale})")
        result1 = run_translate(locale, args.dry_run, worktree, fix_matching=False)
        logger.info(f"Translation 1 result: inserted={result1.inserted}, fixed={result1.fixed}")

        logger.info(f"Running fix-matching-en pass")
        result2 = run_translate(locale, args.dry_run, worktree, fix_matching=True)
        logger.info(f"Translation 2 result: inserted={result2.inserted}, fixed={result2.fixed}")

        combined_result = TranslationResult(
            inserted=result1.inserted + result2.inserted,
            fixed=result1.fixed + result2.fixed,
            had_error=result1.had_error or result2.had_error,
            error_message=result1.error_message or result2.error_message,
        )

        logger.info("Checking for changes to commit")
        has_tmx_changes, _ = get_tmx_diff(worktree)
        has_cache_changes = get_cache_diff(worktree)

        if not has_tmx_changes and not has_cache_changes:
            if has_unpushed_commits(branch_name, worktree):
                logger.warning(
                    f"No new diffs but branch {branch_name} has unpushed commits; "
                    f"keeping for manual recovery"
                )
                return 0
            logger.info("No changes to commit, deleting branch")
            delete_branch(branch_name, worktree)
            return 0

        if not stage_and_commit(locale, combined_result, args.dry_run, worktree):
            logger.error(
                "Failed to commit changes; leaving worktree intact for manual recovery"
            )
            return 1

        if not push_and_create_pr(locale, branch_name, combined_result, args.dry_run, worktree):
            logger.error("Failed to push and create PR")
            if not args.dry_run:
                logger.info("Branch left in place for recovery")
            return 1

        if not args.dry_run:
            run(["git", "checkout", "origin/main"], cwd=worktree)

        logger.info("Nightly i18n refresh completed successfully")
        return 0

    except Exception as e:
        logger.exception(f"Unexpected error: {e}")
        return 1
    finally:
        release_lock(lock_fd)


if __name__ == "__main__":
    sys.exit(main())
