# Erlang review — #4215 Playwright CE Controls write H2

**Date:** 2026-09-03  
**Branch:** `feat/issue-4215-playwright-ce-controls-2`  
**Reviewer persona:** Erlang (independent of implementer)

## Scope

Re-land `tests/developer-ce-controls.spec.js` proving Admin user-control create, PUT, DELETE (204 + GET 404) and system `sys_EditBox` immutability (no save/delete chrome; REST 409). Stacks open #4214 SPA chrome. Does not treat closed PR #4218 as coverage.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | No bugs in REST matching, CSRF same-origin GET/PUT/DELETE, or native-dialog guard | — |
| none | Paths are URL/REST (`/Rhythmyx/services/cecontrols/...`); no OS filesystem I/O | — |
| none | Assertions not weakened vs closed #4218 (POST 2xx, PUT 2xx, DELETE 204, GET 404, PUT/DELETE 409) | — |
| none | Unit tests cover `ceControlPath` encoding, URL/method matching, unique names | — |

## Change-class companions

- Playwright surface spec (required path `developer-ce-controls.spec.js`)
- Helper + `node:test` unit file registered in `package.json` `test:unit`
- Stacked WebUI SPA from #4213/#4214 for C5 (not this slice's product chrome)
- Product-docs N/A (test-only; operator page already in sibling PRs)

## Hard gates

- Bugs: none
- Behavioral tests: unit + live Playwright
- Cross-platform paths: URL `/` only

**Verdict:** pass — eligible for commit after C1/C5 evidence.
