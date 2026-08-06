# Erlang review: issue-2124 keywords live 2xx smoke

## Summary
Adds REST probes for GET /services/keywords and
GET /services/keywords?includeChoices=true (2xx + JSON Keyword wrapper /
embedded choices) to developer-catalog-smoke after live H2 qa-up confirmed
HTTP 200. No product/resource/adaptor code change — static wiring already
healthy after merged unit slice #2125 (PR #2125).

## Scope
- modules/perc-qa-automation/frontend/tests/developer-catalog-smoke.spec.js
- Base: origin/main (includes #2155 slots REST smoke + #2125 keywords unit)
- Live evidence: perc-devctl qa-up H2 → keywords 200 / includeChoices 200 JSON

## Recommendation
approve

## Gate
May commit/push: **yes**

## Cross-platform path review
N/A — no file I/O or path construction; uses BASE_URL + string URL path and
adminBasicAuthHeaders peer pattern from #2121 / bug-1622.

## Issues
None (bug/suggestion/nit).

## Behavioral coverage
- New Playwright REST tests assert 2xx + Keyword array/wrapper payload.
- includeChoices=true asserts non-empty choices on at least one keyword when
  catalog is non-empty.
- Live run against H2 qa-up (TEST_CMS_URL freeport).

## Memory patterns
No new durable pattern; reuses RX_USEBASICAUTH + Authorization Basic from
auth.js / slots residual #2121.
