---
name: erlang-review
description: >-
  Strict pre-commit / pre-PR code review for Percussion CMS (Erlang persona).
  Use when the user says "Erlang", "review my changes", "pre-commit review",
  "pre-PR review", "strict review", or before git commit / gh pr create.
  Independent review only; blocks on bugs, missing behavioral tests, and
  non-portable (Windows/Unix) file path handling.
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
2. **Determine scope** (ask only if unclear):
   - default: uncommitted + unstaged vs `HEAD`, plus commits not in `origin/development`
   - or: explicit PR number / branch
3. **Collect diff** (see agent file for commands).
4. **Read context** for changed symbols; apply module `AGENTS.md` when present.
5. **Produce** the required report (Summary, Recommendation, Gate, Issues).
6. **Write** optional durable copy under `tmp/reviews/` (repo temp, not OS temp).
7. **Gate language (strict)**:
   - If any **bug** (including missing behavioral tests for non-trivial logic,
     or non-portable path/file I/O that breaks Windows or Unix):
     `request-changes` and **May commit/push: no**
   - Else if only nits/low suggestions: `approve` allowed
8. When the diff touches file I/O, paths, installers, packaging, or path
   assertions in tests: apply Erlang's **cross-platform path checklist** and
   state the outcome in the report (even if clean).
9. **Do not implement** fixes unless the user asks after the report.

## Kilo-first invocation

In Kilo Code:

- Slash workflow: `/erlang-review` (see `.kilocode/workflows/erlang-review.md`)
- Or: switch to / paste the Erlang agent and say "Review my uncommitted changes"
- Project rule `.kilocode/rules/pre-commit-review.md` reminds agents to run this
  before commit when acting as implementer

## Related instructions

- Root `AGENTS.md` — **Cross-Platform File I/O & Paths**
- `instructions/code-review-generic.instructions.md`
- `instructions/security-and-owasp.instructions.md`
- `instructions/java-coding-standards.md` (prefer `java.nio.file.Files` / `Path`)
