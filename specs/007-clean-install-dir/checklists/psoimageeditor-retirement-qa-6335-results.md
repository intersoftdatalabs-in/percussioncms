# QA results — Installer psoimageeditor legacy JS retirement (issue #6335)

**Slice:** QA for #6274, delivered by merged PR #6324 (parent audit #5627 / #6069).
**Cell:** `perc-matrix-cms-h2`, `TEST_CMS_URL=http://127.0.0.1:9993` (H2, `qa-up` RESULT:OK).
**Tip verified:** `e040e2039b` (`origin/main` at QA time; branch adds QA-evidence files only).
**Verdict: PASS** — legacy files gone from the payload, no page references the
retired scripts, zero failed subrequests / zero console errors on the served surface.

## Step results

| # | QA step (issue #6335) | Result |
|---|------------------------|--------|
| 1 | Confirm the installer payload no longer ships psoimageeditor legacy files (iUtil/iResizable 2006-era, jquery-form ~2012) | **PASS** — installed payload `PercToolkitExits/InstallDir/rx_resources/addins/psoimageeditor/js/` contains exactly `form.js`, `image_editor_general.js`, `psoimageeditor.js`; `iutil.js` / `iresizable.js` absent. `form.js` carries the npm `jquery-form@4.5.0` provenance banner (`version: 4.5.0`, no `jQuery.browser`). Guard suite `InstallerPayloadVendoredJsAuditTest` **8/8 green** on tip; full `perc-toolkit` module `Tests run: 257, Failures: 0, Errors: 0, Skipped: 16` (pre-existing skips). |
| 2 | Run the installer image-edit/crop path — confirm it works via the npm-managed replacement (or the documented removal path) | **PASS (documented removal path)** — the supported crop surface is the Jcrop-based `sys_ImageCroppingControl` (`rx_ImageCroppingControl.xsl` refs resolve to shipped Jcrop 0.9.12 / jquery-color 3.0.0 / `cropper_custom.js`, pinned by `imageCropControlRefsResolveToPayload`). The retired Interface crop-box code has **no live host page**: no JSP/XSL/Java/HTML in the repo or the installed product references the addin JS, and the wizard JSP views are not wired. The ported glue binds jQuery UI `resizable`/`draggable` + `ajaxSubmit` (npm `jquery-ui`, tracked in WebUI per #5660), pinned by `editorGlueBindsNpmTrackedApis`. |
| 3 | Confirm no page in the installer UI references the removed scripts (no 404s) | **PASS** — repo-wide + installed-product-wide grep for `iutil.js` / `iresizable.js` / `jQuery.iUtil` / `jQuery.iResize` / `.Resizable(` / `ResizeConstraint` finds only the intentional Interface→jQuery-UI mapping note (block comment, stripped by the guard before asserting) plus README/test/doc mentions; **zero runtime references**. Live: `/Rhythmyx/login` serves HTTP 200 with Playwright ledger `failedRequests=[]` (no 4xx, no `requestfailed`) and zero console errors; `server.log` has **0 ERROR** lines for the run. InstallDir statics are staged (not web-mounted) in this matrix cell, so the 404 dimension is proven at the reference level rather than per-URL. |

Out of scope (per issue): Jcrop/jquery-color migration (sibling #6273), non-installer surfaces.

## Out-of-scope observation (pre-existing, not caused by #6324)

The modern login SPA shell serves HTTP 200 with zero failed subrequests but never
mounts the login form in this cell (`perc-login-root` stays hidden, React makes no
API calls, no console errors). The stock `tests/login.spec.js` Admin-login case fails
identically on this unmodified tip, and #6324 touches only installer-payload JS, so
this is unrelated to the retirement. Recorded here for the WebUI/login area; no
residual filed (login coverage belongs to an existing login-surface owner, and the
failure is environmental to this cell, not a product regression attributable to #6324).

## Evidence

- C1: `cd modules/perc-toolkit && ../../mvnw clean install` → BUILD SUCCESS,
  `Tests run: 257, Failures: 0, Errors: 0, Skipped: 16` (pre-existing);
  `InstallerPayloadVendoredJsAuditTest` 8/8. Full log: `scratch/6335-toolkit-build.log`.
- Live cell: `python docker/scripts/perc-devctl.py qa-up` → RESULT:OK
  (`TEST_CMS_URL=http://127.0.0.1:9993`, H2 `perc-matrix-cms-h2`);
  `qa-health` → RESULT:OK (HTTP:200, healthy).
- Installed-payload inventory (`docker exec perc-matrix-cms-h2 ls …/psoimageeditor/js/`):
  `form.js image_editor_general.js psoimageeditor.js` (retired files absent).
- Container-wide retired-reference grep: single hit = intentional mapping comment in
  `image_editor_general.js`; zero runtime references.
- Playwright (temporary spec, deleted before commit; run kept in shell history):
  `tests/qa-6335-psoimageeditor-tmp.spec.js` → 1 passed,
  `QA-6335-LEDGER failedRequests=[]`, zero console errors.
- Proof screenshot (served login shell state; see observation above):
  `docs/ai-generated/proofs/issue-6335-qa-login.png`.

## Live proof

![Login surface served HTTP 200 with zero failed subrequests](docs/ai-generated/proofs/issue-6335-qa-login.png)

Login shell as served by the H2 QA cell (`TEST_CMS_URL=http://127.0.0.1:9993`).
Playwright ledger for this load: `failedRequests=[]`, zero console errors; no
request references the retired `iutil.js` / `iresizable.js` scripts.
