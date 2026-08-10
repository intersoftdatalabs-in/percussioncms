# Erlang review: issue-2118 catalog smoke re-probe matrix (slice D)

## Summary

Consolidates developer-catalog-smoke REST coverage for the nine #1694
catalog endpoints after live H2 qa-up re-probe confirmed HTTP 200 on all
cells. Reuses residual assertion patterns from #2121/#2124/#2140/#2142/

# 2146 (and peer C residuals) rather than duplicating product jaxrs work

(product registration remains #2162 and peers).

## Scope

- modules/perc-qa-automation/frontend/tests/developer-catalog-smoke.spec.js
- Base: origin/main (includes #2155 slots REST smoke + #2065 golden path)
- Live evidence: perc-matrix-cms-h2 `http://127.0.0.1:9993` — all nine → 200

## Recommendation

approve

## Gate

May commit/push: **yes**

## Cross-platform path review

N/A — no file I/O or path construction; uses BASE_URL + string URL path and
adminBasicAuthHeaders peer pattern from #2121 / residual smokes.

## Issues

None (bug/suggestion/nit).

## Behavioral coverage

- Table-driven REST 2xx + Jackson wrapper for slots, keywords, locales,
  searches, views, extensions/catalog, cecontrols, serverconfigs,
  relationshiptypes.
- keywords includeChoices=true retains #2161 choices embedding assert.
- SPA section smoke retained (#1690); content-types row locator updated to
  indexed `developer-ct-row-*` (CatalogTable contract) so H2 rows count.
- Empty wrappers (SearchDef:[], Extension:[]) accepted as valid 2xx.

## Memory patterns

No new durable pattern; consolidates RX_USEBASICAUTH + Authorization Basic
and Jackson wrapper unwrap from residual #2121 family PRs.
