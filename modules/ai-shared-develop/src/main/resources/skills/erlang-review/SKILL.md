---
name: erlang-review
description: >-
  Strict pre-commit / pre-PR code review for Percussion CMS (Erlang persona).
  Use when the user says "Erlang", "review my changes", "pre-commit review",
  "pre-PR review", "strict review", or before git commit / gh pr create.
  Independent review only; blocks on bugs, missing behavioral tests, and
  non-portable (Windows/Unix) file path handling. Loads review memory and
  writes durable reports under docs/ai-generated/code-reviews/.
---

# Erlang Review Skill

## Purpose

Run a **strict, independent** code review of local or branch changes **before**
commit or PR, so GitHub review cycles are shorter and fewer.

Canonical persona and full mandate:

`modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`

Cross-platform path rules (product): root `AGENTS.md` → **Cross-Platform File I/O & Paths**.

## When to activate

- User mentions Erlang / pre-commit / pre-PR / strict review
- User is about to commit, push, or open a PR and has not just completed a clean
  Erlang pass with recommendation `approve`
- After implementing review fixes (re-review required)

## Steps

1. **Load** `agents/erlang-code-review.md` (this module) and root `AGENTS.md`.
2. **Review memory (always, best-effort):**
   - Load `skills/erlang-review/patterns.md` (this directory) — institutional patterns.
   - If a prior report for this topic exists under
     `docs/ai-generated/code-reviews/`, load it (re-review continuity).
   - Do **not** treat `tmp/reviews/` as authoritative (`tmp/` is wipeable).
3. **Determine scope** (ask only if unclear):
   - default: uncommitted + unstaged vs `HEAD`, plus commits not in `origin/development`
   - or: explicit PR number / branch
4. **Collect diff** (see agent file). Use portable **git** commands (not
   bash-only `2>/dev/null || true`). Host shell may be PowerShell, cmd, Git Bash,
   or Unix. **`gh` is optional** — if missing, review via git and note in Scope.
5. **Read context** for changed symbols; apply module `AGENTS.md` when present.
6. **Produce** the required report (Summary, Recommendation, Gate, Issues).
   Note memory patterns that applied when useful (`Memory patterns hit: …`).
   Write **repo-relative paths with `/`** even on Windows hosts.
7. **Write durable report** under:

   ```text
   docs/ai-generated/code-reviews/<ticket-or-branch-slug>-erlang.md
   ```

   Create the directory if needed. See `docs/ai-generated/code-reviews/README.md`.
   - **Required** when gate is `request-changes` or this is a re-review.
   - **Recommended** on approve for feature work.
   - On re-review: update the same file (statuses + `## Re-review` section).
8. **Pattern memory touch (optional, careful):** if this review found **bugs** or
   high-signal suggestions that generalize beyond this task, append or reinforce
   a one-line principle in `patterns.md` (no file:line nits; keep the file short).
   Periodically refresh institutional memory from Kilo/GitHub history:

   ```text
   python3 scripts/erlang-harvest-review-patterns.py --apply
   ```

   (Windows: `scripts\erlang-harvest-review-patterns.bat --apply`.) Review the
   candidates report under `docs/ai-generated/code-reviews/` before relying on
   newly harvested bullets.
9. **Gate language (strict)**:
   - If any **bug** (including missing behavioral tests for non-trivial logic,
     or non-portable path/file I/O that breaks Windows or Unix):
     `request-changes` and **May commit/push: no**
   - Else if only nits/low suggestions: `approve` allowed
10. When the diff touches file I/O, paths, installers, packaging, or path
    assertions in tests: apply Erlang's **cross-platform path checklist** and
    state the outcome in the report (even if clean).
11. **Do not implement** fixes unless the user asks after the report.

## Kilo-first invocation

In Kilo Code:

- Slash workflow: `/erlang-review` (see `.kilocode/workflows/erlang-review.md`)
- Or: switch to / paste the Erlang agent and say "Review my uncommitted changes"
- Project rule `.kilocode/rules/pre-commit-review.md` reminds agents to run this
  before commit when acting as implementer

## Related instructions

- Root `AGENTS.md` — **Cross-Platform File I/O & Paths**
- `docs/ai-generated/code-reviews/README.md` — durable report store
- `skills/erlang-review/patterns.md` — review pattern memory
- `instructions/code-review-generic.instructions.md`
- `instructions/security-and-owasp.instructions.md`
- `instructions/java-coding-standards.md` (prefer `java.nio.file.Files` / `Path`)
