#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

See the License for the specific language governing permissions and
limitations under the License.

Unit tests for scripts/sanitize-erlang-review.py (stdlib unittest).
"""
from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent


def _load():
    path = SCRIPT_DIR / "sanitize-erlang-review.py"
    spec = importlib.util.spec_from_file_location("sanitize_erlang_review", path)
    mod = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(mod)
    return mod


sanitize_erlang_review = _load()


class SanitizeTests(unittest.TestCase):
    def test_persona_source_home_redacted(self) -> None:
        out = sanitize_erlang_review.sanitize_text(
            # mkd-code-review[paths.hardcoded_sep]: redactor fixture data;
            # foreign text path, not a filesystem join.
            "- Persona source: /home/nate/.local/share/mkd/agents/erlang\n"
        )
        # mkd-code-review[paths.hardcoded_sep]: fixture data for the redactor;
        # foreign text path, not a filesystem join.
        self.assertNotIn("/home/nate", out)
        self.assertIn("~/.local/share/mkd/agents/erlang", out)

    def test_models_absolute_path_collapsed(self) -> None:
        out = sanitize_erlang_review.sanitize_text(
            # mkd-code-review[paths.hardcoded_sep]: redactor fixture data;
            # foreign text path, not a filesystem join.
            "--models /home/nate/workspaces/mkd-workspace/mkd-code-review/"
            "config/models.openrouter-free.toml\n"
        )
        # mkd-code-review[paths.hardcoded_sep]: redactor fixture data;
        # foreign text path, not a filesystem join.
        self.assertNotIn("/home/nate", out)
        self.assertIn("models.openrouter-free.toml", out)

    def test_env_var_name_redacted(self) -> None:
        out = sanitize_erlang_review.sanitize_text(
            "require a nonempty OPENROUTER_API_KEY in the environment\n"
        )
        self.assertNotIn("OPENROUTER_API_KEY", out)

    def test_provider_ids_redacted_rule_kept_generic(self) -> None:
        out = sanitize_erlang_review.sanitize_text(
            "- Rule: `llm.openrouter-free`\n"
            "- Description: model `openrouter-free` failed\n"
            "residual model=openrouter/free id=openrouter-free outcome=started\n"
        )
        self.assertNotIn("openrouter-free", out)
        self.assertNotIn("openrouter/free", out)
        self.assertNotIn("residual model=", out)
        self.assertIn("- Rule: `llm`", out)

    def test_windows_home_redacted(self) -> None:
        out = sanitize_erlang_review.sanitize_text(
            # mkd-code-review[paths.hardcoded_sep]: redactor fixture data;
            # foreign text path, not a filesystem join.
            "worktree C:\\Users\\nate\\projects\\x\n"
        )
        # mkd-code-review[paths.hardcoded_sep]: redactor fixture data;
        # foreign text path, not a filesystem join.
        self.assertNotIn("C:\\Users\\nate", out)

    def test_find_leaks_detects_raw_report(self) -> None:
        raw = (
            "residual model=openrouter/free id=openrouter-free outcome=started\n"
            "- Persona source: /home/nate/.local/share/mkd/agents/erlang\n"
            "need OPENROUTER_API_KEY set\n"
        )
        self.assertTrue(sanitize_erlang_review.find_leaks(raw))
        self.assertFalse(
            sanitize_erlang_review.find_leaks(
                sanitize_erlang_review.sanitize_text(raw)
            )
        )


if __name__ == "__main__":
    unittest.main()
