# Erlang code reviews (durable store)

This directory is the **store of record** for Percussion CMS pre-commit /
pre-PR reviews produced by **Erlang** (strict independent code review).

## Why here (not `tmp/`)

|                               Location                               |                                    Role                                    |
|----------------------------------------------------------------------|----------------------------------------------------------------------------|
| **`docs/ai-generated/code-reviews/`**                                | Durable review reports — survives `tmp/` wipes, clones, and clean builds   |
| **`modules/ai-shared-develop/.../skills/erlang-review/patterns.md`** | Institutional pattern memory (generalized hard gates / recurring findings) |
| **`tmp/`**                                                           | Repo temp only — **do not** store Erlang reviews there                     |

Root `AGENTS.md` defines `./tmp` as throwaway. Review memory and re-review
continuity must not depend on it.

## Naming

Prefer one canonical file per topic (update in place on re-review):

```text
docs/ai-generated/code-reviews/<ticket-or-branch-slug>-erlang.md
```

Examples:

- `984-installer-db-targets-erlang.md`
- `986-url-allowlist-config-erlang.md`
- `715-remove-redirect-management-gadget-erlang.md`

Optional date prefix when useful for archaeology:

```text
YYYY-MM-DD-<slug>-erlang.md
```

On re-review, **update the same file** (mark issues fixed, append a
`## Re-review` section) rather than only creating a separate `*-rereview.md`.
Separate rereview files from older runs may still exist for history.

## When to write

|                     Situation                     |                               Action                               |
|---------------------------------------------------|--------------------------------------------------------------------|
| Gate is `request-changes`                         | **Required** — write/update the topic file                         |
| Re-review after fixes                             | **Required** — load prior file, update statuses / append re-review |
| Gate is `approve` for a real feature branch       | **Recommended** — leave an audit trail                             |
| Trivial docs-only nit the author skips committing | Optional                                                           |

## What not to put here

- Secrets, passwords, tokens, private keys, or full proprietary config dumps
- Feature plans / FR / AC (those belong under `docs/ai-generated/tasks/…`)
- Generalized recurring patterns (promote those to
  `skills/erlang-review/patterns.md`, not as one-off prose here)

## Review memory load path (agents)

Before reviewing:

1. Load `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
2. If a prior report for this topic exists in this directory, load it
3. Do **not** treat `tmp/reviews/` as authoritative

Canonical persona:  
`modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`

## Commit policy

- Commit review files that accompany an active branch/PR workstream when useful
  for re-review or team audit.
- Redact secrets before writing.
- Optional archive later: move stale reports under `archive/` if the top level
  grows noisy.

## Cross-platform notes (Windows / Linux / macOS)

- Path strings in this tree and in reports are **repo-relative with `/`** — not
  OS filesystem joins. Agents on Windows must still write
  `docs/ai-generated/code-reviews/...` (not `docs\ai-generated\...`) in markdown.
- Collecting diffs uses **git** (and optionally **gh**). Do not require bash-only
  syntax. Git for Windows + PowerShell is a supported host.
- Pattern memory and product hard gates intentionally catch both Unix-only and
  Windows-only footguns in **product** code under review.

## Seeding patterns from GitHub / Kilo review history

Institutional patterns live in:

`modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`

**Automated harvest (preferred):**

```text
# From repo root — needs gh auth
python3 scripts/erlang-harvest-review-patterns.py              # candidates report
python3 scripts/erlang-harvest-review-patterns.py --apply      # multi-PR → patterns.md
scripts\erlang-harvest-review-patterns.bat --apply             # Windows
```

|      Output      |                               Path                                |
|------------------|-------------------------------------------------------------------|
| Candidate report | `docs/ai-generated/code-reviews/harvest-candidates-YYYY-MM-DD.md` |
| Pattern memory   | `…/skills/erlang-review/patterns.md` (only with `--apply`)        |

What the script does:

1. Fetches all PR **line review comments** (open + closed/merged) via `gh api`.
2. Keeps top-level comments from `kilo-code-bot[bot]` by default (skips replies / mitigations).
3. Generalizes bodies, clusters similar themes, categorizes.
4. Writes the candidate report with counts, PR lists, and sample links.
5. With `--apply`, merges **multi-PR** themes into `patterns.md` (deduped).
   Use `--promote-critical` only when you also want single-PR CRITICAL hard gates.

**Discipline still matters:** review the candidates file (and the patterns diff)
before committing. Rewrite `_(harvested, …)_` bullets into permanent plain
principles when they stabilize. Do not treat harvest as a substitute for a live
Erlang pass on the current diff.

Details / flags: `scripts/README.md` → `erlang-harvest-review-patterns`.
