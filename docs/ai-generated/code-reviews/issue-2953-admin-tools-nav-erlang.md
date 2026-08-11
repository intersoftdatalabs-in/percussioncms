# Erlang review — #2953 Admin tools nav

**Scope:** uncommitted + branch `fix/issue-2953-admin-tools-nav` vs `origin/main`  
**Modules:** WebUI (AdminShell title/i18n, sibling testids), perc-qa-automation (Playwright surface), product-docs/8.2/admin  
**Date:** 2026-08-11

## Summary

After #2784, consolidated top-nav **Admin** correctly lands on `/admin` (Admin tools shell) with an **Administration** sibling to `/workflow`. The shell **page title** still used `perc.ui.admin@Administration`, so operators saw “Administration” as both the H1 and the sibling label and reported the Admin tools affordance as missing (#2953).

Fix reuses the existing localized key `perc.ui.dashboard.modern@Admin tools` for `ADMIN_MSG.ADMIN_TITLE`, adds stable title testids, hardens Vitest sibling href/label assertions, extends Playwright bidirectional sibling navigation, and documents SPA Admin navigation in product-docs.

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral tests: Vitest AdminShell + WorkflowAdminShell sibling/title; topNavConfig landing already covered; Playwright surface extended
- Cross-platform paths: N/A (no filesystem path construction)
- Change-class companions: WebUI unit + Playwright + product-docs present
- May commit/push: **yes**

## Issues

None (blocking).

### Low / nits

- Issue body expected landing on Workflow Administration; product intent from #2784 is Admin tools landing — this PR follows #2784 and triage disposition. Human QA should confirm against product intent.
- Live Playwright against QA H2 not run in this agent session (`agent_safe_only`); Vitest green; surface spec updated for CI/QA mode.

## Build evidence

- `cd WebUI && ../mvnw.cmd clean install` → BUILD SUCCESS; Vitest 1789 passed; Surefire Tests run: 37
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` → BUILD SUCCESS
