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
"""QA Apache httpd frontend cell (issue #7323, unblocks #7233 / #7164).

Sidecar for ``perc-devctl qa-up --with-apache``: a stock ``httpd:2.4``
container on ``perc-matrix-net`` that terminates 80/443, serves the
published fixture static tree from a bind-mounted DocumentRoot, and
reverse-proxies every DTS dynamic service context (plus a ``/``
fallback) to the QA DTS (Tomcat) cell behind it — the documented DTS
frontend pattern.

Stdlib only. All host joins use :class:`pathlib.Path` (spec 994 FR-001
cross-platform rule — no hardcoded ``/`` separators for host
filesystem paths). Container-side paths are POSIX by construction
(Linux httpd cell) and are literal constants, not host joins.

Layout (repo-relative)::

    docker/apache/qa-apache-vhost.conf   static vhost conf (mounted ro)
    docker/logs/qa-apache-certs[-suffix]/ mkcert output (gitignored):
        apache-cert.pem / apache-key.pem / rootCA.pem

Trust model: mkcert mints a throwaway test CA + server cert (SANs: the
test hostname, ``127.0.0.1``, ``localhost``, ``::1``) on the QA host —
``mkcert -install`` is deliberately NOT run (no host trust-store
mutation from inside the cell). Genuine chain verification rides
``curl --cacert`` / :func:`https_get_status_with_ca` in ``qa-health``;
Playwright uses the documented ``ignoreHTTPSErrors`` precedent (same
as the shipped DTS HTTPS flow, #6375) because throwaway browser
profiles cannot consume host CA installs deterministically.
"""

from __future__ import annotations

import os
import re
import shutil
import ssl
import subprocess
import urllib.error
import urllib.request
from pathlib import Path
from typing import List, Optional, Tuple

from perc_host_ports import (
    is_port_free,
    resolve_cell_suffix,
    resolve_host_port,
    suffixed_cell_container_name,
)

# Sidecar identity: perc-matrix-apache-h2[-suffix] (#6704 suffix rule).
APACHE_CELL_ID = "apache-h2"

# Docker network the sidecar joins. Mirrors ``MATRIX_NETWORK`` in
# ``matrix-install-smoke.py`` (``perc-matrix-net``): the CMS/DTS cells
# attach there, and the vhost reaches Tomcat via container DNS on it.
APACHE_CELL_NETWORK = "perc-matrix-net"

# Stock image: no custom Dockerfile, no image build step. The vhost conf
# is bind-mounted and loaded via a trailing httpd `-c` Include arg.
APACHE_IMAGE = "httpd:2.4"
APACHE_FOREGROUND = "httpd-foreground"

# In-cell listeners (container ports; host publishes are freeport-mapped).
APACHE_CONTAINER_HTTP_PORT = 80
APACHE_CONTAINER_HTTPS_PORT = 443

# Host-port env overrides (explicit operator pin wins, #2005 contract).
APACHE_HTTP_PORT_ENV = "QA_APACHE_HTTP_PORT"
APACHE_HTTPS_PORT_ENV = "QA_APACHE_HTTPS_PORT"

# Preferred host publishes honour the issue scope (ports 80/443) when
# free; otherwise an ephemeral freeport (never hardcoded client-side).
PREFERRED_QA_APACHE_HTTP_PORT = 80
PREFERRED_QA_APACHE_HTTPS_PORT = 443

# Test-site hostname the vhost answers as (ServerName; ServerAlias also
# covers localhost/127.0.0.1 so IP-based QA URLs hit the same vhost).
# Override with QA_APACHE_HOSTNAME. IP SANs on the cert keep
# 127.0.0.1-based runs green without /etc/hosts edits.
APACHE_HOSTNAME_ENV = "QA_APACHE_HOSTNAME"
DEFAULT_APACHE_HOSTNAME = "qa-published.percussion.local"

# mkcert output names inside the (gitignored) host cert dir.
APACHE_CERT_FILE = "apache-cert.pem"
APACHE_KEY_FILE = "apache-key.pem"
APACHE_CA_FILE = "rootCA.pem"

# Container-side mount points (POSIX by construction — Linux cell;
# Path joins would emit backslashes on Windows hosts, so literals are
# correct here, not a portability defect).
# mkd-code-review[paths.hardcoded_sep]: adjudicated FP (issue #7323,
# precedent #6570) — Linux-cell-internal mount paths, never host joins.
APACHE_CONF_CONTAINER_PATH = "/usr/local/apache2/conf/qa-cell.conf"
APACHE_DOCROOT_CONTAINER_PATH = "/usr/local/apache2/htdocs/site"
APACHE_CERTS_CONTAINER_PATH = "/certs"

# DTS backend container port (Tomcat HTTP; mirrors QA_DTS_CONTAINER_PORT
# in perc-devctl — resolved dynamically there, constant here for argv).
DTS_CONTAINER_HTTP_PORT = 9980

# DTS dynamic service contexts (Tomcat war base names = artifactIds).
# Everything else is static (DocumentRoot) or falls back to the Tomcat
# ROOT webapp via `ProxyPass /`.
DTS_SERVICE_CONTEXTS: Tuple[str, ...] = (
    "perc-comments-services",
    "feeds",
    "perc-form-processor",
    "perc-membership-services",
    "perc-metadata-services",
    "perc-polls-services",
    "perc-secure-membership",
)

# Backend-owned marker proving fallback-proxy transit (installer-deployed
# DTS ROOT error document; static, unauthenticated, always present).
# Apache holds no local copy, so HTTP 200 here can only come from DTS.
APACHE_PROXY_PROBE_PATH = "/error.html"

# Service-map probe (exercises the per-context ProxyPass entries, not
# just the `/` fallback): the vhost status must equal the direct-DTS
# status. Absolute codes shift with seeds/config, so the gate asserts
# parity, never a literal.
APACHE_SERVICE_PROBE_PATH = "/feeds/rss"

# Agent-parseable qa-up log tokens (mirror the DTS banner convention).
LOG_TOKEN_HTTP_PORT = "QA_APACHE_HTTP_PORT"
LOG_TOKEN_HTTPS_PORT = "QA_APACHE_HTTPS_PORT"

_QA_UP_LOG_APACHE_HTTP_RE = re.compile(
    r"(?:QA_APACHE_HTTP_PORT|APACHE_HTTP_PORT|apache publish http)\s*[=: ]?\s*(\d+)"
)
_QA_UP_LOG_APACHE_HTTPS_RE = re.compile(
    r"(?:QA_APACHE_HTTPS_PORT|APACHE_HTTPS_PORT|apache publish https)\s*[=: ]?\s*(\d+)"
)


def apache_container(suffix: object = None) -> str:
    """Sidecar container name for the current cell suffix (#6704)."""
    return suffixed_cell_container_name(
        APACHE_CELL_ID, resolve_cell_suffix(suffix)
    )


def apache_hostname() -> str:
    """Test-site hostname the vhost answers as (env override allowed)."""
    raw = os.environ.get(APACHE_HOSTNAME_ENV, "").strip()
    return raw or DEFAULT_APACHE_HOSTNAME


def apache_conf_file(repo_root: Path) -> Path:
    """Host path of the static vhost conf (bind-mounted read-only)."""
    return Path(repo_root) / "docker" / "apache" / "qa-apache-vhost.conf"


def apache_cert_dir(repo_root: Path, suffix: object = None) -> Path:
    """Host dir for mkcert output (gitignored under docker/logs/).

    Suffix-scoped so parallel shards never share a private key.
    """
    clean = resolve_cell_suffix(suffix)
    leaf = "qa-apache-certs" if not clean else f"qa-apache-certs-{clean}"
    return Path(repo_root) / "docker" / "logs" / leaf


def _docker_port(container: str, container_port: int) -> Optional[int]:
    """Host port published for ``container:container_port`` (docker port)."""
    try:
        completed = subprocess.run(
            ["docker", "port", container, str(container_port)],
            shell=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.SubprocessError):
        return None
    if completed.returncode != 0:
        return None
    for line in (completed.stdout or "").splitlines():
        # e.g. "0.0.0.0:8080" or "[::]:8080".
        if ":" not in line:
            continue
        tail = line.rsplit(":", 1)[1].strip()
        try:
            port = int(tail)
        except ValueError:
            continue
        if 1 <= port <= 65535:
            return port
    return None


def _latest_qa_up_log_apache_port(
    repo_root: Path, kind: str
) -> Optional[int]:
    """Apache host publish port from the newest ``qa-up-*.log`` (if any)."""
    log_dir = Path(repo_root) / "docker" / "logs"
    try:
        candidates = sorted(log_dir.glob("qa-up-*.log"))
    except OSError:
        return None
    pattern = (
        _QA_UP_LOG_APACHE_HTTP_RE
        if kind == "http"
        else _QA_UP_LOG_APACHE_HTTPS_RE
    )
    for log_file in reversed(candidates):
        try:
            text = log_file.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        matches = pattern.findall(text)
        if matches:
            try:
                return int(matches[-1])
            except ValueError:
                continue
    return None


def resolve_qa_apache_host_port(
    repo_root: Path, kind: str, container: Optional[str] = None
) -> Tuple[int, str]:
    """Host port for the Apache leg (env → docker → qa-up-log → freeport).

    ``kind`` is ``"http"`` (container 80) or ``"https"`` (container 443).
    Returns ``(port, source)`` with source ``env`` / ``docker`` /
    ``qa-up-log`` / ``preferred`` — the freeport contract shared with
    the CMS/DTS legs (#2005). No env pinning (use :func:`ensure`).
    """
    if kind == "http":
        env_key, container_port, preferred = (
            APACHE_HTTP_PORT_ENV,
            APACHE_CONTAINER_HTTP_PORT,
            PREFERRED_QA_APACHE_HTTP_PORT,
        )
    else:
        env_key, container_port, preferred = (
            APACHE_HTTPS_PORT_ENV,
            APACHE_CONTAINER_HTTPS_PORT,
            PREFERRED_QA_APACHE_HTTPS_PORT,
        )
    raw = os.environ.get(env_key, "").strip()
    if raw:
        try:
            port = int(raw)
        except ValueError:
            port = 0
        if 1 <= port <= 65535:
            return port, "env"
    if container is None:
        container = apache_container()
    published = _docker_port(container, container_port)
    if published is not None:
        return published, "docker"
    logged = _latest_qa_up_log_apache_port(repo_root, kind)
    if logged is not None:
        return logged, "qa-up-log"
    return resolve_host_port(env_key, preferred=preferred), "preferred"


def ensure_qa_apache_http_host_port() -> int:
    """Resolve + pin the Apache HTTP host publish port into the env."""
    port, _source = resolve_qa_apache_host_port(Path.cwd(), "http")
    os.environ[APACHE_HTTP_PORT_ENV] = str(port)
    return port


def ensure_qa_apache_https_host_port() -> int:
    """Resolve + pin the Apache HTTPS host publish port into the env."""
    port, _source = resolve_qa_apache_host_port(Path.cwd(), "https")
    os.environ[APACHE_HTTPS_PORT_ENV] = str(port)
    return port


def apache_base_url(host_port: int) -> str:
    """Public HTTP origin for the Apache leg on the given host port."""
    return f"http://127.0.0.1:{host_port}"


def apache_https_base_url(host_port: int) -> str:
    """Public HTTPS origin for the Apache leg on the given host port."""
    return f"https://127.0.0.1:{host_port}"


def build_apache_run_argv(
    *,
    container: str,
    network: str,
    http_port: int,
    https_port: int,
    conf_file: Path,
    site_dir: Path,
    cert_dir: Path,
    dts_backend: str,
    hostname: str,
    image: str = APACHE_IMAGE,
) -> List[str]:
    """``docker run`` argv for the Apache sidecar (pure; unit-tested).

    Stock image + bind mounts only (no image build): the static vhost
    conf, the fixture DocumentRoot, and the mkcert output dir mount
    read-only; ``httpd-foreground`` gets a ``-c`` Include plus the
    backend/hostname as container env — httpd interpolates undefined
    ``${VAR}`` from the environment (command-line ``-D`` defines do
    NOT reach ``-c``-included files, hence env).
    """
    return [
        "docker",
        "run",
        "-d",
        "--name",
        container,
        "--network",
        network,
        "-p",
        f"{http_port}:{APACHE_CONTAINER_HTTP_PORT}",
        "-p",
        f"{https_port}:{APACHE_CONTAINER_HTTPS_PORT}",
        "-e",
        f"DTS_BACKEND={dts_backend}",
        "-e",
        f"APACHE_HOSTNAME={hostname}",
        "-v",
        f"{conf_file}:{APACHE_CONF_CONTAINER_PATH}:ro",
        "-v",
        f"{site_dir}:{APACHE_DOCROOT_CONTAINER_PATH}:ro",
        "-v",
        f"{cert_dir}:{APACHE_CERTS_CONTAINER_PATH}:ro",
        image,
        APACHE_FOREGROUND,
        "-c",
        f"Include {APACHE_CONF_CONTAINER_PATH}",
    ]


# mkcert binary: ``MKCERT_BIN`` env wins (broken-shim hosts), else PATH.
MKCERT_BIN_ENV = "MKCERT_BIN"


def mkcert_binary() -> Optional[str]:
    """Resolve the mkcert binary (env override wins, else PATH)."""
    override = os.environ.get(MKCERT_BIN_ENV, "").strip()
    if override:
        return override
    return shutil.which("mkcert")


def mkcert_available() -> bool:
    """True when a ``mkcert`` binary resolves (QA host prerequisite)."""
    return mkcert_binary() is not None


def mkcert_issue_argv(
    cert_file: Path, key_file: Path, hostname: str
) -> List[str]:
    """``mkcert`` argv minting the sidecar server cert (pure).

    SANs cover the test hostname plus the loopback names/addresses QA
    URLs use, so IP-based runs validate without ``/etc/hosts`` edits.
    """
    return [
        mkcert_binary() or "mkcert",
        "-cert-file",
        str(cert_file),
        "-key-file",
        str(key_file),
        hostname,
        "127.0.0.1",
        "localhost",
        "::1",
    ]


def mkcert_caroot_argv() -> List[str]:
    """``mkcert -CAROOT`` argv (pure)."""
    return [mkcert_binary() or "mkcert", "-CAROOT"]


def ensure_apache_certs(hostname: str, cert_dir: Path) -> Tuple[bool, str]:
    """Mint (or reuse) the sidecar TLS identity with mkcert.

    Idempotent: existing ``apache-cert.pem`` / ``apache-key.pem`` are
    reused; the CA bundle is always refreshed from ``mkcert -CAROOT``.
    Returns ``(ok, detail)`` — never raises on subprocess failure so
    callers can print an agent-parseable FAIL line.
    """
    dest = Path(cert_dir)
    try:
        dest.mkdir(parents=True, exist_ok=True)
    except OSError as exc:
        return False, f"cert_dir_mkdir_failed: {exc}"
    if not mkcert_available():
        return (
            False,
            "mkcert_not_found: install mkcert on the QA host "
            "(https://github.com/FiloSottile/mkcert) — qa-up "
            "--with-apache cannot mint the test-site cert without it.",
        )
    cert_file = dest / APACHE_CERT_FILE
    key_file = dest / APACHE_KEY_FILE
    if not (cert_file.is_file() and key_file.is_file()):
        try:
            completed = subprocess.run(
                mkcert_issue_argv(cert_file, key_file, hostname),
                shell=False,
                capture_output=True,
                text=True,
                timeout=120,
            )
        except (OSError, subprocess.SubprocessError) as exc:
            return False, f"mkcert_issue_failed: {exc}"
        if completed.returncode != 0:
            return (
                False,
                "mkcert_issue_failed: "
                f"{(completed.stderr or completed.stdout or '').strip()[:500]}",
            )
    try:
        completed = subprocess.run(
            mkcert_caroot_argv(),
            shell=False,
            capture_output=True,
            text=True,
            timeout=30,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        return False, f"mkcert_caroot_failed: {exc}"
    if completed.returncode != 0:
        return False, "mkcert_caroot_failed: non-zero exit"
    caroot = Path((completed.stdout or "").strip())
    root_ca = caroot / "rootCA.pem"
    if not root_ca.is_file():
        return False, f"mkcert_caroot_missing_rootCA: {caroot}"
    try:
        (dest / APACHE_CA_FILE).write_bytes(root_ca.read_bytes())
    except OSError as exc:
        return False, f"ca_copy_failed: {exc}"
    return True, f"certs_ready dir={dest} hostname={hostname}"


def http_get_status_and_server(
    url: str, timeout: float = 5.0
) -> Tuple[int, str]:
    """GET ``url`` over plain HTTP; return ``(status, server_header)``.

    ``Server`` distinguishes Apache-direct (``Apache/2.4…``) from
    proxied-to-Tomcat (``Apache-Coyote/1.1``) responses through the
    vhost. Returns ``(0, "")`` on network error; HTTP error codes are
    returned (not raised) with the header when present.
    """
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            return response.status, response.headers.get("Server", "")
    except urllib.error.HTTPError as exc:
        try:
            server = exc.headers.get("Server", "") if exc.headers else ""
        except Exception:  # noqa: BLE001 — best-effort header read
            server = ""
        return exc.code, server
    except (urllib.error.URLError, TimeoutError, ConnectionError, OSError):
        return 0, ""


def https_get_status_with_ca(
    url: str, cafile: Path, timeout: float = 10.0
) -> Tuple[int, str]:
    """GET ``url`` over HTTPS verifying against the mkcert CA bundle.

    Genuine chain verification (no ``ignoreHTTPSErrors`` equivalent):
    an untrusted/forged cert fails closed with ``(0, "")``. Returns
    ``(status, server_header)`` like :func:`http_get_status_and_server`.
    """
    try:
        context = ssl.create_default_context(cafile=str(cafile))
    except (OSError, ssl.SSLError):
        return 0, ""
    try:
        with urllib.request.urlopen(url, timeout=timeout, context=context) as response:
            return response.status, response.headers.get("Server", "")
    except urllib.error.HTTPError as exc:
        try:
            server = exc.headers.get("Server", "") if exc.headers else ""
        except Exception:  # noqa: BLE001 — best-effort header read
            server = ""
        return exc.code, server
    except Exception:  # noqa: BLE001 — TLS/URL/network fail closed
        return 0, ""


def apache_banner_lines(
    http_port: int,
    https_port: int,
    hostname: str,
    ca_pem: Path,
    container: str,
) -> List[str]:
    """Agent-parseable endpoint lines for the qa-up banner (pure)."""
    http_base = apache_base_url(http_port)
    https_base = apache_https_base_url(https_port)
    return [
        f"{LOG_TOKEN_HTTP_PORT}={http_port}",
        f"APACHE_URL={http_base}",
        f"{LOG_TOKEN_HTTPS_PORT}={https_port}",
        f"APACHE_HTTPS_URL={https_base}",
        f"APACHE_HOSTNAME={hostname}",
        f"APACHE_CA_PEM={ca_pem}",
        f"QA_CONTAINER:{container}",
    ]
