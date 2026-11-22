# #6859 — `/actions/find` uiContexts overlay latency: H2 evidence + MySQL/MSSQL runbook

Parent investigation: #6859. Implementation PR slice: H2 mock-backed
enabled-vs-bypassed comparison in `projects/sitemanage`
(`ActionMenuAdaptorTreeUiContextsTest#repeatedFindCachedVsBypassed_loadCountsAndLatency`).

## What #6853 changed (recap)

`ActionMenuAdaptor.overlayTreeUiContexts` (called on every `GET /actions/find`
via `findMenus`) bulk-loads per-mode uiContexts with one unlocked design-WS
`loadActions` call, then caches id→uiContexts snapshots for
`DEFAULT_UI_CONTEXTS_CACHE_TTL_MILLIS` (30s), cleared on every persist/delete
through the adaptor. #6853's H2 evidence: cold overlay ~152ms (one load),
warm overlay ~0ms (zero service calls).

## H2 method (agent-safe subset, this PR)

Model Explorer's selection-change burst pattern as N back-to-back overlays over
a real-sized catalog (40 roots x 10 children + 1 duplicate id per root = 480
nodes, `LinkedHashSet` dedup exercised) with a simulated 150ms design-WS load:

- **Enabled:** default-TTL adaptor, 5 repeats → expect exactly 1 `loadActions`.
- **Bypassed:** TTL=0 adaptor (the issue's suggested bypass knob), 5 repeats →
  expect exactly 5 `loadActions`. Each 150ms simulated load advances the wall
  clock past the zero TTL, so expiry is deterministic (no same-millisecond
  flake).

Reproduce:

```bash
cd projects/sitemanage
../../mvnw clean install -Dtest='ActionMenuAdaptorTreeUiContextsTest' \
  -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

Look for the `[uiContexts-cache-vs-bypass]` stdout line.

## H2 results (2026-11-22, this PR's build)

Full module: `Tests run: 4507, Failures: 0, Errors: 0, Skipped: 125` — BUILD SUCCESS.

| run | nodes | repeats | design loads | wall |
|-----|-------|---------|--------------|------|
| cache enabled (30s TTL) | 480 | 5 | 1 | ~153ms |
| cache bypassed (TTL=0) | 480 | 5 | 5 | ~756ms |

Read: each `/find` in a selection-change burst costs one design load without
the cache (~150ms mock load each) and ~zero with it. **These numbers prove call
elimination, not DB latency** — the 150ms load is a mock knob. Whether the
design-WS load dominates or is negligible on a real install database is exactly
what H2 cannot answer (see runbook).

## MySQL/MSSQL runbook (human env — residual issue)

Requires a host install with a real RDBMS; out of scope for the H2 agent cell.

1. Install CMS backed by MySQL, then MSSQL, with a real-sized action catalog
   (hundreds of actions; import or generate to match production scale).
2. Warm up, then time `GET /actions/find` (repeat ≥ 20x, e.g. `curl -w
   '%{time_total}\n'` with an authenticated session) on the current build
   (cache enabled).
3. Bypass comparison — either check out the pre-#6853 build (parent commit of
   the #6860 merge) or temporarily construct the adaptor with TTL=0 and
   rebuild; repeat the same timing loop.
4. Compare medians/p95 per database. Decision rule per the issue:
   - No material win on real databases → consider removing the cache (simpler
     code) or keeping it only for the Explorer selection-change burst pattern.
   - Material win → keep; consider making the TTL configurable if operators ask.
5. Record numbers back on #6859 (table above is the template) and close the
   residual.
