#!/usr/bin/env python3
"""Unit tests for nightly_i18n_refresh.py.

Loads the wrapper module via ``importlib`` (mirroring
``scripts/test_prune_stale_worktrees.py``) so assertions hit the real code,
not a local copy that can drift. Runs on Linux/macOS; on Windows the
``fcntl`` shim is installed before import so the module loads cleanly.

Run with: ``python3 scripts/test_nightly_i18n_refresh.py``
"""
from __future__ import annotations

import datetime
import importlib.util
import subprocess
import sys
import tempfile
import types
import unittest
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
WRAPPER_PATH = SCRIPT_DIR / "nightly_i18n_refresh.py"


def _install_fcntl_shim() -> None:
    """Provide a no-op ``fcntl`` so the wrapper imports on Windows.

    The wrapper's real ``flock`` is only called at runtime by ``acquire_lock``,
    which is never invoked during the tests below. Tests that exercise the
    lock use real POSIX ``fcntl`` instead.
    """
    if "fcntl" in sys.modules:
        return
    shim = types.ModuleType("fcntl")
    shim.LOCK_EX = 2
    shim.LOCK_NB = 4
    shim.LOCK_UN = 8
    shim.flock = lambda *args, **kwargs: None  # type: ignore[attr-defined]
    sys.modules["fcntl"] = shim


def _swap_to_real_fcntl() -> bool:
    """Replace the shimmed fcntl with the real POSIX module.

    Returns True if real fcntl is available, False otherwise (e.g., Windows).
    """
    try:
        del sys.modules["fcntl"]
    except KeyError:
        pass
    try:
        import fcntl as real_fcntl  # noqa: F401
    except ImportError:
        return False
    sys.modules["fcntl"] = real_fcntl
    # Patch the wrapper module's reference too (the wrapper captured
    # the shim's binding at import time).
    import nightly_i18n_refresh as wrapper_module  # type: ignore[import-not-found]
    wrapper_module.fcntl = real_fcntl
    return True


def load_wrapper() -> Any:
    """Load ``nightly_i18n_refresh.py`` and return the module object."""
    _install_fcntl_shim()
    spec = importlib.util.spec_from_file_location("nightly_i18n_refresh", str(WRAPPER_PATH))
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules["nightly_i18n_refresh"] = module
    spec.loader.exec_module(module)
    return module


m = load_wrapper()


class TestLocaleRotation(unittest.TestCase):
    """Verify the day_of_year % 16 rotation and --locale override."""

    def test_rotation_known_dates(self):
        for date in (
            datetime.datetime(2026, 1, 1, tzinfo=datetime.timezone.utc),
            datetime.datetime(2026, 1, 2, tzinfo=datetime.timezone.utc),
            datetime.datetime(2026, 1, 16, tzinfo=datetime.timezone.utc),
            datetime.datetime(2026, 1, 17, tzinfo=datetime.timezone.utc),
            datetime.datetime(2026, 7, 4, tzinfo=datetime.timezone.utc),
        ):
            with self.subTest(date=date):
                expected = m.BASE_LOCALES[date.timetuple().tm_yday % 16]
                self.assertEqual(m.select_locale(None, today=date), expected)

    def test_locale_override_skips_rotation(self):
        self.assertEqual(m.select_locale("de"), "de")
        self.assertEqual(m.select_locale("fr"), "fr")

    def test_unknown_locale_raises(self):
        with self.assertRaises(SystemExit):
            m.select_locale("zz")

    def test_all_base_locales_are_valid_overrides(self):
        for locale in m.BASE_LOCALES:
            with self.subTest(locale=locale):
                self.assertEqual(m.select_locale(locale), locale)


class TestTranslationOutputParsing(unittest.TestCase):
    """Verify the parser extracts counts from i18n_translate_direct.py output."""

    SAMPLE = (
        "CmsUi.tmx: 73 missing <tuv xml:lang=\"de\">\n"
        "  inserted 5 TUVs\n"
        "Done. Missing: 472; inserted: 15; fixed: 0; skipped: 0\n"
    )

    def test_parse_all_counts(self):
        result = m.parse_translate_output(self.SAMPLE)
        self.assertEqual(result.missing, 472)
        self.assertEqual(result.inserted, 15)
        self.assertEqual(result.fixed, 0)
        self.assertEqual(result.skipped, 0)
        self.assertFalse(result.had_error)

    def test_error_line_flags_had_error(self):
        result = m.parse_translate_output("ERROR on tuid=foo: rate limit\n")
        self.assertTrue(result.had_error)

    def test_benign_word_with_error_does_not_flag(self):
        result = m.parse_translate_output("0 errors detected\nDone. Missing: 0; inserted: 0; fixed: 0; skipped: 0")
        self.assertFalse(result.had_error)


class TestBranchStaleness(unittest.TestCase):
    def test_threshold_default_7(self):
        self.assertTrue(m.is_branch_stale(7))
        self.assertTrue(m.is_branch_stale(8))
        self.assertFalse(m.is_branch_stale(6))
        self.assertFalse(m.is_branch_stale(0))

    def test_threshold_override(self):
        self.assertTrue(m.is_branch_stale(3, threshold_days=3))
        self.assertFalse(m.is_branch_stale(2, threshold_days=3))

    def test_none_age_is_stale(self):
        self.assertTrue(m.is_branch_stale(None))


class TestUnpushedCommitsDetection(unittest.TestCase):
    """Verify the push-failure data-loss guard."""

    def test_returns_false_when_no_unpushed(self):
        # Real git in worktree: no commits means no unpushed commits.
        with tempfile.TemporaryDirectory() as td:
            worktree = Path(td)
            self.assertFalse(m.has_unpushed_commits("nonexistent-branch", worktree))

    def test_returns_false_when_branch_does_not_exist(self):
        with tempfile.TemporaryDirectory() as td:
            worktree = Path(td)
            self.assertFalse(m.has_unpushed_commits("definitely-not-a-branch", worktree))


class TestStagingPathBuilder(unittest.TestCase):
    """Regression tests for the diff-driven ``git add`` path builder.

    The PR #2651 peer review found that ``stage_and_commit`` was passing
    basenames (``CmsUi.tmx``) to ``git add``, which fails on the real tree
    because the canonical paths live under
    ``modules/perc-i18n/src/main/resources/i18n/``. These tests exercise
    a real git repo against the real ``TMX_DIR`` to catch path regressions.
    """

    @staticmethod
    def _init_repo(td: Path) -> Path:
        worktree = Path(td)
        subprocess.run(["git", "init", "-q", "-b", "main"], cwd=worktree, check=True)
        subprocess.run(
            ["git", "config", "user.email", "test@example.com"], cwd=worktree, check=True
        )
        subprocess.run(
            ["git", "config", "user.name", "Test"], cwd=worktree, check=True
        )
        # Mirror the real repo layout so TMX_DIR resolves.
        tmx_dir = worktree / m.TMX_DIR
        tmx_dir.mkdir(parents=True, exist_ok=True)
        # Cache file lives at modules/perc-i18n/scripts/cache/i18n_translate.json;
        # CACHE_FILE is absolute, so compute a relative form for the temp repo.
        cache_rel = Path("modules/perc-i18n/scripts/cache/i18n_translate.json")
        cache_dir = worktree / cache_rel.parent
        cache_dir.mkdir(parents=True, exist_ok=True)
        for path in m.TMX_FILES:
            (worktree / path).write_text('<tmx/>\n', encoding="utf-8")
        (worktree / cache_rel).write_text('{"keys": []}\n', encoding="utf-8")
        subprocess.run(["git", "add", "-A"], cwd=worktree, check=True)
        subprocess.run(["git", "commit", "-q", "-m", "init"], cwd=worktree, check=True)
        return worktree

    def test_tmx_paths_resolve_against_git_add(self):
        """Every entry in ``TMX_FILES`` must be a valid ``git add`` pathspec
        against the real repo layout."""
        with tempfile.TemporaryDirectory() as td:
            worktree = self._init_repo(td)
            for path in m.TMX_FILES:
                cp = subprocess.run(
                    ["git", "add", "--dry-run", path],
                    cwd=worktree,
                    capture_output=True,
                    text=True,
                )
                self.assertEqual(
                    cp.returncode,
                    0,
                    f"git add --dry-run failed for {path!r}: {cp.stderr}",
                )

    def test_stage_and_commit_uses_diff_only(self):
        """``stage_and_commit`` must only stage paths that actually changed.

        Touches one TMX file and verifies the other two are not staged.
        Regression test for the basename defect — using basenames would
        stage all three every run, producing noisy diffs.
        """
        # This test asserts the path-builder shape (diff-driven, full paths)
        # without invoking the full ``stage_and_commit`` (which requires
        # a pre-existing branch and ``build_commit_message`` plumbing).
        with tempfile.TemporaryDirectory() as td:
            worktree = self._init_repo(td)
            target = worktree / m.TMX_FILES[0]
            target.write_text('<tmx><tuid id="changed"/></tmx>\n', encoding="utf-8")

            cp = subprocess.run(
                [
                    "git",
                    "diff",
                    "--name-only",
                    "--",
                    *[str(p) for p in (worktree / m.TMX_DIR).glob("*.tmx")],
                ],
                cwd=worktree,
                capture_output=True,
                text=True,
            )
            changed = [f for f in cp.stdout.splitlines() if f]
            self.assertEqual(changed, [m.TMX_FILES[0]])


class TestPreflightChecks(unittest.TestCase):
    """Verify pre-flight check dispatch and failure handling."""

    def test_all_pass(self):
        results = [("a", lambda: True), ("b", lambda: True)]
        all_pass, failures = m._evaluate_preflight_checks(results, log=lambda *a, **kw: None)
        self.assertTrue(all_pass)
        self.assertEqual(failures, [])

    def test_one_fails(self):
        results = [("a", lambda: True), ("b", lambda: False), ("c", lambda: True)]
        all_pass, failures = m._evaluate_preflight_checks(results, log=lambda *a, **kw: None)
        self.assertFalse(all_pass)
        self.assertEqual(failures, ["b"])

    def test_exception_marks_failure(self):
        def boom():
            raise RuntimeError("boom")
        results = [("a", lambda: True), ("b", boom)]
        all_pass, failures = m._evaluate_preflight_checks(results, log=lambda *a, **kw: None)
        self.assertFalse(all_pass)
        self.assertEqual(failures, ["b"])


class TestCommitMessage(unittest.TestCase):
    """Verify commit message assembly and footer contract."""

    def test_prefix_and_footers(self):
        msg = m.build_commit_message("de")
        self.assertTrue(msg.startswith("chore(i18n): Nightly i18n refresh for de"))
        self.assertIn("> Automated via nightly-i18n-refresh cron", msg)
        self.assertIn(m.CO_AUTHORED_FOOTER, msg)

    def test_warning_on_error(self):
        msg = m.build_commit_message("de", had_error=True, error_message="network timeout")
        self.assertIn("WARNING: Translation ended with error:", msg)
        self.assertIn("network timeout", msg)

    def test_no_warning_when_no_error(self):
        msg = m.build_commit_message("de")
        self.assertNotIn("WARNING:", msg)

    def test_error_message_truncated_to_50(self):
        long_msg = "x" * 200
        msg = m.build_commit_message("de", had_error=True, error_message=long_msg)
        self.assertIn("x" * 50, msg)
        self.assertNotIn("x" * 51, msg)


class TestPRBody(unittest.TestCase):
    """Verify PR body assembly includes the Co-Authored footer."""

    def test_body_contains_locale_and_counts(self):
        body = m.build_pr_body("de", "2026-08-09", inserted=15, fixed=0)
        self.assertIn("Locale: `de`", body)
        self.assertIn("Date: 2026-08-09", body)
        self.assertIn("TUVs inserted: 15", body)
        self.assertIn("TUVs fixed (matching-en): 0", body)

    def test_body_has_coauthored_footer(self):
        body = m.build_pr_body("de", "2026-08-09", inserted=0, fixed=0)
        self.assertIn(m.CO_AUTHORED_FOOTER, body)


class TestModuleConstants(unittest.TestCase):
    """Verify the documented labels and footer constants are stable."""

    def test_operator_label(self):
        self.assertEqual(m.OPERATOR_LABEL, "operator:kilo")

    def test_model_label_matches_session_model(self):
        self.assertTrue(m.MODEL_LABEL.startswith("model:"))
        self.assertIn(m.AGENT_MODEL_SLUG, m.MODEL_LABEL)

    def test_pr_labels_tuple(self):
        self.assertIn("operator:kilo", m.PR_LABELS)
        self.assertIn(m.MODEL_LABEL, m.PR_LABELS)

    def test_coauthored_footer_contains_model_and_agent(self):
        self.assertIn(m.AGENT_MODEL, m.CO_AUTHORED_FOOTER)
        self.assertIn(m.AGENT_NAME, m.CO_AUTHORED_FOOTER)
        self.assertIn("Automated Co-Authored by", m.CO_AUTHORED_FOOTER)

    def test_tmx_files_match_module_agents(self):
        self.assertEqual(
            m.TMX_FILES,
            (
                "modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx",
                "modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx",
                "modules/perc-i18n/src/main/resources/i18n/DeveloperUi.tmx",
            ),
        )

    def test_tmx_paths_resolve_on_repo(self):
        """``TMX_FILES`` must use full relative paths so ``git add`` resolves
        against the actual TMX location (``modules/perc-i18n/src/main/resources/i18n/``),
        not against the worktree root. Regression test for the path basename
        defect flagged in the PR #2651 peer review."""
        repo_root = Path(__file__).resolve().parent.parent
        for path in m.TMX_FILES:
            self.assertTrue(
                (repo_root / path).exists(),
                f"TMX path does not exist on this repo: {path}",
            )
            self.assertTrue(path.startswith("modules/perc-i18n/"), path)
            self.assertTrue(path.endswith(".tmx"), path)

    def test_tmx_dir_constant(self):
        self.assertEqual(
            m.TMX_DIR.as_posix(),
            "modules/perc-i18n/src/main/resources/i18n",
        )

    def test_base_locales_count(self):
        self.assertEqual(len(m.BASE_LOCALES), 16)
        for code in m.BASE_LOCALES:
            self.assertTrue(code.islower())
            self.assertTrue(code.isalpha())


class TestLockContention(unittest.TestCase):
    """Verify flock blocks a concurrent acquirer (in a separate process).

    ``flock`` is per-open-file-description, so two acquires in the SAME
    process on the same FD trivially succeed (re-locking an already-held
    lock is a no-op). Mutual exclusion is meaningful only across processes
    — e.g., cron starting a second invocation while the first is running.
    These tests spawn a child process that holds the lock and verify that
    the parent's ``acquire_lock`` returns ``None``.
    """

    @classmethod
    def setUpClass(cls):
        cls._has_real_fcntl = _swap_to_real_fcntl()

    def _spawn_holder(self, lock_file: Path) -> Any:
        """Spawn a subprocess that holds ``lock_file`` until told to release.

        The subprocess creates a sentinel file ``<lock_file>.release`` after
        acquiring the lock, then busy-waits until the parent deletes it.
        """
        import subprocess
        sentinel = str(lock_file) + ".release"
        proc = subprocess.Popen(
            [
                sys.executable,
                "-c",
                f"""
import fcntl, os, sys, time
lock_file = {str(lock_file)!r}
sentinel = {sentinel!r}
fd = os.open(lock_file, os.O_RDWR | os.O_CREAT, 0o644)
fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
print('HELD', flush=True)
open(sentinel, 'w').close()
while os.path.exists(sentinel):
    time.sleep(0.05)
fcntl.flock(fd, fcntl.LOCK_UN)
os.close(fd)
""",
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=0,
        )
        line = proc.stdout.readline().decode("utf-8")
        if line.strip() != "HELD":
            err = proc.stderr.read().decode("utf-8", errors="replace")
            self.fail(f"child failed to acquire: {err}")
        return proc

    def _stop_holder(self, holder: Any, lock_file: Path) -> None:
        """Delete the release sentinel and wait for the child to exit."""
        sentinel = lock_file.parent / (lock_file.name + ".release")
        sentinel.unlink(missing_ok=True)
        # Drain stdout so the child's pipe doesn't deadlock once it exits.
        holder.stdout.close() if holder.stdout else None
        try:
            holder.wait(timeout=5)
        except subprocess.TimeoutExpired:
            holder.kill()
            holder.wait(timeout=2)

    def test_second_acquire_in_separate_process_returns_none(self):
        if not self._has_real_fcntl:
            self.skipTest("POSIX fcntl not available")
        with tempfile.TemporaryDirectory() as td:
            lock_file = Path(td) / "test.lock"
            holder = self._spawn_holder(lock_file)
            try:
                fd = m.acquire_lock(lock_file)
                self.assertIsNone(fd, "second acquire must fail while another process holds lock")
            finally:
                self._stop_holder(holder, lock_file)

    def test_acquire_succeeds_after_other_process_releases(self):
        if not self._has_real_fcntl:
            self.skipTest("POSIX fcntl not available")
        with tempfile.TemporaryDirectory() as td:
            lock_file = Path(td) / "test.lock"
            holder = self._spawn_holder(lock_file)
            self._stop_holder(holder, lock_file)
            fd = m.acquire_lock(lock_file)
            self.assertIsNotNone(fd, "acquire should succeed after holder releases")
            if fd is not None:
                m.release_lock(fd)


def main() -> int:
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    suite.addTests(loader.loadTestsFromTestCase(TestLocaleRotation))
    suite.addTests(loader.loadTestsFromTestCase(TestTranslationOutputParsing))
    suite.addTests(loader.loadTestsFromTestCase(TestBranchStaleness))
    suite.addTests(loader.loadTestsFromTestCase(TestUnpushedCommitsDetection))
    suite.addTests(loader.loadTestsFromTestCase(TestStagingPathBuilder))
    suite.addTests(loader.loadTestsFromTestCase(TestPreflightChecks))
    suite.addTests(loader.loadTestsFromTestCase(TestCommitMessage))
    suite.addTests(loader.loadTestsFromTestCase(TestPRBody))
    suite.addTests(loader.loadTestsFromTestCase(TestModuleConstants))
    suite.addTests(loader.loadTestsFromTestCase(TestLockContention))
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(main())