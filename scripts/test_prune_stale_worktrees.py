#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Unit tests for prune-stale-worktrees.py (no network, no git)."""

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent


def _load():
    path = SCRIPTS / "prune-stale-worktrees.py"
    name = "prune_stale_worktrees"
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


m = _load()


class TestParsePorcelain(unittest.TestCase):
    def test_parse_worktree_porcelain_basic(self) -> None:
        text = """\
worktree /repo
HEAD abcdef
branch refs/heads/main

worktree /repo/.kilo/worktrees/foo
HEAD 123456
branch refs/heads/feature/foo

worktree /repo/detached-wt
HEAD deadbeef
detached
"""
        rows = m.parse_worktree_porcelain(text)
        self.assertEqual(len(rows), 3)
        self.assertEqual(rows[0].path, Path("/repo"))
        self.assertEqual(rows[0].branch, "main")
        self.assertEqual(rows[1].branch, "feature/foo")
        self.assertIsNone(rows[2].branch)


class TestDecide(unittest.TestCase):
    def test_decide_keeps_main_and_open_pr(self) -> None:
        main = m.Worktree(path=Path("/repo"), branch="main", is_main=True)
        open_wt = m.Worktree(
            path=Path("/repo/wt-open"),
            branch="feature/x",
            pr_states=["OPEN"],
            pr_numbers=[99],
        )
        self.assertEqual(
            m.decide(
                main,
                include_no_pr=False,
                include_open=False,
                require_force_for_dirty=True,
                force=False,
            ).action,
            "keep",
        )
        d = m.decide(
            open_wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "keep")
        self.assertIn("open PR", d.reason)

    def test_decide_removes_merged_clean(self) -> None:
        wt = m.Worktree(
            path=Path("/repo/wt-merged"),
            branch="fix/done",
            pr_states=["MERGED"],
            pr_numbers=[42],
            dirty=False,
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "remove")
        self.assertIn("MERGED", d.reason)

    def test_decide_dirty_merged_requires_force(self) -> None:
        wt = m.Worktree(
            path=Path("/repo/wt-dirty"),
            branch="fix/done",
            pr_states=["MERGED"],
            pr_numbers=[42],
            dirty=True,
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "keep")
        self.assertIn("--force", d.reason)
        d2 = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=True,
        )
        self.assertEqual(d2.action, "remove")

    def test_decide_no_pr_needs_flag(self) -> None:
        wt = m.Worktree(
            path=Path("/repo/wt-orphan"), branch="experiment/x", dirty=False
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "keep")
        d2 = m.decide(
            wt,
            include_no_pr=True,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d2.action, "remove")

    def test_decide_keeps_cwd(self) -> None:
        wt = m.Worktree(
            path=Path("/repo/wt-cwd"),
            branch="fix/done",
            pr_states=["MERGED"],
            pr_numbers=[1],
            is_cwd=True,
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=True,
        )
        self.assertEqual(d.action, "keep")
        self.assertIn("current working directory", d.reason)

    def test_decide_missing_path_merged_is_removed(self) -> None:
        """A worktree directory deleted out-of-band (rm -rf) whose PR is
        MERGED must be removed (metadata prune), not kept."""
        wt = m.Worktree(
            path=Path("/repo/wt-gone"),
            branch="fix/done",
            pr_states=["MERGED"],
            pr_numbers=[42],
            dirty=False,
            path_exists=False,
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "remove")
        self.assertIn("missing", d.reason)

    def test_decide_missing_path_open_pr_is_kept(self) -> None:
        """A missing path with an OPEN PR needs human review before pruning."""
        wt = m.Worktree(
            path=Path("/repo/wt-gone"),
            branch="fix/wip",
            pr_states=["OPEN"],
            pr_numbers=[7],
            path_exists=False,
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=True,
        )
        self.assertEqual(d.action, "keep")

    def test_decide_missing_path_no_pr_needs_flag(self) -> None:
        wt = m.Worktree(
            path=Path("/repo/wt-gone"), branch="experiment/x", path_exists=False
        )
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "keep")
        d2 = m.decide(
            wt,
            include_no_pr=True,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d2.action, "remove")
        self.assertIn("missing", d2.reason)

    def test_decide_missing_path_detached_is_removed(self) -> None:
        wt = m.Worktree(path=Path("/repo/wt-gone"), branch=None, path_exists=False)
        d = m.decide(
            wt,
            include_no_pr=False,
            include_open=False,
            require_force_for_dirty=True,
            force=False,
        )
        self.assertEqual(d.action, "remove")

    def test_list_worktrees_detects_missing_path(self) -> None:
        """list_worktrees must flag entries whose directory was rm -rf'd."""
        import subprocess
        import tempfile

        with tempfile.TemporaryDirectory() as tmp:
            seed = Path(tmp) / "seed"
            subprocess.run(
                ["git", "init", "-b", "main", str(seed)],
                check=True, capture_output=True, timeout=30,
            )
            subprocess.run(
                ["git", "-C", str(seed), "config", "user.email", "ci@example.com"],
                check=True, capture_output=True, timeout=10,
            )
            subprocess.run(
                ["git", "-C", str(seed), "config", "user.name", "ci"],
                check=True, capture_output=True, timeout=10,
            )
            (seed / "README.md").write_text("hi")
            subprocess.run(
                ["git", "-C", str(seed), "add", "README.md"],
                check=True, capture_output=True, timeout=10,
            )
            subprocess.run(
                ["git", "-C", str(seed), "commit", "-m", "init"],
                check=True, capture_output=True, timeout=30,
            )
            wt_path = Path(tmp) / "wt-gone"
            subprocess.run(
                ["git", "-C", str(seed), "worktree", "add", str(wt_path), "-b", "fix/gone"],
                check=True, capture_output=True, timeout=60,
            )
            # Simulate the out-of-band deletion: rm -rf, never git worktree remove.
            import shutil

            shutil.rmtree(wt_path)
            rows = m.list_worktrees(seed)
            gone = [r for r in rows if r.branch == "fix/gone"]
            self.assertEqual(len(gone), 1)
            self.assertFalse(gone[0].path_exists)


if __name__ == "__main__":
    unittest.main()
