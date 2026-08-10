# Developer Playwright smoke gate (#2188)

|        Field        |                                                                               Value                                                                                |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Issue**           | [#2188](https://github.com/intersoftdatalabs-in/percussioncms/issues/2188) (slice 4 of [#2089](https://github.com/intersoftdatalabs-in/percussioncms/issues/2089)) |
| **Grandparent**     | [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690)                                                                                         |
| **Module**          | `modules/perc-qa-automation`                                                                                                                                       |
| **Matrix baseline** | [#2185](https://github.com/intersoftdatalabs-in/percussioncms/issues/2185) H2 qa-up (10 green / 2 red)                                                             |

## Purpose

Acceptance gate for epic **#2089**: **Developer entry** and **critical catalogs** are either:

1. **GREEN** under `@smoke` on H2 `perc-devctl` qa-up, or
2. **Explicitly skipped** with a durable `BUG:` note + issue URL (no silent flakes).

Out of scope: full regression green; #2094 / #1695 / #1894.

## Smoke inventory

Canonical code list:  
`modules/perc-qa-automation/frontend/tests/helpers/developer-smoke-set.js`  
(`DEVELOPER_SMOKE_SET`). Unit tests enforce that every `skip` row has a durable issue URL.

|           Id           |                    Spec                    | Expected |                                                                     Residual                                                                     |
|------------------------|--------------------------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| golden-qa-env          | `golden-unattended-smoke.spec.js`          | green    | —                                                                                                                                                |
| golden-login-explorer  | `golden-unattended-smoke.spec.js`          | green    | —                                                                                                                                                |
| login-admin            | `login.spec.js`                            | green    | —                                                                                                                                                |
| login-base-url         | `login.spec.js`                            | green    | —                                                                                                                                                |
| rest-slots             | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| catalog-content-types  | `developer-catalog-smoke.spec.js`          | **skip** | [#2186](https://github.com/intersoftdatalabs-in/percussioncms/issues/2186) indexed row selectors                                                 |
| catalog-keywords       | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| catalog-locales        | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| catalog-slots          | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| catalog-shared-fields  | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| catalog-system-def     | `developer-catalog-smoke.spec.js`          | green    | —                                                                                                                                                |
| template-source-viewer | `developer-template-source-viewer.spec.js` | **skip** | [#2189](https://github.com/intersoftdatalabs-in/percussioncms/issues/2189) TemplateSummary name/label (also #2186 selectors until harden merges) |

**Counts (gate definition):** 10 green expectations + 2 skip-with-BUG (matches #2185 matrix classification).

## How to run

From `modules/perc-qa-automation/frontend` after H2 qa-up:

```bash
# Prefer TEST_CMS_URL from perc-devctl qa-up (do not hardcode :9993 — freeport #2005/#2014)
export TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT}
export ADMIN_USERNAME=Admin
export ADMIN_PASSWORD=<from-qa-up-or-docker-exec>

# Developer smoke surface (paths + inventory tags)
npm run test:developer-smoke

# Or tag filter (includes any other @smoke in suite)
npm run test:surface -- --tag smoke

# List only (no live CMS)
npm run test:developer-smoke:list
```

Windows PowerShell example (use freeport from qa-up — do not hardcode `:9993`; freeport #2005/#2014):

```powershell
# Prefer TEST_CMS_URL from perc-devctl qa-up (same as bash example above)
$env:TEST_CMS_URL = "http://127.0.0.1:$env:QA_CMS_HOST_PORT"
$env:ADMIN_USERNAME = "Admin"
$env:ADMIN_PASSWORD = "<from-qa-up-or-docker-exec>"
cd modules/perc-qa-automation/frontend
npm run test:developer-smoke
```

## Flip skip → green when residuals land

| Residual |                                When to unskip                                 |                                                  Spec action                                                   |
|----------|-------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| #2186    | Selector harden merged (indexed `developer-ct-row-N` / `developer-tpl-row-N`) | Remove `test.skip` for content-types; set inventory status `green`                                             |
| #2189    | TemplateSummary list emits name/label                                         | Remove `test.skip` for template source viewer; set inventory status `green` (requires #2186 selectors as well) |

Keep inventory helper + this doc + `test.skip` reasons in lockstep.

## Related

- Matrix report: issue #2185 / PR inventory doc when present
- Failure artifacts: [playwright-failure-artifacts.md](./playwright-failure-artifacts.md)
- Surface filter: module README → Surface filter
- Golden unattended: `npm run test:golden`

