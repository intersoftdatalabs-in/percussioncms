# Erlang review — #3661 New Section landing name/title/template

**Branch:** `fix/issue-3661-new-section-landing-fields`  
**Base:** `origin/main`  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (Playwright + product-docs for WebUI screens); URL `/` vs filesystem paths

## Summary

Architecture Create section collected title, URL name, and template, then posted `pageName` as the folder URL (operators could not set a landing-page file name). This slice adds an explicit **landing page name** field (autofill from title, independent of URL name), maps dialog fields through `mapCreateSectionDialogToFields` onto existing `POST /section/create` (`CreateSiteSection`), keeps empty/invalid required fields client-side, and does not POST on Cancel. When the site template catalog is empty (typical H2 sample seed), the picker falls back to `GET /pagemanagement/template/summary/all/readonly`. No rest/sitemanage DTO change (`PSCreateSiteSection` already has `pageName` / `pageTitle` / `templateId`).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests: Vitest mapper + dialog submit/empty/cancel/autofill + shell payload; Playwright H2 no-skip covering POST body (`pageName`/`pageTitle`/`templateId`), HTTP 200, tree child, Cancel, empty required. Product-docs create-section procedure updated.

Cross-platform path checklist: **clean** — REST URLs use `/`; `isCreateSiteSectionRequest` matches URL path (not OS filesystem); no `"/" +` filesystem joins; helper unit tests use `http://127.0.0.1` URLs.

## Issues

None (blocking).

### Suggestions (non-blocking)

1. CM1 defaulted landing `pageName` to `index` + site default extension. This slice lets operators set the file name (autofill `{title}.html`) — closer to Home Create Page than CM1 `index.html`. Acceptable for the issue text; do not invent Widget XML.
2. `copyTemplates: true` is unchanged from the prior SPA mapping (CM1 sent `false`). Out of scope.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Dialog landing page name + template catalog | Present |
| `mapCreateSectionDialogToFields` + `buildCreateSiteSectionBody` | Present |
| Vitest payload + dialog + shell | Present |
| Playwright create-section no-skip (0 skip when tree GET 200) | Present |
| `product-docs/8.2/admin/architecture-navigation.md` | Present |
| rest/sitemanage DTO | N/A (already complete) |

## Tests / builds observed

- `WebUI`: standalone `mvnw.cmd clean install` BUILD SUCCESS; Vitest 389 files / 2979 passed; Java Tests run: 63, Failures: 0
- `modules/perc-qa-automation`: standalone `mvnw.cmd clean install` BUILD SUCCESS; helper unit 8 passed
- Playwright H2: `architecture-create-section-noskip.spec.js` 4 passed, 0 skipped (`TEST_CMS_URL=http://127.0.0.1:9993`)
- Create POST payload includes pageName/pageTitle/templateId and a real `//Sites/…` folder path (not the underscore site-list name). Sample rffNavTree type 315 is not registered on skip-image-build H2, so navon insert may still HTTP 500 after a valid payload (residual).
- console-clean=yes (spec filters known noise); server.log has pre-existing “No homepage for site: Corporate_Investments” plus PSNavException on type 315 — not a UI mapping defect.
