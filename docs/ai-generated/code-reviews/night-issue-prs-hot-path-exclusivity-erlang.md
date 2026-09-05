# Erlang review — night-issue-prs 2.0.3 hot-path exclusivity

**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-09-05  
**Recommendation:** **approve**  
**Gate:** May commit/push: **yes**  
**Human rule review:** explicit operator approval to commit `.grok/workflows/**` (session: “Lets get that committed on a pr for the next run”).

## Summary

Stops closely related overnight slices from opening parallel PRs against `main` (REST + SPA + Playwright supersets that go CONFLICTING on shared chrome: beans, `paths.ts`, `messages.ts`, `DeveloperShell`, `rest.md`, qa `package.json`).

Mechanics:

- Triage: vertical increments, at most one `implement` per parent per run
- Host: skip later same-parent `implement` items (`skipped_hot_path_busy`) without a Work agent
- Work prompt: absorb same-parent OPEN PR; skip different-parent thrash overlap
- README lockstep

Smoke: `workflow validate_only` with `script_path` `.grok/workflows/night-issue-prs.rhai` and `args={"max_issues":1}` passed (canned-host path; does not prove live `gh` or the new host-skip branch).

## Scope

- Uncommitted vs `origin/main` on `chore/night-issue-prs-hot-path-exclusivity`
- Files: `.grok/workflows/night-issue-prs.rhai`, `.grok/workflows/README.md`
- Prior reports: `docs/ai-generated/code-reviews/night-issue-prs-reconcile-erlang.md`, `night-issue-prs-efficiency-erlang.md`
- Memory patterns hit: agent rule files require human review (satisfied this session); missing Java tests N/A for workflow script; no product Maven surface
- Cross-platform path review: **N/A** (orchestration prompts + `gh`; example paths are repo-relative git paths, not OS joins)

## Recommendation

**approve** — 0 bugs. Safe to commit and open PR.

## Gate

| Check | Result |
|-------|--------|
| Bugs | none |
| Behavioral tests for new non-trivial product logic | N/A — Grok workflow script; no Java/Maven surface. Companion is `validate_only` smoke (passed). Host skip is a few lines of Rhai equality on issue numbers. |
| Non-portable path/file I/O | none |
| Change-class companions | README updated in lockstep; user-global `~/.grok/workflows/` copy is untracked (operator machine), not a repo companion |
| Human review of agent rules | yes |
| Product documentation | N/A — agent workflow, not operator/product surface |

## Issues

None at **bug**.

### SUGGESTION — host skip is this-run + `parent_issue` only

- **File:** `.grok/workflows/night-issue-prs.rhai` (Work loop `parents_with_pr`)
- **Description:** Host skip fires only after a this-run `pr_opened` and only if triage set `parent_issue` (or the later item number was already pushed). A previous-night OPEN PR on the same parent or omitted `parent_issue` still depends on the Work prompt (absorb / `skipped_hot_path_busy`). Agents have ignored git-hygiene prompts before (#4331/#4332/#4333).
- **Suggestion:** Accept for 2.0.3. If the next live night still fans out siblings, add a Preflight-owned map of parent → open PR and host-skip before spawning Work.

### SUGGESTION — different-parent thrash is prompt-only

- **File:** `.grok/workflows/night-issue-prs.rhai` (GIT HYGIENE step 2B)
- **Description:** File Explorer vs SY-05 vs snippet PRs still collide on `sitemanage-beans.xml` / `rest.md` across parents. Host does not inspect `gh pr` files. Cluster remains the repair path.
- **Suggestion:** Keep cluster. Do not absorb unrelated epics in Work (correct as written).

## Prior report / memory

Reconcile 2.0.2 review: same rule-file gate; same `validate_only` companion. Efficiency 2.0.0: skip-no-agent for triage `skip` is unchanged; this adds a second no-agent skip for same-parent extras.
