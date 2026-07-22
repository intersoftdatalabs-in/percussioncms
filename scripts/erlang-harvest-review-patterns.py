#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Harvest GitHub PR review comments into Erlang review pattern memory.

Cross-platform (Windows / Linux / macOS). Requires:
  - Python 3.9+
  - GitHub CLI ``gh`` authenticated (``gh auth login``)
  - Network access to the GitHub API for the target repo

Default flow:
  1. Fetch pull-request **line review comments** (includes closed/merged PRs).
  2. Keep top-level comments from configured bots (default: kilo-code-bot).
  3. Generalize bodies, cluster similar themes, categorize.
  4. Write a candidate report under ``docs/ai-generated/code-reviews/``.
  5. With ``--apply``, auto-merge high-count themes into
     ``modules/ai-shared-develop/.../skills/erlang-review/patterns.md``.

Does not call OS temp dirs; optional scratch only under repo ``tmp/`` if needed.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.parse
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Optional


# ---------------------------------------------------------------------------
# Defaults / constants
# ---------------------------------------------------------------------------

DEFAULT_AUTHORS = (
    "kilo-code-bot[bot]",
    "kilo-code-bot",
)

# Optional extra bots when --include-security-bots
SECURITY_BOT_AUTHORS = (
    "github-advanced-security[bot]",
    "copilot-pull-request-reviewer[bot]",
    "Copilot",
)

SEVERITY_ALIASES = {
    "CRITICAL": "critical",
    "ERROR": "critical",
    "BUG": "critical",
    "WARNING": "warning",
    "WARN": "warning",
    "SUGGESTION": "suggestion",
    "NIT": "nit",
    "INFO": "nit",
    "NOTE": "nit",
    "QUESTION": "suggestion",
}

# Minimum token overlap (Jaccard) to treat two generalized lines as the same cluster
CLUSTER_JACCARD = 0.45

# Categories under "## Recurring findings" in patterns.md
CATEGORY_RULES: list[tuple[str, tuple[str, ...]]] = [
    (
        "Installer / Ant / distribution",
        (
            "install",
            "installer",
            "ant ",
            "preinstall",
            "jdbc",
            "class.forname",
            "do.install",
            "distribution",
            "processcode",
            "system.exit",
            "repository",
        ),
    ),
    (
        "Tests",
        (
            "test",
            "assert",
            "junit",
            "mockito",
            "coverage",
            "aftereach",
            "beforeeach",
            "fixture",
            "happy path",
            "unit test",
        ),
    ),
    (
        "Security / config",
        (
            "password",
            "secret",
            "token",
            "ssrf",
            "xss",
            "injection",
            "ldap",
            "auth",
            "allowlist",
            "blocklist",
            "sanitiz",
            "credential",
            "codeql",
        ),
    ),
    (
        "Cross-platform / I/O",
        (
            "path",
            "file.separator",
            "pathseparator",
            "windows",
            "unix",
            "linux",
            "crlf",
            "line ending",
            "toabsolutepath",
            "getabsolutefile",
            "nio.file",
            "hardcoded",
            "case-insensit",
            "case sensit",
        ),
    ),
    (
        "Maintainability",
        (
            "duplicate",
            "dead code",
            "javadoc",
            "typo",
            "naming",
            "unused",
            "redundant",
            "license",
            "stringbuilder",
            "null",
        ),
    ),
]

HARD_GATE_HINTS = (
    "missing test",
    "no test",
    "unit test",
    "behavioral",
    "path join",
    "hardcoded",
    "file.separator",
    "process exit",
    "system.exit",
    "swallowed",
    "empty catch",
    "secret",
    "password",
    "ssrf",
    "false green",
    "compile",
    "does not compile",
    "prevents compilation",
)

SKIP_BODY_PREFIXES = (
    "**mitigation",
    "mitigation",
    "**fixed",
    "fixed in",
    "addressed in",
    "lgtm",
    "nit:",
)


# ---------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------


@dataclass
class RawComment:
    id: int
    user: str
    body: str
    path: str
    pr_number: Optional[int]
    created_at: str
    html_url: str
    in_reply_to_id: Optional[int]


@dataclass
class Cluster:
    key: str
    principle: str
    category: str
    severity_counts: Counter = field(default_factory=Counter)
    count: int = 0
    prs: set[int] = field(default_factory=set)
    sample_urls: list[str] = field(default_factory=list)
    sample_paths: list[str] = field(default_factory=list)
    hard_gate_hint: bool = False

    def dominant_severity(self) -> str:
        if not self.severity_counts:
            return "suggestion"
        return self.severity_counts.most_common(1)[0][0]


# ---------------------------------------------------------------------------
# Repo / gh helpers
# ---------------------------------------------------------------------------


def find_repo_root(start: Optional[Path] = None) -> Path:
    cur = (start or Path.cwd()).resolve()
    for p in [cur, *cur.parents]:
        if (p / ".git").exists() and (p / "AGENTS.md").exists():
            return p
        if (p / ".git").is_file() and (p / "AGENTS.md").exists():
            return p
    # Fallback: cwd if it looks like the monorepo
    if (cur / "modules" / "ai-shared-develop").is_dir():
        return cur
    raise SystemExit(
        "erlang-harvest: cannot find repo root (looked for AGENTS.md + .git). "
        "Run from the percussioncms checkout."
    )


def run_gh_json(args: list[str], *, timeout: int = 120) -> Any:
    cmd = ["gh", *args]
    try:
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=timeout,
            check=False,
        )
    except FileNotFoundError as e:
        raise SystemExit(
            "erlang-harvest: `gh` (GitHub CLI) not found on PATH. "
            "Install from https://cli.github.com/ and run `gh auth login`."
        ) from e
    except subprocess.TimeoutExpired as e:
        raise SystemExit(f"erlang-harvest: gh timed out: {' '.join(cmd)}") from e

    if proc.returncode != 0:
        err = (proc.stderr or proc.stdout or "").strip()
        raise SystemExit(
            f"erlang-harvest: gh failed ({proc.returncode}): {err or 'no stderr'}\n"
            f"  command: {' '.join(cmd)}"
        )
    text = proc.stdout.strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        raise SystemExit(
            f"erlang-harvest: invalid JSON from gh: {e}\n  first 200 chars: {text[:200]!r}"
        ) from e


def detect_repo_slug(explicit: Optional[str]) -> str:
    if explicit:
        return explicit
    env = os.environ.get("GH_REPO") or os.environ.get("GITHUB_REPOSITORY")
    if env and "/" in env:
        return env
    data = run_gh_json(
        ["repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner"]
    )
    if isinstance(data, str) and "/" in data:
        return data
    # gh -q may return raw string without JSON quotes when using -q
    # run_gh_json expects JSON; handle plain string via subprocess fallback
    proc = subprocess.run(
        ["gh", "repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if proc.returncode == 0 and "/" in (proc.stdout or ""):
        return proc.stdout.strip().strip('"')
    raise SystemExit(
        "erlang-harvest: cannot detect owner/repo. Pass --repo owner/name "
        "or set GH_REPO / run inside a gh-aware git checkout."
    )


def fetch_all_pull_comments(repo: str, max_pages: int = 50) -> list[dict[str, Any]]:
    """Fetch all PR review comments (open + closed PRs)."""
    items: list[dict[str, Any]] = []
    page = 1
    per_page = 100
    while page <= max_pages:
        path = (
            f"repos/{repo}/pulls/comments"
            f"?per_page={per_page}&page={page}&sort=created&direction=desc"
        )
        batch = run_gh_json(["api", path], timeout=180)
        if not batch:
            break
        if not isinstance(batch, list):
            raise SystemExit(f"erlang-harvest: unexpected API shape: {type(batch)}")
        items.extend(batch)
        if len(batch) < per_page:
            break
        page += 1
    return items


def pr_number_from_url(url: str) -> Optional[int]:
    if not url:
        return None
    m = re.search(r"/pulls/(\d+)", url)
    return int(m.group(1)) if m else None


# ---------------------------------------------------------------------------
# Text processing
# ---------------------------------------------------------------------------


_SEVERITY_RE = re.compile(
    r"^\s*(?:\*\*)?\[?(CRITICAL|ERROR|BUG|WARNING|WARN|SUGGESTION|NIT|INFO|NOTE|QUESTION)\]?"
    r"(?:\*\*)?\s*[:\-—–]\s*",
    re.IGNORECASE,
)

_CODE_FENCE_RE = re.compile(r"```.*?```", re.DOTALL)
_INLINE_CODE_RE = re.compile(r"`[^`]+`")
_PATH_RE = re.compile(
    r"(?:[A-Za-z]:)?(?:[\\/][\w.\-]+)+\.\w+|(?:[\w.\-]+/)+[\w.\-]+\.\w+"
)
_LINE_RE = re.compile(r"\blines?\s+\d+(?:\s*[-–—]\s*\d+)?\b", re.IGNORECASE)
_LINE_COLON_RE = re.compile(r":\d{1,5}\b")
_PR_RE = re.compile(r"\bPR\s*#?\d+\b", re.IGNORECASE)
_ISSUE_RE = re.compile(r"\b(?:issue|ticket)\s*#?\d+\b", re.IGNORECASE)
_COMMIT_RE = re.compile(r"\b[0-9a-f]{7,40}\b")
_MULTI_SPACE_RE = re.compile(r"\s+")
_TOKEN_RE = re.compile(r"[a-z0-9]{3,}")


def extract_severity(body: str) -> tuple[str, str]:
    """Return (severity, body_without_prefix)."""
    if not body:
        return "suggestion", ""
    first, _, rest = body.strip().partition("\n")
    m = _SEVERITY_RE.match(first)
    if m:
        sev = SEVERITY_ALIASES.get(m.group(1).upper(), "suggestion")
        remainder = first[m.end() :].strip()
        if rest:
            remainder = (remainder + "\n" + rest).strip()
        return sev, remainder
    return "suggestion", body.strip()


def generalize_body(body: str) -> str:
    """Strip one-off detail into a reusable one-line principle."""
    if not body:
        return ""
    text = body.strip()
    text = _CODE_FENCE_RE.sub(" <code> ", text)
    # Prefer first non-empty paragraph / sentence block
    text = text.split("\n\n")[0]
    text = text.replace("\n", " ")
    text = _INLINE_CODE_RE.sub(lambda m: _simplify_inline_code(m.group(0)), text)
    text = _PATH_RE.sub("<path>", text)
    text = _LINE_RE.sub("line N", text)
    text = _LINE_COLON_RE.sub("", text)
    text = _PR_RE.sub("<pr>", text)
    text = _ISSUE_RE.sub("<issue>", text)
    text = _COMMIT_RE.sub("<commit>", text)
    text = _MULTI_SPACE_RE.sub(" ", text).strip()
    # Strip leftover markdown bold / emphasis wrappers
    text = re.sub(r"^\*+\s*", "", text)
    text = re.sub(r"\*+$", "", text)
    text = text.replace("**", "").replace("__", "")
    text = text.strip(" -–—:;")
    # Drop unusable placeholder-only noise
    if text.count("<symbol>") >= 2 and len(tokenize(text)) < 6:
        return ""
    # Truncate long principles
    if len(text) > 220:
        # cut at sentence if possible
        cut = text[:220]
        for sep in (". ", "; ", " — ", " - "):
            idx = cut.rfind(sep)
            if idx >= 80:
                text = cut[: idx + 1].strip()
                break
        else:
            text = cut.rsplit(" ", 1)[0] + "…"
    # Ensure it reads as a principle, not a command to the author of one PR
    text = re.sub(r"^(please|kindly|you should|you must)\s+", "", text, flags=re.I)
    return text.strip()


def _simplify_inline_code(token: str) -> str:
    inner = token.strip("`")
    if not inner:
        return " "
    # Keep short API tokens; scrub long expressions
    if len(inner) > 48 or " " in inner or "(" in inner:
        if re.search(r"[\\/]", inner):
            return " <path> "
        if re.search(r"test|assert|mock", inner, re.I):
            return " <test-symbol> "
        return " <symbol> "
    return f" `{inner}` "


def tokenize(text: str) -> set[str]:
    return set(_TOKEN_RE.findall(text.lower()))


def jaccard(a: set[str], b: set[str]) -> float:
    if not a or not b:
        return 0.0
    inter = len(a & b)
    union = len(a | b)
    return inter / union if union else 0.0


def categorize(principle: str, path: str = "") -> str:
    blob = f"{principle} {path}".lower()
    scores: list[tuple[int, str]] = []
    for name, kws in CATEGORY_RULES:
        score = sum(1 for kw in kws if kw in blob)
        if score:
            scores.append((score, name))
    if not scores:
        return "Maintainability"
    scores.sort(key=lambda x: (-x[0], x[1]))
    return scores[0][1]


def looks_like_hard_gate(principle: str, severity: str) -> bool:
    if severity == "critical":
        return True
    low = principle.lower()
    return any(h in low for h in HARD_GATE_HINTS)


def should_skip_body(body: str) -> bool:
    if not body or not body.strip():
        return True
    low = body.strip().lower()
    return any(low.startswith(p) for p in SKIP_BODY_PREFIXES)


# ---------------------------------------------------------------------------
# Parse / cluster / merge
# ---------------------------------------------------------------------------


def parse_comments(
    raw_items: Iterable[dict[str, Any]],
    authors: set[str],
) -> list[RawComment]:
    out: list[RawComment] = []
    authors_l = {a.lower() for a in authors}
    for item in raw_items:
        user = ((item.get("user") or {}).get("login")) or ""
        if user.lower() not in authors_l:
            continue
        if item.get("in_reply_to_id"):
            # Skip thread replies (mitigations, follow-ups)
            continue
        body = item.get("body") or ""
        if should_skip_body(body):
            continue
        out.append(
            RawComment(
                id=int(item.get("id") or 0),
                user=user,
                body=body,
                path=item.get("path") or "",
                pr_number=pr_number_from_url(item.get("pull_request_url") or ""),
                created_at=item.get("created_at") or "",
                html_url=item.get("html_url") or "",
                in_reply_to_id=item.get("in_reply_to_id"),
            )
        )
    return out


def cluster_comments(comments: list[RawComment]) -> list[Cluster]:
    clusters: list[Cluster] = []
    # Each cluster keeps a token set for matching
    cluster_tokens: list[set[str]] = []

    for c in comments:
        sev, rest = extract_severity(c.body)
        principle = generalize_body(rest)
        if len(principle) < 24:
            continue
        # Drop pure praise / empty generalizations
        if principle.lower() in {"looks good", "lgtm", "nice", "thanks"}:
            continue
        if principle.startswith("<") and principle.count("<") >= 2:
            # Mostly placeholders — low value as institutional memory
            if len(tokenize(re.sub(r"<[^>]+>", " ", principle))) < 4:
                continue
        toks = tokenize(principle)
        if len(toks) < 3:
            continue

        matched_idx: Optional[int] = None
        best = 0.0
        for i, ct in enumerate(cluster_tokens):
            score = jaccard(toks, ct)
            if score >= CLUSTER_JACCARD and score > best:
                best = score
                matched_idx = i

        if matched_idx is None:
            cat = categorize(principle, c.path)
            cl = Cluster(
                key=principle.lower()[:80],
                principle=principle,
                category=cat,
                hard_gate_hint=looks_like_hard_gate(principle, sev),
            )
            cl.severity_counts[sev] += 1
            cl.count = 1
            if c.pr_number:
                cl.prs.add(c.pr_number)
            if c.html_url and len(cl.sample_urls) < 5:
                cl.sample_urls.append(c.html_url)
            if c.path and c.path not in cl.sample_paths and len(cl.sample_paths) < 5:
                cl.sample_paths.append(c.path)
            clusters.append(cl)
            cluster_tokens.append(toks)
        else:
            cl = clusters[matched_idx]
            cl.count += 1
            cl.severity_counts[sev] += 1
            cl.hard_gate_hint = cl.hard_gate_hint or looks_like_hard_gate(
                principle, sev
            )
            if c.pr_number:
                cl.prs.add(c.pr_number)
            if c.html_url and len(cl.sample_urls) < 5:
                cl.sample_urls.append(c.html_url)
            if c.path and c.path not in cl.sample_paths and len(cl.sample_paths) < 5:
                cl.sample_paths.append(c.path)
            # Prefer longer principle if current is short
            if len(principle) > len(cl.principle) and len(principle) <= 220:
                cl.principle = principle
            cluster_tokens[matched_idx] = cluster_tokens[matched_idx] | toks

    clusters.sort(key=lambda c: (-c.count, -len(c.prs), c.principle.lower()))
    return clusters


def parse_patterns_bullets(patterns_text: str) -> set[str]:
    """Normalized existing bullet texts for dedup."""
    found: set[str] = set()
    for line in patterns_text.splitlines():
        s = line.strip()
        if s.startswith("- "):
            found.add(_norm_principle(s[2:]))
    return found


def _norm_principle(text: str) -> str:
    t = text.lower().strip()
    t = re.sub(r"\(seen\s+\d+[×x]?.*?\)\s*$", "", t).strip()
    t = re.sub(r"\s+", " ", t)
    return t


def principles_similar(a: str, b: str, threshold: float = 0.5) -> bool:
    return jaccard(tokenize(a), tokenize(b)) >= threshold


def is_promotable(
    cl: Cluster,
    *,
    min_count: int,
    min_prs: int,
    promote_critical: bool = False,
) -> bool:
    """True if cluster is frequent enough (or optional critical hard-gate).

    Default automation path: multi-PR recurrence only. Single-PR CRITICAL themes
    require ``promote_critical=True`` (``--promote-critical``) so patterns.md
    stays short without a human first pass on the candidates report.
    """
    # Strong signal: same theme across multiple PRs (primary automation path)
    if len(cl.prs) >= min_prs and cl.count >= max(min_count, 2):
        # Drop placeholder-heavy principles even if multi-PR
        if cl.principle.count("<symbol>") >= 2:
            return False
        return True
    # Optional: single-PR critical hard gates (noisier; opt-in)
    if (
        promote_critical
        and cl.hard_gate_hint
        and cl.dominant_severity() == "critical"
        and cl.count >= 1
        and _critical_hard_gate_ok(cl.principle)
        and cl.principle.count("<symbol>") < 2
    ):
        return True
    return False


def _critical_hard_gate_ok(principle: str) -> bool:
    """Avoid promoting one-off CRITICAL noise (typos, copyright, dependabot counts)."""
    low = principle.lower()
    block = (
        "copyright",
        "dependabot count",
        "commit message",
        "typo",
        "aria-label",
        "year regression",
    )
    if any(b in low for b in block):
        return False
    allow = (
        "compil",
        "path",
        "travers",
        "ssrf",
        "inject",
        "secret",
        "password",
        "null",
        "npe",
        "test",
        "exit",
        "securityexception",
        "false green",
        "systemproperty",
        "separator",
    )
    return any(a in low for a in allow)


def select_for_apply(
    clusters: list[Cluster],
    *,
    min_count: int,
    min_prs: int,
    existing: set[str],
    max_new: int,
    promote_critical: bool = False,
) -> list[Cluster]:
    chosen: list[Cluster] = []
    existing_norms = set(existing)
    for cl in clusters:
        if not is_promotable(
            cl,
            min_count=min_count,
            min_prs=min_prs,
            promote_critical=promote_critical,
        ):
            continue
        norm = _norm_principle(cl.principle)
        if norm in existing_norms:
            continue
        if any(principles_similar(cl.principle, e) for e in existing_norms):
            continue
        if any(principles_similar(cl.principle, c.principle) for c in chosen):
            continue
        chosen.append(cl)
        existing_norms.add(norm)
        if len(chosen) >= max_new:
            break
    return chosen


def merge_into_patterns_md(
    patterns_path: Path,
    to_add: list[Cluster],
    *,
    dry_run: bool = False,
) -> tuple[str, int]:
    """Insert new bullets under matching Recurring findings categories.

    Returns (new_text, n_added).
    """
    text = patterns_path.read_text(encoding="utf-8")
    if not to_add:
        return text, 0

    by_cat: dict[str, list[Cluster]] = defaultdict(list)
    for cl in to_add:
        by_cat[cl.category].append(cl)

    lines = text.splitlines(keepends=True)
    # Find "## Recurring findings"
    recurring_idx = None
    for i, line in enumerate(lines):
        if line.strip() == "## Recurring findings":
            recurring_idx = i
            break
    if recurring_idx is None:
        raise SystemExit("erlang-harvest: patterns.md missing '## Recurring findings'")

    # Map category header line indices
    cat_headers: dict[str, int] = {}
    for i, line in enumerate(lines):
        m = re.match(r"^### (.+)\s*$", line)
        if m and i > recurring_idx:
            cat_headers[m.group(1).strip()] = i

    added = 0
    # Insert from bottom so indices stay valid
    for cat, clusters in sorted(by_cat.items(), key=lambda x: -cat_headers.get(x[0], 10**9)):
        bullets = []
        for cl in clusters:
            pr_note = f", {len(cl.prs)} PR(s)" if cl.prs else ""
            bullets.append(
                f"- {cl.principle} _(harvested, seen {cl.count}×{pr_note})_\n"
            )
            added += 1

        if cat in cat_headers:
            # Insert after last bullet in section (before next ### or ##)
            start = cat_headers[cat] + 1
            end = start
            while end < len(lines):
                s = lines[end].strip()
                if s.startswith("### ") or s.startswith("## "):
                    break
                end += 1
            # walk back over blank lines
            insert_at = end
            while insert_at > start and lines[insert_at - 1].strip() == "":
                insert_at -= 1
            for j, b in enumerate(bullets):
                lines.insert(insert_at + j, b)
            # ensure trailing blank before next section
            after = insert_at + len(bullets)
            if after < len(lines) and lines[after].strip().startswith("#"):
                lines.insert(after, "\n")
        else:
            # Create new category before "## False-positive" or at end of recurring
            insert_at = len(lines)
            for i, line in enumerate(lines):
                if line.strip().startswith("## False-positive"):
                    insert_at = i
                    break
            block = [f"\n### {cat}\n", "\n", *bullets, "\n"]
            for j, b in enumerate(block):
                lines.insert(insert_at + j, b)

    new_text = "".join(lines)
    # Normalize: avoid triple blank lines
    new_text = re.sub(r"\n{4,}", "\n\n\n", new_text)
    if not dry_run:
        patterns_path.write_text(new_text, encoding="utf-8", newline="\n")
    return new_text, added


# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------


def render_candidates_report(
    *,
    repo: str,
    clusters: list[Cluster],
    applied: list[Cluster],
    authors: list[str],
    min_count: int,
    min_prs: int,
    comment_total: int,
    kept_total: int,
    patterns_rel: str,
) -> str:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    lines = [
        f"# Erlang pattern harvest candidates",
        "",
        f"**Generated:** {now}  ",
        f"**Repo:** `{repo}`  ",
        f"**Authors:** {', '.join(f'`{a}`' for a in authors)}  ",
        f"**Review comments scanned:** {comment_total}  ",
        f"**Top-level comments kept:** {kept_total}  ",
        f"**Clusters:** {len(clusters)}  ",
        f"**Promotion threshold (multi-PR):** count ≥ {min_count} **and** "
        f"distinct PRs ≥ {min_prs} (use `--promote-critical` for single-PR CRITICAL gates)  ",
        f"**Patterns file:** `{patterns_rel}`  ",
        "",
        "## Auto-apply selection",
        "",
    ]
    if applied:
        lines.append(f"Selected **{len(applied)}** theme(s) for merge into patterns:")
        lines.append("")
        for cl in applied:
            lines.append(
                f"- **[{cl.category}]** {cl.principle}  \n"
                f"  seen {cl.count}× · PRs {sorted(cl.prs)[:12]} · "
                f"severity={cl.dominant_severity()}"
                + (" · hard-gate-hint" if cl.hard_gate_hint else "")
            )
        lines.append("")
    else:
        lines.append("_No new themes met the promotion threshold (or all were duplicates)._")
        lines.append("")

    lines.extend(
        [
            "## All clusters (by frequency)",
            "",
            "| Count | PRs | Sev | Category | Principle |",
            "|------:|----:|-----|----------|-----------|",
        ]
    )
    for cl in clusters[:80]:
        sev = cl.dominant_severity()
        prin = cl.principle.replace("|", "\\|")
        if len(prin) > 120:
            prin = prin[:117] + "…"
        lines.append(
            f"| {cl.count} | {len(cl.prs)} | {sev} | {cl.category} | {prin} |"
        )
    if len(clusters) > 80:
        lines.append("")
        lines.append(f"_…and {len(clusters) - 80} more clusters omitted._")

    lines.extend(
        [
            "",
            "## Sample evidence (top clusters)",
            "",
        ]
    )
    for cl in clusters[:15]:
        lines.append(f"### {cl.principle}")
        lines.append("")
        lines.append(
            f"- category: **{cl.category}** · count: **{cl.count}** · "
            f"PRs: {sorted(cl.prs)[:20] or 'n/a'} · severity: {cl.dominant_severity()}"
        )
        if cl.sample_paths:
            lines.append(f"- paths: {', '.join(f'`{p}`' for p in cl.sample_paths[:5])}")
        for u in cl.sample_urls[:3]:
            lines.append(f"- {u}")
        lines.append("")

    lines.extend(
        [
            "## How to promote",
            "",
            "```text",
            "python3 scripts/erlang-harvest-review-patterns.py --apply",
            "```",
            "",
            "Review the diff to `patterns.md` before committing. Harvested bullets are",
            "marked `_(harvested, seen N×)_` so humans can later rewrite them to cleaner",
            "principles and drop the marker.",
            "",
        ]
    )
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description=(
            "Harvest GitHub PR review comments (Kilo et al.) into Erlang "
            "review pattern candidates and optionally auto-merge into patterns.md."
        )
    )
    p.add_argument(
        "--repo",
        default=None,
        help="owner/name (default: detect via gh / GH_REPO)",
    )
    p.add_argument(
        "--authors",
        default=",".join(DEFAULT_AUTHORS),
        help="Comma-separated GitHub logins to include (default: kilo-code-bot)",
    )
    p.add_argument(
        "--include-security-bots",
        action="store_true",
        help="Also include CodeQL / Copilot reviewer logins",
    )
    p.add_argument(
        "--include-humans",
        action="store_true",
        help="Include all human top-level review comments (noisier; raises min-count)",
    )
    p.add_argument(
        "--min-count",
        type=int,
        default=2,
        help="Minimum comment hits to promote a cluster (default: 2)",
    )
    p.add_argument(
        "--min-prs",
        type=int,
        default=2,
        help="Or promote if seen on this many distinct PRs (default: 2)",
    )
    p.add_argument(
        "--max-new",
        type=int,
        default=15,
        help="Max new bullets to merge into patterns.md on --apply (default: 15)",
    )
    p.add_argument(
        "--apply",
        action="store_true",
        help="Merge selected clusters into skills/erlang-review/patterns.md",
    )
    p.add_argument(
        "--promote-critical",
        action="store_true",
        help=(
            "With --apply, also merge single-PR CRITICAL hard-gate themes "
            "(noisier; default is multi-PR only)"
        ),
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="With --apply, compute merge but do not write patterns.md",
    )
    p.add_argument(
        "--max-pages",
        type=int,
        default=50,
        help="Max API pages of 100 comments (default: 50)",
    )
    p.add_argument(
        "--output",
        default=None,
        help="Candidate report path (default: docs/ai-generated/code-reviews/harvest-candidates-YYYY-MM-DD.md)",
    )
    p.add_argument(
        "--fixture",
        default=None,
        help="Load comments from a JSON file instead of calling gh (for tests)",
    )
    return p


def main(argv: Optional[list[str]] = None) -> int:
    args = build_arg_parser().parse_args(argv)
    root = find_repo_root()
    os.chdir(root)

    patterns_rel = (
        "modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md"
    )
    patterns_path = root / patterns_rel
    if not patterns_path.is_file():
        raise SystemExit(f"erlang-harvest: missing patterns file: {patterns_rel}")

    authors = [a.strip() for a in args.authors.split(",") if a.strip()]
    if args.include_security_bots:
        authors.extend(SECURITY_BOT_AUTHORS)
    # Dedup preserve order
    seen_a: set[str] = set()
    authors_u: list[str] = []
    for a in authors:
        if a.lower() not in seen_a:
            seen_a.add(a.lower())
            authors_u.append(a)
    authors = authors_u

    min_count = args.min_count
    min_prs = args.min_prs
    if args.include_humans:
        # Pull all non-bot? We only add humans if we expand authors via a second pass
        # For simplicity, when --include-humans, do not filter by author list except
        # we still skip empty — handled below with authors=None sentinel
        min_count = max(min_count, 3)
        min_prs = max(min_prs, 2)

    if args.fixture:
        fixture_path = Path(args.fixture)
        if not fixture_path.is_file():
            raise SystemExit(f"erlang-harvest: fixture not found: {fixture_path}")
        raw_items = json.loads(fixture_path.read_text(encoding="utf-8"))
        if not isinstance(raw_items, list):
            raise SystemExit("erlang-harvest: fixture must be a JSON array")
        repo = args.repo or "fixture/local"
    else:
        repo = detect_repo_slug(args.repo)
        print(f"erlang-harvest: fetching review comments from {repo} …", flush=True)
        raw_items = fetch_all_pull_comments(repo, max_pages=args.max_pages)
        print(f"erlang-harvest: fetched {len(raw_items)} comment(s)", flush=True)

    author_set = set(authors)
    if args.include_humans:
        # Include every top-level comment author (bots + humans)
        author_set = {
            ((it.get("user") or {}).get("login") or "")
            for it in raw_items
            if (it.get("user") or {}).get("login")
        }

    comments = parse_comments(raw_items, author_set)
    print(
        f"erlang-harvest: kept {len(comments)} top-level comment(s) "
        f"from {len(author_set)} author(s)",
        flush=True,
    )

    clusters = cluster_comments(comments)
    print(f"erlang-harvest: {len(clusters)} cluster(s)", flush=True)

    existing = parse_patterns_bullets(patterns_path.read_text(encoding="utf-8"))
    applied = select_for_apply(
        clusters,
        min_count=min_count,
        min_prs=min_prs,
        existing=set(existing),
        max_new=args.max_new,
        promote_critical=bool(args.promote_critical),
    )

    out_path = (
        Path(args.output)
        if args.output
        else root
        / "docs"
        / "ai-generated"
        / "code-reviews"
        / f"harvest-candidates-{date.today().isoformat()}.md"
    )
    if not out_path.is_absolute():
        out_path = root / out_path
    out_path.parent.mkdir(parents=True, exist_ok=True)

    report = render_candidates_report(
        repo=repo,
        clusters=clusters,
        applied=applied,
        authors=sorted(author_set) if args.include_humans else authors,
        min_count=min_count,
        min_prs=min_prs,
        comment_total=len(raw_items),
        kept_total=len(comments),
        patterns_rel=patterns_rel,
    )
    out_path.write_text(report, encoding="utf-8", newline="\n")
    # Always write with / in printed path. Resolve both sides first so 8.3
    # short paths (common on Windows under tempfile.TemporaryDirectory()) and
    # their long-form equivalents are treated as the same directory — without
    # this, Path.relative_to() raises ValueError on Windows when the test
    # chdir's into a short-path temp dir while find_repo_root() resolved cwd
    # to the long-path form.
    rel_out = out_path.resolve().relative_to(root.resolve()).as_posix()
    print(f"erlang-harvest: wrote candidates → {rel_out}", flush=True)

    n_added = 0
    if args.apply:
        _, n_added = merge_into_patterns_md(
            patterns_path, applied, dry_run=args.dry_run
        )
        if args.dry_run:
            print(
                f"erlang-harvest: dry-run would add {n_added} bullet(s) to {patterns_rel}",
                flush=True,
            )
        else:
            print(
                f"erlang-harvest: applied {n_added} new bullet(s) → {patterns_rel}",
                flush=True,
            )
    else:
        print(
            "erlang-harvest: candidates only (pass --apply to merge into patterns.md)",
            flush=True,
        )

    print(
        f"erlang-harvest: done (clusters={len(clusters)}, selected={len(applied)}, "
        f"added={n_added})",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
