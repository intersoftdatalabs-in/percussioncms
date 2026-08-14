# Erlang review — night-issue-prs efficiency (v2.0.0 skip guards)

**Date:** 2026-08-13  
**Reviewer:** Erlang Shen (independent; did not author this change)  
**Scope:** uncommitted `.grok/workflows/night-issue-prs.rhai` + `.grok/workflows/README.md` on `chore/night-issue-prs-efficiency` vs `origin/main`  
**Change class:** Grok Build Rhai workflow / agent orchestration (not product Java)  
**Human rule review:** these files are agent-instruction workflows. Root `AGENTS.md` **Human review of agent rules** still applies — even a clean Erlang pass does not authorize committing them without explicit human approval.

## Summary

v2.0.0 short-circuits empty specialist phases from one Preflight scout. The first pass blocked on fail-closed zeros, Cycle verify ignoring Security PRs, and cluster dropping the 2-CONFLICTING exception. Re-review of the working-tree fix pack: missing skip counts are `-1` (`as_count`), `signals_ok` requires `signals_complete=true`, specialists run unless that count is a known 0, C6 omits alert count on 403/404, `cluster_recommended` fail-opens when missing, Cycle verify includes Security mitigation URLs, and empty inventory no longer skips Triage when `issue_numbers` is set. No remaining blocking bugs.

`validate_only` was claimed passed by the author; this re-review did not re-execute it (no workflow tool in-session).

## Scope

- Base: `origin/main`
- Head: uncommitted working tree on `chore/night-issue-prs-efficiency`
- Files: 2 (`.grok/workflows/night-issue-prs.rhai`, `.grok/workflows/README.md`)
- Prior report: this file (first pass 2026-08-13); topic continuity `docs/ai-generated/code-reviews/night-issue-prs-cycle-verify-erlang.md`
- Memory patterns hit: workflow.skip-on-untrusted-zeros (fixed this pack); tests.structural-only (`validate_only` still one canned path); agent rule files need human review; cross-platform path checklist applied (clean)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes** (Erlang correctness). Do **not** commit `.grok/workflows/**` until the human explicitly approves this rule/workflow diff.

## Issues

### Issue 1 -- Severity: bug
- File: `.grok/workflows/night-issue-prs.rhai:811`
- Description: Host treated any schema-valid scout as complete and skipped specialists on `as_int` zeros, including C6 `open_alert_count=0` on CodeQL 403/404.
- Suggestion: `signals_complete` attestation; missing counts unknown; C6 omit count.
- Status: fixed

### Issue 2 -- Severity: bug
- File: `.grok/workflows/night-issue-prs.rhai:2234`
- Description: Cycle verify ignored Security mitigation PRs.
- Suggestion: Treat nonempty `mitigation_pr_urls` as `need_cycle_verify`.
- Status: fixed

### Issue 3 -- Severity: bug
- File: `.grok/workflows/night-issue-prs.rhai:1823`
- Description: Cluster host skip dropped the documented `>=2` CONFLICTING exception.
- Suggestion: `cluster_recommended` flag; missing flag fail-open.
- Status: fixed

### Issue 4 -- Severity: suggestion
- File: `.grok/workflows/night-issue-prs.rhai:934`
- Description: Empty inventory skipped Triage even for `issue_numbers`; list command could be crowded by assigned issues.
- Suggestion: `no:assignee` search; do not skip Triage when `issue_numbers` is set.
- Status: fixed

### Issue 5 -- Severity: suggestion
- File: `.grok/workflows/night-issue-prs.rhai:1310`
- Description: `continue` on skip is valid; skip-prompt arms remain dead (`1361`, `1561`). README now states skip rows do not update parent progress.
- Suggestion: Delete unreachable skip-prompt arms when convenient.
- Status: fixed (documented); residual dead arms are a nit

### Issue 6 -- Severity: suggestion
- File: `.grok/workflows/README.md:357`
- Description: `validate_only` does not prove skip truth tables.
- Suggestion: Fail-open predicates + README skip matrix + re-run smoke.
- Status: fixed (matrix documented; smoke claimed by author, not re-run here)

## Cross-platform path checklist

- [x] No new `".../" +` / `"...\\" +` filesystem joins in host Rhai
- [x] Worktree default is `join(home, worktree_rel)` with `%USERPROFILE%` / `$HOME`
- [x] Maven prompts still say `mvnw.cmd` on Windows
- [x] `TEST_CMS_URL` from `qa-up`; no hardcoded `:9993`
- [x] `/` in prompts is URL / repo-relative / npm path
- [x] N/A line-ending / case-sensitive FS assertions

Cross-platform path review: no issues.

## Tests / Maven

N/A — no Maven module sources changed. Author reports `workflow validate_only name=night-issue-prs args={"max_issues": 1}` passed. Not re-run in this re-review session.

## Product documentation

N/A — agent workflow, not operator/product help.

## Re-review

**Date:** 2026-08-13 (same day, fix pack)

Read current host predicates and README skip matrix. Did not implement or commit.

| Guard | Predicate now | Verdict |
|-------|----------------|---------|
| Preflight parse | `signals_ok` only if `pfo.signals_complete == true` (`811`); counts via `as_count` → `-1` when omitted (`200`, `815`); `cluster_recommended` stays `true` unless scout sets `false` (`605`, `820`) | Fail-open. Matches Issue 1. |
| C6 | 403/404: omit `open_alert_count`, `signals_complete=false` (`790`) | Does not encode API failure as 0. |
| PRE | `!signals_ok \|\| blocker_pr_count != 0` (`867`) | Skip only on known 0. |
| Peer | `!signals_ok \|\| peer_eligible_count != 0` (`1147`) | Same. |
| Security | `!signals_ok \|\| open_alert_count != 0` (`2041`) | Same. |
| Cluster | `!signals_ok \|\| cluster_recommended \|\| estimate < 0 \|\| estimate >= min` (`1819`–`1823`); estimate does not add this-run PRs onto unknown owned (`-1`) | D3 exception is a preflight flag; missing flag runs cluster. |
| Cycle verify | `prs_opened_this_run > 0 \|\| cluster_opened \|\| sec_opened_prs` (`2230`–`2234`); `sec_opened_prs` from nonempty `mitigation_pr_urls` | Issue 2 fixed. |
| Human QA | `!signals_ok \|\| prs_with_approve_count != 0 \|\| peer_approved` (`2434`) | Unknown approve count fail-opens the QA agent; Q2 still required before assign. Correct. |
| Triage empty inventory | `inventory == "" && issue_numbers == ()` (`934`); unassigned list uses `no:assignee` (`758`) | Issue 4 fixed. |
| README | Skip matrix at line 357 | Matches host. Overview bullets 4/8/9/10 are slightly shorter than the matrix (nit only). |

**Remaining blocking bugs:** none.

**Residual nits (do not block):** dead `if disp == "skip"` prompt arms after `continue`; numbered README list still says “cluster if owned ≥ min” without naming `cluster_recommended`.

**Human rule gate unchanged:** wait for explicit human approval before committing `.grok/workflows/night-issue-prs.rhai` and `.grok/workflows/README.md`.

## Handoff

- Re-reviewed: Preflight parse, PRE/peer/cluster/security/CV/HQ skip guards, README skip matrix.
- Prior Issues 1–3 (bugs) and 4/6 (suggestions) are fixed. Issue 5 documented; dead prompt arms leftover.
- Recommendation: **approve**. May commit/push: **yes** (Erlang). Human must still approve the workflow/rule diff.
- Durable report: `docs/ai-generated/code-reviews/night-issue-prs-efficiency-erlang.md`
