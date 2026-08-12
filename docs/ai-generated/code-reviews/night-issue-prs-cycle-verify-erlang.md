# Erlang review — night-issue-prs cycle verify / QA bar / drop dry_run

**Date:** 2026-08-12  
**Scope:** uncommitted `.grok/workflows/night-issue-prs.rhai` + `.grok/workflows/README.md` vs `origin/main`  
**Change class:** agent-instruction / Grok workflow (not product Java)  
**Human rule review:** explicit — operator ordered commit of these workflow files.

## Summary

Nightshift workflow: (1) remove `dry_run`; (2) gate human QA assignment on a quality bar and fix L2 override leak; (3) add **Cycle verify** after Security so Maven/Playwright failures become next-cycle p1 leads instead of dumping unready work on human QA; (4) **Human QA phase after Cycle verify** — Work never assigns humans (Q8 = cycle verify did not fail the PR).

Smoke check (`workflow validate_only name=night-issue-prs args={"max_issues":1}`) passed.

## Recommendation

**approve**

## Gate

**May commit/push: yes** (human approved rule-file commit)

## Memory patterns hit

- Agent rule/instruction files require explicit human review — **satisfied by this session**
- Non-portable paths — prompts use `%USERPROFILE%`/`$HOME`, `mvnw.cmd` on Windows, no hardcoded `:9993`
- Secrets — cycle-verify prompt forbids logging `ADMIN_PASSWORD`

## Issues

None blocking.

### Suggestions (non-blocking)

- Cycle-verify lead-queue and residual create are **prompt-enforced**, not host-enforced. A future host helper (parse surefire/playwright JSON) would be stronger than asking the agent to file residuals honestly.
- Playwright default is golden + this-run surfaces, not `--allow-full`. That matches `perc-qa-automation` unattended policy; operators can set `cycle_verify_allow_full_playwright`.

## Cross-platform path checklist

- [x] No new `".../" +` filesystem path construction in product Java (N/A — Rhai prompts only)
- [x] Prompts tell agents to use `mvnw.cmd` on Windows and portable worktree home
- [x] `TEST_CMS_URL` from `qa-up`, not hardcoded `:9993`
- [x] N/A line-ending / case-sensitive FS assertions

## Tests / Maven

N/A — no Maven module sources changed. Workflow smoke check only.

## Product documentation

N/A — agent workflow, not operator/product help.
