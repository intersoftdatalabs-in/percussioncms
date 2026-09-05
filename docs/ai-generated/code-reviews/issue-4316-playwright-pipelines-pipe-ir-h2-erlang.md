# Erlang review — #4316 Playwright Pipelines pipe IR H2

**Scope:** `modules/perc-qa-automation` surface spec + small WebUI
`data-pipe-name` attribute for exact catalog open (Playwright companion).
Stacked on REST #4314 / SPA #4315.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | Read-only IR path; no lifecycle mutation | OK |
| none | Console/pageerror guards; IR GET asserted before chrome | OK |
| none | Portable paths: spa.jsp query URL only; no OS path joins | OK |
| note | Empty IR resources still pass (meta + empty state) — valid for apps without datasets | accepted |
| note | Product-docs N/A — SPA tip already documents Pipe IR operator path | OK |

## Change-class companions

| Artifact | Status |
|----------|--------|
| Playwright surface `developer-pipelines-pipe-ir.spec.js` | added |
| `data-pipe-name` on catalog open (stable selector) | added + Vitest assert |
| C5 H2 qa-up / qa-deploy / test:surface | required before PR |
| product-docs | N/A (sibling #4315) |

## C5 evidence (H2 QA)

- `perc-devctl qa-up` → `TEST_CMS_URL=http://127.0.0.1:9993`
- `qa-deploy-war-jars --restart-jetty` + `qa-deploy-webui`
- CXF: added `restPipelinesResource` to `sitemanage-beans.xml` (404 without ref); docker-cp into cell + Jetty restart
- `npm run test:surface -- --path tests/developer-pipelines-pipe-ir.spec.js` → **1 passed**
- console-clean=yes (spec guards); server.log-clean=yes (no Failed startup / feature ERROR after deploy)

## Verdict

Pass — C5 surface green on H2 QA.
