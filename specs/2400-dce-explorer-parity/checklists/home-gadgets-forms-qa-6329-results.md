# QA results — #6329 Fresh-H2 Home gadgets/activity + forms 500 fix

QA-executed first pass of issue #6329 against a fresh CMS-only H2 QA cell
(`perc-devctl qa-up`, no DTS, no published demo site; image built from
`origin/main` tip containing merged PR #6302). **Verdict: FAIL on step 2**
(forms REST still 500s) with one additional residual (traffic gadget 500s
for the real site path). Gadgets render with zero console/page errors;
activity stream itself is empty-200 clean.

> Parent: #6083 (CLOSED; T056a follow-on) / merged fix PR #6302.
> This record supplies QA evidence only — it does not close #6329; human
> sign-off still required. Residual follow-on: #6399
> (https://github.com/intersoftdatalabs-in/percussioncms/issues/6399).

## Cell

- `python docker/scripts/perc-devctl.py qa-up` → `RESULT:OK`
  (`TEST_CMS_URL=http://127.0.0.1:9993`, `ADMIN_USERNAME=Admin`)
- `python docker/scripts/perc-devctl.py qa-health` → `RESULT:OK HTTP:200
  HEALTH:healthy`
- Fresh-cell sites present with `pubInfo: null` (no DTS publish path):
  `Corporate_Investments`, `Enterprise_Investments`

## QA record

| # | QA step (issue #6329) | Result |
|---|------------------------|--------|
| 1 | Fresh H2 cell: log in, open Home — gadgets + activity stream render (empty-state payloads, no 500) | **CONDITIONAL FAIL** — tiles render (`home-gadgets-section`, `dashboard-root`, first `-widget` tile, TMX greeting, Error/empty chrome all visible), zero `pageerror`, zero non-resource console errors; BUT the live dashboard fires **2 × HTTP 500** (see REST table): `forms/Corporate_Investments` and `activity/contenttraffic` (real site path) |
| 2 | Open a forms-backed surface — forms REST returns empty payload, not 500 | **FAIL** — `GET /Rhythmyx/services/assetmanagement/asset/forms/Corporate_Investments` (the exact Form Tracker gadget call for the first site) → **HTTP 500** `Unable to connect to delivery server at: {}.https://localhost:8443.` (also 500s for any other site string, e.g. `forms/Demo`) |
| 3 | `server.log`: no new ERROR lines from gadget/activity/forms paths during the session | **FAIL** — 37 `ERROR` lines, all from these paths: `PSFormDataService:L:149` (forms connect failure) × ~12, `PSActivityService Caught error while querying content IDs by path: '/Sites/Corporate_Investments'` + `PSContentActivityService:L:127/153` (traffic) × ~4, `Analytics has not been setup yet` (effectiveness, by-design) × ~4, `PSDeliveryClient Fatal transport error` companions |

## REST probe (Basic-auth GET/POST, same calls as `WebUI/src/main/ts/api/dashboard/gadgetApi.ts`)

| Call | Status | Body |
|------|--------|------|
| `GET sitemanage/site` | 200 | 2 `SiteSummary` (`Corporate_Investments`, `Enterprise_Investments`, `pubInfo: null`) |
| `GET assetmanagement/asset/forms/Corporate_Investments` | **500** | `Unable to connect to delivery server at: {}.https://localhost:8443.` |
| `GET assetmanagement/asset/forms/Demo` | **500** | same delivery-connect failure |
| `POST activitymanagement/activity/contentactivity` `/Sites/` | 200 | `{ContentActivity: []}` — #6302 fix holds |
| `POST activitymanagement/activity/contentactivity` `/Sites/CorporateInvestments` | 200 | `{ContentActivity: [...]}`-shaped, no 500 — #6302 fix holds |
| `POST activitymanagement/activity/contenttraffic` `/Sites/` | 200 | `{ContentTraffic: {dates: [], visits: [], …}}` — empty, no 500 |
| `POST activitymanagement/activity/contenttraffic` `/Sites/Corporate_Investments` (dashboard's resolved default path) | **500** | `Server error processing request, see log for details.` + `PSActivityService Caught error while querying content IDs by path` in `server.log` |
| `POST activitymanagement/activity/effectiveness` `/Sites/` | 500 | `Analytics has not been setup yet.` — by-design (requires GA setup per `gadgetApi.ts`); not filed as defect |

## Browser pass

- `tests/home-gadgets-i18n.spec.js` → **1 passed** (gadget tiles render localized chrome, zero console/page errors; spec tolerates the two known cell 5xx via `KNOWN_CELL_5XX`, so it stays green while the REST defects above stand)
- `tests/golden-unattended-smoke.spec.js` → **2 passed**
- Throwaway dashboard probe (login → `spa.jsp?entry=home&section=gadgets`, 8 s settle): exactly the 2 BAD 500s above, 2 `Failed to load resource` console errors, zero `pageerror`. Proof shot: `docs/ai-generated/proofs/issue-6329-home-gadgets.png`

## Live proof

![Home gadgets dashboard on fresh H2 cell](docs/ai-generated/proofs/issue-6329-home-gadgets.png)

Dashboard renders (Welcome tile, gadget tiles with localized Error/empty
states) while the Form Tracker and Traffic gadget data calls 500 underneath.

## Analysis (for the residual)

1. **Forms 500 (step-2 FAIL):** #6302 made `findServer == null` return `[]`,
   but on the fresh cell `findByService(SERVICE_FORMS, null, null)` returns a
   default delivery record (`https://localhost:8443`) that is unreachable, so
   `getJsonObject` throws → catch wraps in `WebApplicationException` → 500
   (`PSFormDataService.java:149`). Narrowing the catch to connection failure
   would mask real DTS outages — needs a product decision, hence residual.
2. **Traffic 500 (real site path only):** `PSActivityService` fails
   `querying content IDs by path: '/Sites/Corporate_Investments'` (the
   dashboard resolves the default activity path from the site *name*,
   `/Sites/<name>`, which is not the folder path `//Sites/CorporateInvestments`);
   `/Sites/` stays 200-empty. Needs a product decision (empty vs 500 for
   unresolvable activity paths), hence residual.
3. **Effectiveness 500** (`Analytics has not been setup yet`) is by-design
   and out of scope.

Out of scope (per issue): DTS-connected environments; gadget content
correctness beyond empty-state rendering.
