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


if __name__ == "__main__":
    unittest.main()
