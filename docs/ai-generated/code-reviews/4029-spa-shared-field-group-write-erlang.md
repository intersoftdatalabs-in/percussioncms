# Erlang review — #4029 CD-15 SPA shared-field group write

**Branch:** `feat/issue-4029-shared-field-group-write`  
**Date:** 2026-08-30  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Parent:** #1690

## Summary

Developer Shared Fields catalog/detail were GET-only. This slice wires
`POST`/`PUT`/`DELETE /services/sharedfields` (already shipped REST #3944) into
SPA chrome: create/save/delete a group, disabled Save until a REST-safe name,
409 duplicate and 404 missing in the editor, Jackson `SharedFieldGroupDetail`
wrap/unwrap, Playwright H2 surface, and product-docs 8.2.

Peer: CD-18 locale editor (`LocalesPanel` / `LocaleDetailPanel`). Out of scope:
field/control/choice SPA, system-def chrome, auto-translation.

Memory patterns hit: WebUI screen requires Playwright companion; product-docs
for operator chrome; Jackson WRAP_ROOT_VALUE POST/PUT wrap (locales #4005);
in-flight save guard; change-class closure (API + panels + Vitest + Playwright
+ product-docs).

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

Not applicable — no filesystem path/file I/O in the diff (REST client + React
chrome + Playwright selectors + Markdown docs). Playwright unique names are
alphanumeric; REST keys use `encodeURIComponent`.

## Issues

None blocking.

### nit

- `SF_FILENAME_HINT` embeds the `{name}.xml` token as a filename pattern, not
  a TMX placeholder. Acceptable; matches REST default copy.

## Tests

- Vitest: `sharedFieldsApi` name/filename validation, Jackson wrap/unwrap,
  POST/PUT/DELETE; `SharedFieldGroupDetailPanel` disabled-until-valid, 409
  duplicate, 404 missing, double-click create, save, delete confirm;
  `SharedFieldsPanel` New chrome + empty catalog still shows create.
- Playwright: `developer-shared-fields-editor.spec.js` **2 passed** on H2
  qa-up (`TEST_CMS_URL=http://127.0.0.1:9993`).
- Module: `cd WebUI && ../mvnw.cmd clean install` BUILD SUCCESS (Surefire
  Tests run: 63, Failures: 0; Vitest Tests 3271 passed). perc-qa-automation
  `npm run test:unit` 482 passed.

## C5

`python docker/scripts/perc-devctl.py qa-up` TEST_CMS_URL=http://127.0.0.1:9993
CONTAINER perc-matrix-cms-h2; `qa-health` RESULT:OK HTTP:200 HEALTH:healthy;
`qa-deploy-webui` RESULT:OK; `qa-health` again RESULT:OK HTTP:200 HEALTH:healthy;
`npm run test:surface -- --path tests/developer-shared-fields-editor.spec.js`
**2 passed**; console-clean=yes (spec guards); server.log-clean=yes (no
ERROR/FATAL in the test window).

## Change-class closure

WebUI write client + catalog/detail chrome + Vitest + Playwright +
`product-docs/8.2/admin/developer-shared-fields.md` (+ admin/developer indexes
and REST note). No rest/sitemanage API change (write already shipped).
