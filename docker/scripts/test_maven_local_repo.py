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
#
# See the License for the specific language governing permissions and
# limitations under the License.
"""Unit tests for maven_local_repo.py (#6709)."""

from __future__ import annotations

import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

SCRIPTS = Path(__file__).resolve().parent
if str(SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SCRIPTS))

import maven_local_repo as mlr  # noqa: E402

_POSIX = os.name != "nt"


class MavenLocalRepoTest(unittest.TestCase):
    def setUp(self) -> None:
        self._td = tempfile.TemporaryDirectory()
        self.home = Path(self._td.name) / "home"
        self.repo = Path(self._td.name) / "checkout"
        self.home.mkdir()
        self.repo.mkdir()
        patcher = mock.patch.dict(
            os.environ, {"HOME": str(self.home), "USERPROFILE": str(self.home)}
        )
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(self._td.cleanup)

    def _env(self, **extra: str) -> dict:
        env = {k: v for k, v in os.environ.items() if k not in ("MAVEN_ARGS", "MAVEN_OPTS", "MAVEN_SKIP_RC")}
        env.update(extra)
        return env

    def test_parse_repo_local_last_wins_and_quotes(self):
        self.assertIsNone(mlr.parse_repo_local(None))
        self.assertIsNone(mlr.parse_repo_local("-B -ntp"))
        self.assertEqual(
            mlr.parse_repo_local("-Dmaven.repo.local=/a -B -Dmaven.repo.local=/b"), Path("/b")
        )
        self.assertEqual(
            mlr.parse_repo_local('-Dmaven.repo.local="/with space/repo"'), Path("/with space/repo")
        )

    def test_default_is_home_m2_repository(self):
        resolved = mlr.resolve_m2_repository(self.repo, self._env(MAVEN_SKIP_RC="1"))
        self.assertEqual(resolved, self.home / ".m2" / "repository")

    def test_maven_args_wins(self):
        env = self._env(MAVEN_ARGS="-B -Dmaven.repo.local=/night/repo", MAVEN_OPTS="-Dmaven.repo.local=/opts")
        self.assertEqual(mlr.resolve_m2_repository(self.repo, env), Path("/night/repo"))

    def test_maven_opts_used_when_args_absent(self):
        env = self._env(MAVEN_OPTS="-Xmx1g -Dmaven.repo.local=/opts/repo")
        self.assertEqual(mlr.resolve_m2_repository(self.repo, env), Path("/opts/repo"))

    @unittest.skipUnless(_POSIX, "mavenrc is sourced by the POSIX mvn launcher only")
    def test_mavenrc_is_sourced_with_checkout_as_cwd(self):
        (self.home / ".mavenrc").write_text(
            'case "$(pwd -P)" in\n'
            f'  "{self.repo.resolve()}") MAVEN_ARGS="-Dmaven.repo.local=/from/mavenrc" ;;\n'
            "esac\n",
            encoding="utf-8",
        )
        self.assertEqual(
            mlr.resolve_m2_repository(self.repo, self._env()), Path("/from/mavenrc")
        )
        other = Path(self._td.name) / "other"
        other.mkdir()
        self.assertEqual(
            mlr.resolve_m2_repository(other, self._env()), self.home / ".m2" / "repository"
        )

    @unittest.skipUnless(_POSIX, "mavenrc is sourced by the POSIX mvn launcher only")
    def test_maven_skip_rc_ignores_mavenrc(self):
        (self.home / ".mavenrc").write_text(
            'MAVEN_ARGS="-Dmaven.repo.local=/from/mavenrc"\n', encoding="utf-8"
        )
        self.assertEqual(
            mlr.resolve_m2_repository(self.repo, self._env(MAVEN_SKIP_RC="1")),
            self.home / ".m2" / "repository",
        )

    def test_maven_config(self):
        (self.repo / ".mvn").mkdir()
        (self.repo / ".mvn" / "maven.config").write_text(
            "-ntp\n-Dmaven.repo.local=/cfg/repo\n", encoding="utf-8"
        )
        self.assertEqual(
            mlr.resolve_m2_repository(self.repo, self._env(MAVEN_SKIP_RC="1")), Path("/cfg/repo")
        )

    def test_settings_local_repository(self):
        (self.home / ".m2").mkdir()
        (self.home / ".m2" / "settings.xml").write_text(
            '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">'
            "<localRepository>${user.home}/custom-repo</localRepository></settings>",
            encoding="utf-8",
        )
        self.assertEqual(
            mlr.resolve_m2_repository(self.repo, self._env(MAVEN_SKIP_RC="1")),
            self.home / "custom-repo",
        )


class TestWorktreeRepository(unittest.TestCase):
    """Per-worktree local repositories enforce isolation (#6739)."""

    def setUp(self) -> None:
        self._td = tempfile.TemporaryDirectory()
        self.addCleanup(self._td.cleanup)
        self.home = Path(self._td.name) / "home"
        self.repo = Path(self._td.name) / "checkout"
        self.home.mkdir()
        self.repo.mkdir()
        patcher = mock.patch.dict(
            os.environ, {"HOME": str(self.home), "USERPROFILE": str(self.home)}
        )
        patcher.start()
        self.addCleanup(patcher.stop)

    def _env(self, **extra: str) -> dict:
        env = {k: v for k, v in os.environ.items() if k not in ("MAVEN_ARGS", "MAVEN_OPTS", "MAVEN_SKIP_RC")}
        env.update(extra)
        return env

    def _seed_repo(self, seed: Path) -> None:
        third_party = seed / "com" / "example" / "lib" / "1.0"
        third_party.mkdir(parents=True)
        (third_party / "lib-1.0.jar").write_bytes(b"third-party-bytes")
        snap = seed / "com" / "percussion" / "rest" / "rest" / "8.2.0-SNAPSHOT"
        snap.mkdir(parents=True)
        (snap / "rest-8.2.0-SNAPSHOT.jar").write_bytes(b"branch-built-snapshot")
        flat = seed / "com" / "percussion" / "sitemanage" / "8.2.0-SNAPSHOT"
        flat.mkdir(parents=True)
        (flat / "sitemanage-8.2.0-SNAPSHOT.jar").write_bytes(b"branch-built-snapshot")

    def test_worktree_repo_lives_under_repo_tmp(self):
        repo = mlr.worktree_local_repository(self.repo)
        self.assertEqual(
            repo, self.repo.resolve() / "tmp" / "m2-worktree" / "repository"
        )

    def test_distinct_per_worktree(self):
        other = Path(self._td.name) / "other-checkout"
        other.mkdir()
        self.assertNotEqual(
            mlr.worktree_local_repository(self.repo),
            mlr.worktree_local_repository(other),
        )

    def test_repo_local_flag_format(self):
        # Built from the same Path so the expectation holds on Windows
        # (backslash separators) as well as POSIX.
        probe = Path("/w/tmp/m2-worktree/repository")
        self.assertEqual(
            mlr.repo_local_flag(probe),
            f"-Dmaven.repo.local={probe}",
        )

    def test_repo_local_flag_quotes_whitespace(self):
        # MAVEN_ARGS is whitespace-split: paths with spaces must be quoted,
        # and parse_repo_local must round-trip the quoted form.
        probe = Path("/w/tmp/my checkout/m2-worktree/repository")
        flag = mlr.repo_local_flag(probe)
        self.assertEqual(flag, f'-Dmaven.repo.local="{probe}"')
        self.assertEqual(mlr.parse_repo_local(flag), probe)

    def test_ensure_seeds_third_party_only(self):
        seed = Path(self._td.name) / "seed-m2"
        seed.mkdir()
        self._seed_repo(seed)
        repo = mlr.ensure_worktree_repository(self.repo, seed)
        self.assertEqual(repo, mlr.worktree_local_repository(self.repo))
        seeded_jar = repo / "com" / "example" / "lib" / "1.0" / "lib-1.0.jar"
        self.assertEqual(seeded_jar.read_bytes(), b"third-party-bytes")
        leftovers = [
            p for p in repo.rglob("*") if p.is_file() and "SNAPSHOT" in p.name
        ]
        self.assertEqual(leftovers, [])
        marker = repo.parent / mlr._WORKTREE_SEED_MARKER
        self.assertEqual(marker.read_text(encoding="utf-8").strip(), str(seed))

    def test_ensure_is_idempotent(self):
        seed = Path(self._td.name) / "seed-m2"
        seed.mkdir()
        self._seed_repo(seed)
        first = mlr.ensure_worktree_repository(self.repo, seed)
        marker = first.parent / mlr._WORKTREE_SEED_MARKER
        marker_mtime = marker.stat().st_mtime_ns
        second = mlr.ensure_worktree_repository(self.repo, seed)
        self.assertEqual(first, second)
        self.assertEqual(marker.stat().st_mtime_ns, marker_mtime)

    def test_isolation_proof_two_worktrees_resolve_distinct(self):
        """MAVEN_ARGS pointing at each worktree repo resolves back to it."""
        other = Path(self._td.name) / "other-checkout"
        other.mkdir()
        repo_a = mlr.worktree_local_repository(self.repo)
        repo_b = mlr.worktree_local_repository(other)
        for repo in (repo_a, repo_b):
            env = self._env(
                MAVEN_ARGS=mlr.repo_local_flag(repo), MAVEN_SKIP_RC="1"
            )
            self.assertEqual(mlr.resolve_m2_repository(self.repo, env), repo)
        self.assertNotEqual(repo_a, repo_b)

    def test_cli_prints_maven_args_export(self):
        import io
        from contextlib import redirect_stdout

        buf = io.StringIO()
        with redirect_stdout(buf):
            rc = mlr.main(
                ["--repo-root", str(self.repo), "--no-seed"]
            )
        self.assertEqual(rc, 0)
        self.assertIn("-Dmaven.repo.local=", buf.getvalue())
        self.assertIn("MAVEN_ARGS=", buf.getvalue())
        self.assertTrue(mlr.worktree_local_repository(self.repo).is_dir())


if __name__ == "__main__":
    unittest.main()
