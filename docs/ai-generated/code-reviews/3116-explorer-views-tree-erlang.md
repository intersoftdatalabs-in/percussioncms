# Erlang review — #3116 Explorer Views catalog tree

**Change class:** WebUI product Explorer screen (Views system category + run results)

**Companions checked:** SearchPanel / searchesApi execute (peer), ContentExplorerShell seams, EXPLORER_MSG i18n, product-docs `content-explorer.md`, Vitest grouping + shell + `renderA11yGate`, compact Playwright smoke (full V3 remains #3117).

## Findings

* No blocking bugs found.
* Paths: REST URL segments use `/` (correct for HTTP). No filesystem I/O.
* Custom URL views (Inbox) are listed but not executed — 400/unsupported message; Inbox runner is #3118.
* V1 execute is called as `POST /services/views/{idOrName}/execute` (contract from #3115 even if that PR is not merged yet).
* Matrix must stay Missing/Partial — this review does not claim Present.

## Tests

* `viewCatalog.test.ts` — parentCategory 1–4 grouping, unknown → Other
* `viewsApi.test.ts` — unwrap + POST execute
* `ViewsCatalogTree.test.tsx` + `ViewResultsPanel.test.tsx` + shell wiring + a11y gate
* Playwright smoke `explorer-views-catalog.spec.js` (chrome groups only)

## Cross-platform

N/A beyond URL encoding of view keys (`encodeURIComponent`).
