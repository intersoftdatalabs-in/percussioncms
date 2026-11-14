# QA results — issue #6207 (QA for merged #6191, parent #6139)

Merged PR under test: #6191 — `fix(webui): retire AA workflow dialog JSPs
to SPA editor` (MERGED 2026-11-12).
Verification run on tip of `main` (branch
`fix/issue-6207-workflow-jsp-redirect-qa`) against a live H2 QA cell
(`perc-devctl qa-up --skip-image-build --then-qa-deploy-webui`, healthy).

**Result: PASS** — all three numbered test-plan steps verified live.

## Test plan results (from #6207)

### 1. Editor → workflow transitions (comment enforcement + ad-hoc assignees) — PASS (live)

- `npm run test:surface -- --path tests/us4-item-transitions.spec.js`
  against `TEST_CMS_URL=http://127.0.0.1:9993` → **3 passed**:
  - edit mode shows state and triggers, enforces comments, then transitions
  - ad-hoc assignee can be added to and removed from the transition
  - view mode stays read-only and issues no transition requests
- Exercises the real React `EditorWorkflowPanel` (the mounted successor of
  the removed `workflowactions.jsp`), with workflow REST routes mocked per
  the spec's editor-host harness; zero legacy content-explorer requests.

### 2. Retired JSP URLs 301 to the editor post-auth — PASS (live)

- Post-auth `GET` (Admin login, redirect-following disabled) of each retired
  pane → **301** with `Location: /cm/app/?view=editor`:
  - `/Rhythmyx/ui/activeassembly/workflow/workflowactions.jsp` → 301
  - `/Rhythmyx/ui/activeassembly/workflow/adhocsearch.jsp` → 301
  - `/Rhythmyx/ui/activeassembly/workflow/adhocresults.jsp` → 301
- Run via a temporary Playwright probe (3/3 passed, then removed from the
  tree; output retained in the run log). Unauthenticated `curl` of the same
  URLs → 302 to `/login` first (auth still enforced), so the 301 is the
  post-auth behavior the plan asks for.
- Static backing on this tip: the three JSP files are absent (deleted by
  #6191); `WEB-INF/web.xml` carries the exact
  `PSRetiredWorkflowDialogsRedirect` servlet mappings (7 references) plus
  the belt-and-braces `PSWebUiSpaFallbackFilter` /
  `PSEditorHostRedirect.isRetiredWorkflowDialogJsp` coverage, all present.

### 3. No legacy content-explorer requests during transitions — PASS (live)

- Covered inside the step-1 run: `us4-item-transitions.spec.js` asserts zero
  requests whose URL contains `contentexplorer`, `workflowactions.jsp`,
  `adhocsearch.jsp`, or `adhocresults.jsp` during the transition flows —
  green as part of the 3/3 above.
- Zero JS console errors during the surface run; `server.log` shows
  0 ERROR/FATAL lines after the runs.

## Supporting unit evidence (re-run on this tip)

- `cd WebUI && ../mvnw test-compile surefire:test
  -Dtest='PSEditorHostRedirectTest,PSWebUiSpaFallbackFilterTest'` →
  BUILD SUCCESS, **Tests run: 31, Failures: 0, Errors: 0**.
- `npm run test -- ../../test/ts/app/spaCutover.test.ts` (Vitest) →
  **22 passed** (includes the SC-002 retired-dialog mapping assertions
  added by #6191).

## Commands run

- `python docker/scripts/perc-devctl.py qa-up --skip-image-build --then-qa-deploy-webui` → RESULT:OK (`qa-up`, `qa-deploy-webui`, `qa-deploy-war-jars`); `qa-health` → healthy, HTTP 200.
- `TEST_CMS_URL=http://127.0.0.1:9993 ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up> npm run test:surface -- --path tests/us4-item-transitions.spec.js` → 3 passed.
- Temporary `zz-6207-retired-jsp-301.spec.js` probe (login + `maxRedirects: 0` GET × 3) → 3 passed, all 301 to `/cm/app/?view=editor`; probe file removed after the run.
- `curl` (unauthenticated) retired JSP URL → 302 to `/login` (auth enforced pre-login, as expected).
- `docker exec perc-matrix-cms-h2 grep -c ERROR/FATAL .../logs/server.log` → 0.

No production code, REST, WebUI, or Java sources changed **in this QA
verification run** — QA evidence only. (The product changes under test were
shipped by already-merged #6191, which also shipped the `product-docs`
admin note; no `product-docs/` update in this run.)
