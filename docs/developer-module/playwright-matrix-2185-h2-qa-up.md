# Developer Playwright matrix vs H2 qa-up (#2185)

|     Field      |                                                                                                                           Value                                                                                                                            |
|----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Issue**      | [#2185](https://github.com/intersoftdatalabs-in/percussioncms/issues/2185) (slice 1 of [#2089](https://github.com/intersoftdatalabs-in/percussioncms/issues/2089); grandparent [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690)) |
| **Date (UTC)** | 2026-08-06                                                                                                                                                                                                                                                 |
| **Operator**   | Grok night-issue-prs (model grok-4.5)                                                                                                                                                                                                                      |
| **Purpose**    | Inventory-only matrix (green/red + flake bucket). No product mega-fix.                                                                                                                                                                                     |

## Environment (H2 qa-up)

|      Item      |                                                              Value                                                              |
|----------------|---------------------------------------------------------------------------------------------------------------------------------|
| Stack          | Existing `perc-matrix-cms-h2` cell (same stack as `perc-devctl.py qa-up`)                                                       |
| Image          | `percussion-matrix-cell:local`                                                                                                  |
| Host port      | `9993` → container `9992`                                                                                                       |
| **CMS URL**    | `http://127.0.0.1:9993`                                                                                                         |
| **Auth path**  | `GET/POST /Rhythmyx/login` (modern React `perc-login-root` form)                                                                |
| Admin username | `Admin` (password from container `var/config/generated/passwords` — not committed)                                              |
| Health         | `python docker/scripts/perc-devctl.py qa-health --url http://127.0.0.1:9993/Rhythmyx/login` → **RESULT:OK HTTP:200**            |
| Playwright cwd | `modules/perc-qa-automation/frontend`                                                                                           |
| Env            | `TEST_CMS_URL=http://127.0.0.1:9993` `TEST_DB_TYPE=h2` `TEST_PRODUCT=cms` `ADMIN_USERNAME=Admin` `ADMIN_PASSWORD=<from docker>` |
| Out of scope   | #2094 Spanish locale, #1695 Explorer encodePath, #1894 Dashboard Add Gadget i18n                                                |

## Command

```bash
# Worktree: night-issue-prs
python docker/scripts/perc-devctl.py qa-health --url http://127.0.0.1:9993/Rhythmyx/login

cd modules/perc-qa-automation/frontend
# PowerShell example (session env):
#   $env:TEST_CMS_URL = "http://127.0.0.1:9993"
#   $env:TEST_DB_TYPE = "h2"
#   $env:TEST_PRODUCT = "cms"
#   $env:ADMIN_USERNAME = "Admin"
#   $env:ADMIN_PASSWORD = "<from docker passwords file>"
npx playwright test \
  tests/developer-catalog-smoke.spec.js \
  tests/developer-template-source-viewer.spec.js \
  tests/golden-unattended-smoke.spec.js \
  tests/login.spec.js \
  --reporter=list
```

**Result:** 10 passed, 2 failed (12 total), ~28s, 1 worker chromium.

## Green / red matrix

|                 Spec file                  |                      Test                      |  Result   |                 Bucket                 |                                                                                                           Notes                                                                                                            |
|--------------------------------------------|------------------------------------------------|-----------|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `developer-catalog-smoke.spec.js`          | REST: GET `/services/slots` 2xx                | **GREEN** | —                                      | HTTP 200; Jackson-wrapped Slot list                                                                                                                                                                                        |
| `developer-catalog-smoke.spec.js`          | content-types: catalog loads without API error | **RED**   | **#2186 selector**                     | UI shows full labels/names (Archives, Blog Post, …). Spec asserts `[data-testid="developer-ct-row"]` count > 0; product `SimpleCatalogTable` emits **`developer-ct-row-${index}`** only. Not env/auth.                     |
| `developer-catalog-smoke.spec.js`          | keywords                                       | **GREEN** | —                                      | Panel/empty success surface                                                                                                                                                                                                |
| `developer-catalog-smoke.spec.js`          | locales                                        | **GREEN** | —                                      |                                                                                                                                                                                                                            |
| `developer-catalog-smoke.spec.js`          | slots                                          | **GREEN** | —                                      |                                                                                                                                                                                                                            |
| `developer-catalog-smoke.spec.js`          | shared-fields                                  | **GREEN** | —                                      |                                                                                                                                                                                                                            |
| `developer-catalog-smoke.spec.js`          | system-def                                     | **GREEN** | —                                      |                                                                                                                                                                                                                            |
| `developer-template-source-viewer.spec.js` | template detail source line numbers + copy     | **RED**   | **#2186 selector** + **#2187 product** | (1) Same indexed row testid mismatch (`developer-tpl-row` vs `developer-tpl-row-N`). (2) REST list returns only `templateId` — Label/Name render as "—" (empty DTO). Screenshot + live `GET /Rhythmyx/services/templates`. |
| `golden-unattended-smoke.spec.js`          | QA env resolves without host install           | **GREEN** | smoke                                  |                                                                                                                                                                                                                            |
| `golden-unattended-smoke.spec.js`          | Admin login + Content Explorer shell           | **GREEN** | **#2188 smoke**                        | Auth + SPA shell healthy                                                                                                                                                                                                   |
| `login.spec.js`                            | Admin login lands off login page               | **GREEN** | auth                                   | Confirms modern login path                                                                                                                                                                                                 |
| `login.spec.js`                            | BASE_URL auto-discovered                       | **GREEN** | env                                    | `TEST_CMS_URL` wins                                                                                                                                                                                                        |

## Flake classification summary

|               Class                | Count |                                  Specs / evidence                                  |
|------------------------------------|------:|------------------------------------------------------------------------------------|
| Env / auth                         |     0 | Login + golden green; `TEST_CMS_URL` resolved                                      |
| Selector / data-testid (test-only) |     2 | content-types row testid; templates row testid (`SimpleCatalogTable` index suffix) |
| Product (proven)                   |     1 | Templates list wire DTO: ids only, no name/label/description                       |

### Failure artifacts (local, gitignored)

|         Failure         |                                 Paths under `modules/perc-qa-automation/frontend/test-results/`                                 |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| content-types           | `developer-catalog-smoke-De-af1af-log-loads-without-API-error-chromium/` (`error-context.md`, `test-failed-1.png`, `trace.zip`) |
| templates source viewer | `developer-template-source--a4242-ne-numbers-and-copy-control-chromium/` (same trio)                                            |

### REST probes (same Admin Basic + `RX_USEBASICAUTH`)

|                URL                | HTTP |                                     Observation                                      |
|-----------------------------------|------|--------------------------------------------------------------------------------------|
| `/Rhythmyx/services/slots`        | 200  | Healthy catalog (Playwright REST test green)                                         |
| `/Rhythmyx/services/contenttypes` | 200  | Full `label`/`name`/`guid` payload (matches green UI rows)                           |
| `/Rhythmyx/services/templates`    | 200  | Body shape `{"TemplateSummaryList":[{"templateId":N},…]}` — **no** name/label fields |

## Residual buckets (existing children)

|       Slice        |   Issue   |                                                                                                                                       Residuals from this matrix                                                                                                                                       |
|--------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2 Test-only harden | **#2186** | Update Playwright selectors for indexed rows: `developer-ct-row-*`, `developer-tpl-row-*` (and any other SimpleCatalogTable catalogs using the same contract). Prefer `[data-testid^="developer-ct-row-"]` or `developer-ct-row-0`. No product behavior change required for content-types.             |
| 3 Proven product   | **#2187** | Templates list DTO incomplete: adaptor/REST returns only `templateId`. Fix REST/sitemanage list mapping so summary includes name/label (and SPA maps them). Optionally open a dedicated child of #2187 for this single bug. After fix, re-run template source viewer (still needs #2186 selector fix). |
| 4 Smoke gate       | **#2188** | Baseline for smoke already green: golden unattended + login. Include developer catalog smoke (post-#2186) and template source (post-#2186+#2187 or skip-with-BUG linking product residual).                                                                                                            |

## Explicit non-residuals (do not fold)

- 

# 2094 Spanish locale Playwright

- 

# 1695 Explorer encodePath

- 

# 1894 Dashboard Add Gadget i18n

## Acceptance checklist (#2185)

- [x] Live H2 qa-up used; CMS URL + auth path recorded
- [x] Matrix comment posted (this doc + issue comments on #2185 / #2089)
- [x] Residual buckets linked to #2186 / #2187 / #2188
- [x] No mega-PR / no product fix in this slice

> Co-Authored by Grok Build using grok-4.5 with agent main.

