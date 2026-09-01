# Erlang review — issue 4086 SPA UI-05 display format create/delete

## Summary

Developer Display Formats catalog gains create (POST) and delete chrome matching
SearchesPanel. REST write already exists; this slice adds SPA + product-docs.
Adaptor reload/GET-by-name now rejects the By_Author bulk-load replay (#3269)
so POST 201 and catalog GET return the created name.

## Scope

Branch `feat/issue-4086-spa-display-format-create-delete` vs `origin/main`.
Modules: `WebUI`, `projects/sitemanage`, `modules/perc-qa-automation`,
`product-docs/8.2`. Cross-platform path review: no new filesystem path joins.

## Recommendation

approve-with-notes (delete persist residual)

## Gate

May commit/push: yes for the SPA create/catalog slice. Live H2 DELETE of a
newly created format still fails in `sys_DisplayFormats` update
(`Xml Document Expected, none supplied`). File a residual; do not claim
DELETE completeness.

## Issues

### bug — live DELETE of empty user formats

`DisplayFormatAdaptor.deleteDisplayFormat` → `deleteDisplayFormats` hits
`PSTransactionSet` XML datasource with no input document. Fallback
mark-for-deletion save returned 400 on H2. Residual required.

### suggestion — Playwright delete path

Surface-filtered spec proves create + catalog row + duplicate 409. Re-add
delete omit-row once residual persist is green.

## Tests

- WebUI Vitest: invalid name 400, duplicate 409, missing 404, non-Admin 403
- sitemanage: create reload rejects By_Author replay; findByKey catalog fallback
