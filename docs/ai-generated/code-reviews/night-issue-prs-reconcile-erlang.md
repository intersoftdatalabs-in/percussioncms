# Erlang review — night-issue-prs 2.0.2 reconcile / close leftovers

**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-18  
**Recommendation:** **approve**  
**Gate:** May commit/push: **yes**  
**Human rule review:** explicit operator approval to commit `.grok/workflows/**` (session: “yes please commit and create a pr for the workflow changes”).

## Summary

Adds a **Reconcile** phase that closes open issues whose implementation already landed, and stops Triage from treating a **merged** PR comment as coverage. That matches the observed failure: 112 open issues, Work skip-no-agent, empty product queue.

Smoke: `workflow validate_only name=night-issue-prs args={"max_issues":1,"include_reconcile":true}` passed (canned-host path). Live close/implement behavior is prompt-driven; not fully proven by the smoke check.

## Scope

- Uncommitted vs `origin/main` on `chore/night-issue-prs-reconcile`
- Files: `.grok/workflows/night-issue-prs.rhai`, `.grok/workflows/README.md`
- Prior reports: `docs/ai-generated/code-reviews/night-issue-prs-efficiency-erlang.md`, `night-issue-prs-cycle-verify-erlang.md`
- Memory patterns hit: agent rule files require human review (satisfied); no Java I/O; no Maven module
- Cross-platform path review: **N/A** (orchestration prompts + `gh`; no filesystem path joins)

## Recommendation

**approve** — 0 bugs. Safe to commit and open PR after human rule approval (already given).

## Gate

| Check | Result |
|-------|--------|
| Bugs | none |
| Behavioral tests for new non-trivial product logic | N/A — Grok workflow script; no Java/Maven surface. Companion is `validate_only` smoke (passed). |
| Non-portable path/file I/O | none |
| Change-class companions | README args/lifecycle/phase list updated in lockstep; user-global `~/.grok/workflows/` copy is untracked (operator machine), not a repo companion |
| Human review of agent rules | yes |

## Issues

None at **bug**.

### SUGGESTION — close cap is honor-system

- **File:** `.grok/workflows/night-issue-prs.rhai` (Reconcile host after `agent()`)
- **Description:** `max_reconcile_closes` is enforced in the prompt and then the host only **rewrites the reported count** if the agent returns a higher `issues_closed`. Extra closes cannot be undone. Same pattern as stale In Progress `cleared_cap`. Acceptable if operators treat 20 as a soft nightly budget.
- **Suggestion:** Keep as-is unless a live run over-closes; then add a host-side “stop listing more close targets” in the prompt (already present) rather than fake un-close.

### NIT — agent-use table still says v2.0.0

- **File:** `.grok/workflows/README.md` (Rough agent use table intro)
- **Description:** Table intro still says “v2.0.0” while the file is 2.0.2. Reconcile row was added.
- **Suggestion:** Bump the intro label on a docs-only follow-up if it bothers anyone.

## Prior report / memory

Efficiency review (v2.0.0) required human rule gate before commit — still applies; this session has that approval. Cycle-verify review is unrelated to close semantics.
