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
"""Unit tests for night-cycle-notify.py (fail-open webhook on cycle end)."""
from __future__ import annotations

import io
import json
import socket
from pathlib import Path
from typing import Any
from unittest import mock

import pytest

# The module file uses hyphens; tests import it via a path hook below.


SCRIPT_DIR = Path(__file__).resolve().parent


def _load_module():
    import importlib.util

    path = SCRIPT_DIR / "night-cycle-notify.py"
    spec = importlib.util.spec_from_file_location("night_cycle_notify", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


notify = _load_module()


SAMPLE_REPORT = """# night-issue-prs report — 12-issue cycle (2026-11-19)

## PRs

- Opened: 3 (#6683, #6684, #6685). **All 3 MERGED**.
- Follow-up'd: none.
- Cluster'd: none.
- Issues filed: #6686, #6687 (audit gaps). Do not treat these as PRs.

## Cycle verify

- Status: **passed**.
"""

SAMPLE_REPORT_WITH_URLS = """# report

PRs this run:
https://github.com/intersoftdatalabs-in/percussioncms/pull/100
and a markdown link [PR](https://github.com/intersoftdatalabs-in/percussioncms/pull/101)
plus a repeat https://github.com/intersoftdatalabs-in/percussioncms/pull/100
"""


class _FakeResponse:
    def __init__(self, status: int = 204) -> None:
        self.status = status

    def __enter__(self) -> "_FakeResponse":
        return self

    def __exit__(self, *args: Any) -> None:
        return None

    def read(self) -> bytes:
        return b""

    def close(self) -> None:
        return None


def test_unset_url_is_silent_success() -> None:
    captured = io.StringIO()
    with mock.patch.object(notify.sys, "stderr", captured):
        rc = notify.run([], env={"PATH": "/usr/bin"})
    assert rc == 0
    assert captured.getvalue() == ""


def test_empty_url_is_silent_success() -> None:
    rc = notify.run([], env={notify.URL_ENV: "   "})
    assert rc == 0


@pytest.mark.parametrize(
    "remote,expected",
    [
        (
            "git@github.com-intersoft:intersoftdatalabs-in/percussioncms.git",
            "intersoftdatalabs-in/percussioncms",
        ),
        (
            "git@github.com:intersoftdatalabs-in/percussioncms.git",
            "intersoftdatalabs-in/percussioncms",
        ),
        (
            "https://github.com/intersoftdatalabs-in/percussioncms.git",
            "intersoftdatalabs-in/percussioncms",
        ),
        (
            "ssh://git@github.com/intersoftdatalabs-in/percussioncms.git",
            "intersoftdatalabs-in/percussioncms",
        ),
        (
            "https://github.com/intersoftdatalabs-in/percussioncms",
            "intersoftdatalabs-in/percussioncms",
        ),
        ("not-a-remote", ""),
    ],
)
def test_repo_from_remote_url(remote: str, expected: str) -> None:
    assert notify.repo_from_remote_url(remote) == expected


def test_extract_pr_urls_from_opened_line_not_issues_filed() -> None:
    urls = notify.extract_pr_urls(SAMPLE_REPORT, "intersoftdatalabs-in/percussioncms")
    assert urls == [
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6683",
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6684",
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6685",
    ]


def test_extract_pr_urls_from_explicit_links_deduped() -> None:
    urls = notify.extract_pr_urls(
        SAMPLE_REPORT_WITH_URLS, "intersoftdatalabs-in/percussioncms"
    )
    assert urls == [
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/100",
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/101",
    ]


def test_parse_auth_header() -> None:
    assert notify.parse_auth_header("Authorization: Bearer secret-token") == (
        "Authorization",
        "Bearer secret-token",
    )
    assert notify.parse_auth_header("X-Hook-Token:abc") == ("X-Hook-Token", "abc")
    assert notify.parse_auth_header("no-colon") is None
    assert notify.parse_auth_header(": value") is None
    assert notify.parse_auth_header("Name: value\nX-Injected: 1") is None


def test_posts_json_body_and_optional_auth_header(tmp_path: Path) -> None:
    report = tmp_path / "night-report.md"
    report.write_text(SAMPLE_REPORT, encoding="utf-8")
    seen: dict[str, Any] = {}

    def fake_urlopen(request: Any, timeout: int = 0) -> _FakeResponse:
        seen["url"] = request.full_url
        seen["timeout"] = timeout
        seen["method"] = request.get_method()
        seen["body"] = json.loads(request.data.decode("utf-8"))
        seen["headers"] = {k.lower(): v for k, v in request.header_items()}
        return _FakeResponse(204)

    env = {
        notify.URL_ENV: "https://hooks.example.test/night-cycle",
        notify.AUTH_HEADER_ENV: "Authorization: Bearer super-secret-value",
        notify.REPORT_ENV: str(report),
        notify.REPO_ENV: "intersoftdatalabs-in/percussioncms",
    }
    captured_err = io.StringIO()
    captured_out = io.StringIO()
    with mock.patch.object(notify.urllib.request, "urlopen", fake_urlopen), mock.patch.object(
        notify.sys, "stderr", captured_err
    ), mock.patch.object(notify.sys, "stdout", captured_out):
        rc = notify.run(
            ["--finished-at", "2026-11-19T06:00:00Z"],
            env=env,
        )
    assert rc == 0
    assert seen["url"] == "https://hooks.example.test/night-cycle"
    assert seen["method"] == "POST"
    assert seen["timeout"] == 10
    body = seen["body"]
    assert body["event"] == "night-cycle-finished"
    assert body["repo"] == "intersoftdatalabs-in/percussioncms"
    assert body["report_path"] == str(report)
    assert body["finished_at"] == "2026-11-19T06:00:00Z"
    assert body["hostname"] == socket.gethostname()
    assert body["pr_urls"] == [
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6683",
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6684",
        "https://github.com/intersoftdatalabs-in/percussioncms/pull/6685",
    ]
    assert seen["headers"]["authorization"] == "Bearer super-secret-value"
    assert seen["headers"]["content-type"] == "application/json"
    combined = captured_err.getvalue() + captured_out.getvalue()
    assert "super-secret-value" not in combined
    assert "Bearer" not in combined
    assert "night-cycle-finished" in captured_err.getvalue()


def test_http_error_and_timeout_exit_zero_without_leaking_secret() -> None:
    def raise_http(request: Any, timeout: int = 0) -> Any:
        raise notify.urllib.error.HTTPError(
            request.full_url, 503, "unavailable", hdrs=None, fp=io.BytesIO(b"nope")
        )

    env = {
        notify.URL_ENV: "https://hooks.example.test/night-cycle",
        notify.AUTH_HEADER_ENV: "X-Auth: leaked-secret-token",
    }
    captured = io.StringIO()
    with mock.patch.object(notify.urllib.request, "urlopen", raise_http), mock.patch.object(
        notify.sys, "stderr", captured
    ):
        rc = notify.run([], env=env)
    assert rc == 0
    err = captured.getvalue()
    assert "HTTP 503" in err
    assert "leaked-secret-token" not in err
    assert "X-Auth" not in err

    def raise_timeout(request: Any, timeout: int = 0) -> Any:
        raise TimeoutError("took too long")

    captured = io.StringIO()
    with mock.patch.object(notify.urllib.request, "urlopen", raise_timeout), mock.patch.object(
        notify.sys, "stderr", captured
    ):
        rc = notify.run([], env=env)
    assert rc == 0
    assert "TimeoutError" in captured.getvalue()
    assert "leaked-secret-token" not in captured.getvalue()


def test_malformed_auth_header_still_posts() -> None:
    seen: dict[str, Any] = {}

    def fake_urlopen(request: Any, timeout: int = 0) -> _FakeResponse:
        seen["headers"] = {k.lower(): v for k, v in request.header_items()}
        return _FakeResponse(200)

    env = {
        notify.URL_ENV: "https://hooks.example.test/night-cycle",
        notify.AUTH_HEADER_ENV: "not-a-header",
    }
    captured = io.StringIO()
    with mock.patch.object(notify.urllib.request, "urlopen", fake_urlopen), mock.patch.object(
        notify.sys, "stderr", captured
    ):
        rc = notify.run(["--finished-at", "2026-11-19T06:00:00Z"], env=env)
    assert rc == 0
    assert "authorization" not in seen["headers"]
    assert "malformed" in captured.getvalue()
    assert "not-a-header" not in captured.getvalue()


def test_missing_report_still_posts() -> None:
    seen: dict[str, Any] = {}

    def fake_urlopen(request: Any, timeout: int = 0) -> _FakeResponse:
        seen["body"] = json.loads(request.data.decode("utf-8"))
        return _FakeResponse(204)

    env = {
        notify.URL_ENV: "https://hooks.example.test/night-cycle",
        notify.REPORT_ENV: "/no/such/night-report.md",
        notify.REPO_ENV: "intersoftdatalabs-in/percussioncms",
    }
    with mock.patch.object(notify.urllib.request, "urlopen", fake_urlopen):
        rc = notify.run(["--finished-at", "2026-11-19T06:00:00Z"], env=env)
    assert rc == 0
    assert seen["body"]["pr_urls"] == []
    assert seen["body"]["report_path"].endswith("night-report.md")
